package io.github.barqallayl.burkan.feature.connection.data

import io.github.barqallayl.burkan.core.shell.StreamRefusedException
import io.github.barqallayl.burkan.feature.connection.model.ConnectionError
import io.github.barqallayl.burkan.feature.connection.model.PairingError
import io.github.muntashirakon.adb.AdbAuthenticationFailedException
import io.github.muntashirakon.adb.AdbPairingRequiredException
import org.junit.Test
import java.io.IOException
import java.net.ConnectException
import java.net.NoRouteToHostException
import java.net.SocketTimeoutException
import javax.net.ssl.SSLHandshakeException
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertSame

class LibAdbClientErrorsTest {

    @Test
    fun `a refused or unreachable pairing port means the pairing dialog is closed`() {
        assertEquals(PairingError.DialogClosed, pairingError(ConnectException("Connection refused")))
        assertEquals(PairingError.DialogClosed, pairingError(NoRouteToHostException()))
    }

    @Test
    fun `either way libadb words a failed key exchange is a wrong code`() {
        assertEquals(PairingError.WrongCode, pairingError(IOException("Exchanging message wasn't successful.")))
        assertEquals(PairingError.WrongCode, pairingError(IOException("Could not exchange peer info.")))
    }

    @Test
    fun `any other pairing failure is reported as failed`() {
        assertEquals(PairingError.Failed, pairingError(IOException("Something else")))
        assertEquals(PairingError.Failed, pairingError(SocketTimeoutException()))
        // The message alone is not enough: it has to be libadb's IOException.
        assertEquals(PairingError.Failed, pairingError(IllegalStateException("Could not exchange peer info.")))
    }

    @Test
    fun `an unknown key is a pairing the phone no longer holds`() {
        assertEquals(ConnectionError.NotAuthorised, connectionError(AdbAuthenticationFailedException()))
        assertEquals(ConnectionError.NotAuthorised, connectionError(AdbPairingRequiredException("Pairing required")))
    }

    @Test
    fun `any other connect failure is a connection that was not made`() {
        listOf(
            ConnectException("Connection refused"),
            SocketTimeoutException(),
            SSLHandshakeException("handshake"),
            IllegalStateException("Already connected"),
            RuntimeException("From the TLS provider"),
        ).forEach { e ->
            assertEquals(ConnectionError.ConnectFailed, connectionError(e), "for ${e::class.simpleName}")
        }
    }

    @Test
    fun `a refused stream is the daemon saying no, not a lost connection`() {
        val cause = ConnectException("Stream refused")

        val error = streamError(cause)

        assertIs<StreamRefusedException>(error)
        assertSame(cause, error.cause)
    }

    @Test
    fun `the other ways libadb reports a lost connection all become an IOException`() {
        listOf(IllegalStateException("Not connected"), InterruptedException()).forEach { cause ->
            val error = streamError(cause)

            assertEquals(IOException::class, error::class, "for ${cause::class.simpleName}")
            assertSame(cause, error.cause)
        }
    }

    @Test
    fun `an IOException or anything else is passed on as it is`() {
        listOf(IOException("Stream closed"), IllegalArgumentException("bad destination")).forEach { e ->
            assertSame(e, streamError(e))
        }
    }
}
