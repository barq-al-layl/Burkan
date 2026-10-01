package io.github.barqallayl.burkan.feature.connection.data

import arrow.core.Either
import arrow.core.raise.Raise
import arrow.core.raise.either
import arrow.core.raise.ensure
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesBinding
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn
import io.github.barqallayl.burkan.core.model.AppError
import io.github.barqallayl.burkan.core.shell.AdbShellExecutor
import io.github.barqallayl.burkan.core.shell.ShellExecutor
import io.github.barqallayl.burkan.core.storage.DeviceStateStorage
import io.github.barqallayl.burkan.core.storage.SettingsStorage
import io.github.barqallayl.burkan.feature.connection.model.ConnectionError
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlin.time.Duration.Companion.seconds

/** The one way to a shell on the phone. */
interface ShellAccess {
    /**
     * Connects, runs [block] with a shell, and disconnects. Only one block runs at a time: a run holds the connection
     * from start to finish.
     */
    suspend fun <T> withShell(block: suspend (ShellExecutor) -> T): Either<AppError, T>
}

/**
 * Gets a shell through the phone's own wireless debugging: switches it on if the app may and must, finds the port,
 * connects on loopback with the Wi-Fi address as fallback, and switches wireless debugging off again afterwards if
 * it was the one that switched it on and the user has not asked to keep it on.
 */
@Inject
@SingleIn(AppScope::class)
@ContributesBinding(AppScope::class)
class AdbShellAccess(
    private val client: AdbClient,
    private val discovery: AdbDiscovery,
    private val wirelessDebugging: WirelessDebugging,
    private val deviceState: DeviceStateStorage,
    private val settings: SettingsStorage,
) : ShellAccess {

    private val mutex = Mutex()

    override suspend fun <T> withShell(block: suspend (ShellExecutor) -> T): Either<AppError, T> = mutex.withLock {
        either {
            val switchedOn = switchOn()
            try {
                val endpoint = discovery.find(AdbService.Connect, DISCOVERY_TIMEOUT)
                    ?: raise(ConnectionError.DaemonNotFound)
                connect(endpoint)
                try {
                    block(AdbShellExecutor(client))
                } finally {
                    withContext(NonCancellable) { client.disconnect() }
                }
            } finally {
                if (switchedOn) {
                    withContext(NonCancellable) {
                        if (settings.turnOffWirelessDebugging.first()) wirelessDebugging.set(false)
                    }
                }
            }
        }
    }

    /** True when this call switched wireless debugging on, and so must switch it off. */
    private suspend fun Raise<ConnectionError>.switchOn(): Boolean {
        if (wirelessDebugging.isOn()) return false
        ensure(wirelessDebugging.isWifiConnected()) { ConnectionError.NoWifi }
        ensure(wirelessDebugging.set(true)) { ConnectionError.WirelessDebuggingOff }
        // The system may refuse (an untrusted network, or Wi-Fi lost) and put the switch back by itself.
        delay(SETTLE_TIME)
        ensure(wirelessDebugging.isOn()) { ConnectionError.WirelessDebuggingRefused }
        return true
    }

    private suspend fun Raise<ConnectionError>.connect(endpoint: AdbEndpoint) {
        val loopback = client.connect(LOOPBACK, endpoint.port)
        val result = if (loopback == Either.Left(ConnectionError.ConnectFailed)) {
            client.connect(endpoint.host, endpoint.port)
        } else {
            loopback
        }
        result.onLeft { error ->
            // The daemon no longer accepts the key, so setup has to pair again.
            if (error == ConnectionError.NotAuthorised) deviceState.setPaired(false)
            raise(error)
        }
    }

    companion object {
        const val LOOPBACK = "127.0.0.1"
        val SETTLE_TIME = 2.seconds
        val DISCOVERY_TIMEOUT = 30.seconds
    }
}
