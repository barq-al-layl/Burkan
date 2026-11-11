package io.github.barqallayl.burkan.core.model

import androidx.annotation.StringRes
import io.github.barqallayl.burkan.R
import io.github.barqallayl.burkan.core.shell.ShellError
import io.github.barqallayl.burkan.feature.apply.model.ApplyError
import io.github.barqallayl.burkan.feature.connection.model.ConnectionError
import io.github.barqallayl.burkan.feature.connection.model.PairingError
import io.github.barqallayl.burkan.feature.settings.model.SettingsError
import io.github.barqallayl.burkan.feature.setup.model.SetupError

/** One entry per message a user can be shown about a failure. */
/**
 * [resource] says what went wrong and, where there is something to do, what to do now: for a screen or a
 * notification where it can be done. [logResource] says only what happened, for the record of a run that is over: the
 * Log, Home's last run and the shared log, where an instruction to try again has nothing to try. Where the two would be
 * the same, there is one. The two that leave the phone in need of a fix (a setting, the keyboard) keep the fix in both.
 */
enum class AppErrorType(@StringRes val resource: Int, @StringRes val logResource: Int = resource) {
    NoWifi(R.string.error_no_wifi, R.string.log_error_no_wifi),
    WirelessDebuggingOff(R.string.error_wireless_debugging_off, R.string.log_error_wireless_debugging_off),
    WirelessDebuggingRefused(R.string.error_wireless_debugging_refused, R.string.log_error_wireless_debugging_refused),
    LocalNetworkDenied(R.string.error_local_network_denied, R.string.log_error_local_network_denied),
    DaemonNotFound(R.string.error_daemon_not_found, R.string.log_error_daemon_not_found),
    NotAuthorised(R.string.error_not_authorised, R.string.log_error_not_authorised),
    ConnectFailed(R.string.error_connect_failed, R.string.log_error_connect_failed),
    ShizukuNotRunning(R.string.error_shizuku_not_running, R.string.log_error_shizuku_not_running),
    ShizukuNotAllowed(R.string.error_shizuku_not_allowed, R.string.log_error_shizuku_not_allowed),
    ShizukuShellFailed(R.string.error_shizuku_shell_failed, R.string.log_error_shizuku_shell_failed),
    ConnectionLost(R.string.error_connection_lost, R.string.log_error_connection_lost),
    CommandTimedOut(R.string.error_command_timed_out, R.string.log_error_command_timed_out),
    ShellFailed(R.string.error_shell_failed, R.string.log_error_shell_failed),
    InvalidCode(R.string.error_invalid_code),
    PairingDialogClosed(R.string.error_pairing_dialog_closed),
    WrongCode(R.string.error_wrong_code),
    PairingTimedOut(R.string.error_pairing_timed_out),
    PairingFailed(R.string.error_pairing_failed),
    GrantFailed(R.string.error_grant_failed, R.string.log_error_grant_failed),
    CommandFailed(R.string.error_command_failed),
    NoPackagesListed(R.string.error_no_packages_listed),
    RecentAppsUnknown(R.string.error_recent_apps_unknown, R.string.log_error_recent_apps_unknown),
    SettingNotRestored(R.string.error_setting_not_restored),
    SystemUiRestartedRecently(
        R.string.error_system_ui_restarted_recently,
        R.string.log_error_system_ui_restarted_recently,
    ),
    SystemUiNotOnVulkan(R.string.error_system_ui_not_on_vulkan),
    SystemUiStillOnVulkan(R.string.error_system_ui_still_on_vulkan),
    KeyboardNotRestored(R.string.error_keyboard_not_restored),
    AppsNotListed(R.string.error_apps_not_listed),
    LicencesUnreadable(R.string.error_licences_unreadable),
    Unexpected(R.string.error_unexpected),
}

fun AppError.type(): AppErrorType = when (this) {
    ConnectionError.NoWifi -> AppErrorType.NoWifi
    ConnectionError.WirelessDebuggingOff -> AppErrorType.WirelessDebuggingOff
    ConnectionError.WirelessDebuggingRefused -> AppErrorType.WirelessDebuggingRefused
    ConnectionError.LocalNetworkDenied, PairingError.LocalNetworkDenied -> AppErrorType.LocalNetworkDenied
    ConnectionError.DaemonNotFound -> AppErrorType.DaemonNotFound
    ConnectionError.NotAuthorised -> AppErrorType.NotAuthorised
    ConnectionError.ConnectFailed -> AppErrorType.ConnectFailed
    ConnectionError.ShizukuNotRunning -> AppErrorType.ShizukuNotRunning
    ConnectionError.ShizukuNotAllowed -> AppErrorType.ShizukuNotAllowed
    ConnectionError.ShizukuShellFailed -> AppErrorType.ShizukuShellFailed
    ShellError.NotConnected, ShellError.ConnectionLost -> AppErrorType.ConnectionLost
    ShellError.TimedOut -> AppErrorType.CommandTimedOut
    ShellError.Failed -> AppErrorType.ShellFailed
    PairingError.InvalidCode -> AppErrorType.InvalidCode
    PairingError.DialogClosed -> AppErrorType.PairingDialogClosed
    PairingError.WrongCode -> AppErrorType.WrongCode
    PairingError.TimedOut -> AppErrorType.PairingTimedOut
    PairingError.Failed -> AppErrorType.PairingFailed
    SetupError.GrantFailed -> AppErrorType.GrantFailed
    is ApplyError.CommandFailed -> AppErrorType.CommandFailed
    ApplyError.NoPackagesListed -> AppErrorType.NoPackagesListed
    ApplyError.RecentAppsUnknown -> AppErrorType.RecentAppsUnknown
    is ApplyError.SettingNotRestored -> AppErrorType.SettingNotRestored
    ApplyError.SystemUiRestartedRecently -> AppErrorType.SystemUiRestartedRecently
    ApplyError.SystemUiNotOnVulkan -> AppErrorType.SystemUiNotOnVulkan
    ApplyError.SystemUiStillOnVulkan -> AppErrorType.SystemUiStillOnVulkan
    ApplyError.KeyboardNotRestored -> AppErrorType.KeyboardNotRestored
    SettingsError.AppsNotListed -> AppErrorType.AppsNotListed
    SettingsError.LicencesUnreadable -> AppErrorType.LicencesUnreadable
    else -> AppErrorType.Unexpected
}

/** The message for this error. State holds the [AppError]; the UI calls `stringResource(error.messageRes())`. */
@StringRes
fun AppError.messageRes(): Int = type().resource
