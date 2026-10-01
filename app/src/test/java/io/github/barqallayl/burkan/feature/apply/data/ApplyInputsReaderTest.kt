package io.github.barqallayl.burkan.feature.apply.data

import arrow.core.getOrElse
import arrow.core.left
import arrow.core.right
import io.github.barqallayl.burkan.core.shell.FakeShellExecutor
import io.github.barqallayl.burkan.core.shell.PackageName
import io.github.barqallayl.burkan.core.shell.SettingKey
import io.github.barqallayl.burkan.core.shell.ShellCommands
import io.github.barqallayl.burkan.core.shell.ShellError
import io.github.barqallayl.burkan.core.shell.fixture
import io.github.barqallayl.burkan.feature.apply.data.FixtureDevice.replyLikeFixtureDevice
import io.github.barqallayl.burkan.feature.apply.model.ApplyError
import io.github.barqallayl.burkan.feature.apply.model.RestoredSetting
import kotlinx.coroutines.test.runTest
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.fail

class ApplyInputsReaderTest {

    private val shell = FakeShellExecutor().apply { replyLikeFixtureDevice() }
    private val reader = ApplyInputsReader(shell)

    @Test
    fun `the fixture phone reads as expected`() = runTest {
        val inputs = reader.readFull().getOrElse { fail("read failed: $it") }

        assertEquals(20, inputs.installed.size)
        assertEquals(PackageName.known("com.samsung.android.wallpaper.live"), inputs.wallpaper)
        assertEquals(FixtureDevice.Keyboard, inputs.keyboard)
        assertEquals(
            mapOf(
                RestoredSetting.AutoRotation to "1",
                RestoredSetting.AccessibilityServices to FixtureDevice.ACCESSIBILITY,
                RestoredSetting.EdgeEnabled to "1",
                RestoredSetting.EdgePanels to null,
            ),
            inputs.captured,
        )
    }

    @Test
    fun `stderr from pm does not fail the read, whatever its exit code`() = runTest {
        listOf(0, 1, 255).forEach { exitCode ->
            shell.reply(
                ShellCommands.listPackages(),
                stdout = fixture("pm-list-packages.txt"),
                stderr = FixtureDevice.SECURE_FOLDER_ERROR,
                exitCode = exitCode,
            )

            val installed = reader.readFull().getOrElse { fail("exit $exitCode failed the read: $it") }.installed

            assertEquals(20, installed.size, "exit $exitCode")
        }
    }

    @Test
    fun `pm listing nothing fails the read`() = runTest {
        shell.reply(ShellCommands.listPackages(), stderr = FixtureDevice.SECURE_FOLDER_ERROR, exitCode = 1)

        assertEquals(ApplyError.NoPackagesListed.left(), reader.readFull())
    }

    @Test
    fun `a dump that fails fails the read`() = runTest {
        shell.reply(ShellCommands.dumpAppWidgets(), stderr = "Can't find service: appwidget\n", exitCode = 1)

        assertEquals(ApplyError.CommandFailed(1, "Can't find service: appwidget\n").left(), reader.readFull())
    }

    @Test
    fun `a lost connection fails the read`() = runTest {
        shell.fail(ShellCommands.dumpWallpaper(), ShellError.ConnectionLost)

        assertEquals(ShellError.ConnectionLost.left(), reader.readFull())
    }

    @Test
    fun `no default input method means no keyboard to restart`() = runTest {
        shell.reply(ShellCommands.getSetting(SettingKey.DefaultInputMethod), stdout = "null\n")

        assertEquals(null.right(), reader.readKeyboard())
    }
}
