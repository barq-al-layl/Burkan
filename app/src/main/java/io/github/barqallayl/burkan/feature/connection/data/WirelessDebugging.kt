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

    /**
     * Whether Android forgets a paired device that has not connected for a while: 7 days, unless the user set
     * another time or switched on Developer options' "Disable adb authorization timeout". A forgotten app has to
     * pair again before it can connect. False when the phone will not say.
     */
    fun forgetsPairings(): Boolean
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

    override fun forgetsPairings(): Boolean = try {
        Settings.Global.getLong(application.contentResolver, ADB_ALLOWED_CONNECTION_TIME, DEFAULT_CONNECTION_TIME) != 0L
    } catch (_: SecurityException) {
        // A key Android does not let this app read: say nothing rather than warn on a guess.
        false
    }

    private companion object {
        /** `Settings.Global.ADB_WIFI_ENABLED`, which is hidden from the SDK. */
        const val ADB_WIFI_ENABLED = "adb_wifi_enabled"

        /**
         * `Settings.Global.ADB_ALLOWED_CONNECTION_TIME`, hidden from the SDK: how long, in milliseconds, a key that
         * has not connected stays authorised. 0 when the timeout is switched off.
         */
        const val ADB_ALLOWED_CONNECTION_TIME = "adb_allowed_connection_time"

        /** What Android takes when the key is unset: 7 days. */
        const val DEFAULT_CONNECTION_TIME = 7L * 24 * 60 * 60 * 1000
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
