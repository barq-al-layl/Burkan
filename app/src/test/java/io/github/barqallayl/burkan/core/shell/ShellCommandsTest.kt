package io.github.barqallayl.burkan.core.shell

import org.junit.Assume.assumeTrue
import org.junit.Test
import java.io.File
import java.util.concurrent.TimeUnit
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

class ShellCommandsTest {

    private val awkwardValues = listOf(
        "com.example/com.example.Outer\$Inner",
        "a;reboot",
        "two words",
        "it's",
        "'",
        "''",
        "\$(id)",
        "`id`",
        "a\\b",
        "x && y | z > /dev/null",
        "com.example/.A:org.example/.B\$C",
    )

    @Test
    fun `a package name has only letters, digits, underscores and dots`() {
        assertEquals("com.example.notes_2", PackageName.parse("com.example.notes_2")?.value)
        listOf("", "com.example;reboot", "com example", "com.example'", "com/example", "\$x", "com.example\n")
            .forEach { assertNull(PackageName.parse(it), it) }
    }

    @Test
    fun `a setting value is single-quoted with embedded quotes escaped`() {
        assertEquals(
            "settings put secure enabled_accessibility_services 'com.example/com.example.Outer\$Inner'",
            ShellCommands.putSetting(SettingKey.AccessibilityServices, "com.example/com.example.Outer\$Inner").line,
        )
        assertEquals(
            "settings put system accelerometer_rotation 'it'\\''s'",
            ShellCommands.putSetting(SettingKey.AutoRotation, "it's").line,
        )
    }

    @Test
    fun `quoted values reach the program unchanged through a POSIX shell`() {
        // The device shell re-parses the line; /bin/sh parses quoting the same way.
        assumeTrue("needs /bin/sh", File("/bin/sh").canExecute())
        awkwardValues.forEach { value ->
            val line = ShellCommands.putSetting(SettingKey.AutoRotation, value).line
            // Replace the command with printf, so the shell prints the single argument it parsed.
            val printed = sh(line.replaceFirst("settings put system accelerometer_rotation ", "printf %s "))

            assertEquals(value, printed, line)
        }
    }

    @Test
    fun `an empty value is never written`() {
        assertFailsWith<IllegalArgumentException> { ShellCommands.putSetting(SettingKey.EdgePanels, "") }
    }

    @Test
    fun `the bulk stop ends with true so its status is the line's, not the last package's`() {
        val packages = listOf("com.example.a", "org.example.b").map(PackageName::known)

        assertEquals(
            "am force-stop com.example.a; am force-stop org.example.b; true",
            ShellCommands.forceStopAll(packages).line,
        )
    }

    @Test
    fun `launching starts the launcher activity and sends the output nowhere`() {
        assertEquals(
            "am start -a android.intent.action.MAIN -c android.intent.category.LAUNCHER -p com.example.a " +
                ">/dev/null 2>&1; true",
            ShellCommands.launchAll(listOf(PackageName.known("com.example.a"))).line,
        )
    }

    @Test
    fun `launching never uses monkey, which switches auto-rotate on`() {
        val line = ShellCommands.launchAll(listOf(PackageName.known("com.example.a"))).line

        assertFalse("monkey" in line)
    }

    @Test
    fun `dumps get 20 seconds and bulk commands three minutes`() {
        listOf(ShellCommands.dumpWallpaper(), ShellCommands.dumpProcesses(), ShellCommands.dumpAppWidgets())
            .forEach { assertEquals(20.seconds, it.timeout, it.line) }
        val packages = listOf(PackageName.known("com.example.a"))
        assertEquals(3.minutes, ShellCommands.forceStopAll(packages).timeout)
        assertEquals(3.minutes, ShellCommands.launchAll(packages).timeout)
    }

    private fun sh(line: String): String {
        val process = ProcessBuilder("/bin/sh", "-c", line).redirectErrorStream(true).start()
        check(process.waitFor(10, TimeUnit.SECONDS)) { "sh did not finish: $line" }
        return process.inputStream.bufferedReader().readText()
    }
}
