package io.github.barqallayl.burkan.feature.connection.data

import arrow.core.left
import arrow.core.right
import io.github.barqallayl.burkan.core.storage.FakeDeviceStateStorage
import io.github.barqallayl.burkan.feature.connection.model.ConnectionError
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.currentTime
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class AdbShellAccessTest {

    private val client = FakeAdbClient()
    private val discovery = FakeAdbDiscovery().apply {
        endpoints[AdbService.Connect] = AdbEndpoint("192.168.1.20", 37_215)
    }
    private val wirelessDebugging = FakeWirelessDebugging()
    private val deviceState = FakeDeviceStateStorage(paired = true)
    private val access = AdbShellAccess(client, discovery, wirelessDebugging, deviceState)

    @Test
    fun `with wireless debugging already on, it is left on`() = runTest {
        wirelessDebugging.on = true

        val result = access.withShell { "ran" }

        assertEquals("ran".right(), result)
        assertEquals(listOf("connect 127.0.0.1:37215", "disconnect"), client.calls)
        assertEquals(emptyList(), wirelessDebugging.writes)
        assertTrue(wirelessDebugging.on)
    }

    @Test
    fun `wireless debugging switched on by the app is switched off again`() = runTest {
        val result = access.withShell { currentTime }

        assertEquals(2_000L.right(), result, "connects only after the switch has settled")
        assertEquals(listOf(true, false), wirelessDebugging.writes)
        assertFalse(wirelessDebugging.on)
    }

    @Test
    fun `no Wi-Fi is reported before touching the switch`() = runTest {
        wirelessDebugging.wifi = false

        assertEquals(ConnectionError.NoWifi.left(), access.withShell { })
        assertEquals(emptyList(), wirelessDebugging.writes)
        assertEquals(emptyList(), client.calls)
    }

    @Test
    fun `without the permission the switch stays off`() = runTest {
        wirelessDebugging.permitted = false

        assertEquals(ConnectionError.WirelessDebuggingOff.left(), access.withShell { })
        assertEquals(emptyList(), client.calls)
    }

    @Test
    fun `a switch the system puts back is refused, not retried`() = runTest {
        wirelessDebugging.systemRefuses = true

        assertEquals(ConnectionError.WirelessDebuggingRefused.left(), access.withShell { })
        assertEquals(listOf(true), wirelessDebugging.writes)
        assertEquals(emptyList(), discovery.searches)
    }

    @Test
    fun `no advertised daemon fails and still switches off`() = runTest {
        discovery.endpoints.clear()

        assertEquals(ConnectionError.DaemonNotFound.left(), access.withShell { })
        assertEquals(listOf(true, false), wirelessDebugging.writes)
    }

    @Test
    fun `loopback refused falls back to the Wi-Fi address`() = runTest {
        wirelessDebugging.on = true
        client.connectResults["127.0.0.1"] = ConnectionError.ConnectFailed.left()

        assertEquals(Unit.right(), access.withShell { })
        assertEquals(listOf("connect 127.0.0.1:37215", "connect 192.168.1.20:37215", "disconnect"), client.calls)
    }

    @Test
    fun `an unauthorised key sends setup back to pairing`() = runTest {
        wirelessDebugging.on = true
        client.connectResults["127.0.0.1"] = ConnectionError.NotAuthorised.left()

        assertEquals(ConnectionError.NotAuthorised.left(), access.withShell { })
        assertFalse(deviceState.isPaired.value)
        assertEquals(listOf("connect 127.0.0.1:37215"), client.calls)
    }

    @Test
    fun `a failing block still disconnects and switches off`() = runTest {
        assertFailsWith<IllegalStateException> { access.withShell { error("boom") } }

        assertEquals("disconnect", client.calls.last())
        assertEquals(listOf(true, false), wirelessDebugging.writes)
    }

    @Test
    fun `a cancelled run still disconnects and switches off`() = runTest {
        val run = launch { access.withShell { awaitCancellation() } }
        runCurrent()
        testScheduler.advanceTimeBy(5_000)

        run.cancel()
        run.join()

        assertEquals("disconnect", client.calls.last())
        assertEquals(listOf(true, false), wirelessDebugging.writes)
    }

    @Test
    fun `runs do not overlap`() = runTest {
        wirelessDebugging.on = true
        val firstMayFinish = CompletableDeferred<Unit>()

        val first = async { access.withShell { firstMayFinish.await() } }
        runCurrent()
        val second = async { access.withShell { } }
        runCurrent()
        assertEquals(listOf("connect 127.0.0.1:37215"), client.calls, "the second waits for the first")

        firstMayFinish.complete(Unit)
        first.await()
        second.await()

        assertEquals(
            listOf("connect 127.0.0.1:37215", "disconnect", "connect 127.0.0.1:37215", "disconnect"),
            client.calls,
        )
    }
}
