package io.github.barqallayl.burkan.feature.apply.data

import io.github.barqallayl.burkan.core.model.AppErrorType
import io.github.barqallayl.burkan.core.model.Renderer
import io.github.barqallayl.burkan.core.model.RendererStatus
import io.github.barqallayl.burkan.core.shell.FakeShellExecutor
import io.github.barqallayl.burkan.core.shell.PackageName
import io.github.barqallayl.burkan.core.shell.ShellCommands
import io.github.barqallayl.burkan.core.shell.ShellError
import io.github.barqallayl.burkan.core.shell.fixture
import io.github.barqallayl.burkan.core.storage.FakeSettingsStorage
import io.github.barqallayl.burkan.feature.apply.FakeLockEvents
import io.github.barqallayl.burkan.feature.apply.FakeSystemUiRestarts
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
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.time.Clock
import kotlin.time.Duration.Companion.seconds
import kotlin.time.Instant

class ApplyControllerTest {

    private val shell = FakeShellExecutor().apply {
        replyLikeFixtureDevice()
        reply(ShellCommands.getRenderer(), stdout = "\n")
        listOf(ShellCommands.SystemUi, FixtureDevice.Launcher, FixtureDevice.Keyboard).forEach {
            reply(ShellCommands.gfxInfo(it), stdout = fixture("gfxinfo-opengl.txt"))
        }
        // Once restarted, System UI answers on Vulkan.
        thenReply(ShellCommands.gfxInfo(ShellCommands.SystemUi), stdout = fixture("gfxinfo-vulkan.txt"))
    }
    private val access = FakeShellAccess(shell)
    private val log = FakeRunLogStorage()
    private val settings = FakeSettingsStorage()
    private val clock = SteppingClock()
    private val cooldown = SystemUiCooldown(FakeSystemUiRestarts(), FixedClock)
    private val lockEvents = FakeLockEvents()
    private val controller = ApplyController(access, log, settings, cooldown, lockEvents, clock, FixtureDevice.Self)

    @Test
    fun `a light run is logged with its steps`() = runTest {
        assertEquals(RunResult.Succeeded, controller.run(ApplyKind.Light, RunTrigger.Manual)?.result)

        val entry = log.runs.value.single()
        assertEquals(ApplyKind.Light, entry.kind)
        assertEquals(RunTrigger.Manual, entry.trigger)
        assertEquals(RunResult.Succeeded, entry.result)
        assertEquals(Instant.parse("2026-10-01T09:00:00Z"), entry.startedAt)
        assertEquals(5.seconds, entry.duration)
        assertEquals(
            listOf(StepKind.SetRenderer, StepKind.RestartLauncher, StepKind.RestartKeyboard, StepKind.RestartSystemUi),
            entry.steps.map { it.kind },
        )
        assertEquals(ApplyRunState.Idle, controller.state.value)
    }

    @Test
    fun `the status read at the end of a run is kept, and the connection is let go`() = runTest {
        val outcome = controller.run(ApplyKind.Light, RunTrigger.Manual)

        val finalStatus = RendererStatus(Renderer.OpenGL, Renderer.Vulkan, Renderer.OpenGL, Renderer.OpenGL)
        assertEquals(finalStatus, outcome?.status)
        assertEquals(VerifiedStatus(finalStatus, Instant.parse("2026-10-01T09:00:05Z")), controller.verified.value)
        assertEquals(1, access.releases)
    }

    @Test
    fun `after a restart System UI is left for the next lock`() = runTest {
        val outcome = controller.run(ApplyKind.Light, RunTrigger.Boot)

        assertEquals(RunResult.Succeeded, outcome?.result)
        assertTrue(outcome?.systemUiDeferred == true)
        assertTrue("am crash com.android.systemui" !in shell.lines)
        assertTrue(shell.lines.any { it.startsWith("am force-stop com.sec.android.app.launcher;") })
    }

    @Test
    fun `at the lock, adbd is given time to settle and only System UI is read before it restarts`() = runTest {
        lockEvents.lock()

        val outcome = controller.run(ApplyKind.Light, RunTrigger.AtLock)

        assertEquals(RunResult.Succeeded, outcome?.result)
        assertFalse(outcome?.systemUiDeferred == true)
        assertEquals(listOf(ApplyController.ADBD_SETTLE), access.settles)
        assertEquals(
            listOf(
                "getprop debug.hwui.renderer",
                "dumpsys gfxinfo com.android.systemui",
                "setprop debug.hwui.renderer skiavk",
                "am crash com.android.systemui",
            ),
            shell.lines.take(4),
        )
        val entry = log.runs.value.single()
        assertEquals(RunTrigger.AtLock, entry.trigger)
        assertEquals(StepKind.RestartSystemUi, entry.steps.last().kind)
    }

    @Test
    fun `a phone unlocked during the wait at the lock is not locked again`() = runTest {
        lockEvents.lock()

        val run = async { controller.run(ApplyKind.Light, RunTrigger.AtLock) }
        // The user unlocks while adbd settles, before the run has connected.
        advanceTimeBy(1.seconds)
        lockEvents.locked = false
        val outcome = run.await()

        assertEquals(RunResult.Postponed, outcome?.result)
        assertTrue(outcome?.systemUiDeferred == true, "left for the next lock")
        assertTrue(shell.lines.none { it.startsWith("am crash") }, "System UI was not restarted")
        assertEquals(1, access.releases, "the connection is closed and wireless debugging switched back")
        val entry = log.runs.value.single()
        assertEquals(RunTrigger.AtLock to RunResult.Postponed, entry.trigger to entry.result)
        assertTrue(entry.steps.none { it.kind == StepKind.RestartSystemUi })
    }

    @Test
    fun `a phone unlocked just before the crash is not locked again`() = runTest {
        lockEvents.lock()
        // The last thing read before the crash is System UI's renderer: the user unlocks while it is read.
        val systemUiRead = ShellCommands.gfxInfo(ShellCommands.SystemUi).line
        shell.beforeEach = { if (it.line == systemUiRead) lockEvents.locked = false }

        val outcome = controller.run(ApplyKind.Light, RunTrigger.AtLock)

        assertEquals(RunResult.Postponed, outcome?.result)
        assertTrue(shell.lines.none { it.startsWith("am crash") })
    }

    @Test
    fun `a full run logs its restore steps too`() = runTest {
        assertEquals(RunResult.Succeeded, controller.run(ApplyKind.Full, RunTrigger.Manual)?.result)

        val steps = log.runs.value.single().steps.map { it.kind }
        assertEquals(StepKind.StopApps(12), steps[1])
        assertEquals(StepKind.RestoreSetting(RestoredSetting.AutoRotation), steps[steps.size - 4])
        assertEquals(StepKind.RestartSystemUi, steps.last())
    }

    @Test
    fun `the user's exclusions are left running`() = runTest {
        settings.userExclusions.value = setOf(PackageName.known("org.example.weather"))

        controller.run(ApplyKind.Full, RunTrigger.Manual)

        val stopAll = shell.lines.single { it.startsWith("am force-stop com.android.systemui;") }
        assertFalse("org.example.weather" in stopAll)
        assertTrue("com.example.notes;" in stopAll)
    }

    @Test
    fun `a run that cannot connect is logged as failed, with why`() = runTest {
        access.failure = ConnectionError.NoWifi

        assertEquals(
            RunOutcome(RunResult.Failed, ConnectionError.NoWifi),
            controller.run(ApplyKind.Light, RunTrigger.Manual),
        )

        val entry = log.runs.value.single()
        assertEquals(AppErrorType.NoWifi, entry.error)
        assertEquals(emptyList(), entry.steps)
    }

    @Test
    fun `a failed step fails the run, and System UI is left alone`() = runTest {
        shell.fail(ShellCommands.crash(FixtureDevice.Keyboard), ShellError.ConnectionLost)

        val outcome = controller.run(ApplyKind.Light, RunTrigger.Manual)

        assertEquals(RunResult.Failed, outcome?.result)
        assertEquals(ShellError.ConnectionLost, outcome?.error)
        assertEquals(
            LoggedStep(StepKind.RestartKeyboard, AppErrorType.ConnectionLost),
            log.runs.value.single().steps.last(),
        )
        assertTrue("am crash com.android.systemui" !in shell.lines)
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

        assertEquals(RunResult.Succeeded, first.await()?.result)
        assertEquals(1, log.runs.value.size)
    }

    @Test
    fun `a run that finds Vulkan already in place changes nothing`() = runTest {
        shell.reply(ShellCommands.getRenderer(), stdout = "skiavk\n")
        listOf(ShellCommands.SystemUi, FixtureDevice.Launcher, FixtureDevice.Keyboard).forEach {
            shell.reply(ShellCommands.gfxInfo(it), stdout = fixture("gfxinfo-vulkan.txt"))
        }

        val outcome = controller.run(ApplyKind.Light, RunTrigger.Boot)

        val vulkan = RendererStatus(Renderer.Vulkan, Renderer.Vulkan, Renderer.Vulkan, Renderer.Vulkan)
        assertEquals(RunOutcome(RunResult.AlreadyApplied, status = vulkan), outcome)
        assertEquals(
            listOf(
                "getprop debug.hwui.renderer",
                "cmd package resolve-activity --brief -a android.intent.action.MAIN -c android.intent.category.HOME",
                "settings get secure default_input_method",
                "dumpsys gfxinfo com.android.systemui",
                "dumpsys gfxinfo com.sec.android.app.launcher",
                "dumpsys gfxinfo com.samsung.android.honeyboard",
            ),
            shell.lines,
        )
        assertEquals(RunResult.AlreadyApplied, log.runs.value.single().result)
    }

    @Test
    fun `only what is not in place yet is done`() = runTest {
        shell.reply(ShellCommands.getRenderer(), stdout = "skiavk\n")
        shell.reply(ShellCommands.gfxInfo(FixtureDevice.Launcher), stdout = fixture("gfxinfo-vulkan.txt"))
        shell.reply(ShellCommands.gfxInfo(FixtureDevice.Keyboard), stdout = fixture("gfxinfo-vulkan.txt"))

        val outcome = controller.run(ApplyKind.Light, RunTrigger.Manual)

        assertEquals(RunResult.Succeeded, outcome?.result)
        assertEquals(listOf(StepKind.RestartSystemUi), log.runs.value.single().steps.map { it.kind })
        assertTrue(shell.lines.none { it.startsWith("setprop") }, "the property was set already")
    }

    @Test
    fun `the run at the lock, after the first part set the property, only restarts System UI`() = runTest {
        shell.reply(ShellCommands.getRenderer(), stdout = "skiavk\n")
        lockEvents.lock()

        controller.run(ApplyKind.Light, RunTrigger.AtLock)

        assertEquals(
            listOf(
                "getprop debug.hwui.renderer",
                "dumpsys gfxinfo com.android.systemui",
                "am crash com.android.systemui",
            ),
            shell.lines.take(3),
        )
        assertEquals(listOf(StepKind.RestartSystemUi), log.runs.value.single().steps.map { it.kind })
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
                running(RunPhase.Step(StepKind.RestartLauncher)),
                running(RunPhase.Step(StepKind.RestartKeyboard)),
                running(RunPhase.Step(StepKind.RestartSystemUi)),
                running(RunPhase.Checking),
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
