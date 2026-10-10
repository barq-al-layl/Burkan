package io.github.barqallayl.burkan.feature.apply.data

import android.app.AlarmManager
import android.app.Application
import android.app.PendingIntent
import android.content.Intent
import android.net.ConnectivityManager
import android.os.SystemClock
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesBinding
import dev.zacsweers.metro.Inject
import io.github.barqallayl.burkan.feature.apply.NetworkReceiver
import io.github.barqallayl.burkan.feature.connection.data.wifiNetwork
import io.github.barqallayl.burkan.feature.connection.data.wifiNetworkRequest
import kotlin.time.Duration.Companion.minutes

/** Wakes the app when a Wi-Fi network comes up, also after its process has gone, until stopped or the next restart. */
interface WifiWatch {
    /** Starts watching, or starts again: a watch that has told of a network is used up by it. */
    fun start()

    fun stop()

    /** The handle of the Wi-Fi network the phone is connected to, or null when it is on none. */
    fun wifiNetwork(): Long?
}

/**
 * A network callback registered with a [PendingIntent] rather than a callback object: the system holds it and
 * delivers to [NetworkReceiver], so the wait outlives the app's process.
 *
 * Android tells such a callback of one network and then lets go of it, and it tells of the network that is up
 * already as readily as of one that comes up later. So a watch started on the network an attempt has just failed
 * on is used up at once, by news of that same network, and a different network connecting afterwards would be
 * told to nobody. An alarm therefore starts the watch again every [RENEW_AFTER], for as long as it is wanted. The
 * alarm does not wake the phone: on a phone that is asleep it waits until the phone is next awake.
 */
@Inject
@ContributesBinding(AppScope::class)
class ConnectivityWifiWatch(private val application: Application) : WifiWatch {

    private val connectivity: ConnectivityManager
        get() = application.getSystemService(ConnectivityManager::class.java)

    private val alarms: AlarmManager
        get() = application.getSystemService(AlarmManager::class.java)

    override fun start() {
        // Registering the same intent twice would hold two requests; replace instead.
        stop()
        connectivity.registerNetworkCallback(wifiNetworkRequest(), callbackIntent())
        val renewAt = SystemClock.elapsedRealtime() + RENEW_AFTER.inWholeMilliseconds
        alarms.set(AlarmManager.ELAPSED_REALTIME, renewAt, renewIntent())
    }

    override fun stop() {
        alarms.cancel(renewIntent())
        try {
            connectivity.unregisterNetworkCallback(callbackIntent())
        } catch (_: IllegalArgumentException) {
            // Nothing was registered.
        }
    }

    override fun wifiNetwork(): Long? = connectivity.wifiNetwork()?.networkHandle

    // Mutable, because the system adds the network to the intent it sends. The intent is explicit, so nothing else
    // can receive it.
    private fun callbackIntent(): PendingIntent = PendingIntent.getBroadcast(
        application,
        0,
        Intent(application, NetworkReceiver::class.java),
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE,
    )

    private fun renewIntent(): PendingIntent = PendingIntent.getBroadcast(
        application,
        0,
        Intent(application, NetworkReceiver::class.java).setAction(ACTION_RENEW),
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
    )

    companion object {
        /** Sent to [NetworkReceiver] by the alarm, to start the watch again. */
        const val ACTION_RENEW = "io.github.barqallayl.burkan.action.RENEW_WIFI_WATCH"

        /**
         * How long a watch stands before it is started again. Android may deliver the alarm later than this, and
         * does when the phone is asleep.
         */
        val RENEW_AFTER = 15.minutes
    }
}
