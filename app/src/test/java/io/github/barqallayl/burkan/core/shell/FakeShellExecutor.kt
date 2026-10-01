package io.github.barqallayl.burkan.core.shell

import arrow.core.Either
import arrow.core.left
import arrow.core.right
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive

/**
 * A shell that records every command and answers from canned replies. Replies are keyed by the command line that
 * [ShellCommands] builds, so a test cannot drift from the commands the app really sends.
 */
class FakeShellExecutor : ShellExecutor {

    private val replies = mutableMapOf<String, Either<ShellError, ShellResult>>()

    /** Every command run, in order. */
    val commands = mutableListOf<ShellCommand>()

    val lines: List<String> get() = commands.map { it.line }

    /** Runs before each command is answered; a test can suspend or throw here. */
    var beforeEach: suspend (ShellCommand) -> Unit = {}

    fun reply(command: ShellCommand, stdout: String = "", stderr: String = "", exitCode: Int = 0) {
        replies[command.line] = ShellResult(exitCode = exitCode, stdout = stdout, stderr = stderr).right()
    }

    fun fail(command: ShellCommand, error: ShellError) {
        replies[command.line] = error.left()
    }

    /** Any command without a reply succeeds with no output. Like a real connection, it refuses once cancelled. */
    override suspend fun run(command: ShellCommand): Either<ShellError, ShellResult> {
        currentCoroutineContext().ensureActive()
        commands += command
        beforeEach(command)
        return replies[command.line] ?: ShellResult(exitCode = 0, stdout = "", stderr = "").right()
    }
}

/** A file from `src/test/resources/fixtures/`. */
fun fixture(name: String): String =
    checkNotNull(FakeShellExecutor::class.java.getResource("/fixtures/$name")) { "No fixture $name" }.readText()
