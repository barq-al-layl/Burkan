package io.github.barqallayl.burkan.feature.apply.data

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import co.touchlab.kermit.Logger
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesBinding
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.Named
import dev.zacsweers.metro.SingleIn
import io.github.barqallayl.burkan.core.storage.DataStoreDeviceStateStorage
import io.github.barqallayl.burkan.feature.apply.model.RestoredSetting
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import java.io.IOException

/**
 * The settings a full apply captured before it changed anything, kept until they are back.
 *
 * A run puts them back through the shell, at its end. This is for when it cannot: the connection has gone for good,
 * or the app's process died under the run. The app holds `WRITE_SECURE_SETTINGS`, so it can write them back itself,
 * through [DeviceSettings].
 */
@Inject
class CapturedSettings(private val storage: CapturedSettingsStorage, private val settings: DeviceSettings) {

    /** Called before a run changes anything, so that what it captured outlives the run's process. */
    suspend fun keep(values: Map<RestoredSetting, String>) {
        if (values.isNotEmpty()) storage.set(values)
    }

    /** Every value is back where it was, so none is kept any longer. */
    suspend fun letGo() = storage.set(emptyMap())

    /** Writes one value back without the shell. False when Android would not take it. */
    fun putBack(setting: RestoredSetting, value: String): Boolean = settings.put(setting.key, value)

    /**
     * Puts back whatever is still kept: what a run left behind when its process died under it, or when neither the
     * shell nor the app could write a value at the time. Each is tried once and then let go of, as a value Android
     * refuses now it would refuse again.
     */
    suspend fun putBackKept() {
        val kept = storage.get()
        if (kept.isEmpty()) return
        kept.forEach { (setting, value) ->
            if (!putBack(setting, value)) Logger.w { "Could not put $setting back" }
        }
        letGo()
    }
}

/** Where [CapturedSettings] keeps the values. Setting none removes them all. */
interface CapturedSettingsStorage {
    suspend fun get(): Map<RestoredSetting, String>

    suspend fun set(values: Map<RestoredSetting, String>)
}

/** Kept with the device state, which is not backed up: the values belong to this phone, and one names its apps. */
@Inject
@SingleIn(AppScope::class)
@ContributesBinding(AppScope::class)
class DataStoreCapturedSettingsStorage(
    @Named(DataStoreDeviceStateStorage.DEVICE_STATE_STORE) private val dataStore: DataStore<Preferences>,
) : CapturedSettingsStorage {

    override suspend fun get(): Map<RestoredSetting, String> {
        val preferences = dataStore.data
            .catch { error -> if (error is IOException) emit(emptyPreferences()) else throw error }
            .first()
        return RestoredSetting.entries
            .mapNotNull { setting -> preferences[key(setting)]?.let { setting to it } }
            .toMap()
    }

    override suspend fun set(values: Map<RestoredSetting, String>) {
        dataStore.edit { preferences ->
            RestoredSetting.entries.forEach { setting ->
                val value = values[setting]
                if (value == null) preferences.remove(key(setting)) else preferences[key(setting)] = value
            }
        }
    }

    private fun key(setting: RestoredSetting) = stringPreferencesKey("captured_${setting.name}")
}
