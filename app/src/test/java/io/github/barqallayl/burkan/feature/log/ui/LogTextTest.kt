package io.github.barqallayl.burkan.feature.log.ui

import io.github.barqallayl.burkan.core.model.AppErrorType
import io.github.barqallayl.burkan.feature.apply.model.ApplyKind
import io.github.barqallayl.burkan.feature.apply.model.RunTrigger
import io.github.barqallayl.burkan.feature.apply.model.StepKind
import io.github.barqallayl.burkan.feature.log.model.LoggedStep
import io.github.barqallayl.burkan.feature.log.model.RunLogEntry
import io.github.barqallayl.burkan.feature.log.model.RunResult
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.seconds
import kotlin.time.Instant

/** What the Log says about a run before it is opened. */
class LogTextTest {

    private fun run(result: RunResult, error: AppErrorType? = null, steps: List<LoggedStep> = emptyList()) =
        RunLogEntry(
            startedAt = Instant.parse("2026-10-01T09:30:00Z"),
            trigger = RunTrigger.Boot,
            kind = ApplyKind.Light,
            result = result,
            duration = 4.seconds,
            steps = steps,
            error = error,
        )

    @Test
    fun `a failed run's reason is its own error, or else the failed step's`() {
        assertEquals(AppErrorType.NoWifi, run(RunResult.Failed, error = AppErrorType.NoWifi).failureReason)
        val stepFailed = run(
            RunResult.Failed,
            steps = listOf(
                LoggedStep(StepKind.SetRenderer),
                LoggedStep(StepKind.RestartLauncher, AppErrorType.CommandFailed),
            ),
        )
        assertEquals(AppErrorType.CommandFailed, stepFailed.failureReason)
    }

    @Test
    fun `a run that did not fail has no reason to show`() {
        assertNull(run(RunResult.Succeeded).failureReason)
        assertNull(run(RunResult.Cancelled, error = AppErrorType.ConnectionLost).failureReason)
    }

    @Test
    fun `only results that want attention carry a pill`() {
        assertTrue(RunResult.Failed.wantsAttention)
        assertTrue(RunResult.Cancelled.wantsAttention)
        assertTrue(RunResult.Postponed.wantsAttention)
        assertFalse(RunResult.Succeeded.wantsAttention)
        assertFalse(RunResult.AlreadyApplied.wantsAttention)
    }

    @Test
    fun `the Not finished filter holds the cancelled runs and those left for the lock`() {
        assertTrue(LogFilter.Other.accepts(RunResult.Cancelled))
        assertTrue(LogFilter.Other.accepts(RunResult.Postponed))
        assertFalse(LogFilter.Other.accepts(RunResult.Failed))
    }
}
