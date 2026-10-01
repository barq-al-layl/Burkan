package io.github.barqallayl.burkan.feature.log.model

import io.github.barqallayl.burkan.core.model.AppErrorType
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
}

/**
 * One run, as the log keeps it. [error] is why a run failed before any step started (no connection, say). Steps hold
 * counts, never package names, so the log says what was done without listing anyone's apps.
 */
data class RunLogEntry(
    val startedAt: Instant,
    val trigger: RunTrigger,
    val kind: ApplyKind,
    val result: RunResult,
    val duration: Duration,
    val steps: List<LoggedStep>,
    val error: AppErrorType? = null,
)

/** A finished step; [error] is null when it succeeded. */
data class LoggedStep(val kind: StepKind, val error: AppErrorType? = null)
