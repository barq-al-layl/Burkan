package io.github.barqallayl.burkan.feature.connection.data

import android.os.Build
import arrow.core.Either
import arrow.core.left
import arrow.core.right
import co.touchlab.kermit.Logger
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesBinding
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn
import io.github.barqallayl.burkan.core.shell.ShellStream
import io.github.barqallayl.burkan.core.shell.ShellStreamOpener
import io.github.barqallayl.burkan.feature.connection.model.ConnectionError
import io.github.barqallayl.burkan.feature.connection.model.PairingError
import io.github.muntashirakon.adb.AbsAdbConnectionManager
import io.github.muntashirakon.adb.AdbAuthenticationFailedException
import io.github.muntashirakon.adb.AdbPairingRequiredException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runInterruptible
import kotlinx.coroutines.withContext
import java.io.IOException
import java.net.ConnectException
import java.net.NoRouteToHostException
import java.security.PrivateKey
import java.security.cert.Certificate
import java.util.concurrent.TimeUnit

/** The adb client: pairing, connecting, and the streams commands run on. */
interface AdbClient : ShellStreamOpener {
    suspend fun pair(host: String, port: Int, code: String): Either<PairingError, Unit>

    suspend fun connect(host: String, port: Int): Either<ConnectionError, Unit>

    suspend fun disconnect()
}

/** [AdbClient] on libadb-android, with Conscrypt for the TLS 1.3 that wireless debugging uses. */
@Inject
@SingleIn(AppScope::class)
@ContributesBinding(AppScope::class)
class LibAdbClient(private val keyStore: AdbKeyStore) : AdbClient {

    @Volatile
    private var manager: Manager? = null

    override suspend fun pair(host: String, port: Int, code: String): Either<PairingError, Unit> =
        withContext(Dispatchers.IO) {
            // Loading, or on first use generating, the key is too slow for the main thread.
            val manager = manager()
            try {
                runInterruptible { manager.pair(host, port, code) }
                Unit.right()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                pairingError(e).left()
            }
        }

    override suspend fun connect(host: String, port: Int): Either<ConnectionError, Unit> =
        withContext(Dispatchers.IO) {
            val manager = manager()
            try {
                // The manager refuses to connect while it holds a connection, so drop any stale one first.
                manager.disconnect()
                if (runInterruptible { manager.connect(host, port) }) {
                    Unit.right()
                } else {
                    ConnectionError.ConnectFailed.left()
                }
            } catch (e: CancellationException) {
                throw e
            } catch (_: AdbAuthenticationFailedException) {
                ConnectionError.NotAuthorised.left()
            } catch (_: AdbPairingRequiredException) {
                ConnectionError.NotAuthorised.left()
            } catch (e: IOException) {
                Logger.w(e) { "Connecting to $host:$port failed" }
                ConnectionError.ConnectFailed.left()
            } catch (e: InterruptedException) {
                Logger.w(e) { "Connecting to $host:$port timed out" }
                ConnectionError.ConnectFailed.left()
            }
        }

    override suspend fun disconnect() {
        val manager = manager ?: return
        withContext(Dispatchers.IO) {
            try {
                // Never close(): libadb's close() destroys the private key.
                manager.disconnect()
            } catch (e: IOException) {
                Logger.w(e) { "Disconnecting failed" }
            }
        }
    }

    override fun open(destination: String): ShellStream {
        val stream = (manager ?: throw IOException("Not connected")).openStream(destination)
        return object : ShellStream {
            override fun read(buffer: ByteArray): Int = stream.read(buffer, 0, buffer.size)
            override fun close() = stream.close()
        }
    }

    private suspend fun manager(): Manager =
        manager ?: Manager(keyStore.keys()).also { manager = it }

    /**
     * libadb reports pairing failures as plain exceptions. A refused connection means the pairing dialog, which owns
     * the port, is closed; a failed key exchange means the code was wrong. These are expected, not verified on a
     * device.
     */
    private fun pairingError(e: Exception): PairingError = when {
        e is ConnectException || e is NoRouteToHostException -> PairingError.DialogClosed
        e is IOException && e.message == KEY_EXCHANGE_FAILED -> PairingError.WrongCode
        else -> PairingError.Failed.also { Logger.w(e) { "Pairing failed" } }
    }

    private class Manager(private val keys: AdbKeys) : AbsAdbConnectionManager() {
        init {
            setApi(Build.VERSION.SDK_INT)
            setTimeout(CONNECT_TIMEOUT_SECONDS, TimeUnit.SECONDS)
            // Report an unknown key as an error instead of waiting for an authorisation dialog nobody will see.
            setThrowOnUnauthorised(true)
        }

        override fun getPrivateKey(): PrivateKey = keys.privateKey

        override fun getCertificate(): Certificate = keys.certificate

        override fun getDeviceName(): String = DEVICE_NAME
    }

    private companion object {
        /** The name the phone lists this app under, in Wireless debugging's paired devices. */
        const val DEVICE_NAME = "Burkan"
        const val CONNECT_TIMEOUT_SECONDS = 10L
        const val KEY_EXCHANGE_FAILED = "Exchanging message wasn't successful."
    }
}
