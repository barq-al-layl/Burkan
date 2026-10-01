package io.github.barqallayl.burkan.feature.apply

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.net.ConnectivityManager
import android.net.Network
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesIntoMap
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.binding
import dev.zacsweers.metrox.android.BroadcastReceiverKey
import io.github.barqallayl.burkan.feature.apply.data.AutoApply
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/**
 * Starts the automatic apply after a restart. `BOOT_COMPLETED` arrives after the first unlock, when encrypted storage
 * is readable; the app does nothing before it.
 */
@Inject
@BroadcastReceiverKey
@ContributesIntoMap(AppScope::class, binding = binding<BroadcastReceiver>())
class BootReceiver(private val autoApply: AutoApply) : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED) return
        goAsync { autoApply.onBoot() }
    }
}

/** Receives the Wi-Fi network callback registered by the automatic apply while it waits. */
@Inject
@BroadcastReceiverKey
@ContributesIntoMap(AppScope::class, binding = binding<BroadcastReceiver>())
class NetworkReceiver(private val autoApply: AutoApply) : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val network = intent.getParcelableExtra(ConnectivityManager.EXTRA_NETWORK, Network::class.java)
        goAsync { autoApply.onWifiAvailable(network?.networkHandle) }
    }
}

/** Runs [block] off the main thread while keeping the broadcast, and so the process, alive until it ends. */
private fun BroadcastReceiver.goAsync(block: suspend () -> Unit) {
    val pending = goAsync()
    CoroutineScope(SupervisorJob() + Dispatchers.Default).launch {
        try {
            block()
        } finally {
            pending.finish()
        }
    }
}
