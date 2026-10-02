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

    /** Answers per command line, in order; the last one repeats. */
    private val replies = mutableMapOf<String, ArrayDeque<Either<ShellError, ShellResult>>>()

    /** Every command run, in order. */
    val commands = mutableListOf<ShellCommand>()

    val lines: List<String> get() = commands.map { it.line }

    /** Runs before each command is answered; a test can suspend or throw here. */
    var beforeEach: suspend (ShellCommand) -> Unit = {}

    fun reply(command: ShellCommand, stdout: String = "", stderr: String = "", exitCode: Int = 0) {
        replies[command.line] = ArrayDeque(listOf(ShellResult(exitCode, stdout, stderr).right()))
    }

    /** Adds an answer after those already given for [command]: the command answers differently the next time. */
    fun thenReply(command: ShellCommand, stdout: String = "", stderr: String = "", exitCode: Int = 0) {
        replies.getOrPut(command.line) { ArrayDeque() } += ShellResult(exitCode, stdout, stderr).right()
    }

    fun fail(command: ShellCommand, error: ShellError) {
        replies[command.line] = ArrayDeque(listOf(error.left()))
    }

    /** [command] fails with [error] once, then answers as before. */
    fun failOnce(command: ShellCommand, error: ShellError) {
        val answers = replies.getOrPut(command.line) { ArrayDeque() }
        if (answers.isEmpty()) answers += ShellResult(0, "", "").right()
        answers.addFirst(error.left())
    }

    /** Any command without a reply succeeds with no output. Like a real connection, it refuses once cancelled. */
    override suspend fun run(command: ShellCommand): Either<ShellError, ShellResult> {
        currentCoroutineContext().ensureActive()
        commands += command
        beforeEach(command)
        val answers = replies[command.line] ?: return ShellResult(exitCode = 0, stdout = "", stderr = "").right()
        return if (answers.size > 1) answers.removeFirst() else answers.first()
    }
}

/** A file from `src/test/resources/fixtures/`. */
fun fixture(name: String): String =
    checkNotNull(FakeShellExecutor::class.java.getResource("/fixtures/$name")) { "No fixture $name" }.readText()
