package io.github.barqallayl.burkan.feature.apply.data

import io.github.barqallayl.burkan.feature.apply.model.isUnattended
import arrow.core.Either
import arrow.core.flatten
import arrow.core.raise.either
import arrow.core.raise.ensure
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
import io.github.barqallayl.burkan.core.shell.ShellCommands
import io.github.barqallayl.burkan.core.shell.ShellExecutor
import io.github.barqallayl.burkan.core.storage.SettingsStorage
import io.github.barqallayl.burkan.feature.apply.model.ApplyError
import io.github.barqallayl.burkan.feature.apply.model.ApplyKind
import io.github.barqallayl.burkan.feature.apply.model.RestartScope
import io.github.barqallayl.burkan.feature.apply.model.RunTrigger
import io.github.barqallayl.burkan.feature.apply.model.StepKind
import io.github.barqallayl.burkan.feature.apply.model.StepRecord
import io.github.barqallayl.burkan.feature.connection.data.ShellAccess
import io.github.barqallayl.burkan.feature.log.data.RunLogStorage
import io.github.barqallayl.burkan.feature.log.model.LoggedStep
import io.github.barqallayl.burkan.feature.log.model.RunLogEntry
import io.github.barqallayl.burkan.feature.log.model.RunResult
import io.github.barqallayl.burkan.feature.log.model.commandDetail
import java.util.concurrent.atomic.AtomicBoolean
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlin.time.Clock
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds
import kotlin.time.Instant
import kotlin.time.TimeSource

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
    /** The open apps the run found still on the other renderer at its end; null when it did not read them. */
    val behind: Set<PackageName>? = null,
)

/**
 * A status read by a run, and when. [behind] is the open apps the run found still on the other renderer at its
 * end; null when it did not look, or could not.
 */
data class VerifiedStatus(val status: RendererStatus, val at: Instant, val behind: Set<PackageName>? = null)

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
    private val captured: CapturedSettings,
    private val lockEvents: LockEvents,
    private val recentApps: RecentApps,
    private val clock: Clock,
    @Named(AppBindings.OWN_PACKAGE) private val ownPackage: PackageName,
    private val announcer: RunAnnouncer,
) {

    private val running = AtomicBoolean(false)
    private val mutableState = MutableStateFlow<ApplyRunState>(ApplyRunState.Idle)
    val state: StateFlow<ApplyRunState> = mutableState.asStateFlow()

    private val mutableVerified = MutableStateFlow<VerifiedStatus?>(null)

    /** The status the last run read at its end. Home shows it rather than connecting again. */
    val verified: StateFlow<VerifiedStatus?> = mutableVerified.asStateFlow()

    /** Held while settings an earlier run left changed are put back, so that no run starts in the middle of it. */
    private val puttingBack = Mutex()

    /**
     * Returns how the run ended, or null without running when another run is in progress. A light apply restarts only
     * the surfaces not on Vulkan yet, and changes nothing when all of them are. Going back ([ApplyKind.Revert]) is
     * the same towards OpenGL, and [ApplyKind.OpenApps] restarts only the open apps that are behind.
     *
     * [scope] says how many apps a full apply restarts; a light apply restarts none, whatever it is. [only] narrows
     * [ApplyKind.OpenApps] to those of the apps behind that it names: one app, restarted from its row.
     *
     * The [trigger] decides System UI. After a restart ([RunTrigger.Boot]) it is left alone, and the outcome says so:
     * the user has just unlocked. At the lock ([RunTrigger.AtLock]) the run connects only once adbd has had
     * [ADBD_SETTLE] to restart, reads no more than System UI needs, and crashes System UI only if the phone is still
     * locked just before; otherwise it is left for the next lock again.
     */
    suspend fun run(
        kind: ApplyKind,
        trigger: RunTrigger,
        scope: RestartScope = RestartScope.All,
        only: Set<PackageName>? = null,
    ): RunOutcome? {
        if (!running.compareAndSet(false, true)) return null
        val deferSystemUi = trigger == RunTrigger.Boot
        val atLock = trigger == RunTrigger.AtLock
        val startedAt = clock.now()
        val records = mutableListOf<StepRecord>()
        // For the log: how long each step took, how long it took to get to the first, and what the phone ran on
        // before anything was changed. Measured against a clock that only runs forwards, not the phone's.
        val began = TimeSource.Monotonic.markNow()
        val stepTimes = mutableListOf<Duration>()
        var stepBegan = began
        var preparing: Duration? = null
        var beforeStatus: RendererStatus? = null
        var runError: AppError? = null
        var result = RunResult.Cancelled
        var alreadyApplied = false
        var systemUiDeferred = false
        var systemUiLeft = false
        var status: RendererStatus? = null
        var behind: Set<PackageName>? = null
        fun show(phase: RunPhase) {
            mutableState.value = ApplyRunState.Running(kind, trigger, phase)
        }
        try {
            // What a full apply left changed when the app's process died under it goes back first, before this run
            // reads the phone and takes that for how it was.
            puttingBack.withLock { captured.putBackKept() }
            show(RunPhase.Connecting)
            // Locking restarted adbd, which takes about a second to accept connections again.
            val outcome = shellAccess.withShell(settle = if (atLock) ADBD_SETTLE else Duration.ZERO) { shell ->
                either {
                    show(RunPhase.Reading)
                    val reader = RendererReader(shell)
                    val before = (if (atLock) reader.readSystemUi() else reader.read()).bind()
                    beforeStatus = before.status
                    val userExclusions = settings.userExclusions.first()
                    val plan = when (kind) {
                        ApplyKind.Light -> LightApplyPlan.create(before, deferSystemUi)
                        ApplyKind.Revert -> LightApplyPlan.create(before, target = Renderer.OpenGL)
                        ApplyKind.OpenApps -> {
                            // Whatever new apps get now is what the open ones are brought to: Vulkan after an
                            // apply, OpenGL after going back.
                            val target = before.status.newApps
                            FullApplyPlan.create(
                                inputs = ApplyInputsReader(shell).readFull().bind(),
                                userExclusions = userExclusions,
                                self = ownPackage,
                                before = before,
                                only = OpenAppsReader(shell).notOn(target).bind()
                                    .let { open -> if (only == null) open else open intersect only },
                                target = target,
                            )
                        }

                        ApplyKind.Full -> {
                            val limit = scope.limit
                            FullApplyPlan.create(
                                inputs = ApplyInputsReader(shell).readFull().bind(),
                                userExclusions = userExclusions,
                                self = ownPackage,
                                before = before,
                                recent = if (limit == null) null else readRecentApps(shell).bind(),
                                limit = limit,
                            )
                        }
                    }
                    // Connected, read and planned: everything before the first step.
                    preparing = began.elapsedNow()
                    systemUiDeferred = deferSystemUi && before.status.systemUi != Renderer.Vulkan
                    if (kind != ApplyKind.Full && plan.changesNothing && !systemUiDeferred) {
                        alreadyApplied = true
                        // At the lock only System UI was read; Home reads the rest itself.
                        status = before.status.takeUnless { atLock }
                        // Nothing was restarted, so the open apps are as they were: Home still has to be told.
                        if (!atLock) behind = OpenAppsReader(shell).behind(before, userExclusions, ownPackage).getOrNull()
                        return@either
                    }
                    // A run nobody confirmed on the screen restarts System UI only under a locked phone. Left
                    // alone, it is done at the next lock, like the run after a restart.
                    val mayRestartSystemUi: () -> Boolean =
                        if (atLock || trigger.isUnattended) lockEvents::isLocked else ({ true })
                    systemUiLeft = ApplyRunner(shell, cooldown, captured, mayRestartSystemUi).run(
                        plan,
                        onStepStarted = {
                            stepBegan = TimeSource.Monotonic.markNow()
                            show(RunPhase.Step(it))
                        },
                    ) {
                        records += it
                        stepTimes += stepBegan.elapsedNow()
                    }
                    show(RunPhase.Checking)
                    val after = RendererReader(shell).read().getOrNull()
                    status = after?.status
                    // Home, and the notice that the apply after a restart is done, say which open apps the run
                    // left behind, without connecting again to ask.
                    if (after != null) {
                        behind = OpenAppsReader(shell).behind(after, userExclusions, ownPackage).getOrNull()
                    }
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
            return RunOutcome(result, runError ?: stepError, status, leftForLock, behind)
        } catch (e: CancellationException) {
            result = RunResult.Cancelled
            throw e
        } finally {
            withContext(NonCancellable) {
                // Home shows what the run read, so nothing needs the connection now: close it and put the switch back.
                shellAccess.release()
                val finishedAt = clock.now()
                status?.let { mutableVerified.value = VerifiedStatus(it, finishedAt, behind) }
                // Idle before the log entry lands, so whoever reacts to the entry finds the connection free and the
                // verified status in place.
                mutableState.value = ApplyRunState.Idle
                running.set(false)
                val entry = RunLogEntry(
                    startedAt = startedAt,
                    trigger = trigger,
                    kind = kind,
                    result = result,
                    duration = finishedAt - startedAt,
                    steps = records.mapIndexed { index, record ->
                        LoggedStep(
                            kind = record.kind,
                            error = record.failure?.type(),
                            duration = stepTimes.getOrNull(index),
                            detail = commandDetail(record.failure),
                        )
                    },
                    error = runError?.type(),
                    before = beforeStatus,
                    after = status,
                    preparing = preparing,
                    behind = behind?.size,
                    detail = commandDetail(runError),
                )
                runLog.add(entry)
                // Last, once the log has it: whoever hears of the run can read all of it.
                announcer.finished(entry)
            }
        }
    }

    /**
     * Puts back the settings a full apply left changed when the app's process died under it. Home asks when it
     * opens. Nothing is done while a run is in progress: what is kept then is the run's own, which it has still to
     * put back itself.
     */
    suspend fun putBackInterrupted() = puttingBack.withLock {
        if (!running.get()) captured.putBackKept()
    }

    /**
     * The apps used lately, most recent first. Android answers that only to an app allowed to ask, which Burkan allows
     * itself over the connection the first time it needs to.
     */
    private suspend fun readRecentApps(shell: ShellExecutor): Either<AppError, List<PackageName>> = either {
        if (!recentApps.isAllowed()) {
            val allowed = shell.run(ShellCommands.allowUsageAccess(ownPackage)).bind()
            ensure(allowed.exitCode == 0) { ApplyError.CommandFailed(allowed.exitCode, allowed.stderr) }
        }
        val recent = recentApps.byRecency()
        ensure(recent.isNotEmpty()) { ApplyError.RecentAppsUnknown }
        recent
    }

    companion object {
        /** How long adbd is given after the phone locks. It was measured accepting connections again after a second. */
        val ADBD_SETTLE = 3.seconds
    }
}
