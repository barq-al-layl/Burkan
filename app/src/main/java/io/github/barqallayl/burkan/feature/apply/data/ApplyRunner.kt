package io.github.barqallayl.burkan.feature.apply.data

import arrow.core.Either
import arrow.core.getOrElse
import io.github.barqallayl.burkan.core.model.AppError
import io.github.barqallayl.burkan.core.model.Renderer
import io.github.barqallayl.burkan.core.shell.RendererReader
import io.github.barqallayl.burkan.core.shell.SettingKey
import io.github.barqallayl.burkan.core.shell.ShellCommands
import io.github.barqallayl.burkan.core.shell.ShellError
import io.github.barqallayl.burkan.core.shell.ShellExecutor
import io.github.barqallayl.burkan.core.shell.parseSettingValue
import io.github.barqallayl.burkan.feature.apply.model.ApplyError
import io.github.barqallayl.burkan.feature.apply.model.StepKind
import io.github.barqallayl.burkan.feature.apply.model.StepRecord
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import kotlin.time.Duration.Companion.seconds

/**
 * Executes an [ApplyPlan] through the shell. [shell] should reconnect by itself: restarting System UI brings up the
 * lock screen, which restarts adbd under the run. [mayRestartSystemUi] is asked immediately before System UI is
 * crashed; the run at the lock passes whether the phone is still locked. [captured] keeps what the plan has to put
 * back until it is back, and writes it back where the shell no longer can.
 */
class ApplyRunner(
    private val shell: ShellExecutor,
    private val cooldown: SystemUiCooldown,
    private val captured: CapturedSettings,
    private val mayRestartSystemUi: () -> Boolean = { true },
) {

    /**
     * Runs [plan], reporting each step to [onStepStarted] as it begins and to [onStep] once it has finished. Stops at
     * the first failed step. The restore steps run afterwards in every case: after success, after a failure, and
     * after cancellation, which is rethrown once the settings are back. System UI is restarted last, and only when
     * nothing failed.
     *
     * Returns true when System UI was due but [mayRestartSystemUi] said no: it was left alone, and no step recorded.
     */
    suspend fun run(plan: ApplyPlan, onStepStarted: (StepKind) -> Unit = {}, onStep: (StepRecord) -> Unit): Boolean {
        var failed = false
        try {
            // Kept from before the first step, so that a run whose process dies under it can still be undone.
            captured.keep(plan.restore.associate { it.setting to it.value })
            for (step in plan.steps) {
                onStepStarted(step.kind)
                val failure = if (step.kind == StepKind.RestartKeyboard) keepingDefaultKeyboard(step) else execute(step)
                onStep(StepRecord(step.kind, failure))
                if (failure != null) {
                    failed = true
                    break
                }
            }
        } finally {
            withContext(NonCancellable) {
                var allBack = true
                plan.restore.forEach { step ->
                    onStepStarted(step.kind)
                    val failure = restore(step)
                    if (failure != null) allBack = false
                    onStep(StepRecord(step.kind, failure))
                }
                // One that is not back stays kept, to be tried again when the app next opens or runs.
                if (allBack) captured.letGo() else failed = true
            }
        }
        if (plan.restartSystemUi && !failed) {
            onStepStarted(StepKind.RestartSystemUi)
            // As late as it can be asked. Between it and the crash there is only the cooldown, a local write.
            if (!mayRestartSystemUi()) return true
            val failure = if (cooldown.claim()) restartSystemUi() else ApplyError.SystemUiRestartedRecently
            onStep(StepRecord(StepKind.RestartSystemUi, failure))
        }
        return false
    }

    /**
     * Crashes System UI, the only way to restart it, then asks until the new process answers on Vulkan. The lock
     * screen it brings up restarts adbd, so the crash usually loses its connection: that is the expected outcome, and
     * the shell reconnects for the checks after it.
     */
    private suspend fun restartSystemUi(): AppError? {
        shell.run(ShellCommands.crash(ShellCommands.SystemUi)).fold(
            ifLeft = { error -> if (error != ShellError.ConnectionLost) return error },
            ifRight = { result ->
                if (result.exitCode != 0) return ApplyError.CommandFailed(result.exitCode, result.stderr)
            },
        )
        val reader = RendererReader(shell)
        repeat(VERIFY_ATTEMPTS) {
            delay(VERIFY_PAUSE)
            // Until the old process is gone it still answers, on OpenGL; until the new one is up, nothing does.
            if (reader.rendererOf(ShellCommands.SystemUi).getOrElse { return it } == Renderer.Vulkan) return null
        }
        return ApplyError.SystemUiNotOnVulkan
    }

    /**
     * Runs [step], which restarts the keyboard, and puts the default keyboard back if the restart changed it: with
     * `ime set`, and failing that by writing the setting.
     */
    private suspend fun keepingDefaultKeyboard(step: ApplyStep.Run): AppError? {
        val before = defaultKeyboard().getOrElse { return it }
        execute(step)?.let { return it }
        if (before == null) return null
        delay(KEYBOARD_SETTLE)
        if (defaultKeyboard().getOrElse { return it } == before) return null
        val restores = listOf(ShellCommands.setInputMethod(before), ShellCommands.putSetting(DefaultKeyboard, before))
        for (restore in restores) {
            shell.run(restore).getOrElse { return it }
            if (defaultKeyboard().getOrElse { return it } == before) return null
        }
        return ApplyError.KeyboardNotRestored
    }

    /** The default keyboard's component, `package/class`, or null when none is set. */
    private suspend fun defaultKeyboard(): Either<AppError, String?> =
        shell.run(ShellCommands.getSetting(DefaultKeyboard)).map { parseSettingValue(it.stdout) }

    private suspend fun execute(step: ApplyStep.Run): AppError? =
        shell.run(step.command).fold(
            ifLeft = { it },
            ifRight = { result ->
                if (result.exitCode == 0) null else ApplyError.CommandFailed(result.exitCode, result.stderr)
            },
        )

    /**
     * Writes the value and reads it back, retrying with a pause while it does not match: the original scripts needed
     * this for rotation and accessibility. When the shell does not answer, the app writes the value itself.
     */
    private suspend fun restore(step: ApplyStep.RestoreSetting): AppError? {
        val key = step.setting.key
        var failure: AppError = ApplyError.SettingNotRestored(step.setting)
        repeat(RESTORE_ATTEMPTS) { attempt ->
            if (attempt > 0) delay(RESTORE_PAUSE)
            val written = shell.run(ShellCommands.putSetting(key, step.value))
                .getOrElse { return withoutShell(step, it) }
            if (written.exitCode != 0) {
                failure = ApplyError.CommandFailed(written.exitCode, written.stderr)
                return@repeat
            }
            val readBack = shell.run(ShellCommands.getSetting(key)).getOrElse { return withoutShell(step, it) }
            if (parseSettingValue(readBack.stdout) == step.value) return null
            failure = ApplyError.SettingNotRestored(step.setting)
        }
        return failure
    }

    /**
     * The shell did not answer: the connection has gone and would not come back, or the command hung. The app
     * writes the value itself. [lost] is the step's failure when Android will not take the value from the app
     * either.
     */
    private fun withoutShell(step: ApplyStep.RestoreSetting, lost: ShellError): AppError? =
        if (captured.putBack(step.setting, step.value)) null else lost

    private companion object {
        const val RESTORE_ATTEMPTS = 3
        val RESTORE_PAUSE = 1.seconds
        val DefaultKeyboard = SettingKey.DefaultInputMethod
        val KEYBOARD_SETTLE = 1.seconds
        const val VERIFY_ATTEMPTS = 15
        val VERIFY_PAUSE = 1.seconds
    }
}
