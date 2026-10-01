package io.github.barqallayl.burkan.feature.apply.data

import io.github.barqallayl.burkan.core.shell.FakeShellExecutor
import io.github.barqallayl.burkan.core.shell.PackageName
import io.github.barqallayl.burkan.core.shell.SettingKey
import io.github.barqallayl.burkan.core.shell.ShellCommands
import io.github.barqallayl.burkan.core.shell.fixture

/** The phone the fixtures describe. */
object FixtureDevice {

    val Self = PackageName.known("io.github.barqallayl.burkan")
    val Keyboard = PackageName.known("com.samsung.android.honeyboard")

    /** Contains `$`, which an unquoted write would truncate. */
    const val ACCESSIBILITY = "org.example.chat/org.example.chat.a11y.Outer\$Reader:com.example.notes/.Assist"

    const val SECURE_FOLDER_ERROR =
        "Error: java.lang.SecurityException: Shell does not have permission to access user 150\n"

    /** Answers every read a full apply makes, as this phone would. Restores read back what was captured. */
    fun FakeShellExecutor.replyLikeFixtureDevice() {
        reply(ShellCommands.listPackages(), stdout = fixture("pm-list-packages.txt"), stderr = SECURE_FOLDER_ERROR)
        reply(ShellCommands.listInputMethods(), stdout = fixture("ime-list.txt"))
        reply(ShellCommands.dumpWallpaper(), stdout = fixture("dumpsys-wallpaper.txt"))
        reply(ShellCommands.dumpProcesses(), stdout = fixture("dumpsys-activity-processes.txt"))
        reply(ShellCommands.dumpAppWidgets(), stdout = fixture("dumpsys-appwidget.txt"))
        reply(
            ShellCommands.getSetting(SettingKey.DefaultInputMethod),
            stdout = "com.samsung.android.honeyboard/.service.HoneyBoardService\n",
        )
        reply(ShellCommands.getSetting(SettingKey.AutoRotation), stdout = "1\n")
        reply(ShellCommands.getSetting(SettingKey.AccessibilityServices), stdout = "$ACCESSIBILITY\n")
        reply(ShellCommands.getSetting(SettingKey.EdgeEnabled), stdout = "1\n")
        reply(ShellCommands.getSetting(SettingKey.EdgePanels), stdout = "null\n")
    }
}
