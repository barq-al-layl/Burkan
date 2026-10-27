package io.github.barqallayl.burkan.feature.apply.data

import io.github.barqallayl.burkan.core.shell.ShellCommands
import io.github.barqallayl.burkan.core.model.Renderer
import io.github.barqallayl.burkan.core.shell.SettingKey
import io.github.barqallayl.burkan.core.shell.ShellCommand
import io.github.barqallayl.burkan.feature.apply.model.RestoredSetting
import io.github.barqallayl.burkan.feature.apply.model.StepKind

/** One step of a run. */
sealed interface ApplyStep {
    val kind: StepKind

    /** Runs [command]; a non-zero exit fails the step. */
    data class Run(override val kind: StepKind, val command: ShellCommand) : ApplyStep

    /** Writes a captured [value] back, then reads it back until it matches. */
    data class RestoreSetting(val setting: RestoredSetting, val value: String) : ApplyStep {
        override val kind: StepKind = StepKind.RestoreSetting(setting)
    }
}

/**
 * What a run does: [steps] in order, stopping at the first failure, then [restore], which runs whatever happened
 * to [steps], including cancellation. Last of all, when nothing failed and [restartSystemUi] is set, System UI is
 * restarted: that brings up the lock screen, which restarts adbd and drops the connection, so nothing that matters may
 * come after it. [target] is the renderer the plan leaves the phone on, which System UI is checked against.
 */
data class ApplyPlan(
    val steps: List<ApplyStep.Run>,
    val restore: List<ApplyStep.RestoreSetting> = emptyList(),
    val restartSystemUi: Boolean = false,
    val target: Renderer = Renderer.Vulkan,
) {
    /** True when there is nothing to do: the property and every surface are on the target already. */
    val changesNothing: Boolean
        get() = steps.isEmpty() && !restartSystemUi
}

/**
 * The step that makes new apps start on [target], or null when [now], what the property gives them already, is it.
 * Vulkan is set by name. OpenGL is the phone's own default, so the property is cleared, as a restart leaves it.
 */
fun rendererStep(now: Renderer, target: Renderer): ApplyStep.Run? = when {
    now == target -> null
    target == Renderer.Vulkan -> ApplyStep.Run(StepKind.SetRenderer, ShellCommands.setVulkanRenderer())
    else -> ApplyStep.Run(StepKind.ClearRenderer, ShellCommands.clearRenderer())
}

/**
 * Whether the keyboard, reporting [now], has to be restarted to be on [target]. One that reports nothing is not
 * running (`dumpsys gfxinfo` answers "No process found"), and starts on whatever the property gives when it is next
 * shown, so there is nothing to restart.
 */
fun keyboardNeedsRestart(now: Renderer, target: Renderer): Boolean = now != target && now != Renderer.Unknown

val RestoredSetting.key: SettingKey
    get() = when (this) {
        RestoredSetting.AutoRotation -> SettingKey.AutoRotation
        RestoredSetting.AccessibilityServices -> SettingKey.AccessibilityServices
        RestoredSetting.EdgeEnabled -> SettingKey.EdgeEnabled
        RestoredSetting.EdgePanels -> SettingKey.EdgePanels
    }
