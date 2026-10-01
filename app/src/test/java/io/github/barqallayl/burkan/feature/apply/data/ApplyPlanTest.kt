package io.github.barqallayl.burkan.feature.apply.data

import io.github.barqallayl.burkan.core.shell.PackageName
import io.github.barqallayl.burkan.core.shell.ShellCommands
import io.github.barqallayl.burkan.feature.apply.model.RestoredSetting
import io.github.barqallayl.burkan.feature.apply.model.StepKind
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ApplyPlanTest {

    @Test
    fun `light apply without a keyboard restarts the other two surfaces`() {
        val plan = LightApplyPlan.create(keyboard = null)

        assertEquals(
            listOf(
                "setprop debug.hwui.renderer skiavk",
                "am crash com.android.systemui",
                "am force-stop com.sec.android.app.launcher",
            ),
            plan.steps.map { it.command.line },
        )
        assertTrue(plan.restore.isEmpty())
    }

    @Test
    fun `full apply matches exclusions by whole name`() {
        val plan = fullPlan(
            installed = names("com.example.notes", "com.example.notes.widget", "com.example", "com.example.notesx"),
            userExclusions = names("com.example.notes").toSet(),
        )

        assertEquals(names("com.example.notes.widget", "com.example", "com.example.notesx"), stopped(plan))
    }

    @Test
    fun `full apply keeps keyboards, the wallpaper, fixed exclusions, the media provider and itself running`() {
        val kept = names(
            "com.example.keyboard",
            "com.example.current.keyboard",
            "com.example.wallpaper",
            "com.sec.imsservice",
            "com.netflix.mediaclient",
            "com.android.providers.media.module",
            "com.google.android.providers.media.module",
            FixtureDevice.Self.value,
        )
        val plan = fullPlan(
            installed = kept + names("org.example.app"),
            inputMethods = names("com.example.keyboard").toSet(),
            keyboard = PackageName.known("com.example.current.keyboard"),
            wallpaper = PackageName.known("com.example.wallpaper"),
        )

        assertEquals(names("org.example.app"), stopped(plan))
    }

    @Test
    fun `full apply relaunches what was stopped and was running or backs a widget`() {
        val plan = fullPlan(
            installed = names(
                "com.android.systemui",
                "com.sec.android.app.launcher",
                "org.example.running",
                "org.example.provider",
                "org.example.host",
                "org.example.idle",
                "org.example.excluded",
            ),
            running = names(
                "com.android.systemui",
                "com.sec.android.app.launcher",
                "org.example.running",
                "org.example.excluded",
            ).toSet(),
            widgets = WidgetPackages(
                providers = names("org.example.provider").toSet(),
                hosts = names("org.example.host", "com.sec.android.app.launcher").toSet(),
            ),
            userExclusions = names("org.example.excluded").toSet(),
        )

        val relaunch = plan.steps.single { it.kind is StepKind.RelaunchApps }
        assertEquals(
            ShellCommands.launchAll(names("org.example.running", "org.example.provider", "org.example.host")),
            relaunch.command,
        )
    }

    @Test
    fun `an unset or empty setting is never written back`() {
        val plan = fullPlan(
            installed = names("org.example.app"),
            captured = mapOf(
                RestoredSetting.AutoRotation to null,
                RestoredSetting.AccessibilityServices to "",
                RestoredSetting.EdgeEnabled to "null",
                RestoredSetting.EdgePanels to "0",
            ),
        )

        assertEquals(listOf(ApplyStep.RestoreSetting(RestoredSetting.EdgePanels, "0")), plan.restore)
    }

    @Test
    fun `nothing to stop or relaunch means no such steps`() {
        val plan = fullPlan(installed = names(FixtureDevice.Self.value))

        assertEquals(
            listOf(StepKind.SetRenderer, StepKind.RestartSystemUi, StepKind.RestartLauncher),
            plan.steps.map { it.kind },
        )
    }

    private fun fullPlan(
        installed: List<PackageName>,
        inputMethods: Set<PackageName> = emptySet(),
        wallpaper: PackageName? = null,
        running: Set<PackageName> = emptySet(),
        widgets: WidgetPackages = WidgetPackages(emptySet(), emptySet()),
        keyboard: PackageName? = null,
        captured: Map<RestoredSetting, String?> = emptyMap(),
        userExclusions: Set<PackageName> = emptySet(),
    ): ApplyPlan = FullApplyPlan.create(
        inputs = FullApplyInputs(installed, inputMethods, wallpaper, running, widgets, keyboard, captured),
        userExclusions = userExclusions,
        self = FixtureDevice.Self,
    )

    private fun stopped(plan: ApplyPlan): List<PackageName> {
        val line = plan.steps.single { it.kind is StepKind.StopApps }.command.line
        return line.split("; ").filter { it.startsWith("am force-stop ") }
            .map { PackageName.known(it.removePrefix("am force-stop ")) }
    }

    private fun names(vararg names: String): List<PackageName> = names.map(PackageName::known)
}
