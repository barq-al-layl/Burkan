package io.github.barqallayl.burkan.feature.connection.data

import arrow.core.left
import arrow.core.right
import io.github.barqallayl.burkan.core.shell.ShellCommands
import io.github.barqallayl.burkan.core.shell.ShellError
import io.github.barqallayl.burkan.core.shell.ShellResult
import io.github.barqallayl.burkan.feature.connection.model.ConnectionError
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.delay
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
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

class ShizukuShellAccessTest {

    private val client = FakeShizukuClient()

    private fun TestScope.access() = ShizukuShellAccess(client, backgroundScope)

    /** Lets the shell kept after a block end. */
    private fun TestScope.afterLinger() {
        advanceTimeBy(ShizukuShellAccess.LINGER + 1.seconds)
        runCurrent()
    }

    @Test
    fun `a block runs on Shizuku's shell, which is ended after a moment`() = runTest {
        val result = access().withShell { shell -> shell.run(ShellCommands.echoOk()) }
        afterLinger()

        assertEquals(ShellResult(0, "", "").right().right(), result)
        assertEquals(listOf("connect", "disconnect"), client.calls)
    }

    @Test
    fun `the shell is kept for a moment, and a block in that time reuses it`() = runTest {
        val access = access()

        access.withShell { }
        advanceTimeBy(5.seconds)
        access.withShell { }
        assertEquals(listOf("connect"), client.calls, "one shell, one process in Shizuku")

        afterLinger()
        assertEquals(listOf("connect", "disconnect"), client.calls)
    }

    @Test
    fun `release ends the shell at once`() = runTest {
        val access = access()

        access.withShell { }
        access.release()

        assertEquals(listOf("connect", "disconnect"), client.calls)
        afterLinger()
        assertEquals(2, client.calls.size, "nothing is ended twice")
    }

    @Test
    fun `release with no shell does nothing`() = runTest {
        access().release()

        assertEquals(emptyList(), client.calls)
    }

    @Test
    fun `Shizuku not running is reported at once when the run has no patience`() = runTest {
        client.running = false

        assertEquals(ConnectionError.ShizukuNotRunning.left(), access().withShell { currentTime })
        assertEquals(0L, currentTime)
        assertEquals(emptyList(), client.calls)
    }

    @Test
    fun `Shizuku starting itself is waited for, as long as the run is patient`() = runTest {
        client.running = false
        launch {
            delay(20.seconds)
            client.running = true
        }

        val result = access().withShell(patience = 2.minutes) { currentTime }

        assertEquals(20_000L.right(), result)
        assertEquals(listOf("connect"), client.calls)
    }

    @Test
    fun `patience runs out`() = runTest {
        client.running = false

        val result = access().withShell(patience = 2.minutes) { }

        assertEquals(ConnectionError.ShizukuNotRunning.left(), result)
        assertEquals(2.minutes.inWholeMilliseconds, currentTime)
    }

    @Test
    fun `Shizuku that does not allow the app is reported, and no shell is started`() = runTest {
        client.allowed = false

        assertEquals(ConnectionError.ShizukuNotAllowed.left(), access().withShell { })
        assertEquals(emptyList(), client.calls)
    }

    @Test
    fun `a shell that does not start is reported`() = runTest {
        client.connectResult = ConnectionError.ShizukuShellFailed.left()

        assertEquals(ConnectionError.ShizukuShellFailed.left(), access().withShell { })
        assertEquals(listOf("connect"), client.calls)
        afterLinger()
        assertEquals(listOf("connect"), client.calls, "there was nothing to end")
    }

    @Test
    fun `a failing block ends the shell at once`() = runTest {
        assertFailsWith<IllegalStateException> { access().withShell { error("boom") } }

        assertEquals("disconnect", client.calls.last())
    }

    @Test
    fun `a cancelled run ends the shell at once`() = runTest {
        val access = access()
        val run = launch { access.withShell { awaitCancellation() } }
        runCurrent()

        run.cancel()
        run.join()

        assertEquals("disconnect", client.calls.last())
    }

    @Test
    fun `runs do not overlap`() = runTest {
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
    fun `a shell lost part way is started again, and the command carries on`() = runTest {
        val result = access().withShell { shell ->
            // Shizuku was restarted: the shell it had is gone.
            client.connected = false
            shell.run(ShellCommands.echoOk())
        }

        assertEquals(ShellResult(0, "", "").right().right(), result)
        assertEquals(listOf("connect", "connect"), client.calls)
    }

    @Test
    fun `a shell that does not come back within 30 seconds fails the command`() = runTest {
        val result = access().withShell { shell ->
            client.connected = false
            client.running = false
            shell.run(ShellCommands.echoOk()) to currentTime
        }

        val (command, elapsed) = result.getOrNull()!!
        assertEquals(ShellError.NotConnected.left(), command)
        assertTrue(elapsed in 30_000L..31_000L, "gave up after $elapsed ms")
    }

    @Test
    fun `a permission taken back meanwhile ends the wait for the shell at once`() = runTest {
        val result = access().withShell { shell ->
            client.connected = false
            client.allowed = false
            shell.run(ShellCommands.echoOk()) to currentTime
        }

        assertEquals(ShellError.NotConnected.left() to 500L, result.getOrNull())
    }
}
