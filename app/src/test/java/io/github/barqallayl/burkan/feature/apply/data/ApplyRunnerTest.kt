package io.github.barqallayl.burkan.feature.apply.data

import arrow.core.getOrElse
import io.github.barqallayl.burkan.core.model.Renderer
import io.github.barqallayl.burkan.core.model.RendererStatus
import io.github.barqallayl.burkan.core.shell.FakeShellExecutor
import io.github.barqallayl.burkan.core.shell.PackageName
import io.github.barqallayl.burkan.core.shell.ReconnectingShellExecutor
import io.github.barqallayl.burkan.core.shell.SettingKey
import io.github.barqallayl.burkan.core.shell.ShellCommands
import io.github.barqallayl.burkan.core.shell.ShellError
import io.github.barqallayl.burkan.core.shell.Surfaces
import io.github.barqallayl.burkan.core.shell.fixture
import io.github.barqallayl.burkan.feature.apply.data.FixtureDevice.replyLikeFixtureDevice
import io.github.barqallayl.burkan.feature.apply.model.ApplyError
import io.github.barqallayl.burkan.feature.apply.model.RestoredSetting
import io.github.barqallayl.burkan.feature.apply.model.StepKind
import io.github.barqallayl.burkan.feature.apply.model.StepRecord
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.currentTime
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.test.fail
import kotlin.time.Duration.Companion.seconds

class ApplyRunnerTest {

    private val shell = FakeShellExecutor().apply {
        replyLikeFixtureDevice()
        reply(ShellCommands.gfxInfo(ShellCommands.SystemUi), stdout = fixture("gfxinfo-vulkan.txt"))
    }
    private val clock = TestClock()
    private val cooldown = SystemUiCooldown(InMemorySystemUiRestarts(), clock)
    private val runner = ApplyRunner(shell, cooldown)
    private val records = mutableListOf<StepRecord>()

    private val restoreLines = listOf(
        "settings put system accelerometer_rotation '1'",
        "settings get system accelerometer_rotation",
        "settings put secure enabled_accessibility_services '${FixtureDevice.ACCESSIBILITY}'",
        "settings get secure enabled_accessibility_services",
        "settings put secure edge_enable '1'",
        "settings get secure edge_enable",
    )

    @Test
    fun `a failed step stops the run and the settings are still restored`() = runTest {
        val plan = fullPlan()
        plan.steps.forEachIndexed { failing, step ->
            val shell = FakeShellExecutor().apply {
                replyLikeFixtureDevice()
                fail(step.command, ShellError.ConnectionLost)
            }
            val records = mutableListOf<StepRecord>()

            ApplyRunner(shell, cooldown).run(plan) { records += it }

            // Nothing after the restore: System UI is not restarted after a failure.
            val ran = plan.steps.take(failing + 1).map { it.command.line }
            assertEquals(ran + restoreLines, shell.lines, "failing at step $failing")
            assertEquals(StepRecord(step.kind, ShellError.ConnectionLost), records[failing])
            assertEquals(plan.restore.map { StepRecord(it.kind) }, records.drop(failing + 1))
        }
    }

    @Test
    fun `a non-zero exit fails the step`() = runTest {
        shell.reply(ShellCommands.crash(FixtureDevice.Keyboard), stderr = "Unknown package\n", exitCode = 1)

        runner.run(LightApplyPlan.create(openGlEverywhere)) { records += it }

        assertEquals(
            listOf(
                StepRecord(StepKind.SetRenderer),
                StepRecord(StepKind.RestartLauncher),
                StepRecord(StepKind.RestartKeyboard, ApplyError.CommandFailed(1, "Unknown package\n")),
            ),
            records,
        )
        assertTrue("am crash com.android.systemui" !in shell.lines)
    }

    @Test
    fun `System UI is restarted after the restore, then asked until it answers on Vulkan`() = runTest {
        val systemUi = ShellCommands.gfxInfo(ShellCommands.SystemUi)
        shell.reply(systemUi, stdout = fixture("gfxinfo-opengl.txt"))
        shell.thenReply(systemUi, stdout = "")
        shell.thenReply(systemUi, stdout = fixture("gfxinfo-vulkan.txt"))

        runner.run(fullPlan()) { records += it }

        assertEquals(
            restoreLines + listOf(
                "am crash com.android.systemui",
                "dumpsys gfxinfo com.android.systemui",
                "dumpsys gfxinfo com.android.systemui",
                "dumpsys gfxinfo com.android.systemui",
            ),
            shell.lines.takeLast(restoreLines.size + 4),
        )
        assertEquals(StepRecord(StepKind.RestartSystemUi), records.last())
        assertEquals(3_000, currentTime)
    }

    @Test
    fun `System UI that never comes back on Vulkan fails the step`() = runTest {
        shell.reply(ShellCommands.gfxInfo(ShellCommands.SystemUi), stdout = fixture("gfxinfo-opengl.txt"))

        runner.run(systemUiOnly) { records += it }

        assertEquals(listOf(StepRecord(StepKind.RestartSystemUi, ApplyError.SystemUiNotOnVulkan)), records)
        assertEquals(15, shell.lines.count { it == "dumpsys gfxinfo com.android.systemui" })
    }

    @Test
    fun `System UI is never restarted twice within a minute`() = runTest {
        runner.run(systemUiOnly) { records += it }
        clock.advance(59.seconds)
        runner.run(systemUiOnly) { records += it }
        clock.advance(2.seconds)
        runner.run(systemUiOnly) { records += it }

        assertEquals(
            listOf(
                StepRecord(StepKind.RestartSystemUi),
                StepRecord(StepKind.RestartSystemUi, ApplyError.SystemUiRestartedRecently),
                StepRecord(StepKind.RestartSystemUi),
            ),
            records,
        )
        assertEquals(2, shell.lines.count { it == "am crash com.android.systemui" })
    }

    @Test
    fun `a connection dropped at any step is picked up again, and only harmless commands are sent twice`() =
        runTest {
            val plan = LightApplyPlan.create(openGlEverywhere)
            val commands = plan.steps.map { it.command } + ShellCommands.crash(ShellCommands.SystemUi) +
                ShellCommands.gfxInfo(ShellCommands.SystemUi)
            commands.forEach { dropped ->
                val shell = FakeShellExecutor().apply {
                    reply(ShellCommands.gfxInfo(ShellCommands.SystemUi), stdout = fixture("gfxinfo-vulkan.txt"))
                    failOnce(dropped, ShellError.ConnectionLost)
                }
                var reconnects = 0
                val reconnecting = ReconnectingShellExecutor(shell) {
                    reconnects++
                    true
                }
                val records = mutableListOf<StepRecord>()

                ApplyRunner(reconnecting, SystemUiCooldown(InMemorySystemUiRestarts(), clock)).run(plan) {
                    records += it
                }

                val keyboard = ShellCommands.crash(FixtureDevice.Keyboard)
                if (dropped == keyboard) {
                    // Whether the keyboard restarted is unknown, and crashing it again is not harmless: the run stops.
                    assertEquals(StepRecord(StepKind.RestartKeyboard, ShellError.ConnectionLost), records.last())
                    assertEquals(0, reconnects, "nothing was sent after it")
                } else {
                    assertEquals(1, reconnects, "dropped at ${dropped.line}")
                    assertTrue(records.all { it.failure == null }, "dropped at ${dropped.line}: $records")
                    assertEquals(
                        if (dropped.repeatable) 2 else 1,
                        shell.lines.count { it == dropped.line },
                        "dropped at ${dropped.line}",
                    )
                }
            }
        }

    @Test
    fun `cancelling a run still restores the settings, then cancels`() = runTest {
        val plan = fullPlan()
        val stopAll = plan.steps.single { it.kind is StepKind.StopApps }.command
        shell.beforeEach = { if (it == stopAll) awaitCancellation() }

        val run = launch { runner.run(plan) { records += it } }
        runCurrent()
        run.cancel()
        run.join()

        assertTrue(run.isCancelled)
        assertEquals(restoreLines, shell.lines.takeLast(restoreLines.size))
        assertEquals(plan.restore.map { StepRecord(it.kind) }, records.drop(1))
    }

    @Test
    fun `a restore that does not read back is retried after a pause`() = runTest {
        val get = ShellCommands.getSetting(SettingKey.AutoRotation)
        shell.reply(get, stdout = "0\n")
        shell.beforeEach = { command ->
            // The second read-back sees the value.
            if (command == get && shell.commands.count { it == get } == 2) shell.reply(get, stdout = "1\n")
        }

        runner.run(restoreOnly(RestoredSetting.AutoRotation to "1")) { records += it }

        assertEquals(listOf(StepRecord(StepKind.RestoreSetting(RestoredSetting.AutoRotation))), records)
        assertEquals(2, shell.lines.count { it == "settings put system accelerometer_rotation '1'" })
        assertEquals(1_000, currentTime)
    }

    @Test
    fun `a restore that never reads back fails after three attempts`() = runTest {
        shell.reply(ShellCommands.getSetting(SettingKey.AutoRotation), stdout = "0\n")

        runner.run(restoreOnly(RestoredSetting.AutoRotation to "1")) { records += it }

        val failure = ApplyError.SettingNotRestored(RestoredSetting.AutoRotation)
        assertEquals(listOf(StepRecord(StepKind.RestoreSetting(RestoredSetting.AutoRotation), failure)), records)
        assertEquals(3, shell.lines.count { it.startsWith("settings put") })
        assertEquals(2_000, currentTime)
    }

    @Test
    fun `a restore that cannot reach the shell does not stop the other restores`() = runTest {
        shell.fail(ShellCommands.putSetting(SettingKey.AutoRotation, "1"), ShellError.TimedOut)

        runner.run(restoreOnly(RestoredSetting.AutoRotation to "1", RestoredSetting.EdgeEnabled to "1")) {
            records += it
        }

        assertEquals(
            listOf(
                StepRecord(StepKind.RestoreSetting(RestoredSetting.AutoRotation), ShellError.TimedOut),
                StepRecord(StepKind.RestoreSetting(RestoredSetting.EdgeEnabled)),
            ),
            records,
        )
    }

    private suspend fun fullPlan(): ApplyPlan {
        val inputs = ApplyInputsReader(shell).readFull().getOrElse { fail("read failed: $it") }
        shell.commands.clear()
        return FullApplyPlan.create(
            inputs = inputs,
            userExclusions = setOf(PackageName.known("com.example.notes")),
            self = FixtureDevice.Self,
            before = openGlEverywhere.status,
        )
    }

    private val openGlEverywhere = Surfaces(
        status = RendererStatus(Renderer.OpenGL, Renderer.OpenGL, Renderer.OpenGL, Renderer.OpenGL),
        keyboard = FixtureDevice.Keyboard,
    )

    private val systemUiOnly = ApplyPlan(steps = emptyList(), restartSystemUi = true)

    private fun restoreOnly(vararg values: Pair<RestoredSetting, String>): ApplyPlan = ApplyPlan(
        steps = emptyList(),
        restore = values.map { (setting, value) -> ApplyStep.RestoreSetting(setting, value) },
    )
}
