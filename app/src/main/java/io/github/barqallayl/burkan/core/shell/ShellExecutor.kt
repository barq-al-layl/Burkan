package io.github.barqallayl.burkan.core.shell

import arrow.core.Either
import io.github.barqallayl.burkan.core.model.AppError

/** Runs commands as the `shell` user. Everything above this interface is plain Kotlin, tested with a fake. */
interface ShellExecutor {
    /**
     * Runs one command line, giving up after [ShellCommand.timeout]. A non-zero exit is a [ShellResult], not an
     * error.
     */
    suspend fun run(command: ShellCommand): Either<ShellError, ShellResult>
}

data class ShellResult(val exitCode: Int, val stdout: String, val stderr: String)

/** What stops a command from running to the end at all. */
sealed interface ShellError : AppError {
    data object NotConnected : ShellError
    data object ConnectionLost : ShellError
    data object TimedOut : ShellError
}
