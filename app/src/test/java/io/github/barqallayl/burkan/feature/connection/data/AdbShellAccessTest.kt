package io.github.barqallayl.burkan.feature.connection.data

import arrow.core.left
import arrow.core.right
import io.github.barqallayl.burkan.core.shell.ShellCommands
import io.github.barqallayl.burkan.core.shell.ShellError
import io.github.barqallayl.burkan.core.shell.ShellResult
import io.github.barqallayl.burkan.core.storage.FakeDeviceStateStorage
import io.github.barqallayl.burkan.core.storage.FakeSettingsStorage
import io.github.barqallayl.burkan.feature.connection.model.ConnectionError
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.currentTime
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.seconds

class AdbShellAccessTest {

    private val client = FakeAdbClient()
    private val discovery = FakeAdbDiscovery().apply {
        endpoints[AdbService.Connect] = AdbEndpoint("192.168.1.20", 37_215)
    }
    private val wirelessDebugging = FakeWirelessDebugging()
    private val deviceState = FakeDeviceStateStorage(paired = true)
    private val settings = FakeSettingsStorage()

    private fun TestScope.access() =
        AdbShellAccess(client, discovery, wirelessDebugging, deviceState, settings, backgroundScope)

    /** Lets the connection kept after a block close. */
    private fun TestScope.afterLinger() {
        advanceTimeBy(AdbShellAccess.LINGER + 1.seconds)
        runCurrent()
    }

    @Test
    fun `with wireless debugging already on, it is left on`() = runTest {
        wirelessDebugging.on = true

        val result = access().withShell { "ran" }
        afterLinger()

        assertEquals("ran".right(), result)
        assertEquals(listOf("connect 127.0.0.1:37215", "disconnect"), client.calls)
        assertEquals(emptyList(), wirelessDebugging.writes)
        assertTrue(wirelessDebugging.on)
    }

    @Test
    fun `wireless debugging switched on by the app is switched off again`() = runTest {
        val result = access().withShell { currentTime }
        afterLinger()

        assertEquals(2_000L.right(), result, "connects only after the switch has settled")
        assertEquals(listOf(true, false), wirelessDebugging.writes)
        assertFalse(wirelessDebugging.on)
    }

    @Test
    fun `waiting for adbd to settle overlaps the wait for the switch`() = runTest {
        val result = access().withShell(settle = 3.seconds) { currentTime }

        assertEquals(3_000L.right(), result, "3 seconds after the lock, not 2 + 3")
        assertEquals(listOf(true), wirelessDebugging.writes, "switched on at once")
    }

    @Test
    fun `with wireless debugging already on, the settle alone is waited`() = runTest {
        wirelessDebugging.on = true

        assertEquals(3_000L.right(), access().withShell(settle = 3.seconds) { currentTime })
    }

    @Test
    fun `a user who keeps wireless debugging on finds it on after the run`() = runTest {
        settings.turnOffWirelessDebugging.value = false

        assertEquals(Unit.right(), access().withShell { })
        afterLinger()

        assertEquals(listOf(true), wirelessDebugging.writes)
        assertTrue(wirelessDebugging.on)
    }

    @Test
    fun `a connection is kept for a moment, and a block in that time reuses it`() = runTest {
        val access = access()

        access.withShell { }
        advanceTimeBy(5.seconds)
        access.withShell { }
        assertEquals(listOf("connect 127.0.0.1:37215"), client.calls, "one connection, one system notification")
        assertEquals(listOf(true), wirelessDebugging.writes)

        afterLinger()
        assertEquals(listOf("connect 127.0.0.1:37215", "disconnect"), client.calls)
        assertEquals(listOf(true, false), wirelessDebugging.writes)
    }

    @Test
    fun `release closes the connection and switches back at once`() = runTest {
        val access = access()

        access.withShell { }
        access.release()

        assertEquals(listOf("connect 127.0.0.1:37215", "disconnect"), client.calls)
        assertEquals(listOf(true, false), wirelessDebugging.writes)
        afterLinger()
        assertEquals(2, client.calls.size, "nothing is closed twice")
    }

    @Test
    fun `no Wi-Fi is reported before touching the switch`() = runTest {
        wirelessDebugging.wifi = false

        assertEquals(ConnectionError.NoWifi.left(), access().withShell { })
        assertEquals(emptyList(), wirelessDebugging.writes)
        assertEquals(emptyList(), client.calls)
    }

    @Test
    fun `without the permission the switch stays off`() = runTest {
        wirelessDebugging.permitted = false

        assertEquals(ConnectionError.WirelessDebuggingOff.left(), access().withShell { })
        assertEquals(emptyList(), client.calls)
    }

    @Test
    fun `a switch the system puts back is refused, not retried`() = runTest {
        wirelessDebugging.systemRefuses = true

        assertEquals(ConnectionError.WirelessDebuggingRefused.left(), access().withShell { })
        assertEquals(listOf(true), wirelessDebugging.writes)
        assertEquals(emptyList(), discovery.searches)
    }

    @Test
    fun `no advertised daemon fails and still switches off`() = runTest {
        discovery.endpoints.clear()

        assertEquals(ConnectionError.DaemonNotFound.left(), access().withShell { })
        assertEquals(listOf(true, false), wirelessDebugging.writes)
    }

    @Test
    fun `loopback refused falls back to the Wi-Fi address`() = runTest {
        wirelessDebugging.on = true
        client.connectResults["127.0.0.1"] = ConnectionError.ConnectFailed.left()

        assertEquals(Unit.right(), access().withShell { })
        assertEquals(listOf("connect 127.0.0.1:37215", "connect 192.168.1.20:37215"), client.calls)
    }

    @Test
    fun `an unauthorised key sends setup back to pairing, also once setup was complete`() = runTest {
        wirelessDebugging.on = true
        deviceState.isSetupComplete.value = true
        client.connectResults["127.0.0.1"] = ConnectionError.NotAuthorised.left()

        assertEquals(ConnectionError.NotAuthorised.left(), access().withShell { })
        assertFalse(deviceState.isPaired.value)
        assertFalse(deviceState.isSetupComplete.value, "so that App shows Setup again, at the Pair step")
        assertEquals(listOf("connect 127.0.0.1:37215"), client.calls)
    }

    @Test
    fun `a failing block disconnects and switches off at once`() = runTest {
        assertFailsWith<IllegalStateException> { access().withShell { error("boom") } }

        assertEquals("disconnect", client.calls.last())
        assertEquals(listOf(true, false), wirelessDebugging.writes)
    }

    @Test
    fun `a cancelled run disconnects and switches off at once`() = runTest {
        val access = access()
        val run = launch { access.withShell { awaitCancellation() } }
        runCurrent()
        advanceTimeBy(5.seconds)

        run.cancel()
        run.join()

        assertEquals("disconnect", client.calls.last())
        assertEquals(listOf(true, false), wirelessDebugging.writes)
    }

    @Test
    fun `a run cancelled while the switch settles switches it off again`() = runTest {
        val access = access()
        val run = launch { access.withShell { } }
        advanceTimeBy(1.seconds)

        run.cancel()
        run.join()

        assertEquals(listOf(true, false), wirelessDebugging.writes)
        assertFalse(wirelessDebugging.on)
        assertEquals(emptyList(), client.calls, "it never got as far as connecting")
    }

    @Test
    fun `runs do not overlap`() = runTest {
        wirelessDebugging.on = true
        val access = access()
        val firstMayFinish = CompletableDeferred<Unit>()

        val first = async { access.withShell { firstMayFinish.await() } }
        runCurrent()
        val second = async { access.withShell { "second" } }
        runCurrent()
        assertFalse(second.isCompleted, "the second waits for the first")

        firstMayFinish.complete(Unit)
        first.await()
        assertEquals("second".right(), second.await())
    }

    @Test
    fun `a connection lost part way is found again on its new port, and the command carries on`() = runTest {
        wirelessDebugging.on = true

        val result = access().withShell { shell ->
            // adbd restarts, as it does when the keyguard changes, and comes back on another port.
            client.dropOpens = 1
            discovery.endpoints[AdbService.Connect] = AdbEndpoint("192.168.1.20", 40_001)
            shell.run(ShellCommands.echoOk())
        }

        assertEquals(ShellResult(0, "ok\n", "").right().right(), result)
        assertEquals(listOf("connect 127.0.0.1:37215", "connect 127.0.0.1:40001"), client.calls)
    }

    @Test
    fun `a connection that does not come back within 30 seconds fails the command`() = runTest {
        wirelessDebugging.on = true

        val result = access().withShell { shell ->
            client.dropOpens = Int.MAX_VALUE
            discovery.endpoints.clear()
            shell.run(ShellCommands.echoOk()) to currentTime
        }

        val (command, elapsed) = result.getOrNull()!!
        assertEquals(ShellError.NotConnected.left(), command)
        assertTrue(elapsed in 30_000L..31_000L, "gave up after $elapsed ms")
    }

    @Test
    fun `wireless debugging switched off meanwhile ends the reconnection at once`() = runTest {
        wirelessDebugging.on = true

        val result = access().withShell { shell ->
            client.dropOpens = 1
            wirelessDebugging.on = false
            shell.run(ShellCommands.echoOk()) to currentTime
        }

        assertEquals(ShellError.NotConnected.left() to 1_000L, result.getOrNull())
    }
}
