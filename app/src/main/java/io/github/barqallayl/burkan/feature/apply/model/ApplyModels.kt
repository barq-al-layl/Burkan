package io.github.barqallayl.burkan.feature.apply.model

import io.github.barqallayl.burkan.core.model.AppError

/** The two applies. */
enum class ApplyKind {
    /** Set the property and restart the three system surfaces. */
    Light,

    /** Restart every app, then put back what that disturbs. */
    Full,
}

/** What started a run. */
enum class RunTrigger {
    Manual,
    Boot,
}

/** The device settings a full apply disturbs, and puts back. */
enum class RestoredSetting {
    AutoRotation,
    AccessibilityServices,
    EdgeEnabled,
    EdgePanels,
}

/** What a step of a run does, in terms a person can be shown. */
sealed interface StepKind {
    data object SetRenderer : StepKind
    data class StopApps(val count: Int) : StepKind
    data object RestartSystemUi : StepKind
    data object RestartLauncher : StepKind
    data class RelaunchApps(val count: Int) : StepKind
    data object RestartKeyboard : StepKind
    data class RestoreSetting(val setting: RestoredSetting) : StepKind
}

/** One finished step: [failure] is null when it succeeded. */
data class StepRecord(val kind: StepKind, val failure: AppError? = null)

sealed interface ApplyError : AppError {
    /** The command ran but reported failure. */
    data class CommandFailed(val exitCode: Int, val stderr: String) : ApplyError

    /** `pm list packages` listed nothing, so a full apply has nothing to work from. */
    data object NoPackagesListed : ApplyError

    /** The value was written but did not read back the same after every retry. */
    data class SettingNotRestored(val setting: RestoredSetting) : ApplyError
}
