package io.github.barqallayl.burkan.feature.connection.data

import android.app.Application
import android.content.pm.PackageManager
import android.os.Build
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesBinding
import dev.zacsweers.metro.Inject
import io.github.muntashirakon.adb.android.AdbMdns
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.time.Duration

/** The two services wireless debugging advertises over mDNS. */
enum class AdbService(val type: String) {
    /** `_adb-tls-pairing._tcp`: only while the "Pair device with pairing code" dialog is open. */
    Pairing(AdbMdns.SERVICE_TYPE_TLS_PAIRING),

    /** `_adb-tls-connect._tcp`: while wireless debugging is on. The port changes every time. */
    Connect(AdbMdns.SERVICE_TYPE_TLS_CONNECT),
}

/** Where a service listens. [host] is the phone's own Wi-Fi address. */
data class AdbEndpoint(val host: String, val port: Int)

interface AdbDiscovery {
    /**
     * Whether the app may look for services on the network. From Android 17 that takes [LOCAL_NETWORK_PERMISSION];
     * an app without it that looks anyway gets the system's device picker over whatever is on screen, and opening it
     * closes the pairing dialog. So nothing looks without it.
     */
    fun isAllowed(): Boolean

    /** The service on this phone, or null if it is not advertised within [timeout]. */
    suspend fun find(service: AdbService, timeout: Duration): AdbEndpoint?
}

/** libadb's mDNS helper, which keeps only services on this phone's own addresses. */
@Inject
@ContributesBinding(AppScope::class)
class NsdAdbDiscovery(private val application: Application) : AdbDiscovery {

    override fun isAllowed(): Boolean = Build.VERSION.SDK_INT < Build.VERSION_CODES.CINNAMON_BUN ||
        application.checkSelfPermission(LOCAL_NETWORK_PERMISSION) == PackageManager.PERMISSION_GRANTED

    override suspend fun find(service: AdbService, timeout: Duration): AdbEndpoint? {
        val found = CompletableDeferred<AdbEndpoint>()
        val mdns = AdbMdns(application, service.type) { host, port ->
            // A port of -1 means the service went away.
            val address = host?.hostAddress
            if (address != null && port > 0) found.complete(AdbEndpoint(address, port))
        }
        mdns.start()
        return try {
            withTimeoutOrNull(timeout) { found.await() }
        } finally {
            mdns.stop()
        }
    }
}

/**
 * `ACCESS_LOCAL_NETWORK`, a runtime permission from Android 17 in the Nearby devices group. Before that, `INTERNET`
 * carries it.
 */
const val LOCAL_NETWORK_PERMISSION = "android.permission.ACCESS_LOCAL_NETWORK"
