package io.github.barqallayl.burkan.feature.connection.data

import android.app.Application
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
    /** The service on this phone, or null if it is not advertised within [timeout]. */
    suspend fun find(service: AdbService, timeout: Duration): AdbEndpoint?
}

/** libadb's mDNS helper, which keeps only services on this phone's own addresses. */
@Inject
@ContributesBinding(AppScope::class)
class NsdAdbDiscovery(private val application: Application) : AdbDiscovery {

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
