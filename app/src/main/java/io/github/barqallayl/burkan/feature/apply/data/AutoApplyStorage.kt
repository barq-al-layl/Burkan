package io.github.barqallayl.burkan.feature.apply.data

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesBinding
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.Named
import dev.zacsweers.metro.SingleIn
import io.github.barqallayl.burkan.core.storage.DataStoreDeviceStateStorage
import io.github.barqallayl.burkan.feature.apply.model.AutoApplyState
import io.github.barqallayl.burkan.feature.apply.model.WaitReason
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import java.io.IOException

/**
 * Where the automatic apply stands. Kept with the device state, which is not backed up: it is about this phone's
 * current boot and its networks.
 */
interface AutoApplyStorage {
    val state: Flow<AutoApplyState>

    suspend fun set(state: AutoApplyState)
}

@Inject
@SingleIn(AppScope::class)
@ContributesBinding(AppScope::class)
class DataStoreAutoApplyStorage(
    @Named(DataStoreDeviceStateStorage.DEVICE_STATE_STORE) private val dataStore: DataStore<Preferences>,
) : AutoApplyStorage {

    override val state: Flow<AutoApplyState> = dataStore.data
        .catch { error -> if (error is IOException) emit(emptyPreferences()) else throw error }
        .map { preferences ->
            val waitingFor = preferences[Keys.waitingFor]
            AutoApplyState(
                waitingFor = WaitReason.entries.firstOrNull { it.name == waitingFor },
                triedNetwork = preferences[Keys.triedNetwork],
                systemUiAtNextLock = preferences[Keys.systemUiAtNextLock] ?: false,
            )
        }
        .distinctUntilChanged()

    override suspend fun set(state: AutoApplyState) {
        dataStore.edit { preferences ->
            preferences.remove(Keys.waitingFor)
            preferences.remove(Keys.triedNetwork)
            state.waitingFor?.let { preferences[Keys.waitingFor] = it.name }
            state.triedNetwork?.let { preferences[Keys.triedNetwork] = it }
            preferences[Keys.systemUiAtNextLock] = state.systemUiAtNextLock
        }
    }

    private object Keys {
        val waitingFor = stringPreferencesKey("auto_apply_waiting_for")
        val triedNetwork = longPreferencesKey("auto_apply_tried_network")
        val systemUiAtNextLock = booleanPreferencesKey("auto_apply_system_ui_at_next_lock")
    }
}
