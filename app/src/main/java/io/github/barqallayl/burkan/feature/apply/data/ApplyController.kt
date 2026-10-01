package io.github.barqallayl.burkan.feature.apply.data

import arrow.core.flatten
import arrow.core.raise.either
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.Named
import dev.zacsweers.metro.SingleIn
import io.github.barqallayl.burkan.core.di.AppBindings
import io.github.barqallayl.burkan.core.model.AppError
import io.github.barqallayl.burkan.core.model.type
import io.github.barqallayl.burkan.core.shell.PackageName
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
import kotlinx.coroutines.withContext
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.time.Clock

/** What a run is doing now, in terms a person can be shown. */
sealed interface RunPhase {
    data object Connecting : RunPhase
    data object Reading : RunPhase
    data class Step(val kind: StepKind) : RunPhase
}

sealed interface ApplyRunState {
    data object Idle : ApplyRunState
    data class Running(val kind: ApplyKind, val trigger: RunTrigger, val phase: RunPhase) : ApplyRunState
}

/**
 * Runs an apply from start to finish: connects, reads the phone, plans, runs, and records the run in the log, also
 * when it fails or is cancelled. One run at a time, for the whole app.
 */
@Inject
@SingleIn(AppScope::class)
class ApplyController(
    private val shellAccess: ShellAccess,
    private val runLog: RunLogStorage,
    private val clock: Clock,
    @Named(AppBindings.OWN_PACKAGE) private val ownPackage: PackageName,
) {

    private val running = AtomicBoolean(false)
    private val mutableState = MutableStateFlow<ApplyRunState>(ApplyRunState.Idle)
    val state: StateFlow<ApplyRunState> = mutableState.asStateFlow()

    /** Returns how the run ended, or null without running when another run is in progress. */
    suspend fun run(kind: ApplyKind, trigger: RunTrigger): RunResult? {
        if (!running.compareAndSet(false, true)) return null
        val startedAt = clock.now()
        val records = mutableListOf<StepRecord>()
        var runError: AppError? = null
        var result = RunResult.Cancelled
        fun show(phase: RunPhase) {
            mutableState.value = ApplyRunState.Running(kind, trigger, phase)
        }
        try {
            show(RunPhase.Connecting)
            val outcome = shellAccess.withShell { shell ->
                either {
                    show(RunPhase.Reading)
                    val reader = ApplyInputsReader(shell)
                    val plan = when (kind) {
                        ApplyKind.Light -> LightApplyPlan.create(reader.readKeyboard().bind())
                        // The user's own exclusions arrive with the Settings screen in M4.
                        ApplyKind.Full -> FullApplyPlan.create(reader.readFull().bind(), emptySet(), ownPackage)
                    }
                    ApplyRunner(shell).run(plan, onStepStarted = { show(RunPhase.Step(it)) }) { records += it }
                }
            }.flatten()
            runError = outcome.leftOrNull()
            result = if (runError == null && records.none { it.failure != null }) {
                RunResult.Succeeded
            } else {
                RunResult.Failed
            }
            return result
        } catch (e: CancellationException) {
            result = RunResult.Cancelled
            throw e
        } finally {
            // Idle before the log entry lands, so whoever reacts to the entry finds the connection free.
            mutableState.value = ApplyRunState.Idle
            running.set(false)
            withContext(NonCancellable) {
                runLog.add(
                    RunLogEntry(
                        startedAt = startedAt,
                        trigger = trigger,
                        kind = kind,
                        result = result,
                        duration = clock.now() - startedAt,
                        steps = records.map { LoggedStep(it.kind, it.failure?.type()) },
                        error = runError?.type(),
                    ),
                )
            }
        }
    }
}
