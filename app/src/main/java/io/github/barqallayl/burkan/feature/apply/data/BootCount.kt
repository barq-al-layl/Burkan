package io.github.barqallayl.burkan.feature.apply.data

import android.app.Application
import android.os.SystemClock
import android.provider.Settings
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesBinding
import dev.zacsweers.metro.Inject

/** Which boot the phone is in, to tell a restart from a `BOOT_COMPLETED` sent without one. */
interface BootCount {
    /**
     * A number that is the same for as long as the phone stays up and different after a restart: how many times the
     * phone has booted, or failing that the minute it last started. Null when neither can be told.
     */
    fun current(): Int?
}

/**
 * `Settings.Global.BOOT_COUNT`, which the system raises by one on every boot. A phone that does not give it is told
 * apart by when it started instead: the clock, less the time the phone has been up, to the minute. Without that,
 * every `BOOT_COMPLETED` would count as a restart, and Android sends one after a force-stop too.
 */
@Inject
@ContributesBinding(AppScope::class)
class SettingsBootCount(private val application: Application) : BootCount {

    override fun current(): Int? =
        Settings.Global.getInt(application.contentResolver, Settings.Global.BOOT_COUNT, UNKNOWN)
            .takeIf { it != UNKNOWN }
            ?: bootMinute(System.currentTimeMillis(), SystemClock.elapsedRealtime())

    private companion object {
        const val UNKNOWN = -1
    }
}

/** The minute the phone started, counted from 1970, from the clock [nowMillis] and the time it has been [upMillis]. */
fun bootMinute(nowMillis: Long, upMillis: Long): Int = ((nowMillis - upMillis) / MILLIS_IN_MINUTE).toInt()

private const val MILLIS_IN_MINUTE = 60_000L
