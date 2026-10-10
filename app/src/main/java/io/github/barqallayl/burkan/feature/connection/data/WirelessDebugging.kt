package io.github.barqallayl.burkan.feature.connection.data

import android.Manifest
import android.app.Application
import android.content.pm.PackageManager
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
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

    /** Whether the phone is connected to a Wi-Fi network, which need not be the one it reaches the internet through. */
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

    override fun isWifiConnected(): Boolean =
        application.getSystemService(ConnectivityManager::class.java).wifiNetwork() != null

    private companion object {
        /** `Settings.Global.ADB_WIFI_ENABLED`, which is hidden from the SDK. */
        const val ADB_WIFI_ENABLED = "adb_wifi_enabled"
    }
}

/** What counts as a Wi-Fi network, both when asking for the one that is up and when waiting for one to come up. */
fun wifiNetworkRequest(): NetworkRequest =
    NetworkRequest.Builder().addTransportType(NetworkCapabilities.TRANSPORT_WIFI).build()

/**
 * The Wi-Fi network the phone is connected to, or null when it is on none.
 *
 * Not the network the phone reaches the internet through, which is still mobile data for a moment after Wi-Fi has
 * connected, and for as long as the Wi-Fi has no internet behind it. Wireless debugging needs Wi-Fi to be
 * connected, not to be the way out.
 */
// Deprecated for apps that would call it again and again to follow the networks. Asked once, it is the only way to
// hear of a network that is not the default one without waiting on a callback.
@Suppress("DEPRECATION")
fun ConnectivityManager.wifiNetwork(): Network? {
    val wifi = wifiNetworkRequest()
    return allNetworks.firstOrNull { network -> wifi.canBeSatisfiedBy(getNetworkCapabilities(network)) }
}
