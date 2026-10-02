package io.github.barqallayl.burkan.feature.apply.data

import io.github.barqallayl.burkan.core.model.Renderer
import io.github.barqallayl.burkan.core.shell.ShellCommands
import io.github.barqallayl.burkan.core.shell.Surfaces
import io.github.barqallayl.burkan.feature.apply.model.StepKind

/**
 * The light apply: set the property unless it is set, and restart the system surfaces that are not on Vulkan yet.
 * What runs after boot, when almost nothing with a UI is running yet. It force-stops only the launcher, so there is
 * no state to capture.
 */
object LightApplyPlan {

    /**
     * [before] is what the phone was running with when the run started. With [deferSystemUi], System UI is left
     * alone: restarting it brings up the lock screen, which a phone the user has just unlocked should not show.
     */
    fun create(before: Surfaces, deferSystemUi: Boolean = false): ApplyPlan = ApplyPlan(
        steps = buildList {
            if (before.status.newApps != Renderer.Vulkan) {
                add(ApplyStep.Run(StepKind.SetRenderer, ShellCommands.setVulkanRenderer()))
            }
            if (before.launcher != null && before.status.launcher != Renderer.Vulkan) {
                add(ApplyStep.Run(StepKind.RestartLauncher, ShellCommands.restartLauncher(before.launcher)))
            }
            // Never force-stop an input method: that clears it as the default keyboard.
            if (before.keyboard != null && before.status.keyboard != Renderer.Vulkan) {
                add(ApplyStep.Run(StepKind.RestartKeyboard, ShellCommands.crash(before.keyboard)))
            }
        },
        restartSystemUi = !deferSystemUi && before.status.systemUi != Renderer.Vulkan,
    )
}
