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
import io.github.barqallayl.burkan.core.shell.StreamRefusedException
import io.github.barqallayl.burkan.feature.connection.model.ConnectionError
import io.github.barqallayl.burkan.feature.connection.model.PairingError
import io.github.muntashirakon.adb.AbsAdbConnectionManager
import io.github.muntashirakon.adb.AdbAuthenticationFailedException
import io.github.muntashirakon.adb.AdbPairingRequiredException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runInterruptible
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
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

    /** Held while the manager is made, so that pairing and connecting at once on first use make only one. */
    private val making = Mutex()

    override suspend fun pair(host: String, port: Int, code: String): Either<PairingError, Unit> =
        withContext(Dispatchers.IO) {
            try {
                // Loading, or on first use generating, the key is too slow for the main thread.
                val manager = manager()
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
            try {
                val manager = manager()
                // The manager refuses to connect while it holds a connection, so drop any stale one first.
                manager.disconnect()
                if (runInterruptible { manager.connect(host, port) }) {
                    Unit.right()
                } else {
                    ConnectionError.ConnectFailed.left()
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                connectionError(e).also {
                    if (it == ConnectionError.ConnectFailed) Logger.w(e) { "Connecting to $host:$port failed" }
                }.left()
            }
        }

    override suspend fun disconnect() {
        val manager = manager ?: return
        withContext(Dispatchers.IO) {
            try {
                // disconnect() ends, as close() begins, by asking the private key to destroy itself. The key
                // Android made refuses, and has to: the next connection is made with the same one.
                manager.disconnect()
            } catch (e: IOException) {
                Logger.w(e) { "Disconnecting failed" }
            }
        }
    }

    override fun open(destination: String): ShellStream {
        val manager = manager ?: throw IOException("Not connected")
        val stream = try {
            manager.openStream(destination)
        } catch (e: Exception) {
            throw streamError(e)
        }
        return object : ShellStream {
            override fun read(buffer: ByteArray): Int = stream.read(buffer, 0, buffer.size)
            override fun close() = stream.close()
        }
    }

    private suspend fun manager(): Manager =
        manager ?: making.withLock { manager ?: Manager(keyStore.keys()).also { manager = it } }

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
    }
}

/** How libadb words the two failures a wrong code can cause. */
private val WRONG_CODE_MESSAGES = setOf("Exchanging message wasn't successful.", "Could not exchange peer info.")

/**
 * libadb reports pairing failures as plain exceptions. A refused connection means the pairing dialog, which owns the
 * port, is closed; a failed key exchange means the code was wrong. On a Galaxy S23 (Android 16) a wrong code fails one
 * step later, when the two sides cannot read each other's details with the key the code gave them.
 */
internal fun pairingError(e: Exception): PairingError = when {
    e is ConnectException || e is NoRouteToHostException -> PairingError.DialogClosed
    e is IOException && e.message in WRONG_CODE_MESSAGES -> PairingError.WrongCode
    else -> PairingError.Failed.also { Logger.w(e) { "Pairing failed" } }
}

/**
 * What a failed connect means to the caller. An unknown key is a pairing the phone no longer holds. Anything else,
 * including the runtime failures libadb and the TLS provider under it throw without declaring them, is a connection
 * that was not made: none of them is a reason to take the app down.
 */
internal fun connectionError(e: Exception): ConnectionError = when (e) {
    is AdbAuthenticationFailedException, is AdbPairingRequiredException -> ConnectionError.NotAuthorised
    else -> ConnectionError.ConnectFailed
}

/**
 * The exception a failed stream open is rethrown as. A refused connection is the daemon saying no to this stream,
 * which is not a connection that went away. libadb reports one that went away in more ways than one; to the caller
 * they all mean not connected, an [IOException]. Anything else is not libadb's to report and is passed on as it is.
 */
internal fun streamError(e: Exception): Exception = when (e) {
    is ConnectException -> StreamRefusedException(e)
    is IllegalStateException, is InterruptedException -> IOException(e)
    else -> e
}
