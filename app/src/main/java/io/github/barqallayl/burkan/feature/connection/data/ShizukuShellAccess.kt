package io.github.barqallayl.burkan.feature.connection.data

import arrow.core.Either
import arrow.core.raise.Raise
import arrow.core.raise.either
import arrow.core.raise.ensure
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.Named
import dev.zacsweers.metro.SingleIn
import io.github.barqallayl.burkan.core.di.AppBindings
import io.github.barqallayl.burkan.core.model.AppError
import io.github.barqallayl.burkan.core.shell.ReconnectingShellExecutor
import io.github.barqallayl.burkan.core.shell.ShellExecutor
import io.github.barqallayl.burkan.feature.connection.model.ConnectionError
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

/**
 * Gets a shell through Shizuku, which the user keeps running: Burkan starts none of it, and switches nothing on that
 * it has to switch off again. Wi-Fi, wireless debugging and the pairing are Shizuku's concern, not this app's.
 *
 * Shizuku may still be starting when the phone has just restarted, so a run that is given [withShell]'s `patience`
 * waits that long for it to come up. A run that is not, such as one the user starts, finds out at once that it is not
 * running.
 *
 * The shell is kept for [LINGER] after a block, as the wireless debugging connection is, so that a block starting
 * soon after another does not start a new process in Shizuku.
 */
@Inject
@SingleIn(AppScope::class)
class ShizukuShellAccess(
    private val client: ShizukuClient,
    @Named(AppBindings.APP_SCOPE) private val scope: CoroutineScope,
) : ShellAccess {

    private val mutex = Mutex()

    /** Whether the shell is up, also while it lingers between blocks. Guarded by [mutex]. */
    private var connected = false
    private var closing: Job? = null

    override suspend fun <T> withShell(
        settle: Duration,
        patience: Duration,
        block: suspend (ShellExecutor) -> T,
    ): Either<AppError, T> = mutex.withLock {
        closing?.cancel()
        closing = null
        either {
            if (!connected) {
                open(patience)
                connected = true
            }
            val result = try {
                block(ReconnectingShellExecutor(client, ::reconnect))
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

    private suspend fun Raise<ConnectionError>.open(patience: Duration) {
        ensure(awaitRunning(patience)) { ConnectionError.ShizukuNotRunning }
        ensure(client.isAllowed()) { ConnectionError.ShizukuNotAllowed }
        client.connect().bind()
    }

    private suspend fun close() {
        if (!connected) return
        connected = false
        client.disconnect()
    }

    /** True once Shizuku is running, at once if it is, and false when [patience] has run out first. */
    private suspend fun awaitRunning(patience: Duration): Boolean =
        client.isRunning() || withTimeoutOrNull(patience) {
            while (!client.isRunning()) delay(POLL)
            true
        } == true

    /**
     * Gets the shell again after it was lost: Shizuku was restarted, or ended it. Waits for Shizuku to be back and
     * starts the shell afresh, until [RECONNECT_BUDGET] runs out. Runs inside a block, which holds [mutex].
     */
    private suspend fun reconnect(): Boolean = withTimeoutOrNull(RECONNECT_BUDGET) { reconnectUntilDone() } ?: false

    private suspend fun reconnectUntilDone(): Boolean {
        while (true) {
            delay(POLL)
            if (!client.isRunning()) continue
            // Without the permission there is nothing to wait for.
            if (!client.isAllowed()) return false
            if (client.connect().isRight()) return true
        }
    }

    companion object {
        /** How long a shell is kept after a block, for the next one to reuse. */
        val LINGER = 10.seconds

        /** How long a lost shell is tried again before the command fails. */
        val RECONNECT_BUDGET = 30.seconds

        /** How often Shizuku is looked for while waiting for it. */
        val POLL = 500.milliseconds
    }
}
