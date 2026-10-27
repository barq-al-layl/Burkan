package io.github.barqallayl.burkan.core.shell

import arrow.core.Either
import arrow.core.left
import arrow.core.right
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.runInterruptible
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import java.io.Closeable
import java.io.IOException
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.time.TimeSource

/** One adb stream: blocking reads, and a close that wakes a blocked read. */
interface ShellStream : Closeable {
    /** Reads into [buffer] and returns the count, or -1 at the end. Throws once the stream is closed. */
    fun read(buffer: ByteArray): Int
}

/** Opens streams on a live adb connection. */
fun interface ShellStreamOpener {
    /** Throws [IOException] when there is no connection to open it on. */
    fun open(destination: String): ShellStream
}

/**
 * Runs commands through adb's `shell,v2` service, which reports the exit code and keeps stderr apart.
 *
 * A command that outlives its timeout has its stream closed, which ends the blocked read. Opening the stream counts
 * towards the timeout too: libadb waits for the daemon to answer an open with no limit of its own, so a daemon that
 * takes the connection and never answers would otherwise hold the run, and the connection, for good.
 */
class AdbShellExecutor(
    private val streams: ShellStreamOpener,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
) : ShellExecutor {

    override suspend fun run(command: ShellCommand): Either<ShellError, ShellResult> = withContext(ioDispatcher) {
        val began = TimeSource.Monotonic.markNow()
        val stream = try {
            // Interruptible, so that the timeout or a cancelled run wakes the thread waiting for the daemon.
            withTimeout(command.timeout) { runInterruptible { streams.open("shell,v2,raw:${command.line}") } }
        } catch (_: TimeoutCancellationException) {
            return@withContext ShellError.TimedOut.left()
        } catch (_: IOException) {
            // libadb reports being interrupted as an IOException as well.
            ensureActive()
            val error = if (began.elapsedNow() >= command.timeout) ShellError.TimedOut else ShellError.NotConnected
            return@withContext error.left()
        }
        val timedOut = AtomicBoolean(false)
        val watchdog = launch {
            delay(command.timeout - began.elapsedNow())
            timedOut.set(true)
            stream.closeQuietly()
        }
        val decoder = ShellProtocolDecoder()
        try {
            runInterruptible { readUntilExit(stream, decoder) }
            decoder.result()?.right() ?: ShellError.ConnectionLost.left()
        } catch (_: IOException) {
            // The daemon may close the stream right after the exit packet; the result is complete by then.
            val error = if (timedOut.get()) ShellError.TimedOut else ShellError.ConnectionLost
            decoder.result()?.right() ?: error.left()
        } finally {
            watchdog.cancel()
            stream.closeQuietly()
        }
    }

    private fun readUntilExit(stream: ShellStream, decoder: ShellProtocolDecoder) {
        val buffer = ByteArray(READ_BUFFER_SIZE)
        while (decoder.exitCode == null) {
            val count = stream.read(buffer)
            if (count < 0) return
            decoder.feed(buffer, count)
        }
    }

    private fun Closeable.closeQuietly() {
        try {
            close()
        } catch (_: IOException) {
            // Already closed, or the connection is gone; either way there is nothing left to release.
        }
    }

    private companion object {
        const val READ_BUFFER_SIZE = 64 * 1024
    }
}
