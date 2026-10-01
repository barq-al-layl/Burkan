package io.github.barqallayl.burkan.feature.apply.data

import io.github.barqallayl.burkan.core.shell.PackageName
import io.github.barqallayl.burkan.core.shell.ShellCommands
import io.github.barqallayl.burkan.feature.apply.model.StepKind

/**
 * The light apply: set the property and restart the three system surfaces. What runs after boot, when almost
 * nothing with a UI is running yet. It force-stops only the launcher, so there is no state to capture.
 */
object LightApplyPlan {

    /** [keyboard] is the current input method's package, or null when none is set. */
    fun create(keyboard: PackageName?): ApplyPlan = ApplyPlan(
        steps = buildList {
            add(ApplyStep.Run(StepKind.SetRenderer, ShellCommands.setVulkanRenderer()))
            // SystemUI is persistent: force-stop leaves it running, so it has to be crashed.
            add(ApplyStep.Run(StepKind.RestartSystemUi, ShellCommands.crash(ShellCommands.SystemUi)))
            add(ApplyStep.Run(StepKind.RestartLauncher, ShellCommands.forceStop(ShellCommands.Launcher)))
            // Never force-stop an input method: that clears it as the default keyboard.
            if (keyboard != null) add(ApplyStep.Run(StepKind.RestartKeyboard, ShellCommands.crash(keyboard)))
        },
    )
}
