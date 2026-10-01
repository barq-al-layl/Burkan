package io.github.barqallayl.burkan.feature.connection.data

import arrow.core.left
import arrow.core.right
import io.github.barqallayl.burkan.core.storage.FakeDeviceStateStorage
import io.github.barqallayl.burkan.feature.connection.model.PairingError
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class PairingRepositoryTest {

    private val client = FakeAdbClient()
    private val discovery = FakeAdbDiscovery().apply {
        endpoints[AdbService.Pairing] = AdbEndpoint("192.168.1.20", 41_003)
    }
    private val deviceState = FakeDeviceStateStorage()
    private val repository = PairingRepository(client, discovery, deviceState)

    @Test
    fun `a correct code pairs on loopback and is remembered`() = runTest {
        assertEquals(Unit.right(), repository.pair("123 456"))

        assertEquals(listOf("pair 127.0.0.1:41003 123456"), client.calls)
        assertTrue(deviceState.isPaired.value)
        assertEquals(PairingStatus.Paired, repository.status.value)
    }

    @Test
    fun `anything but six digits is rejected before looking for the dialog`() = runTest {
        listOf("12345", "1234567", "12a456", "").forEach { code ->
            assertEquals(PairingError.InvalidCode.left(), repository.pair(code), code)
        }
        assertEquals(emptyList(), discovery.searches)
        assertEquals(PairingStatus.Failed(PairingError.InvalidCode), repository.status.value)
    }

    @Test
    fun `no pairing service means the dialog is closed`() = runTest {
        discovery.endpoints.clear()

        assertEquals(PairingError.DialogClosed.left(), repository.pair("123456"))
        assertEquals(emptyList(), client.calls)
        assertFalse(deviceState.isPaired.value)
    }

    @Test
    fun `a wrong code is its own failure`() = runTest {
        client.pairResults["127.0.0.1"] = PairingError.WrongCode.left()

        assertEquals(PairingError.WrongCode.left(), repository.pair("123456"))
        assertEquals(PairingStatus.Failed(PairingError.WrongCode), repository.status.value)
        assertFalse(deviceState.isPaired.value)
    }

    @Test
    fun `loopback refused falls back to the Wi-Fi address`() = runTest {
        client.pairResults["127.0.0.1"] = PairingError.DialogClosed.left()

        assertEquals(Unit.right(), repository.pair("123456"))
        assertEquals(listOf("pair 127.0.0.1:41003 123456", "pair 192.168.1.20:41003 123456"), client.calls)
    }

    @Test
    fun `a session with no code times out after five minutes`() = runTest {
        val session = launch { repository.runSession() }
        runCurrent()
        assertEquals(PairingStatus.WaitingForCode, repository.status.value)

        advanceTimeBy(PairingRepository.SESSION_TIMEOUT)
        runCurrent()

        assertTrue(session.isCompleted)
        assertEquals(PairingStatus.Failed(PairingError.TimedOut), repository.status.value)
    }

    @Test
    fun `a session ends as soon as pairing succeeds`() = runTest {
        val session = launch { repository.runSession() }
        runCurrent()

        assertEquals(Unit.right(), repository.pair("123456"))
        runCurrent()

        assertTrue(session.isCompleted)
        assertEquals(PairingStatus.Paired, repository.status.value)
    }
}
