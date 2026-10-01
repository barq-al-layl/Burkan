package io.github.barqallayl.burkan.core.ui

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.provider.Settings

/** Opens a Settings screen, or Settings itself if this phone does not have that one. */
fun Context.openSettings(intent: Intent) {
    try {
        startActivity(intent)
    } catch (_: ActivityNotFoundException) {
        startActivity(Intent(Settings.ACTION_SETTINGS))
    }
}
