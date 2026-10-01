package io.github.barqallayl.burkan.feature.apply.data

import arrow.core.flatten
import arrow.core.raise.Raise
import arrow.core.raise.either
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.Named
import dev.zacsweers.metro.SingleIn
import io.github.barqallayl.burkan.core.di.AppBindings
import io.github.barqallayl.burkan.core.model.AppError
import io.github.barqallayl.burkan.core.model.Renderer
import io.github.barqallayl.burkan.core.model.type
import io.github.barqallayl.burkan.core.shell.PackageName
import io.github.barqallayl.burkan.core.shell.ShellCommands
import io.github.barqallayl.burkan.core.shell.ShellExecutor
import io.github.barqallayl.burkan.core.shell.parseRenderer
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

/** What a run is doing now, in terms a person can be shown. */
sealed interface RunPhase {
    data object Connecting : RunPhase
    data object Reading : RunPhase
    data class Step(val kind: StepKind) : RunPhase
}

/** How a run ended, and why when it failed. */
data class RunOutcome(val result: RunResult, val error: AppError? = null)

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
    private val settings: SettingsStorage,
    private val clock: Clock,
    @Named(AppBindings.OWN_PACKAGE) private val ownPackage: PackageName,
) {

    private val running = AtomicBoolean(false)
    private val mutableState = MutableStateFlow<ApplyRunState>(ApplyRunState.Idle)
    val state: StateFlow<ApplyRunState> = mutableState.asStateFlow()

    /**
     * Returns how the run ended, or null without running when another run is in progress. With [skipIfApplied], a run
     * that finds the property set and SystemUI on Vulkan changes nothing.
     */
    suspend fun run(kind: ApplyKind, trigger: RunTrigger, skipIfApplied: Boolean = false): RunOutcome? {
        if (!running.compareAndSet(false, true)) return null
        val startedAt = clock.now()
        val records = mutableListOf<StepRecord>()
        var runError: AppError? = null
        var result = RunResult.Cancelled
        var alreadyApplied = false
        fun show(phase: RunPhase) {
            mutableState.value = ApplyRunState.Running(kind, trigger, phase)
        }
        try {
            show(RunPhase.Connecting)
            val outcome = shellAccess.withShell { shell ->
                either {
                    show(RunPhase.Reading)
                    if (skipIfApplied && isApplied(shell)) {
                        alreadyApplied = true
                        return@either
                    }
                    val reader = ApplyInputsReader(shell)
                    val plan = when (kind) {
                        ApplyKind.Light -> LightApplyPlan.create(reader.readKeyboard().bind())
                        ApplyKind.Full -> FullApplyPlan.create(
                            inputs = reader.readFull().bind(),
                            userExclusions = settings.userExclusions.first(),
                            self = ownPackage,
                        )
                    }
                    ApplyRunner(shell).run(plan, onStepStarted = { show(RunPhase.Step(it)) }) { records += it }
                }
            }.flatten()
            runError = outcome.leftOrNull()
            val stepError = records.firstNotNullOfOrNull { it.failure }
            result = when {
                alreadyApplied -> RunResult.AlreadyApplied
                runError == null && stepError == null -> RunResult.Succeeded
                else -> RunResult.Failed
            }
            return RunOutcome(result, runError ?: stepError)
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

    /** The property is set and SystemUI already runs on Vulkan: nothing a light apply would change. */
    private suspend fun Raise<AppError>.isApplied(shell: ShellExecutor): Boolean {
        val property = shell.run(ShellCommands.getRenderer()).bind().stdout
        if (property.trim() != VULKAN_PROPERTY) return false
        val systemUi = shell.run(ShellCommands.gfxInfo(ShellCommands.SystemUi)).bind()
        return systemUi.exitCode == 0 && parseRenderer(systemUi.stdout) == Renderer.Vulkan
    }

    private companion object {
        const val VULKAN_PROPERTY = "skiavk"
    }
}
