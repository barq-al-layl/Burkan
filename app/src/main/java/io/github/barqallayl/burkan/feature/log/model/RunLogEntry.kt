package io.github.barqallayl.burkan.feature.log.model

import io.github.barqallayl.burkan.core.model.AppError
import io.github.barqallayl.burkan.core.model.AppErrorType
import io.github.barqallayl.burkan.core.model.RendererStatus
import io.github.barqallayl.burkan.feature.apply.model.ApplyError
import io.github.barqallayl.burkan.feature.apply.model.ApplyKind
import io.github.barqallayl.burkan.feature.apply.model.RunTrigger
import io.github.barqallayl.burkan.feature.apply.model.StepKind
import kotlin.time.Duration
import kotlin.time.Instant

enum class RunResult {
    Succeeded,
    Failed,
    Cancelled,

    /** An automatic run found Vulkan already in place and changed nothing. */
    AlreadyApplied,

    /**
     * The run at the lock found the phone unlocked again before it restarted System UI, and left it for the next
     * lock rather than lock the phone in front of the user.
     */
    Postponed,
}

/**
 * One run, as the log keeps it. [error] is why a run failed before any step started (no connection, say). Steps hold
 * counts, never package names, so the log says what was done without listing anyone's apps.
 *
 * The rest is what someone reading the log to find a fault needs, and each is null in a run logged before it was
 * kept, or when the run never got that far:
 * - [before] and [after]: what the phone ran on when the run read it at its start, and at its end;
 * - [preparing]: how long it took to connect and read the phone, before the first step;
 * - [behind]: how many open apps the run left on the other renderer;
 * - [detail]: with [error], the failed command's own words (see [commandDetail]).
 */
data class RunLogEntry(
    val startedAt: Instant,
    val trigger: RunTrigger,
    val kind: ApplyKind,
    val result: RunResult,
    val duration: Duration,
    val steps: List<LoggedStep>,
    val error: AppErrorType? = null,
    val before: RendererStatus? = null,
    val after: RendererStatus? = null,
    val preparing: Duration? = null,
    val behind: Int? = null,
    val detail: String? = null,
)

/**
 * A finished step; [error] is null when it succeeded. [duration] is how long it took, and [detail] the failed
 * command's own words; both null in a run logged before they were kept.
 */
data class LoggedStep(
    val kind: StepKind,
    val error: AppErrorType? = null,
    val duration: Duration? = null,
    val detail: String? = null,
)

/**
 * What a command that failed said for itself: its exit status and the first line it wrote to its error output, cut
 * short. Null for any other failure, which has a message of its own.
 *
 * It can name the package the command was about. That is System UI, the home app or the keyboard, since the
 * commands that go through the user's apps end in `true` and never fail by their status: the log still lists
 * nobody's apps.
 */
fun commandDetail(failure: AppError?): String? {
    val failed = failure as? ApplyError.CommandFailed ?: return null
    val said = failed.stderr.lineSequence().map { it.trim() }.firstOrNull { it.isNotEmpty() }
    val status = "exit ${failed.exitCode}"
    return if (said == null) status else "$status: ${said.take(DETAIL_LENGTH)}"
}

private const val DETAIL_LENGTH = 200
