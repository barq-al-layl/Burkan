package io.github.barqallayl.burkan.core.shell

import arrow.core.left
import arrow.core.right
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Test
import java.io.IOException
import java.util.concurrent.CountDownLatch
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

class AdbShellExecutorTest {

    private val command = ShellCommand("pm list packages", 10.seconds)

    @Test
    fun `stdout, stderr and the exit code come back apart`() = runTest {
        val bytes = packet(1, "package:com.example.a\n") + packet(2, "Error: user 150\n") + packet(1, "package:b\n") +
            exit(0)
        val opener = RecordingOpener { BytesStream(bytes) }

        val result = AdbShellExecutor(opener, StandardTestDispatcher(testScheduler)).run(command)

        assertEquals(ShellResult(0, "package:com.example.a\npackage:b\n", "Error: user 150\n").right(), result)
        assertEquals(listOf("shell,v2,raw:pm list packages"), opener.destinations)
    }

    @Test
    fun `a failing command returns its real exit code`() = runTest {
        val executor = AdbShellExecutor(
            { BytesStream(packet(2, "no\n") + exit(255)) },
            StandardTestDispatcher(testScheduler),
        )

        assertEquals(ShellResult(255, "", "no\n").right(), executor.run(command))
    }

    @Test
    fun `packets split across reads are reassembled`() = runTest {
        val bytes = packet(1, "x".repeat(70_000)) + exit(3)
        // One byte at a time crosses every header and length boundary.
        val executor = AdbShellExecutor({ BytesStream(bytes, chunk = 1) }, StandardTestDispatcher(testScheduler))

        assertEquals(ShellResult(3, "x".repeat(70_000), "").right(), executor.run(command))
    }

    @Test
    fun `the stream closing after the exit packet is a normal end`() = runTest {
        val executor = AdbShellExecutor(
            { BytesStream(packet(1, "ok\n") + exit(0), throwAtEnd = true) },
            StandardTestDispatcher(testScheduler),
        )

        assertEquals(ShellResult(0, "ok\n", "").right(), executor.run(command))
    }

    @Test
    fun `the stream ending before an exit packet is a lost connection`() = runTest {
        val executor = AdbShellExecutor({ BytesStream(packet(1, "par")) }, StandardTestDispatcher(testScheduler))
        assertEquals(ShellError.ConnectionLost.left(), executor.run(command))

        val throwing = AdbShellExecutor(
            { BytesStream(packet(1, "par"), throwAtEnd = true) },
            StandardTestDispatcher(testScheduler),
        )
        assertEquals(ShellError.ConnectionLost.left(), throwing.run(command))
    }

    @Test
    fun `no connection to open a stream on is not connected`() = runTest {
        val executor = AdbShellExecutor(
            { throw IOException("Not connected to ADB.") },
            StandardTestDispatcher(testScheduler),
        )

        assertEquals(ShellError.NotConnected.left(), executor.run(command))
    }

    @Test
    fun `a command that outlives its timeout is closed and reported`() = runBlocking {
        // Real threads: the read blocks until the watchdog closes the stream.
        val stream = BlockingStream()
        val executor = AdbShellExecutor({ stream })

        val result = executor.run(ShellCommand("dumpsys activity processes", 200.milliseconds))

        assertEquals(ShellError.TimedOut.left(), result)
        assertEquals(true, stream.closed)
    }

    @Test
    fun `the decoder waits for a whole packet`() {
        val decoder = ShellProtocolDecoder()
        val bytes = packet(1, "hello") + exit(0)

        decoder.feed(bytes.copyOfRange(0, 7))
        assertNull(decoder.result())
        decoder.feed(bytes.copyOfRange(7, bytes.size))

        assertEquals(ShellResult(0, "hello", ""), decoder.result())
    }

    @Test
    fun `a packet of a length adbd could not send is a lost connection, not a crash`() = runTest {
        val negative = byteArrayOf(1, 0xFF.toByte(), 0xFF.toByte(), 0xFF.toByte(), 0xFF.toByte(), 0)
        val enormous = byteArrayOf(1, 0, 0, 0, 0x7F, 0)
        listOf(negative, enormous).forEach { bytes ->
            val executor = AdbShellExecutor({ BytesStream(bytes) }, StandardTestDispatcher(testScheduler))

            assertEquals(ShellError.ConnectionLost.left(), executor.run(command))
        }
    }

    /** Never delivers data; closing it wakes the read with an IOException, as libadb's stream does. */
    private class BlockingStream : ShellStream {
        private val released = CountDownLatch(1)
        @Volatile
        var closed = false

        override fun read(buffer: ByteArray): Int {
            released.await()
            throw IOException("Stream closed.")
        }

        override fun close() {
            closed = true
            released.countDown()
        }
    }
}
