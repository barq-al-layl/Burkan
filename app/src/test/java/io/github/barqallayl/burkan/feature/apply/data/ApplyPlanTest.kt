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
                "am force-stop com.sec.android.app.launcher; sleep 2; " +
                    "am start -a android.intent.action.MAIN -c android.intent.category.HOME",
                "am crash \"\$(pidof -s com.samsung.android.honeyboard || echo com.samsung.android.honeyboard)\"",
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
        assertFalse(plan.changesNothing, "the property is still to set")
    }

    @Test
    fun `a property already set is not set again`() {
        val plan = LightApplyPlan.create(surfaces(newApps = Vulkan))

        assertEquals(listOf(StepKind.RestartLauncher, StepKind.RestartKeyboard), plan.steps.map { it.kind })
        assertTrue(plan.restartSystemUi)
    }

    @Test
    fun `with the property set and every surface on Vulkan there is nothing to do`() {
        val plan = LightApplyPlan.create(
            surfaces(newApps = Vulkan, systemUi = Vulkan, launcher = Vulkan, keyboard = Vulkan),
        )

        assertTrue(plan.changesNothing)
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
    fun `going back clears the property and restarts each surface that is on Vulkan`() {
        val plan = LightApplyPlan.create(
            surfaces(newApps = Vulkan, systemUi = Vulkan, launcher = Vulkan, keyboard = Vulkan),
            target = OpenGL,
        )

        assertEquals(
            listOf(
                "setprop debug.hwui.renderer ''",
                "am force-stop com.sec.android.app.launcher; sleep 2; " +
                    "am start -a android.intent.action.MAIN -c android.intent.category.HOME",
                "am crash \"\$(pidof -s com.samsung.android.honeyboard || echo com.samsung.android.honeyboard)\"",
            ),
            plan.steps.map { it.command.line },
        )
        assertEquals(StepKind.ClearRenderer, plan.steps.first().kind)
        assertTrue(plan.restartSystemUi)
        assertEquals(OpenGL, plan.target)
    }

    @Test
    fun `going back from a phone that is on OpenGL throughout has nothing to do`() {
        assertTrue(LightApplyPlan.create(surfaces(), target = OpenGL).changesNothing)
    }

    @Test
    fun `going back leaves alone a surface that never went to Vulkan`() {
        val plan = LightApplyPlan.create(surfaces(newApps = Vulkan, systemUi = Vulkan), target = OpenGL)

        assertEquals(listOf(StepKind.ClearRenderer), plan.steps.map { it.kind })
        assertTrue(plan.restartSystemUi)
    }

    @Test
    fun `catching up the open apps restarts only those, and leaves the property and the surfaces as they are`() {
        val notes = PackageName.known("com.example.notes")
        val chat = PackageName.known("org.example.chat")
        val reader = PackageName.known("net.example.reader")
        val plan = fullPlan(
            installed = listOf(ShellCommands.SystemUi, FixtureDevice.Launcher, notes, reader, chat),
            running = setOf(notes, chat, reader),
            keyboard = FixtureDevice.Keyboard,
            // The surfaces are behind too, and named among the open apps: they are still not this run's to restart.
            only = setOf(chat, notes, ShellCommands.SystemUi, FixtureDevice.Launcher),
        )

        assertEquals(listOf(notes, chat), stopped(plan))
        assertEquals(
            listOf(StepKind.StopApps(2), StepKind.RelaunchApps(2)),
            plan.steps.map { it.kind },
        )
        assertFalse(plan.restartSystemUi)
    }

    @Test
    fun `catching up never stops what no run stops`() {
        val chat = PackageName.known("org.example.chat")
        val plan = fullPlan(
            installed = listOf(FixtureDevice.Keyboard, FixtureDevice.Self, chat),
            inputMethods = setOf(FixtureDevice.Keyboard),
            userExclusions = setOf(chat),
            only = setOf(FixtureDevice.Keyboard, FixtureDevice.Self, chat),
        )

        assertTrue(plan.changesNothing)
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
        assertEquals(listOf(StepKind.StopApps(1)), plan.steps.map { it.kind }, "the property is set already")
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
        // Burkan itself is launched last, so it is what the user comes back to.
        assertEquals(
            ShellCommands.launchAll(
                names("org.example.running", "org.example.provider", "org.example.host"),
                front = FixtureDevice.Self,
            ),
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

    @Test
    fun `a limit restarts that many of the most recent apps, and reopens only those`() {
        val plan = fullPlan(
            installed = names("com.a", "com.b", "com.c", "com.d", "com.keep"),
            running = names("com.a", "com.c", "com.d").toSet(),
            userExclusions = names("com.keep").toSet(),
            recent = names("com.d", "com.keep", "com.gone", "com.b", "com.d", "com.a"),
            limit = 2,
        )

        // In order of use; an excluded app and an uninstalled one do not use up the limit.
        assertEquals(names("com.d", "com.b"), stopped(plan))
        assertEquals(StepKind.RelaunchApps(1), plan.steps.single { it.kind is StepKind.RelaunchApps }.kind)
    }

    @Test
    fun `a limit larger than what was used restarts only what was used`() {
        val plan = fullPlan(installed = names("com.a", "com.b", "com.c"), recent = names("com.b"), limit = 50)

        assertEquals(names("com.b"), stopped(plan))
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
        recent: List<PackageName>? = null,
        limit: Int? = null,
        only: Set<PackageName>? = null,
        target: Renderer = Vulkan,
    ): ApplyPlan = FullApplyPlan.create(
        inputs = FullApplyInputs(installed, inputMethods, wallpaper, running, widgets, keyboard, captured),
        userExclusions = userExclusions,
        self = FixtureDevice.Self,
        before = Surfaces(before, FixtureDevice.Launcher, keyboard),
        recent = recent,
        limit = limit,
        only = only,
        target = target,
    )

    private fun surfaces(
        newApps: Renderer = OpenGL,
        systemUi: Renderer = OpenGL,
        launcher: Renderer = OpenGL,
        keyboard: Renderer? = OpenGL,
    ) = Surfaces(
        status = RendererStatus(newApps, systemUi, launcher, keyboard ?: Unknown),
        launcher = FixtureDevice.Launcher,
        keyboard = FixtureDevice.Keyboard.takeIf { keyboard != null },
    )

    private fun stopped(plan: ApplyPlan): List<PackageName> {
        val line = plan.steps.single { it.kind is StepKind.StopApps }.command.line
        return line.split("; ").filter { it.startsWith("am force-stop ") }
            .map { PackageName.known(it.removePrefix("am force-stop ")) }
    }

    private fun names(vararg names: String): List<PackageName> = names.map(PackageName::known)
}
