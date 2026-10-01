package io.github.barqallayl.burkan.feature.connection.data

import arrow.core.Either
import arrow.core.right
import io.github.barqallayl.burkan.core.shell.ShellStream
import io.github.barqallayl.burkan.feature.connection.model.ConnectionError
import io.github.barqallayl.burkan.feature.connection.model.PairingError
import java.io.IOException
import kotlin.time.Duration

/** Records every call; answers per host from [connectResults] and [pairResults], succeeding by default. */
class FakeAdbClient : AdbClient {
    val calls = mutableListOf<String>()
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

    override fun open(destination: String): ShellStream = throw IOException("not used by these tests")
}

class FakeAdbDiscovery : AdbDiscovery {
    val endpoints = mutableMapOf<AdbService, AdbEndpoint>()
    val searches = mutableListOf<AdbService>()

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
}
