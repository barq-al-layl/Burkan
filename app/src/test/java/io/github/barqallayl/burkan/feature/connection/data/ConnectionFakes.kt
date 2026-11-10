package io.github.barqallayl.burkan.feature.connection.data

import arrow.core.Either
import arrow.core.left
import arrow.core.right
import io.github.barqallayl.burkan.core.model.AppError
import io.github.barqallayl.burkan.core.model.ShellBackend
import io.github.barqallayl.burkan.core.shell.BytesStream
import io.github.barqallayl.burkan.core.shell.FakeShellExecutor
import io.github.barqallayl.burkan.core.shell.PackageName
import io.github.barqallayl.burkan.core.shell.ShellCommand
import io.github.barqallayl.burkan.core.shell.ShellError
import io.github.barqallayl.burkan.core.shell.ShellExecutor
import io.github.barqallayl.burkan.core.shell.ShellResult
import io.github.barqallayl.burkan.core.shell.ShellStream
import io.github.barqallayl.burkan.core.shell.exit
import io.github.barqallayl.burkan.core.shell.packet
import io.github.barqallayl.burkan.feature.connection.model.ConnectionError
import io.github.barqallayl.burkan.feature.connection.model.PairingError
import kotlinx.coroutines.delay
import java.io.IOException
import kotlin.time.Duration

/**
 * Records every call; answers per host from [connectResults] and [pairResults], succeeding by default. Each stream
 * opened answers `ok`, except that the next [dropOpens] fail as they do once adbd has restarted.
 */
class FakeAdbClient : AdbClient {
    val calls = mutableListOf<String>()
    var dropOpens = 0
    val connectResults = mutableMapOf<String, Either<ConnectionError, Unit>>()
    val pairResults = mutableMapOf<String, Either<PairingError, Unit>>()

    override suspend fun pair(host: String, port: Int, code: String): Either<PairingError, Unit> {
        calls += "pair $host:$port $code"
        return pairResults[host] ?: Unit.right()
    }

    override suspend fun connect(host: String, port: Int): Either<ConnectionError, Unit> {
        calls += "connect $host:$port"
        return connectResults[host] ?: Unit.right()
    }

    override suspend fun disconnect() {
        calls += "disconnect"
    }

    override fun open(destination: String): ShellStream {
        if (dropOpens > 0) {
            dropOpens--
            throw IOException("Not connected")
        }
        return BytesStream(packet(1, "ok\n") + exit(0))
    }
}

class FakeAdbDiscovery : AdbDiscovery {
    val endpoints = mutableMapOf<AdbService, AdbEndpoint>()
    val searches = mutableListOf<AdbService>()
    var allowed = true

    override fun isAllowed(): Boolean = allowed

    override suspend fun find(service: AdbService, timeout: Duration): AdbEndpoint? {
        searches += service
        return endpoints[service]
    }
}

/** A switch that holds what is written, unless the system "refuses" by resetting it. */
class FakeWirelessDebugging(
    var on: Boolean = false,
    var permitted: Boolean = true,
    var wifi: Boolean = true,
    var systemRefuses: Boolean = false,
    var forgetsPairings: Boolean = false,
) : WirelessDebugging {
    val writes = mutableListOf<Boolean>()

    override fun isOn(): Boolean = on

    override fun set(on: Boolean): Boolean {
        if (!permitted) return false
        writes += on
        this.on = on && !systemRefuses
        return true
    }

    override fun canSwitch(): Boolean = permitted

    override fun isWifiConnected(): Boolean = wifi

    override fun forgetsPairings(): Boolean = forgetsPairings
}

/**
 * Shizuku as a test sets it up: running or not, allowing the app or not. The shell it hands over answers from
 * [shell] while it is [connected].
 */
class FakeShizukuClient(
    var running: Boolean = true,
    var allowed: Boolean = true,
    private val shell: ShellExecutor = FakeShellExecutor(),
) : ShizukuClient {
    val calls = mutableListOf<String>()
    var connectResult: Either<ConnectionError, Unit> = Unit.right()
    var permissionRequests = 0
    var connected = false
    var app: PackageName? = PackageName.known("moe.shizuku.privileged.api")

    override fun isRunning(): Boolean = running

    override fun isAllowed(): Boolean = running && allowed

    override fun requestPermission() {
        permissionRequests++
    }

    override suspend fun appPackage(): PackageName? = app

    override suspend fun connect(): Either<ConnectionError, Unit> {
        calls += "connect"
        connected = connectResult.isRight()
        return connectResult
    }

    override suspend fun disconnect() {
        calls += "disconnect"
        connected = false
    }

    override suspend fun run(command: ShellCommand): Either<ShellError, ShellResult> =
        if (connected) shell.run(command) else ShellError.NotConnected.left()
}

/** Runs each block on [shell] once its settle time has passed, or fails before connecting with [failure]. */
class FakeShellAccess(private val shell: ShellExecutor) : ShellAccess {
    var failure: AppError? = null
    var runs = 0
    var releases = 0
    val settles = mutableListOf<Duration>()
    val patiences = mutableListOf<Duration>()
    val backends = mutableListOf<ShellBackend?>()

    override suspend fun <T> withShell(
        settle: Duration,
        patience: Duration,
        backend: ShellBackend?,
        block: suspend (ShellExecutor) -> T,
    ): Either<AppError, T> {
        runs++
        settles += settle
        patiences += patience
        backends += backend
        delay(settle)
        return failure?.left() ?: block(shell).right()
    }

    override suspend fun release() {
        releases++
    }
}
