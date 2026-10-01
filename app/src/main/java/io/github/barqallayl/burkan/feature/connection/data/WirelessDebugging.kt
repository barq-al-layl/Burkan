package io.github.barqallayl.burkan.feature.connection.data

import android.Manifest
import android.app.Application
import android.content.pm.PackageManager
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.provider.Settings
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesBinding
import dev.zacsweers.metro.Inject

/** The phone's wireless-debugging switch and what it depends on. */
interface WirelessDebugging {
    fun isOn(): Boolean

    /** Writes the switch. False when the app does not hold `WRITE_SECURE_SETTINGS`, so nothing was written. */
    fun set(on: Boolean): Boolean

    fun canSwitch(): Boolean

    fun isWifiConnected(): Boolean
}

@Inject
@ContributesBinding(AppScope::class)
class SettingsWirelessDebugging(private val application: Application) : WirelessDebugging {

    override fun isOn(): Boolean = Settings.Global.getInt(application.contentResolver, ADB_WIFI_ENABLED, 0) == 1

    override fun set(on: Boolean): Boolean {
        if (!canSwitch()) return false
        return Settings.Global.putInt(application.contentResolver, ADB_WIFI_ENABLED, if (on) 1 else 0)
    }

    override fun canSwitch(): Boolean =
        application.checkSelfPermission(Manifest.permission.WRITE_SECURE_SETTINGS) == PackageManager.PERMISSION_GRANTED

    override fun isWifiConnected(): Boolean {
        val connectivity = application.getSystemService(ConnectivityManager::class.java)
        val capabilities = connectivity.getNetworkCapabilities(connectivity.activeNetwork) ?: return false
        return capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)
    }

    private companion object {
        /** `Settings.Global.ADB_WIFI_ENABLED`, which is hidden from the SDK. */
        const val ADB_WIFI_ENABLED = "adb_wifi_enabled"
    }
}
