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

    private val ShortTimeout = 10.seconds
    private val DumpTimeout = 20.seconds
    private val BulkTimeout = 3.minutes

    /** The smallest command that proves the connection works. */
    fun echoOk(): ShellCommand = ShellCommand("echo ok", ShortTimeout)

    /** Lets the app switch wireless debugging itself, so nothing after setup needs the user. */
    fun grantWriteSecureSettings(packageName: PackageName): ShellCommand =
        ShellCommand("pm grant $packageName android.permission.WRITE_SECURE_SETTINGS", ShortTimeout)

    /** Lets [packageName] ask Android when each app was last used. Survives restarts; cleared when it is uninstalled. */
    fun allowUsageAccess(packageName: PackageName): ShellCommand =
        ShellCommand("appops set $packageName GET_USAGE_STATS allow", ShortTimeout)

    fun setVulkanRenderer(): ShellCommand = ShellCommand("setprop debug.hwui.renderer skiavk", ShortTimeout)

    /**
     * Restarts a process the system brings back by itself, without clearing it as a default (keyboard). Not
     * repeatable: crashing System UI twice in a row makes One UI turn off its customisation modules, and crashing it
     * at all restarts adbd, so the connection that sent it is usually gone before the answer.
     *
     * The process is named by its id, not by its package. `am crash <package>` crashes whichever of the package's
     * processes Android comes to first, and System UI has more than one: on a Galaxy S23 it took the edge lighting
     * helper and left System UI itself running on its old renderer. `pidof` matches the whole process name, which
     * for the main process is the package name and nothing more. When nothing by that name is running, the package
     * name is passed as before, for `am` to say so.
     */
    fun crash(packageName: PackageName): ShellCommand =
        ShellCommand(
            "am crash \"\$(pidof -s $packageName || echo $packageName)\"",
            ShortTimeout,
            repeatable = false,
        )

    /**
     * Force-stops every package in one line. The trailing `true` makes the exit status say whether the line ran to
     * the end, not whether the last package stopped; each `force-stop` complaining on stderr is normal.
     */
    fun forceStopAll(packages: List<PackageName>): ShellCommand =
        ShellCommand(packages.joinToString(separator = "") { "am force-stop $it; " } + "true", BulkTimeout)

    /**
     * Stops the home app, gives it about two seconds, and starts home again. The exit status is that of starting it.
     */
    fun restartLauncher(launcher: PackageName): ShellCommand =
        ShellCommand("am force-stop $launcher; sleep 2; am start -a $ACTION_MAIN -c $CATEGORY_HOME", ShortTimeout)

    /** The activity that answers the home intent: the user's home app, or the chooser when none is set. */
    fun resolveHomeActivity(): ShellCommand =
        ShellCommand("cmd package resolve-activity --brief -a $ACTION_MAIN -c $CATEGORY_HOME", ShortTimeout)

    /** The app holding the home role, for when the home intent does not resolve to one app. */
    fun homeRoleHolders(): ShellCommand = ShellCommand("cmd role get-role-holders android.app.role.HOME", ShortTimeout)

    /** Makes [component] (`package/class`) the default keyboard. */
    fun setInputMethod(component: String): ShellCommand = ShellCommand("ime set ${quote(component)}", ShortTimeout)

    /**
     * Launches each package. A package with no launcher activity is passed over. [front] is launched last, so it
     * is the one left in front: without it, whichever app was reopened last stays on the screen.
     */
    fun launchAll(packages: List<PackageName>, front: PackageName? = null): ShellCommand =
        ShellCommand(
            (packages + listOfNotNull(front)).joinToString(separator = "") { "${launch(it)}; " } + "true",
            BulkTimeout,
        )

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

    private const val ACTION_MAIN = "android.intent.action.MAIN"
    private const val CATEGORY_HOME = "android.intent.category.HOME"
    private const val CATEGORY_LAUNCHER = "android.intent.category.LAUNCHER"

    /**
     * Starts the package's launcher activity, as tapping its icon would: the activity is looked up, then started
     * by name. A package without one answers the lookup with a sentence instead of `package/activity`, and is
     * passed over.
     *
     * Not with `monkey`, the usual shortcut for this: `monkey` unlocks the screen's rotation as it exits, which
     * switches auto-rotate on for a user who had it off, once for every app it launches. And not with `am start`
     * on the bare intent either: starting an activity that way only finds launcher activities that also declare
     * the default category, which most do not, so most apps would not come back.
     */
    private fun launch(packageName: PackageName): String =
        "c=\$(cmd package resolve-activity --brief -a $ACTION_MAIN -c $CATEGORY_LAUNCHER $packageName | tail -n 1); " +
            "case \"\$c\" in */*) am start -n \"\$c\" -a $ACTION_MAIN -c $CATEGORY_LAUNCHER >/dev/null 2>&1;; esac"

    /** Single quotes keep `$`, `;`, spaces and the rest literal; an embedded quote closes, escapes and reopens. */
    private fun quote(value: String): String = "'" + value.replace("'", "'\\''") + "'"
}
