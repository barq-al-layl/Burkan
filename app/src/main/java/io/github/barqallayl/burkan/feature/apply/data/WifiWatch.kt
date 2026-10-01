package io.github.barqallayl.burkan.feature.apply.data

import android.app.Application
import android.app.PendingIntent
import android.content.Intent
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesBinding
import dev.zacsweers.metro.Inject
import io.github.barqallayl.burkan.feature.apply.NetworkReceiver

/** Wakes the app when a Wi-Fi network comes up, also after its process has gone, until stopped or the next restart. */
interface WifiWatch {
    fun start()

    fun stop()

    /** The handle of the network the phone uses now, or null without one. */
    fun activeNetwork(): Long?
}

/**
 * A network callback registered with a [PendingIntent] rather than a callback object: the system holds it and
 * delivers to [NetworkReceiver], so the wait outlives the app's process.
 */
@Inject
@ContributesBinding(AppScope::class)
class ConnectivityWifiWatch(private val application: Application) : WifiWatch {

    private val connectivity: ConnectivityManager
        get() = application.getSystemService(ConnectivityManager::class.java)

    override fun start() {
        // Registering the same intent twice would hold two requests; replace instead.
        stop()
        val request = NetworkRequest.Builder().addTransportType(NetworkCapabilities.TRANSPORT_WIFI).build()
        connectivity.registerNetworkCallback(request, callbackIntent())
    }

    override fun stop() {
        try {
            connectivity.unregisterNetworkCallback(callbackIntent())
        } catch (_: IllegalArgumentException) {
            // Nothing was registered.
        }
    }

    override fun activeNetwork(): Long? = connectivity.activeNetwork?.networkHandle

    // Mutable, because the system adds the network to the intent it sends. The intent is explicit, so nothing else
    // can receive it.
    private fun callbackIntent(): PendingIntent = PendingIntent.getBroadcast(
        application,
        0,
        Intent(application, NetworkReceiver::class.java),
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE,
    )
}
