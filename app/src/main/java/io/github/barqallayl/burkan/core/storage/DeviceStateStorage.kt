package io.github.barqallayl.burkan.core.storage

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.intPreferencesKey
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesBinding
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.Named
import dev.zacsweers.metro.SingleIn
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import java.io.IOException

/**
 * State that belongs to this phone and its pairing, kept apart from the settings because it must not be backed up:
 * restored on another phone it would claim a pairing that phone never made.
 */
interface DeviceStateStorage {
    /** Whether this app's key has been paired with the phone's wireless debugging. */
    val isPaired: Flow<Boolean>

    /** How many setup steps the user has finished, counted from the first. */
    val setupStepsDone: Flow<Int>

    /** Whether every setup step is done. `App.kt` chooses the start screen from it. */
    val isSetupComplete: Flow<Boolean>

    suspend fun setPaired(paired: Boolean)

    suspend fun setSetupStepsDone(count: Int)

    suspend fun setSetupComplete(complete: Boolean)
}

@Inject
@SingleIn(AppScope::class)
@ContributesBinding(AppScope::class)
class DataStoreDeviceStateStorage(
    @Named(DEVICE_STATE_STORE) private val dataStore: DataStore<Preferences>,
) : DeviceStateStorage {

    private val preferences: Flow<Preferences> = dataStore.data.catch { error ->
        if (error is IOException) emit(emptyPreferences()) else throw error
    }

    override val isPaired: Flow<Boolean> = read(Keys.paired)
    override val setupStepsDone: Flow<Int> =
        preferences.map { it[Keys.setupStepsDone] ?: 0 }.distinctUntilChanged()
    override val isSetupComplete: Flow<Boolean> = read(Keys.setupComplete)

    override suspend fun setPaired(paired: Boolean) = write(Keys.paired, paired)

    override suspend fun setSetupStepsDone(count: Int) {
        dataStore.edit { it[Keys.setupStepsDone] = count }
    }

    override suspend fun setSetupComplete(complete: Boolean) = write(Keys.setupComplete, complete)

    private fun read(key: Preferences.Key<Boolean>): Flow<Boolean> =
        preferences.map { it[key] ?: false }.distinctUntilChanged()

    private suspend fun write(key: Preferences.Key<Boolean>, value: Boolean) {
        dataStore.edit { it[key] = value }
    }

    private object Keys {
        val paired = booleanPreferencesKey("paired")
        val setupStepsDone = intPreferencesKey("setup_steps_done")
        val setupComplete = booleanPreferencesKey("setup_complete")
    }

    companion object {
        /** Names the DataStore file, which the backup rules exclude by this name. */
        const val DEVICE_STATE_STORE = "device_state"
    }
}
