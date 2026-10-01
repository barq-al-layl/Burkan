package io.github.barqallayl.burkan.feature.apply.data

import arrow.core.getOrElse
import io.github.barqallayl.burkan.core.shell.FakeShellExecutor
import io.github.barqallayl.burkan.core.shell.PackageName
import io.github.barqallayl.burkan.feature.apply.data.FixtureDevice.replyLikeFixtureDevice
import io.github.barqallayl.burkan.feature.apply.model.StepRecord
import kotlinx.coroutines.test.runTest
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.test.fail

/** Each flow end to end against the fixture phone: read, plan, run. Pins the exact command list. */
class ApplyFlowTest {

    private val shell = FakeShellExecutor().apply { replyLikeFixtureDevice() }
    private val reader = ApplyInputsReader(shell)
    private val runner = ApplyRunner(shell)
    private val records = mutableListOf<StepRecord>()

    @Test
    fun `light apply sends exactly these commands`() = runTest {
        val keyboard = reader.readKeyboard().getOrElse { fail("read failed: $it") }

        runner.run(LightApplyPlan.create(keyboard)) { records += it }

        assertEquals(
            listOf(
                "settings get secure default_input_method",
                "setprop debug.hwui.renderer skiavk",
                "am crash com.android.systemui",
                "am force-stop com.sec.android.app.launcher",
                "am crash com.samsung.android.honeyboard",
            ),
            shell.lines,
        )
        assertTrue(records.all { it.failure == null })
    }

    @Test
    fun `full apply sends exactly these commands`() = runTest {
        val inputs = reader.readFull().getOrElse { fail("read failed: $it") }
        val plan = FullApplyPlan.create(
            inputs = inputs,
            userExclusions = setOf(PackageName.known("com.example.notes")),
            self = FixtureDevice.Self,
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
                "am crash com.android.systemui",
                "am force-stop com.sec.android.app.launcher; sleep 2; " +
                    "monkey -p com.sec.android.app.launcher -c android.intent.category.LAUNCHER 1 >/dev/null 2>&1; " +
                    "true",
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
                "am crash com.samsung.android.honeyboard",
                // edge_panels_enabled was unset, so it is not written back.
                "settings put system accelerometer_rotation '1'",
                "settings get system accelerometer_rotation",
                "settings put secure enabled_accessibility_services '${FixtureDevice.ACCESSIBILITY}'",
                "settings get secure enabled_accessibility_services",
                "settings put secure edge_enable '1'",
                "settings get secure edge_enable",
            ),
            shell.lines,
        )
        assertTrue(records.all { it.failure == null }, "$records")
    }
}
