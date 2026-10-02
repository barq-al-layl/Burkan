package io.github.barqallayl.burkan.feature.apply.data

import io.github.barqallayl.burkan.core.model.Renderer
import io.github.barqallayl.burkan.core.shell.PackageName
import io.github.barqallayl.burkan.core.shell.ShellCommands
import io.github.barqallayl.burkan.core.shell.Surfaces
import io.github.barqallayl.burkan.feature.apply.model.RestoredSetting
import io.github.barqallayl.burkan.feature.apply.model.StepKind

/** What the device looked like before a full apply. Everything is read before anything is changed. */
data class FullApplyInputs(
    /** Every installed package, in `pm list packages` order. */
    val installed: List<PackageName>,
    val inputMethods: Set<PackageName>,
    val wallpaper: PackageName?,
    val running: Set<PackageName>,
    val widgets: WidgetPackages,
    val keyboard: PackageName?,
    /** Each setting's value before the run; null when it was unset. */
    val captured: Map<RestoredSetting, String?>,
)

/**
 * The full apply: restart every app so that each one comes back on Vulkan, then put back what force-stopping
 * disturbs.
 */
object FullApplyPlan {

    /**
     * Packages kept running because stopping them breaks Wi-Fi calling or connectivity. From the original script.
     */
    val FixedExclusions: List<PackageName> = listOf(
        "com.samsung.android.bluelightfilter",
        "com.samsung.android.wcmurlsnetworkstack",
        "com.sec.unifiedwfc",
        "com.samsung.android.net.wifi.wifiguider",
        "com.sec.imsservice",
        "com.samsung.ims.smk",
        "com.sec.epdg",
        "com.samsung.android.networkstack",
        "com.samsung.android.networkdiagnostic",
        "com.samsung.android.ConnectivityOverlay",
        "com.netflix.mediaclient",
    ).map(PackageName::known)

    /** The media provider module; its package name varies, so it is matched by this part of the name. */
    const val MEDIA_PROVIDER_MODULE = "providers.media.module"

    /**
     * @param userExclusions packages the user chose never to restart, matched by whole name.
     * @param self Burkan's own package, which has to survive to report the result.
     * @param before what the system surfaces ran with when the run started, and the home app. One already on Vulkan
     * is not restarted.
     */
    fun create(
        inputs: FullApplyInputs,
        userExclusions: Set<PackageName>,
        self: PackageName,
        before: Surfaces,
    ): ApplyPlan {
        val launcher = before.launcher
        val restartLauncher = launcher != null && before.status.launcher != Renderer.Vulkan
        val excluded = buildSet {
            // Never force-stop an input method: that clears it as the default keyboard.
            addAll(inputs.inputMethods)
            if (inputs.keyboard != null) add(inputs.keyboard)
            if (inputs.wallpaper != null) add(inputs.wallpaper)
            addAll(FixedExclusions)
            addAll(userExclusions)
            add(self)
            if (!restartLauncher && launcher != null) add(launcher)
        }
        val stopped = inputs.installed.filter { it !in excluded && MEDIA_PROVIDER_MODULE !in it.value }
        val wanted = inputs.running + inputs.widgets.providers + inputs.widgets.hosts
        // Bring back what was running or backs a widget, if it was stopped. SystemUI and the launcher have their own
        // steps.
        val relaunched = stopped.filter { it in wanted && it != ShellCommands.SystemUi && it != launcher }

        return ApplyPlan(
            steps = buildList {
                add(ApplyStep.Run(StepKind.SetRenderer, ShellCommands.setVulkanRenderer()))
                if (stopped.isNotEmpty()) {
                    add(ApplyStep.Run(StepKind.StopApps(stopped.size), ShellCommands.forceStopAll(stopped)))
                }
                if (restartLauncher && launcher != null) {
                    add(ApplyStep.Run(StepKind.RestartLauncher, ShellCommands.restartLauncher(launcher)))
                }
                if (relaunched.isNotEmpty()) {
                    add(ApplyStep.Run(StepKind.RelaunchApps(relaunched.size), ShellCommands.launchAll(relaunched)))
                }
                if (inputs.keyboard != null && before.status.keyboard != Renderer.Vulkan) {
                    add(ApplyStep.Run(StepKind.RestartKeyboard, ShellCommands.crash(inputs.keyboard)))
                }
            },
            // An unset value is never written back: `null` would be stored as the string "null", and an empty
            // argument is dropped by the shell.
            restore = RestoredSetting.entries.mapNotNull { setting ->
                inputs.captured[setting]
                    ?.takeIf { it.isNotEmpty() && it != "null" }
                    ?.let { ApplyStep.RestoreSetting(setting, it) }
            },
            restartSystemUi = before.status.systemUi != Renderer.Vulkan,
        )
    }
}
