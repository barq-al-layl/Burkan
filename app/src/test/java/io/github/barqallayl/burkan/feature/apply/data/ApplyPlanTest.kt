package io.github.barqallayl.burkan.feature.apply.data

import io.github.barqallayl.burkan.core.model.Renderer
import io.github.barqallayl.burkan.core.model.Renderer.OpenGL
import io.github.barqallayl.burkan.core.model.Renderer.Unknown
import io.github.barqallayl.burkan.core.model.Renderer.Vulkan
import io.github.barqallayl.burkan.core.model.RendererStatus
import io.github.barqallayl.burkan.core.shell.PackageName
import io.github.barqallayl.burkan.core.shell.ShellCommands
import io.github.barqallayl.burkan.core.shell.Surfaces
import io.github.barqallayl.burkan.feature.apply.model.RestoredSetting
import io.github.barqallayl.burkan.feature.apply.model.StepKind
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ApplyPlanTest {

    @Test
    fun `light apply restarts each surface, System UI last of all`() {
        val plan = LightApplyPlan.create(surfaces())

        assertEquals(
            listOf(
                "setprop debug.hwui.renderer skiavk",
                "am force-stop com.sec.android.app.launcher",
                "am crash com.samsung.android.honeyboard",
            ),
            plan.steps.map { it.command.line },
        )
        assertTrue(plan.restartSystemUi)
        assertTrue(plan.restore.isEmpty())
    }

    @Test
    fun `light apply without a keyboard restarts the other two surfaces`() {
        val plan = LightApplyPlan.create(surfaces(keyboard = null))

        assertEquals(listOf(StepKind.SetRenderer, StepKind.RestartLauncher), plan.steps.map { it.kind })
        assertTrue(plan.restartSystemUi)
    }

    @Test
    fun `light apply leaves alone every surface already on Vulkan`() {
        val plan = LightApplyPlan.create(surfaces(systemUi = Vulkan, launcher = Vulkan, keyboard = Vulkan))

        assertEquals(listOf(StepKind.SetRenderer), plan.steps.map { it.kind })
        assertFalse(plan.restartSystemUi)
        assertTrue(plan.restartsNothing)
    }

    @Test
    fun `a surface that reports nothing is restarted, as only Vulkan counts as done`() {
        val plan = LightApplyPlan.create(surfaces(systemUi = Unknown, launcher = Unknown, keyboard = Unknown))

        assertEquals(
            listOf(StepKind.SetRenderer, StepKind.RestartLauncher, StepKind.RestartKeyboard),
            plan.steps.map { it.kind },
        )
        assertTrue(plan.restartSystemUi)
    }

    @Test
    fun `after a restart System UI is left for later`() {
        val plan = LightApplyPlan.create(surfaces(), deferSystemUi = true)

        assertEquals(
            listOf(StepKind.SetRenderer, StepKind.RestartLauncher, StepKind.RestartKeyboard),
            plan.steps.map { it.kind },
        )
        assertFalse(plan.restartSystemUi)
    }

    @Test
    fun `full apply restarts System UI only after everything else, the restore included`() {
        val plan = fullPlan(
            installed = names("org.example.app"),
            running = names("org.example.app").toSet(),
            keyboard = FixtureDevice.Keyboard,
            captured = mapOf(RestoredSetting.AutoRotation to "1"),
        )

        assertEquals(
            listOf(
                StepKind.SetRenderer,
                StepKind.StopApps(1),
                StepKind.RestartLauncher,
                StepKind.RelaunchApps(1),
                StepKind.RestartKeyboard,
            ),
            plan.steps.map { it.kind },
        )
        assertEquals(listOf(ApplyStep.RestoreSetting(RestoredSetting.AutoRotation, "1")), plan.restore)
        assertTrue(plan.restartSystemUi)
        assertTrue(plan.steps.none { it.command == ShellCommands.crash(ShellCommands.SystemUi) })
    }

    @Test
    fun `full apply keeps a launcher already on Vulkan running and skips surfaces already on Vulkan`() {
        val plan = fullPlan(
            installed = names("com.sec.android.app.launcher", "org.example.app"),
            keyboard = FixtureDevice.Keyboard,
            before = RendererStatus(newApps = Vulkan, systemUi = Vulkan, launcher = Vulkan, keyboard = Vulkan),
        )

        assertEquals(names("org.example.app"), stopped(plan))
        assertEquals(listOf(StepKind.SetRenderer, StepKind.StopApps(1)), plan.steps.map { it.kind })
        assertFalse(plan.restartSystemUi)
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

        assertEquals(listOf(StepKind.SetRenderer, StepKind.RestartLauncher), plan.steps.map { it.kind })
        assertTrue(plan.restartSystemUi)
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
        before: RendererStatus = RendererStatus(OpenGL, OpenGL, OpenGL, OpenGL),
    ): ApplyPlan = FullApplyPlan.create(
        inputs = FullApplyInputs(installed, inputMethods, wallpaper, running, widgets, keyboard, captured),
        userExclusions = userExclusions,
        self = FixtureDevice.Self,
        before = before,
    )

    private fun surfaces(
        systemUi: Renderer = OpenGL,
        launcher: Renderer = OpenGL,
        keyboard: Renderer? = OpenGL,
    ) = Surfaces(
        status = RendererStatus(OpenGL, systemUi, launcher, keyboard ?: Unknown),
        keyboard = FixtureDevice.Keyboard.takeIf { keyboard != null },
    )

    private fun stopped(plan: ApplyPlan): List<PackageName> {
        val line = plan.steps.single { it.kind is StepKind.StopApps }.command.line
        return line.split("; ").filter { it.startsWith("am force-stop ") }
            .map { PackageName.known(it.removePrefix("am force-stop ")) }
    }

    private fun names(vararg names: String): List<PackageName> = names.map(PackageName::known)
}
