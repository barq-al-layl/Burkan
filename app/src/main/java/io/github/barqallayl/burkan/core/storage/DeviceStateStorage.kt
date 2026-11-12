package io.github.barqallayl.burkan.core.storage

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
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

    /** Which list of steps [setupStepsDone] counts in: `SETUP_STEPS_VERSION`, or 1 for a count from before it. */
    val setupStepsVersion: Flow<Int>

    /** Whether every setup step is done. `App.kt` chooses the start screen from it. */
    val isSetupComplete: Flow<Boolean>

    /** Whether the Welcome screen has been seen, or need not be: it opens a fresh install, and only that. */
    val isWelcomeSeen: Flow<Boolean>

    /** Setup has just been completed and its First apply is still on offer; Home waits until it is taken or put off. */
    val isFirstApplyPending: Flow<Boolean>

    /**
     * Whether wireless debugging is on because this app switched it on, from just before it does until it is
     * switched off again. Kept on disk so that a process that dies in between leaves the switch to the next one.
     */
    val switchedOnWirelessDebugging: Flow<Boolean>

    suspend fun setPaired(paired: Boolean)

    suspend fun setSetupStepsDone(count: Int)

    /** Stores [count] as counted in [version] of the steps. */
    suspend fun setSetupStepsDone(count: Int, version: Int)

    suspend fun setSetupComplete(complete: Boolean)

    suspend fun setWelcomeSeen(seen: Boolean)

    suspend fun setFirstApplyPending(pending: Boolean)

    suspend fun setSwitchedOnWirelessDebugging(switchedOn: Boolean)
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
    override val setupStepsVersion: Flow<Int> =
        preferences.map { it[Keys.setupStepsVersion] ?: 1 }.distinctUntilChanged()
    override val isSetupComplete: Flow<Boolean> = read(Keys.setupComplete)
    override val isWelcomeSeen: Flow<Boolean> = read(Keys.welcomeSeen)
    override val isFirstApplyPending: Flow<Boolean> = read(Keys.firstApplyPending)
    override val switchedOnWirelessDebugging: Flow<Boolean> = read(Keys.switchedOnWirelessDebugging)

    override suspend fun setPaired(paired: Boolean) = write(Keys.paired, paired)

    override suspend fun setSetupStepsDone(count: Int) {
        dataStore.editOrLog { it[Keys.setupStepsDone] = count }
    }

    override suspend fun setSetupStepsDone(count: Int, version: Int) {
        dataStore.editOrLog {
            it[Keys.setupStepsDone] = count
            it[Keys.setupStepsVersion] = version
        }
    }

    override suspend fun setSetupComplete(complete: Boolean) = write(Keys.setupComplete, complete)

    override suspend fun setWelcomeSeen(seen: Boolean) = write(Keys.welcomeSeen, seen)

    override suspend fun setFirstApplyPending(pending: Boolean) = write(Keys.firstApplyPending, pending)

    override suspend fun setSwitchedOnWirelessDebugging(switchedOn: Boolean) =
        write(Keys.switchedOnWirelessDebugging, switchedOn)

    private fun read(key: Preferences.Key<Boolean>): Flow<Boolean> =
        preferences.map { it[key] ?: false }.distinctUntilChanged()

    private suspend fun write(key: Preferences.Key<Boolean>, value: Boolean) {
        dataStore.editOrLog { it[key] = value }
    }

    private object Keys {
        val paired = booleanPreferencesKey("paired")
        val setupStepsDone = intPreferencesKey("setup_steps_done")
        val setupStepsVersion = intPreferencesKey("setup_steps_version")
        val setupComplete = booleanPreferencesKey("setup_complete")
        val welcomeSeen = booleanPreferencesKey("welcome_seen")
        val firstApplyPending = booleanPreferencesKey("first_apply_pending")
        val switchedOnWirelessDebugging = booleanPreferencesKey("switched_on_wireless_debugging")
    }

    companion object {
        /** Names the DataStore file, which the backup rules exclude by this name. */
        const val DEVICE_STATE_STORE = "device_state"
    }
}
