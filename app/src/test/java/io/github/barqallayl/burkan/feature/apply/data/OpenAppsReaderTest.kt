package io.github.barqallayl.burkan.feature.apply.data

import arrow.core.right
import io.github.barqallayl.burkan.core.model.Renderer
import io.github.barqallayl.burkan.core.model.RendererStatus
import io.github.barqallayl.burkan.core.shell.FakeShellExecutor
import io.github.barqallayl.burkan.core.shell.PackageName
import io.github.barqallayl.burkan.core.shell.ShellCommands
import io.github.barqallayl.burkan.core.shell.ShellError
import io.github.barqallayl.burkan.core.shell.Surfaces
import io.github.barqallayl.burkan.feature.apply.data.FixtureDevice.replyLikeFixtureDevice
import io.github.barqallayl.burkan.core.shell.fixture
import kotlinx.coroutines.test.runTest
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class OpenAppsReaderTest {

    private val shell = FakeShellExecutor().apply {
        replyLikeFixtureDevice()
        reply(ShellCommands.gfxInfoAll(), stdout = fixture("gfxinfo-all.txt"))
    }
    private val reader = OpenAppsReader(shell)

    private fun surfaces(newApps: Renderer) = Surfaces(
        status = RendererStatus(newApps, Renderer.Vulkan, Renderer.OpenGL, Renderer.OpenGL),
        launcher = FixtureDevice.Launcher,
        keyboard = FixtureDevice.Keyboard,
    )

    private fun packages(vararg names: String) = names.mapTo(mutableSetOf(), PackageName::known)

    @Test
    fun `an app is behind when any of its processes draws with the other renderer`() = runTest {
        assertEquals(
            packages(
                "com.example.notes",
                "org.example.chat",
                "com.samsung.android.wallpaper.live",
                "com.samsung.android.honeyboard",
                "com.sec.android.app.launcher",
                // Only its widget process has drawn, and that is enough.
                "org.example.weather",
                "com.sec.imsservice",
                // Not `system`, the system's own process, though it draws with OpenGL: no restart moves it.
            ).right(),
            reader.notOn(Renderer.Vulkan),
        )
    }

    @Test
    fun `after going back to OpenGL it is the apps on Vulkan that are behind`() = runTest {
        assertEquals(packages("com.android.systemui", "net.example.reader").right(), reader.notOn(Renderer.OpenGL))
    }

    @Test
    fun `with no renderer to compare with, nothing is behind and the phone is not asked`() = runTest {
        assertEquals(emptySet<PackageName>().right(), reader.notOn(Renderer.Unknown))
        assertTrue(shell.lines.isEmpty())
    }

    @Test
    fun `what a run would not restart is not counted as behind`() = runTest {
        // Not the launcher and the keyboard, which have rows of their own; not the wallpaper; not what keeps the
        // phone connected.
        assertEquals(
            packages("com.example.notes", "org.example.chat", "org.example.weather").right(),
            reader.behind(surfaces(Renderer.Vulkan), userExclusions = emptySet(), self = FixtureDevice.Self),
        )
        assertEquals(
            packages("com.example.notes", "org.example.weather").right(),
            reader.behind(surfaces(Renderer.Vulkan), packages("org.example.chat"), FixtureDevice.Self),
        )
    }

    @Test
    fun `System UI is not counted either, having a row and a restart of its own`() = runTest {
        assertEquals(
            packages("net.example.reader").right(),
            reader.behind(surfaces(Renderer.OpenGL), emptySet(), FixtureDevice.Self),
        )
    }

    @Test
    fun `with nothing behind, the keyboards and the wallpaper are not asked about`() = runTest {
        shell.reply(ShellCommands.gfxInfoAll(), stdout = "")

        assertEquals(emptySet<PackageName>().right(), reader.behind(surfaces(Renderer.Vulkan), emptySet(), FixtureDevice.Self))
        assertEquals(listOf("dumpsys gfxinfo"), shell.lines)
    }

    @Test
    fun `a connection lost while asking is an error, not an empty list`() = runTest {
        shell.fail(ShellCommands.gfxInfoAll(), ShellError.ConnectionLost)

        assertTrue(reader.notOn(Renderer.Vulkan).isLeft())
    }
}
