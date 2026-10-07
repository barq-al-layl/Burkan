package io.github.barqallayl.burkan.core.shell

import arrow.core.left
import arrow.core.right
import kotlinx.coroutines.test.runTest
import org.junit.Test
import kotlin.test.assertEquals

class ReconnectingShellExecutorTest {

    private val shell = FakeShellExecutor()
    private var reconnects = 0
    private var reconnectSucceeds = true
    private val executor = ReconnectingShellExecutor(shell) {
        reconnects++
        reconnectSucceeds
    }

    private val ok = ShellResult(0, "", "").right()

    @Test
    fun `a harmless command that lost its connection is sent again once reconnected`() = runTest {
        shell.failOnce(ShellCommands.getRenderer(), ShellError.ConnectionLost)

        assertEquals(ok, executor.run(ShellCommands.getRenderer()))
        assertEquals(1, reconnects)
        assertEquals(listOf("getprop debug.hwui.renderer", "getprop debug.hwui.renderer"), shell.lines)
    }

    @Test
    fun `a command whose stream never opened was not sent, so it is sent, crash or not`() = runTest {
        val crash = ShellCommands.crash(ShellCommands.SystemUi)
        shell.failOnce(crash, ShellError.NotConnected)

        assertEquals(ok, executor.run(crash))
        assertEquals(1, reconnects)
        assertEquals(2, shell.lines.size)
    }

    @Test
    fun `a crash that lost its connection is not sent again, and the next command reconnects first`() = runTest {
        val crash = ShellCommands.crash(ShellCommands.SystemUi)
        shell.failOnce(crash, ShellError.ConnectionLost)

        assertEquals(ShellError.ConnectionLost.left(), executor.run(crash))
        assertEquals(0, reconnects, "nothing to wait for until another command is sent")

        assertEquals(ok, executor.run(ShellCommands.gfxInfo(ShellCommands.SystemUi)))
        assertEquals(1, reconnects)
        assertEquals(listOf("am crash \"\$(pidof -s com.android.systemui || echo com.android.systemui)\"", "dumpsys gfxinfo com.android.systemui"), shell.lines)
    }

    @Test
    fun `when reconnecting fails, the error stands`() = runTest {
        reconnectSucceeds = false
        shell.failOnce(ShellCommands.getRenderer(), ShellError.ConnectionLost)

        assertEquals(ShellError.ConnectionLost.left(), executor.run(ShellCommands.getRenderer()))
        assertEquals(1, shell.lines.size)
    }

    @Test
    fun `a command that timed out is not a lost connection`() = runTest {
        shell.fail(ShellCommands.dumpProcesses(), ShellError.TimedOut)

        assertEquals(ShellError.TimedOut.left(), executor.run(ShellCommands.dumpProcesses()))
        assertEquals(0, reconnects)
    }

    @Test
    fun `a connection that keeps dropping is given up after three reconnections`() = runTest {
        shell.fail(ShellCommands.getRenderer(), ShellError.ConnectionLost)

        assertEquals(ShellError.ConnectionLost.left(), executor.run(ShellCommands.getRenderer()))
        assertEquals(3, reconnects)
        assertEquals(4, shell.lines.size)
    }
}
