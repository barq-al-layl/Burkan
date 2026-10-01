package io.github.barqallayl.burkan.feature.apply.data

import arrow.core.getOrElse
import dev.zacsweers.metro.Inject
import io.github.barqallayl.burkan.core.model.AppError
import io.github.barqallayl.burkan.core.shell.ShellCommands
import io.github.barqallayl.burkan.core.shell.ShellExecutor
import io.github.barqallayl.burkan.feature.apply.model.ApplyError
import io.github.barqallayl.burkan.feature.apply.model.StepRecord
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import kotlin.time.Duration.Companion.seconds

/** Executes an [ApplyPlan] through the shell. */
@Inject
class ApplyRunner(private val shell: ShellExecutor) {

    /**
     * Runs [plan], reporting each finished step to [onStep]. Stops at the first failed step. The restore steps run
     * afterwards in every case: after success, after a failure, and after cancellation, which is rethrown once the
     * settings are back.
     */
    suspend fun run(plan: ApplyPlan, onStep: (StepRecord) -> Unit) {
        try {
            for (step in plan.steps) {
                val failure = execute(step)
                onStep(StepRecord(step.kind, failure))
                if (failure != null) break
            }
        } finally {
            withContext(NonCancellable) {
                plan.restore.forEach { step -> onStep(StepRecord(step.kind, restore(step))) }
            }
        }
    }

    private suspend fun execute(step: ApplyStep.Run): AppError? =
        shell.run(step.command).fold(
            ifLeft = { it },
            ifRight = { result ->
                if (result.exitCode == 0) null else ApplyError.CommandFailed(result.exitCode, result.stderr)
            },
        )

    /**
     * Writes the value and reads it back, retrying with a pause while it does not match: the original scripts needed
     * this for rotation and accessibility. A lost connection ends the step at once.
     */
    private suspend fun restore(step: ApplyStep.RestoreSetting): AppError? {
        val key = step.setting.key
        var failure: AppError = ApplyError.SettingNotRestored(step.setting)
        repeat(RESTORE_ATTEMPTS) { attempt ->
            if (attempt > 0) delay(RESTORE_PAUSE)
            val written = shell.run(ShellCommands.putSetting(key, step.value)).getOrElse { return it }
            if (written.exitCode != 0) {
                failure = ApplyError.CommandFailed(written.exitCode, written.stderr)
                return@repeat
            }
            val readBack = shell.run(ShellCommands.getSetting(key)).getOrElse { return it }
            if (parseSettingValue(readBack.stdout) == step.value) return null
            failure = ApplyError.SettingNotRestored(step.setting)
        }
        return failure
    }

    private companion object {
        const val RESTORE_ATTEMPTS = 3
        val RESTORE_PAUSE = 1.seconds
    }
}
