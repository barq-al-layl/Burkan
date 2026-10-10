package io.github.barqallayl.burkan.feature.connection.data

import arrow.core.Either
import arrow.core.raise.Raise
import arrow.core.raise.either
import arrow.core.raise.ensure
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesBinding
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.Named
import dev.zacsweers.metro.SingleIn
import io.github.barqallayl.burkan.core.di.AppBindings
import io.github.barqallayl.burkan.core.model.AppError
import io.github.barqallayl.burkan.core.shell.AdbShellExecutor
import io.github.barqallayl.burkan.core.shell.ReconnectingShellExecutor
import io.github.barqallayl.burkan.core.shell.ShellExecutor
import io.github.barqallayl.burkan.core.storage.DeviceStateStorage
import io.github.barqallayl.burkan.core.storage.SettingsStorage
import io.github.barqallayl.burkan.feature.connection.model.ConnectionError
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

/** The one way to a shell on the phone. */
interface ShellAccess {
    /**
     * Connects, or reuses a connection still open from just before, and runs [block] with a shell. Only one block runs
     * at a time: a run holds the connection from start to finish. The shell reconnects by itself when adbd restarts
     * part way through.
     *
     * [settle] is how long adbd may still be restarting from now, as it does when the phone has just locked: a new
     * connection is not tried before it has passed. Switching wireless debugging on, which has its own wait, starts at
     * once, so the two waits overlap.
     */
    suspend fun <T> withShell(
        settle: Duration = Duration.ZERO,
        block: suspend (ShellExecutor) -> T,
    ): Either<AppError, T>

    /** Closes the connection now instead of keeping it for the next block, and switches wireless debugging back. */
    suspend fun release()
}

/**
 * Gets a shell through the phone's own wireless debugging: switches it on if the app may and must, finds the port,
 * connects on loopback with the Wi-Fi address as fallback, and switches wireless debugging off again afterwards if
 * it was the one that switched it on and the user has not asked to keep it on.
 *
 * Every new connection raises the system's "Wireless debugging connected" notification, so a connection is kept for
 * [LINGER] after a block ends, and a block that starts in that time reuses it.
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
    @Named(AppBindings.APP_SCOPE) private val scope: CoroutineScope,
) : ShellAccess {

    private val mutex = Mutex()

    /** The open connection, also while it lingers between blocks. Guarded by [mutex]. */
    private var connection: OpenConnection? = null
    private var closing: Job? = null

    override suspend fun <T> withShell(
        settle: Duration,
        block: suspend (ShellExecutor) -> T,
    ): Either<AppError, T> = mutex.withLock {
        closing?.cancel()
        closing = null
        either {
            if (connection == null) connection = open(settle)
            val result = try {
                block(ReconnectingShellExecutor(AdbShellExecutor(client), ::reconnect))
            } catch (e: Throwable) {
                // A failed or cancelled block leaves nothing worth keeping open.
                withContext(NonCancellable) { close() }
                throw e
            }
            closing = scope.launch {
                delay(LINGER)
                mutex.withLock { withContext(NonCancellable) { close() } }
            }
            result
        }
    }

    override suspend fun release() {
        mutex.withLock {
            closing?.cancel()
            closing = null
            withContext(NonCancellable) { close() }
        }
    }

    /**
     * Switches wireless debugging on if needed and connects, not before [settle] has passed. On failure, puts the
     * switch back first.
     */
    private suspend fun Raise<ConnectionError>.open(settle: Duration): OpenConnection = coroutineScope {
        val settling = launch { delay(settle) }
        val switchedOn = switchOn()
        try {
            settling.join()
            val endpoint =
                discovery.find(AdbService.Connect, DISCOVERY_TIMEOUT) ?: raise(ConnectionError.DaemonNotFound)
            connect(endpoint).bind()
            OpenConnection(switchedOn)
        } catch (e: Throwable) {
            if (switchedOn) withContext(NonCancellable) { switchOff() }
            throw e
        }
    }

    private suspend fun close() {
        val open = connection ?: return
        connection = null
        client.disconnect()
        if (open.switchedOn) switchOff()
    }

    /** True when this call switched wireless debugging on, and so must switch it off. */
    private suspend fun Raise<ConnectionError>.switchOn(): Boolean {
        if (wirelessDebugging.isOn()) return false
        ensure(wirelessDebugging.isWifiConnected()) { ConnectionError.NoWifi }
        ensure(wirelessDebugging.set(true)) { ConnectionError.WirelessDebuggingOff }
        try {
            // The system may refuse (an untrusted network, or Wi-Fi lost) and put the switch back by itself.
            delay(SETTLE_TIME)
        } catch (e: CancellationException) {
            // The switch is on by now, and a run cancelled here leaves nothing behind that would switch it off.
            withContext(NonCancellable) { switchOff() }
            throw e
        }
        ensure(wirelessDebugging.isOn()) { ConnectionError.WirelessDebuggingRefused }
        return true
    }

    private suspend fun switchOff() {
        if (settings.turnOffWirelessDebugging.first()) wirelessDebugging.set(false)
    }

    /**
     * Connects again after adbd restarted: it comes back on a new port about a second later, so wait, find the port
     * again and connect, until [RECONNECT_BUDGET] runs out. Runs inside a block, which holds [mutex].
     */
    private suspend fun reconnect(): Boolean = withTimeoutOrNull(RECONNECT_BUDGET) { reconnectUntilDone() } ?: false

    private suspend fun reconnectUntilDone(): Boolean {
        while (true) {
            delay(RECONNECT_PAUSE)
            if (!wirelessDebugging.isOn()) return false
            val endpoint = discovery.find(AdbService.Connect, RECONNECT_DISCOVERY_TIMEOUT) ?: continue
            when (connect(endpoint).leftOrNull()) {
                null -> return true
                ConnectionError.NotAuthorised -> return false
                else -> Unit
            }
        }
    }

    private suspend fun connect(endpoint: AdbEndpoint): Either<ConnectionError, Unit> {
        val loopback = client.connect(LOOPBACK, endpoint.port)
        val result = if (loopback == Either.Left(ConnectionError.ConnectFailed)) {
            client.connect(endpoint.host, endpoint.port)
        } else {
            loopback
        }
        // The daemon no longer accepts the key, so setup has to pair again: it is no longer complete, and `App`
        // goes back to it.
        if (result == Either.Left(ConnectionError.NotAuthorised)) {
            deviceState.setPaired(false)
            deviceState.setSetupComplete(false)
        }
        return result
    }

    /** [switchedOn]: this app switched wireless debugging on for the connection, so it switches it off after. */
    private class OpenConnection(val switchedOn: Boolean)

    companion object {
        const val LOOPBACK = "127.0.0.1"
        val SETTLE_TIME = 2.seconds
        val DISCOVERY_TIMEOUT = 30.seconds

        /** How long a connection is kept after a block, for the next one to reuse. */
        val LINGER = 10.seconds

        /** How long a lost connection is tried again before the command fails. */
        val RECONNECT_BUDGET = 30.seconds

        /** adbd accepts connections again about a second after it restarts. */
        val RECONNECT_PAUSE = 1.seconds
        val RECONNECT_DISCOVERY_TIMEOUT = 5.seconds
    }
}
