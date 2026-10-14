package io.github.barqallayl.burkan.core.ui

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.os.Build
import android.provider.Settings

/**
 * Opens a Settings screen, or Settings itself if this phone does not have that one.
 *
 * It opens as Settings' own window, not as a screen of this app: started plainly it would sit on top of the app in
 * the app's own entry under recent apps, and coming back to the app would mean backing out of Settings.
 */
fun Context.openSettings(intent: Intent) {
    try {
        startActivity(Intent(intent).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    } catch (_: ActivityNotFoundException) {
        startActivity(Intent(Settings.ACTION_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    }
}

/**
 * The Settings screen [action] names, asked to scroll to the row called [key] and mark it out, so that the one
 * switch the user was sent for does not have to be looked for among a hundred others.
 *
 * Settings has done this for its own search results since Android 8, by an extra it never published. A phone whose
 * Settings does not know the extra, or has no row of that name, opens the screen as it always did.
 */
fun settingsIntent(action: String, key: String): Intent = Intent(action).putExtra(EXTRA_FRAGMENT_ARG_KEY, key)

/** Developer options, at the Wireless debugging row. */
fun wirelessDebuggingIntent(): Intent =
    settingsIntent(Settings.ACTION_APPLICATION_DEVELOPMENT_SETTINGS, KEY_WIRELESS_DEBUGGING)

/**
 * About phone, at the way to the build number, which is tapped seven times to bring Developer options out. Android's
 * own Settings has the build number on that screen; Samsung's keeps it one screen further in, under Software
 * information, so there it is that row which is marked.
 */
fun buildNumberIntent(): Intent = settingsIntent(
    Settings.ACTION_DEVICE_INFO_SETTINGS,
    if (Build.MANUFACTURER.equals(SAMSUNG, ignoreCase = true)) KEY_SOFTWARE_INFORMATION else KEY_BUILD_NUMBER,
)

private const val EXTRA_FRAGMENT_ARG_KEY = ":settings:fragment_args_key"

/** The rows' names in Android's own Settings, and in Samsung's. */
private const val KEY_WIRELESS_DEBUGGING = "toggle_adb_wireless"
private const val KEY_BUILD_NUMBER = "build_number"
private const val KEY_SOFTWARE_INFORMATION = "software_info"
private const val SAMSUNG = "samsung"
