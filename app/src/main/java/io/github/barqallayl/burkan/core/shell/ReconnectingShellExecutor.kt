package io.github.barqallayl.burkan.core.shell

import arrow.core.Either

/**
 * A [ShellExecutor] that carries on after the connection drops. adbd restarts whenever the keyguard changes, and a
 * run that restarts System UI brings up the lock screen itself, so a lost connection is expected rather than fatal.
 *
 * On a lost connection it calls [reconnect], which finds the daemon's new port and connects again within its own time
 * budget, and then:
 * - a command whose stream never opened was never sent, so it is sent now;
 * - a command that lost its connection part way is sent again only when it is [ShellCommand.repeatable]. Otherwise its
 *   outcome is unknown: the error is returned, and the next command reconnects before it is sent.
 *
 * Gives up after [MAX_RECONNECTS] reconnections for one command, so a connection that keeps dropping ends the run.
 */
class ReconnectingShellExecutor(
    private val delegate: ShellExecutor,
    private val reconnect: suspend () -> Boolean,
) : ShellExecutor {

    /** Set when a command lost its connection and was not sent again: the next one reconnects first. */
    private var lost = false

    override suspend fun run(command: ShellCommand): Either<ShellError, ShellResult> {
        if (lost) {
            if (!reconnect()) return Either.Left(ShellError.NotConnected)
            lost = false
        }
        var result = delegate.run(command)
        repeat(MAX_RECONNECTS) {
            when (result.leftOrNull()) {
                ShellError.NotConnected -> Unit
                ShellError.ConnectionLost -> if (!command.repeatable) {
                    lost = true
                    return result
                }
                ShellError.TimedOut, null -> return result
            }
            if (!reconnect()) return result
            result = delegate.run(command)
        }
        return result
    }

    private companion object {
        const val MAX_RECONNECTS = 3
    }
}
