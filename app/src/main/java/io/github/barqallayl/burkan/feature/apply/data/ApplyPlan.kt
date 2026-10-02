package io.github.barqallayl.burkan.feature.apply.data

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
 * come after it.
 */
data class ApplyPlan(
    val steps: List<ApplyStep.Run>,
    val restore: List<ApplyStep.RestoreSetting> = emptyList(),
    val restartSystemUi: Boolean = false,
) {
    /** True when the plan only sets the property: every surface it would restart is on Vulkan already. */
    val restartsNothing: Boolean
        get() = steps.all { it.kind == StepKind.SetRenderer } && !restartSystemUi
}

val RestoredSetting.key: SettingKey
    get() = when (this) {
        RestoredSetting.AutoRotation -> SettingKey.AutoRotation
        RestoredSetting.AccessibilityServices -> SettingKey.AccessibilityServices
        RestoredSetting.EdgeEnabled -> SettingKey.EdgeEnabled
        RestoredSetting.EdgePanels -> SettingKey.EdgePanels
    }
