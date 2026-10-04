package io.github.barqallayl.burkan.feature.apply.model

import io.github.barqallayl.burkan.core.model.AppError

/** The two applies. */
enum class ApplyKind {
    /** Set the property and restart the three system surfaces. */
    Light,

    /** Restart every app, then put back what that disturbs. */
    Full,
}

/**
 * How many apps a full apply restarts, chosen each time one is started: every app unless the user picks fewer.
 * [limit] is the number of most recently used apps to restart; null restarts every app.
 */
enum class RestartScope(val limit: Int?) {
    All(null),
    Recent30(30),
    Recent70(70),
}

/** What started a run. */
enum class RunTrigger {
    Manual,

    /** The automatic apply after a restart. The user has just unlocked, so System UI is left for the next lock. */
    Boot,

    /** The rest of the automatic apply after a restart: System UI, restarted once the phone has locked. */
    AtLock,
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

    /** Android would not say which apps were used lately, so a run limited to those had nothing to go by. */
    data object RecentAppsUnknown : ApplyError

    /** The value was written but did not read back the same after every retry. */
    data class SettingNotRestored(val setting: RestoredSetting) : ApplyError

    /** System UI was restarted less than a minute ago, so it was left alone: see `SystemUiCooldown`. */
    data object SystemUiRestartedRecently : ApplyError

    /** System UI came back, or was still answering, on a renderer other than Vulkan. */
    data object SystemUiNotOnVulkan : ApplyError

    /** Restarting the keyboard changed the default keyboard, and it could not be put back. */
    data object KeyboardNotRestored : ApplyError
}

/** What the automatic apply after a restart is waiting for before it can connect. */
enum class WaitReason {
    /** The phone is not on Wi-Fi. */
    Wifi,

    /** Android refused wireless debugging on this network until the user allows it. */
    TrustedNetwork,
}

/**
 * Where the automatic apply after a restart stands. [triedNetwork] is the handle of the network the last attempt was
 * made on, so that network coming up again does not start the same failing attempt. [systemUiAtNextLock] is set when
 * the run after a restart left System UI for the next time the phone locks, so as not to lock a phone the user has
 * just unlocked.
 */
data class AutoApplyState(
    val waitingFor: WaitReason? = null,
    val triedNetwork: Long? = null,
    val systemUiAtNextLock: Boolean = false,
)
