package io.github.barqallayl.burkan.feature.status.data

import kotlin.test.assertNull
import io.github.barqallayl.burkan.feature.status.model.BehindApp
import io.github.barqallayl.burkan.core.storage.FakeSettingsStorage
import io.github.barqallayl.burkan.feature.settings.data.FakeInstalledApps
import arrow.core.left
import arrow.core.right
import io.github.barqallayl.burkan.core.model.Renderer
import io.github.barqallayl.burkan.core.model.RendererStatus
import io.github.barqallayl.burkan.core.shell.FakeShellExecutor
import io.github.barqallayl.burkan.core.shell.PackageName
import io.github.barqallayl.burkan.core.shell.SettingKey
import io.github.barqallayl.burkan.core.shell.ShellCommands
import io.github.barqallayl.burkan.core.shell.ShellError
import io.github.barqallayl.burkan.core.shell.fixture
import io.github.barqallayl.burkan.feature.apply.data.FixtureDevice
import io.github.barqallayl.burkan.feature.connection.data.FakeShellAccess
import io.github.barqallayl.burkan.feature.connection.data.FakeWirelessDebugging
import io.github.barqallayl.burkan.feature.connection.model.ConnectionError
import kotlinx.coroutines.test.runTest
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class StatusRepositoryTest {

    private val keyboard = PackageName.known("com.samsung.android.honeyboard")
    private val shell = FakeShellExecutor().apply {
        reply(ShellCommands.getRenderer(), stdout = "skiavk\n")
        reply(ShellCommands.resolveHomeActivity(), stdout = fixture("resolve-activity-home.txt"))
        reply(
            ShellCommands.getSetting(SettingKey.DefaultInputMethod),
            stdout = "com.samsung.android.honeyboard/.service.HoneyBoardService\n",
        )
        reply(ShellCommands.gfxInfo(ShellCommands.SystemUi), stdout = fixture("gfxinfo-vulkan.txt"))
        reply(ShellCommands.gfxInfo(FixtureDevice.Launcher), stdout = fixture("gfxinfo-vulkan.txt"))
        reply(ShellCommands.gfxInfo(keyboard), stdout = fixture("gfxinfo-opengl.txt"))
    }
    private val access = FakeShellAccess(shell)
    private val settings = FakeSettingsStorage()
    private val repository = StatusRepository(
        access,
        FakeInstalledApps(mapOf("com.example.notes" to "Notes", "org.example.chat" to "Chat")),
        settings,
        FakeWirelessDebugging(),
        FixtureDevice.Self,
    )

    @Test
    fun `each surface is read from its own gfxinfo`() = runTest {
        assertEquals(
            RendererStatus(Renderer.Vulkan, Renderer.Vulkan, Renderer.Vulkan, Renderer.OpenGL).right(),
            repository.read(),
        )
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
    }

    @Test
    fun `no keyboard set, or a dump that fails, reads as unknown`() = runTest {
        shell.reply(ShellCommands.getSetting(SettingKey.DefaultInputMethod), stdout = "null\n")
        shell.reply(ShellCommands.gfxInfo(FixtureDevice.Launcher), stderr = "Can't find service\n", exitCode = 1)

        val status = repository.read().getOrNull()

        assertEquals(Renderer.Unknown, status?.launcher)
        assertEquals(Renderer.Unknown, status?.keyboard)
    }

    @Test
    fun `no connection, or a connection lost while reading, is an error`() = runTest {
        access.failure = ConnectionError.NoWifi
        assertEquals(ConnectionError.NoWifi.left(), repository.read())

        access.failure = null
        shell.fail(ShellCommands.gfxInfo(ShellCommands.SystemUi), ShellError.ConnectionLost)
        assertEquals(ShellError.ConnectionLost.left(), repository.read())
    }

    @Test
    fun `the launcher read is the user's home app, from the home role when the home intent has no default`() =
        runTest {
            val home = PackageName.known("org.example.home")
            shell.reply(ShellCommands.resolveHomeActivity(), stdout = fixture("resolve-activity-home-chooser.txt"))
            shell.reply(ShellCommands.homeRoleHolders(), stdout = "${home.value}\n")
            shell.reply(ShellCommands.gfxInfo(home), stdout = fixture("gfxinfo-opengl.txt"))

            assertEquals(Renderer.OpenGL, repository.read().getOrNull()?.launcher)
            assertTrue("dumpsys gfxinfo org.example.home" in shell.lines)
        }

    @Test
    fun `with no home app named, the launcher is unknown and not asked`() = runTest {
        shell.reply(ShellCommands.resolveHomeActivity(), stdout = "No activity found\n")
        shell.reply(ShellCommands.homeRoleHolders(), stdout = "\n")

        assertEquals(Renderer.Unknown, repository.read().getOrNull()?.launcher)
        assertTrue(shell.lines.none { it.startsWith("dumpsys gfxinfo com.sec") })
    }

    @Test
    fun `the open apps that are behind are named, in the order of their names`() = runTest {
        shell.reply(ShellCommands.gfxInfoAll(), stdout = fixture("gfxinfo-all.txt"))
        shell.reply(ShellCommands.listInputMethods(), stdout = fixture("ime-list.txt"))
        shell.reply(ShellCommands.dumpWallpaper(), stdout = fixture("dumpsys-wallpaper.txt"))
        repository.read()

        // The weather app is behind too, but the fake phone has no such app installed: a process name that is no
        // package is left out the same way.
        assertEquals(
            listOf(
                BehindApp(PackageName.known("org.example.chat"), "Chat"),
                BehindApp(PackageName.known("com.example.notes"), "Notes"),
            ),
            repository.readBehind(),
        )
    }

    @Test
    fun `an app the user set aside is not named as behind`() = runTest {
        shell.reply(ShellCommands.gfxInfoAll(), stdout = fixture("gfxinfo-all.txt"))
        settings.userExclusions.value = setOf(PackageName.known("org.example.chat"))
        repository.read()

        assertEquals(listOf(BehindApp(PackageName.known("com.example.notes"), "Notes")), repository.readBehind())
    }

    @Test
    fun `with no status to go by, nothing is known about the open apps`() = runTest {
        assertNull(repository.readBehind())

        access.failure = ConnectionError.NoWifi
        repository.read()
        assertNull(repository.readBehind())
    }
}
