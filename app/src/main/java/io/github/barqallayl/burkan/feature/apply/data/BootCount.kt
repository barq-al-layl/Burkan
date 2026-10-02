package io.github.barqallayl.burkan.feature.apply.data

import android.app.Application
import android.provider.Settings
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesBinding
import dev.zacsweers.metro.Inject

/** Which boot the phone is in, to tell a restart from a `BOOT_COMPLETED` sent without one. */
interface BootCount {
    /** How many times the phone has booted, this boot included; null when the system does not say. */
    fun current(): Int?
}

/** `Settings.Global.BOOT_COUNT`, which the system raises by one on every boot. */
@Inject
@ContributesBinding(AppScope::class)
class SettingsBootCount(private val application: Application) : BootCount {

    override fun current(): Int? =
        Settings.Global.getInt(application.contentResolver, Settings.Global.BOOT_COUNT, UNKNOWN)
            .takeIf { it != UNKNOWN }

    private companion object {
        const val UNKNOWN = -1
    }
}
