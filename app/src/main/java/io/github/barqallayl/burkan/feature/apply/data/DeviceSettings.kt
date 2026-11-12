package io.github.barqallayl.burkan.feature.apply.data

import android.app.Application
import android.provider.Settings
import co.touchlab.kermit.Logger
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesBinding
import dev.zacsweers.metro.Inject
import io.github.barqallayl.burkan.core.shell.SettingKey
import io.github.barqallayl.burkan.core.shell.SettingsNamespace

/** What the app itself read of a setting. */
sealed interface SettingRead {
    /** The setting as the phone holds it; [value] is null when it is unset. */
    data class Value(val value: String?) : SettingRead

    /** Android would not let the app read it. */
    data object Refused : SettingRead
}

/** The phone's settings, read and written by the app itself rather than through the shell. */
interface DeviceSettings {
    /** Reads [key] from the phone. Nothing but Android stands between the app and the answer. */
    fun get(key: SettingKey): SettingRead

    /** Writes [value] to [key]. False when Android would not take it. */
    fun put(key: SettingKey, value: String): Boolean
}

/**
 * Through the settings provider, which lets an app holding `WRITE_SECURE_SETTINGS` write the secure settings and the
 * system ones. The app is granted that permission during setup.
 */
@Inject
@ContributesBinding(AppScope::class)
class ProviderDeviceSettings(private val application: Application) : DeviceSettings {

    override fun get(key: SettingKey): SettingRead = try {
        val resolver = application.contentResolver
        SettingRead.Value(
            when (key.namespace) {
                SettingsNamespace.System -> Settings.System.getString(resolver, key.name)
                SettingsNamespace.Secure -> Settings.Secure.getString(resolver, key.name)
            },
        )
    } catch (e: SecurityException) {
        Logger.w(e) { "Android would not let the app read ${key.name}" }
        SettingRead.Refused
    }

    override fun put(key: SettingKey, value: String): Boolean = try {
        val resolver = application.contentResolver
        when (key.namespace) {
            SettingsNamespace.System -> Settings.System.putString(resolver, key.name, value)
            SettingsNamespace.Secure -> Settings.Secure.putString(resolver, key.name, value)
        }
    } catch (e: SecurityException) {
        Logger.w(e) { "Android would not let the app write ${key.name}" }
        false
    } catch (e: IllegalArgumentException) {
        // How the provider refuses a key it does not let an app write.
        Logger.w(e) { "Android would not let the app write ${key.name}" }
        false
    }
}
