package io.github.barqallayl.burkan.core.shell

import kotlin.time.Duration
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

/**
 * One command line and how long it may take. Built only by [ShellCommands].
 *
 * [repeatable] says whether running it twice does no harm, so it may be sent again when the connection drops part
 * way through. Reads, `setprop`, `settings put` and stopping or starting an app are; restarting a process by crashing
 * it is not.
 */
@ConsistentCopyVisibility
data class ShellCommand internal constructor(val line: String, val timeout: Duration, val repeatable: Boolean = true)

/** A `settings` namespace. */
enum class SettingsNamespace(val value: String) {
    System("system"),
    Secure("secure"),
    Global("global"),
}

/** A key in the `settings` provider. The keys are constants of this app, never user input. */
@ConsistentCopyVisibility
data class SettingKey private constructor(val namespace: SettingsNamespace, val name: String) {
    companion object {
        val AutoRotation = SettingKey(SettingsNamespace.System, "accelerometer_rotation")
        val AccessibilityServices = SettingKey(SettingsNamespace.Secure, "enabled_accessibility_services")
        val EdgeEnabled = SettingKey(SettingsNamespace.Secure, "edge_enable")
        val EdgePanels = SettingKey(SettingsNamespace.Secure, "edge_panels_enabled")
        val DefaultInputMethod = SettingKey(SettingsNamespace.Secure, "default_input_method")
    }
}

/**
 * The one place command lines are built. Each function takes validated types; quoting lives here and nowhere else,
 * because the device shell re-parses the whole line.
 */
object ShellCommands {

    val SystemUi: PackageName = PackageName.known("com.android.systemui")
    val Launcher: PackageName = PackageName.known("com.sec.android.app.launcher")

    private val ShortTimeout = 10.seconds
    private val DumpTimeout = 20.seconds
    private val BulkTimeout = 3.minutes

    /** The smallest command that proves the connection works. */
    fun echoOk(): ShellCommand = ShellCommand("echo ok", ShortTimeout)

    /** Lets the app switch wireless debugging itself, so nothing after setup needs the user. */
    fun grantWriteSecureSettings(packageName: PackageName): ShellCommand =
        ShellCommand("pm grant $packageName android.permission.WRITE_SECURE_SETTINGS", ShortTimeout)

    fun setVulkanRenderer(): ShellCommand = ShellCommand("setprop debug.hwui.renderer skiavk", ShortTimeout)

    /**
     * Restarts a process the system brings back by itself, without clearing it as a default (keyboard). Not
     * repeatable: crashing System UI twice in a row makes One UI turn off its customisation modules, and crashing it
     * at all restarts adbd, so the connection that sent it is usually gone before the answer.
     */
    fun crash(packageName: PackageName): ShellCommand =
        ShellCommand("am crash $packageName", ShortTimeout, repeatable = false)

    fun forceStop(packageName: PackageName): ShellCommand = ShellCommand("am force-stop $packageName", ShortTimeout)

    /**
     * Force-stops every package in one line. The trailing `true` makes the exit status say whether the line ran to
     * the end, not whether the last package stopped; each `force-stop` complaining on stderr is normal.
     */
    fun forceStopAll(packages: List<PackageName>): ShellCommand =
        ShellCommand(packages.joinToString(separator = "") { "am force-stop $it; " } + "true", BulkTimeout)

    /** Stops the launcher, gives it about two seconds, and starts it again. */
    fun restartLauncher(): ShellCommand =
        ShellCommand("am force-stop $Launcher; sleep 2; ${launch(Launcher)}; true", ShortTimeout)

    /** Launches each package. A package with no launcher activity makes `monkey` complain; that is normal. */
    fun launchAll(packages: List<PackageName>): ShellCommand =
        ShellCommand(packages.joinToString(separator = "") { "${launch(it)}; " } + "true", BulkTimeout)

    /** What new processes will render with: `skiavk` when Vulkan is set, an empty line when nothing is. */
    fun getRenderer(): ShellCommand = ShellCommand("getprop debug.hwui.renderer", ShortTimeout)

    /** What a running process renders with, in its `Pipeline=` line. */
    fun gfxInfo(packageName: PackageName): ShellCommand = ShellCommand("dumpsys gfxinfo $packageName", DumpTimeout)

    fun listPackages(): ShellCommand = ShellCommand("pm list packages", ShortTimeout)

    fun listInputMethods(): ShellCommand = ShellCommand("ime list -s", ShortTimeout)

    fun dumpWallpaper(): ShellCommand = ShellCommand("dumpsys wallpaper", DumpTimeout)

    fun dumpProcesses(): ShellCommand = ShellCommand("dumpsys activity processes", DumpTimeout)

    fun dumpAppWidgets(): ShellCommand = ShellCommand("dumpsys appwidget", DumpTimeout)

    fun getSetting(key: SettingKey): ShellCommand =
        ShellCommand("settings get ${key.namespace.value} ${key.name}", ShortTimeout)

    /**
     * Writes [value], which must not be empty: the shell drops an empty argument and `settings put` then fails.
     * `null` is not a value either; it is how `settings get` reports an unset key.
     */
    fun putSetting(key: SettingKey, value: String): ShellCommand {
        require(value.isNotEmpty()) { "An empty value cannot be written; skip the write instead" }
        return ShellCommand("settings put ${key.namespace.value} ${key.name} ${quote(value)}", ShortTimeout)
    }

    private fun launch(packageName: PackageName): String =
        "monkey -p $packageName -c android.intent.category.LAUNCHER 1 >/dev/null 2>&1"

    /** Single quotes keep `$`, `;`, spaces and the rest literal; an embedded quote closes, escapes and reopens. */
    private fun quote(value: String): String = "'" + value.replace("'", "'\\''") + "'"
}
