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
enum class AppErrorType(@StringRes val resource: Int) {
    NoWifi(R.string.error_no_wifi),
    WirelessDebuggingOff(R.string.error_wireless_debugging_off),
    WirelessDebuggingRefused(R.string.error_wireless_debugging_refused),
    DaemonNotFound(R.string.error_daemon_not_found),
    NotAuthorised(R.string.error_not_authorised),
    ConnectFailed(R.string.error_connect_failed),
    ConnectionLost(R.string.error_connection_lost),
    CommandTimedOut(R.string.error_command_timed_out),
    InvalidCode(R.string.error_invalid_code),
    PairingDialogClosed(R.string.error_pairing_dialog_closed),
    WrongCode(R.string.error_wrong_code),
    PairingTimedOut(R.string.error_pairing_timed_out),
    PairingFailed(R.string.error_pairing_failed),
    GrantFailed(R.string.error_grant_failed),
    CommandFailed(R.string.error_command_failed),
    NoPackagesListed(R.string.error_no_packages_listed),
    RecentAppsUnknown(R.string.error_recent_apps_unknown),
    SettingNotRestored(R.string.error_setting_not_restored),
    SystemUiRestartedRecently(R.string.error_system_ui_restarted_recently),
    SystemUiNotOnVulkan(R.string.error_system_ui_not_on_vulkan),
    KeyboardNotRestored(R.string.error_keyboard_not_restored),
    AppsNotListed(R.string.error_apps_not_listed),
    LicencesUnreadable(R.string.error_licences_unreadable),
    Unexpected(R.string.error_unexpected),
}

fun AppError.type(): AppErrorType = when (this) {
    ConnectionError.NoWifi -> AppErrorType.NoWifi
    ConnectionError.WirelessDebuggingOff -> AppErrorType.WirelessDebuggingOff
    ConnectionError.WirelessDebuggingRefused -> AppErrorType.WirelessDebuggingRefused
    ConnectionError.DaemonNotFound -> AppErrorType.DaemonNotFound
    ConnectionError.NotAuthorised -> AppErrorType.NotAuthorised
    ConnectionError.ConnectFailed -> AppErrorType.ConnectFailed
    ShellError.NotConnected, ShellError.ConnectionLost -> AppErrorType.ConnectionLost
    ShellError.TimedOut -> AppErrorType.CommandTimedOut
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
    ApplyError.KeyboardNotRestored -> AppErrorType.KeyboardNotRestored
    SettingsError.AppsNotListed -> AppErrorType.AppsNotListed
    SettingsError.LicencesUnreadable -> AppErrorType.LicencesUnreadable
    else -> AppErrorType.Unexpected
}

/** The message for this error. State holds the [AppError]; the UI calls `stringResource(error.messageRes())`. */
@StringRes
fun AppError.messageRes(): Int = type().resource
