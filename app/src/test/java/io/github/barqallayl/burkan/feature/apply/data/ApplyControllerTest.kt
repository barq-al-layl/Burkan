package io.github.barqallayl.burkan.feature.apply.data

import io.github.barqallayl.burkan.core.model.AppErrorType
import io.github.barqallayl.burkan.core.shell.FakeShellExecutor
import io.github.barqallayl.burkan.core.shell.ShellCommands
import io.github.barqallayl.burkan.core.shell.ShellError
import io.github.barqallayl.burkan.feature.apply.data.FixtureDevice.replyLikeFixtureDevice
import io.github.barqallayl.burkan.feature.apply.model.ApplyKind
import io.github.barqallayl.burkan.feature.apply.model.RestoredSetting
import io.github.barqallayl.burkan.feature.apply.model.RunTrigger
import io.github.barqallayl.burkan.feature.apply.model.StepKind
import io.github.barqallayl.burkan.feature.connection.data.FakeShellAccess
import io.github.barqallayl.burkan.feature.connection.model.ConnectionError
import io.github.barqallayl.burkan.feature.log.data.FakeRunLogStorage
import io.github.barqallayl.burkan.feature.log.model.LoggedStep
import io.github.barqallayl.burkan.feature.log.model.RunResult
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.time.Clock
import kotlin.time.Duration.Companion.seconds
import kotlin.time.Instant

class ApplyControllerTest {

    private val shell = FakeShellExecutor().apply { replyLikeFixtureDevice() }
    private val access = FakeShellAccess(shell)
    private val log = FakeRunLogStorage()
    private val clock = SteppingClock()
    private val controller = ApplyController(access, log, clock, FixtureDevice.Self)

    @Test
    fun `a light run is logged with its steps`() = runTest {
        assertEquals(RunResult.Succeeded, controller.run(ApplyKind.Light, RunTrigger.Manual))

        val entry = log.runs.value.single()
        assertEquals(ApplyKind.Light, entry.kind)
        assertEquals(RunTrigger.Manual, entry.trigger)
        assertEquals(RunResult.Succeeded, entry.result)
        assertEquals(Instant.parse("2026-10-01T09:00:00Z"), entry.startedAt)
        assertEquals(5.seconds, entry.duration)
        assertEquals(
            listOf(StepKind.SetRenderer, StepKind.RestartSystemUi, StepKind.RestartLauncher, StepKind.RestartKeyboard),
            entry.steps.map { it.kind },
        )
        assertEquals(ApplyRunState.Idle, controller.state.value)
    }

    @Test
    fun `a full run logs its restore steps too`() = runTest {
        assertEquals(RunResult.Succeeded, controller.run(ApplyKind.Full, RunTrigger.Manual))

        val steps = log.runs.value.single().steps.map { it.kind }
        assertEquals(StepKind.StopApps(12), steps[1])
        assertEquals(StepKind.RestoreSetting(RestoredSetting.AutoRotation), steps[steps.size - 3])
    }

    @Test
    fun `a run that cannot connect is logged as failed, with why`() = runTest {
        access.failure = ConnectionError.NoWifi

        assertEquals(RunResult.Failed, controller.run(ApplyKind.Light, RunTrigger.Manual))

        val entry = log.runs.value.single()
        assertEquals(AppErrorType.NoWifi, entry.error)
        assertEquals(emptyList(), entry.steps)
    }

    @Test
    fun `a failed step fails the run`() = runTest {
        shell.fail(ShellCommands.crash(ShellCommands.SystemUi), ShellError.ConnectionLost)

        assertEquals(RunResult.Failed, controller.run(ApplyKind.Light, RunTrigger.Manual))

        assertEquals(
            LoggedStep(StepKind.RestartSystemUi, AppErrorType.ConnectionLost),
            log.runs.value.single().steps.last(),
        )
    }

    @Test
    fun `a cancelled run restores the settings and is logged as cancelled`() = runTest {
        val stopAll = CompletableDeferred<Unit>()
        shell.beforeEach = { command ->
            // Hold the bulk stop open until the run is cancelled.
            if (command.line.startsWith("am force-stop com.android.systemui;")) {
                stopAll.complete(Unit)
                awaitCancellation()
            }
        }

        val run = launch { controller.run(ApplyKind.Full, RunTrigger.Manual) }
        stopAll.await()
        run.cancel()
        run.join()

        val entry = log.runs.value.single()
        assertEquals(RunResult.Cancelled, entry.result)
        assertTrue(entry.steps.any { it.kind == StepKind.RestoreSetting(RestoredSetting.AutoRotation) })
        assertTrue(shell.lines.any { it.startsWith("settings put system accelerometer_rotation") })
        assertEquals(ApplyRunState.Idle, controller.state.value)
    }

    @Test
    fun `a second run while one is going does not start`() = runTest {
        val crashing = CompletableDeferred<Unit>()
        val release = CompletableDeferred<Unit>()
        shell.beforeEach = {
            if (it == ShellCommands.crash(ShellCommands.SystemUi)) {
                crashing.complete(Unit)
                release.await()
            }
        }

        val first = async { controller.run(ApplyKind.Light, RunTrigger.Manual) }
        crashing.await()
        assertNull(controller.run(ApplyKind.Full, RunTrigger.Manual))
        release.complete(Unit)

        assertEquals(RunResult.Succeeded, first.await())
        assertEquals(1, log.runs.value.size)
    }

    @Test
    fun `the state follows the run, step by step`() = runTest {
        val states = mutableListOf<ApplyRunState>()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { controller.state.collect { states += it } }

        controller.run(ApplyKind.Light, RunTrigger.Manual)
        runCurrent()

        val running = { phase: RunPhase -> ApplyRunState.Running(ApplyKind.Light, RunTrigger.Manual, phase) }
        assertEquals(
            listOf(
                ApplyRunState.Idle,
                running(RunPhase.Connecting),
                running(RunPhase.Reading),
                running(RunPhase.Step(StepKind.SetRenderer)),
                running(RunPhase.Step(StepKind.RestartSystemUi)),
                running(RunPhase.Step(StepKind.RestartLauncher)),
                running(RunPhase.Step(StepKind.RestartKeyboard)),
                ApplyRunState.Idle,
            ),
            states,
        )
    }

    /** Starts at a fixed time and moves five seconds on each reading. */
    private class SteppingClock : Clock {
        private var next = Instant.parse("2026-10-01T09:00:00Z")

        override fun now(): Instant = next.also { next += 5.seconds }
    }
}
