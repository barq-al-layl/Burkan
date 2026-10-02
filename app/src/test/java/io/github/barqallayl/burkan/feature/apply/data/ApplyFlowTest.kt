package io.github.barqallayl.burkan.feature.apply.data

import arrow.core.getOrElse
import io.github.barqallayl.burkan.core.model.Renderer
import io.github.barqallayl.burkan.core.model.RendererStatus
import io.github.barqallayl.burkan.core.shell.FakeShellExecutor
import io.github.barqallayl.burkan.core.shell.PackageName
import io.github.barqallayl.burkan.core.shell.RendererReader
import io.github.barqallayl.burkan.core.shell.ShellCommands
import io.github.barqallayl.burkan.core.shell.Surfaces
import io.github.barqallayl.burkan.core.shell.fixture
import io.github.barqallayl.burkan.feature.apply.FakeSystemUiRestarts
import io.github.barqallayl.burkan.feature.apply.data.FixtureDevice.replyLikeFixtureDevice
import io.github.barqallayl.burkan.feature.apply.model.StepRecord
import kotlinx.coroutines.test.runTest
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.test.fail
import kotlin.time.Clock
import kotlin.time.Duration
import kotlin.time.Instant

/** Each flow end to end against the fixture phone: read, plan, run. Pins the exact command list. */
class ApplyFlowTest {

    private val shell = FakeShellExecutor().apply {
        replyLikeFixtureDevice()
        reply(ShellCommands.getRenderer(), stdout = "\n")
        listOf(ShellCommands.SystemUi, FixtureDevice.Launcher, FixtureDevice.Keyboard).forEach {
            reply(ShellCommands.gfxInfo(it), stdout = fixture("gfxinfo-opengl.txt"))
        }
        // Once restarted, System UI answers on Vulkan.
        thenReply(ShellCommands.gfxInfo(ShellCommands.SystemUi), stdout = fixture("gfxinfo-vulkan.txt"))
    }
    private val reader = ApplyInputsReader(shell)
    private val runner = ApplyRunner(shell, SystemUiCooldown(FakeSystemUiRestarts(), FixedClock))
    private val records = mutableListOf<StepRecord>()

    @Test
    fun `light apply sends exactly these commands`() = runTest {
        val before = RendererReader(shell).read().getOrElse { fail("read failed: $it") }

        runner.run(LightApplyPlan.create(before)) { records += it }

        assertEquals(
            listOf(
                // What everything runs with now decides what is restarted.
                "getprop debug.hwui.renderer",
                "cmd package resolve-activity --brief -a android.intent.action.MAIN -c android.intent.category.HOME",
                "settings get secure default_input_method",
                "dumpsys gfxinfo com.android.systemui",
                "dumpsys gfxinfo com.sec.android.app.launcher",
                "dumpsys gfxinfo com.samsung.android.honeyboard",
                "setprop debug.hwui.renderer skiavk",
                "am force-stop com.sec.android.app.launcher; sleep 2; " +
                    "am start -a android.intent.action.MAIN -c android.intent.category.HOME",
                // The default keyboard before and after its restart: it is put back if the restart changed it.
                "settings get secure default_input_method",
                "am crash com.samsung.android.honeyboard",
                "settings get secure default_input_method",
                // Last, because it locks the screen; then asked until it is back on Vulkan.
                "am crash com.android.systemui",
                "dumpsys gfxinfo com.android.systemui",
            ),
            shell.lines,
        )
        assertTrue(records.all { it.failure == null }, "$records")
    }

    @Test
    fun `full apply sends exactly these commands`() = runTest {
        shell.reply(ShellCommands.gfxInfo(ShellCommands.SystemUi), stdout = fixture("gfxinfo-vulkan.txt"))
        val inputs = reader.readFull().getOrElse { fail("read failed: $it") }
        val plan = FullApplyPlan.create(
            inputs = inputs,
            userExclusions = setOf(PackageName.known("com.example.notes")),
            self = FixtureDevice.Self,
            before = Surfaces(
                status = RendererStatus(Renderer.OpenGL, Renderer.OpenGL, Renderer.OpenGL, Renderer.OpenGL),
                launcher = FixtureDevice.Launcher,
                keyboard = FixtureDevice.Keyboard,
            ),
        )

        runner.run(plan) { records += it }

        assertEquals(
            listOf(
                // Everything is read before anything changes.
                "pm list packages",
                "ime list -s",
                "dumpsys wallpaper",
                "dumpsys activity processes",
                "dumpsys appwidget",
                "settings get secure default_input_method",
                "settings get system accelerometer_rotation",
                "settings get secure enabled_accessibility_services",
                "settings get secure edge_enable",
                "settings get secure edge_panels_enabled",
                "setprop debug.hwui.renderer skiavk",
                // Not stopped: keyboards, the wallpaper, fixed exclusions, the media provider, the user's
                // exclusion, and Burkan. Whole names only: com.example.notes.widget is still stopped.
                "am force-stop com.android.systemui; " +
                    "am force-stop com.sec.android.app.launcher; " +
                    "am force-stop com.google.android.googlequicksearchbox; " +
                    "am force-stop com.samsung.android.app.dressroom; " +
                    "am force-stop com.android.settings; " +
                    "am force-stop com.android.settings.intelligence; " +
                    "am force-stop com.example.notes.widget; " +
                    "am force-stop org.example.weather; " +
                    "am force-stop net.example.reader; " +
                    "am force-stop com.example.camera.pro; " +
                    "am force-stop org.example.chat; " +
                    "true",
                "am force-stop com.sec.android.app.launcher; sleep 2; " +
                    "am start -a android.intent.action.MAIN -c android.intent.category.HOME",
                // What was running or backs a widget, and was stopped.
                "monkey -p com.google.android.googlequicksearchbox -c android.intent.category.LAUNCHER 1 " +
                    ">/dev/null 2>&1; " +
                    "monkey -p com.samsung.android.app.dressroom -c android.intent.category.LAUNCHER 1 " +
                    ">/dev/null 2>&1; " +
                    "monkey -p com.android.settings -c android.intent.category.LAUNCHER 1 >/dev/null 2>&1; " +
                    "monkey -p com.example.notes.widget -c android.intent.category.LAUNCHER 1 >/dev/null 2>&1; " +
                    "monkey -p org.example.weather -c android.intent.category.LAUNCHER 1 >/dev/null 2>&1; " +
                    "monkey -p org.example.chat -c android.intent.category.LAUNCHER 1 >/dev/null 2>&1; " +
                    "true",
                "settings get secure default_input_method",
                "am crash com.samsung.android.honeyboard",
                "settings get secure default_input_method",
                // edge_panels_enabled was unset, so it is not written back.
                "settings put system accelerometer_rotation '1'",
                "settings get system accelerometer_rotation",
                "settings put secure enabled_accessibility_services '${FixtureDevice.ACCESSIBILITY}'",
                "settings get secure enabled_accessibility_services",
                "settings put secure edge_enable '1'",
                "settings get secure edge_enable",
                // Last of all, after the restore, because it locks the screen and drops the connection.
                "am crash com.android.systemui",
                "dumpsys gfxinfo com.android.systemui",
            ),
            shell.lines,
        )
        assertTrue(records.all { it.failure == null }, "$records")
    }
}

/** Always the same moment: no test reads the real clock. */
object FixedClock : Clock {
    override fun now(): Instant = Instant.parse("2026-10-02T09:30:00Z")
}

/** A clock a test moves by hand. */
class TestClock(private var now: Instant = Instant.parse("2026-10-02T09:30:00Z")) : Clock {
    override fun now(): Instant = now

    fun advance(by: Duration) {
        now += by
    }
}
