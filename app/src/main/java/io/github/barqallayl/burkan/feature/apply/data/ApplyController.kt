package io.github.barqallayl.burkan.feature.apply.data

import arrow.core.flatten
import arrow.core.raise.either
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.Named
import dev.zacsweers.metro.SingleIn
import io.github.barqallayl.burkan.core.di.AppBindings
import io.github.barqallayl.burkan.core.model.AppError
import io.github.barqallayl.burkan.core.model.Renderer
import io.github.barqallayl.burkan.core.model.RendererStatus
import io.github.barqallayl.burkan.core.model.type
import io.github.barqallayl.burkan.core.shell.PackageName
import io.github.barqallayl.burkan.core.shell.RendererReader
import io.github.barqallayl.burkan.core.storage.SettingsStorage
import io.github.barqallayl.burkan.feature.apply.model.ApplyKind
import io.github.barqallayl.burkan.feature.apply.model.RunTrigger
import io.github.barqallayl.burkan.feature.apply.model.StepKind
import io.github.barqallayl.burkan.feature.apply.model.StepRecord
import io.github.barqallayl.burkan.feature.connection.data.ShellAccess
import io.github.barqallayl.burkan.feature.log.data.RunLogStorage
import io.github.barqallayl.burkan.feature.log.model.LoggedStep
import io.github.barqallayl.burkan.feature.log.model.RunLogEntry
import io.github.barqallayl.burkan.feature.log.model.RunResult
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.time.Clock
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds
import kotlin.time.Instant

/** What a run is doing now, in terms a person can be shown. */
sealed interface RunPhase {
    data object Connecting : RunPhase
    data object Reading : RunPhase
    data class Step(val kind: StepKind) : RunPhase
    data object Checking : RunPhase
}

/**
 * How a run ended, and why when it failed. [status] is what the phone ran with at the end, when it could be read, so
 * that nobody has to connect again to show it. [systemUiDeferred] says System UI was left for the next lock: by the
 * run after a restart, or by the run at the lock when the phone was unlocked again before it.
 */
data class RunOutcome(
    val result: RunResult,
    val error: AppError? = null,
    val status: RendererStatus? = null,
    val systemUiDeferred: Boolean = false,
)

/** A status read by a run, and when. */
data class VerifiedStatus(val status: RendererStatus, val at: Instant)

sealed interface ApplyRunState {
    data object Idle : ApplyRunState
    data class Running(val kind: ApplyKind, val trigger: RunTrigger, val phase: RunPhase) : ApplyRunState
}

/**
 * Runs an apply from start to finish: connects, reads the phone, plans, runs, checks the result, and records the run
 * in the log, also when it fails or is cancelled. One run at a time, for the whole app.
 */
@Inject
@SingleIn(AppScope::class)
class ApplyController(
    private val shellAccess: ShellAccess,
    private val runLog: RunLogStorage,
    private val settings: SettingsStorage,
    private val cooldown: SystemUiCooldown,
    private val lockEvents: LockEvents,
    private val clock: Clock,
    @Named(AppBindings.OWN_PACKAGE) private val ownPackage: PackageName,
) {

    private val running = AtomicBoolean(false)
    private val mutableState = MutableStateFlow<ApplyRunState>(ApplyRunState.Idle)
    val state: StateFlow<ApplyRunState> = mutableState.asStateFlow()

    private val mutableVerified = MutableStateFlow<VerifiedStatus?>(null)

    /** The status the last run read at its end. Home shows it rather than connecting again. */
    val verified: StateFlow<VerifiedStatus?> = mutableVerified.asStateFlow()

    /**
     * Returns how the run ended, or null without running when another run is in progress. A light apply restarts only
     * the surfaces not on Vulkan yet, and changes nothing when all of them are.
     *
     * The [trigger] decides System UI. After a restart ([RunTrigger.Boot]) it is left alone, and the outcome says so:
     * the user has just unlocked. At the lock ([RunTrigger.AtLock]) the run connects only once adbd has had
     * [ADBD_SETTLE] to restart, reads no more than System UI needs, and crashes System UI only if the phone is still
     * locked just before; otherwise it is left for the next lock again.
     */
    suspend fun run(kind: ApplyKind, trigger: RunTrigger): RunOutcome? {
        if (!running.compareAndSet(false, true)) return null
        val deferSystemUi = trigger == RunTrigger.Boot
        val atLock = trigger == RunTrigger.AtLock
        val startedAt = clock.now()
        val records = mutableListOf<StepRecord>()
        var runError: AppError? = null
        var result = RunResult.Cancelled
        var alreadyApplied = false
        var systemUiDeferred = false
        var systemUiLeft = false
        var status: RendererStatus? = null
        fun show(phase: RunPhase) {
            mutableState.value = ApplyRunState.Running(kind, trigger, phase)
        }
        try {
            show(RunPhase.Connecting)
            // Locking restarted adbd, which takes about a second to accept connections again.
            val outcome = shellAccess.withShell(settle = if (atLock) ADBD_SETTLE else Duration.ZERO) { shell ->
                either {
                    show(RunPhase.Reading)
                    val reader = RendererReader(shell)
                    val before = (if (atLock) reader.readSystemUi() else reader.read()).bind()
                    val plan = when (kind) {
                        ApplyKind.Light -> LightApplyPlan.create(before, deferSystemUi)
                        ApplyKind.Full -> FullApplyPlan.create(
                            inputs = ApplyInputsReader(shell).readFull().bind(),
                            userExclusions = settings.userExclusions.first(),
                            self = ownPackage,
                            before = before,
                        )
                    }
                    systemUiDeferred = deferSystemUi && before.status.systemUi != Renderer.Vulkan
                    if (kind == ApplyKind.Light && plan.changesNothing && !systemUiDeferred) {
                        alreadyApplied = true
                        // At the lock only System UI was read; Home reads the rest itself.
                        status = before.status.takeUnless { atLock }
                        return@either
                    }
                    val mayRestartSystemUi: () -> Boolean = if (atLock) lockEvents::isLocked else ({ true })
                    systemUiLeft = ApplyRunner(shell, cooldown, mayRestartSystemUi)
                        .run(plan, onStepStarted = { show(RunPhase.Step(it)) }) { records += it }
                    show(RunPhase.Checking)
                    status = RendererReader(shell).read().getOrNull()?.status
                }
            }.flatten()
            runError = outcome.leftOrNull()
            val stepError = records.firstNotNullOfOrNull { it.failure }
            result = when {
                alreadyApplied -> RunResult.AlreadyApplied
                runError == null && stepError == null -> if (systemUiLeft) RunResult.Postponed else RunResult.Succeeded
                else -> RunResult.Failed
            }
            val leftForLock = (systemUiDeferred || systemUiLeft) && result != RunResult.Failed
            return RunOutcome(result, runError ?: stepError, status, leftForLock)
        } catch (e: CancellationException) {
            result = RunResult.Cancelled
            throw e
        } finally {
            withContext(NonCancellable) {
                // Home shows what the run read, so nothing needs the connection now: close it and put the switch back.
                shellAccess.release()
                val finishedAt = clock.now()
                status?.let { mutableVerified.value = VerifiedStatus(it, finishedAt) }
                // Idle before the log entry lands, so whoever reacts to the entry finds the connection free and the
                // verified status in place.
                mutableState.value = ApplyRunState.Idle
                running.set(false)
                runLog.add(
                    RunLogEntry(
                        startedAt = startedAt,
                        trigger = trigger,
                        kind = kind,
                        result = result,
                        duration = finishedAt - startedAt,
                        steps = records.map { LoggedStep(it.kind, it.failure?.type()) },
                        error = runError?.type(),
                    ),
                )
            }
        }
    }

    companion object {
        /** How long adbd is given after the phone locks. It was measured accepting connections again after a second. */
        val ADBD_SETTLE = 3.seconds
    }
}
