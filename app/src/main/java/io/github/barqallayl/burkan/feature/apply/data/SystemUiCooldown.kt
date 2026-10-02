package io.github.barqallayl.burkan.feature.apply.data

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.longPreferencesKey
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesBinding
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.Named
import dev.zacsweers.metro.SingleIn
import io.github.barqallayl.burkan.core.storage.DataStoreDeviceStateStorage
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.io.IOException
import kotlin.time.Clock
import kotlin.time.Duration.Companion.seconds
import kotlin.time.Instant

/** When System UI was last restarted by this app. */
interface SystemUiRestarts {
    suspend fun last(): Instant?

    suspend fun record(at: Instant)
}

/** Kept with the device state, so a process that died just after a restart still waits out the minute. */
@Inject
@SingleIn(AppScope::class)
@ContributesBinding(AppScope::class)
class DataStoreSystemUiRestarts(
    @Named(DataStoreDeviceStateStorage.DEVICE_STATE_STORE) private val dataStore: DataStore<Preferences>,
) : SystemUiRestarts {

    override suspend fun last(): Instant? = dataStore.data
        .catch { error -> if (error is IOException) emit(emptyPreferences()) else throw error }
        .first()[LAST_RESTART]
        ?.let(Instant::fromEpochMilliseconds)

    override suspend fun record(at: Instant) {
        dataStore.edit { it[LAST_RESTART] = at.toEpochMilliseconds() }
    }

    private companion object {
        val LAST_RESTART = longPreferencesKey("system_ui_last_restart")
    }
}

/**
 * Never restarts System UI twice within [COOLDOWN]: One UI switches off its customisation add-ons when System UI
 * crashes repeatedly.
 */
@Inject
@SingleIn(AppScope::class)
class SystemUiCooldown(private val restarts: SystemUiRestarts, private val clock: Clock) {

    private val mutex = Mutex()

    /**
     * Records a restart now and returns true, unless one was recorded within [COOLDOWN]. A restart is recorded before
     * it is sent: if the app dies with it, the next run must still wait.
     */
    suspend fun claim(): Boolean = mutex.withLock {
        val now = clock.now()
        val last = restarts.last()
        // A recorded time in the future means the clock was set back; it says nothing about the last minute.
        if (last != null && last <= now && now - last < COOLDOWN) return false
        restarts.record(now)
        true
    }

    companion object {
        val COOLDOWN = 60.seconds
    }
}
