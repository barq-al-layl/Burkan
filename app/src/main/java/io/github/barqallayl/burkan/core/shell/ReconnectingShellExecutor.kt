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
 *
 * One reconnection that fails is final: [reconnect] has spent its whole budget, and the next command would only spend
 * it again. Every command after it fails at once with [ShellError.NotConnected], so a run that lost its shell gets to
 * the steps that do without one, putting the settings back from the app, straight away. On a Galaxy S23 the three of
 * them had each waited 30 seconds for a Shizuku that had stopped.
 */
class ReconnectingShellExecutor(
    private val delegate: ShellExecutor,
    private val reconnect: suspend () -> Boolean,
) : ShellExecutor {

    /** Set when a command lost its connection and was not sent again: the next one reconnects first. */
    private var lost = false

    /** Set when a reconnection failed. The shell is taken as gone for good. */
    private var gone = false

    override suspend fun run(command: ShellCommand): Either<ShellError, ShellResult> {
        if (gone) return Either.Left(ShellError.NotConnected)
        if (lost) {
            if (!reconnected()) return Either.Left(ShellError.NotConnected)
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
                ShellError.TimedOut, ShellError.Failed, null -> return result
            }
            if (!reconnected()) return result
            result = delegate.run(command)
        }
        return result
    }

    private suspend fun reconnected(): Boolean = reconnect().also { if (!it) gone = true }

    private companion object {
        const val MAX_RECONNECTS = 3
    }
}
