package io.github.barqallayl.burkan.core.ui

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
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
