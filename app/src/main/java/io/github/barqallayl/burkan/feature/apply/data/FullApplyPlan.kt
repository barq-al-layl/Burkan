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
     * @param recent the apps used lately, most recent first, when the run is limited to [limit] of them; null
     * restarts every app. An app that may not be stopped does not count towards the limit.
     * @param only when given, the only apps to restart, and the run is about apps alone: the property and the
     * system surfaces are left as they are. It is how the open apps that are behind are caught up.
     * @param target the renderer the run leaves the phone on.
     */
    fun create(
        inputs: FullApplyInputs,
        userExclusions: Set<PackageName>,
        self: PackageName,
        before: Surfaces,
        recent: List<PackageName>? = null,
        limit: Int? = null,
        only: Set<PackageName>? = null,
        target: Renderer = Renderer.Vulkan,
    ): ApplyPlan {
        val appsOnly = only != null
        val launcher = before.launcher
        val restartLauncher = !appsOnly && launcher != null && before.status.launcher != target
        val kept = kept(inputs.inputMethods, inputs.keyboard, inputs.wallpaper, userExclusions, self) +
            listOfNotNull(launcher.takeUnless { restartLauncher }) +
            // System UI is never force-stopped to any effect; among apps alone it is not even tried.
            listOfNotNull(ShellCommands.SystemUi.takeIf { appsOnly })
        val stoppable = inputs.installed.filter { mayStop(it, kept) }
        val stopped = when {
            only != null -> stoppable.filter { it in only }
            recent == null || limit == null -> stoppable
            else -> {
                val stoppableSet = stoppable.toSet()
                recent.filter { it in stoppableSet }.distinct().take(limit)
            }
        }
        val wanted = inputs.running + inputs.widgets.providers + inputs.widgets.hosts
        // Bring back what was running or backs a widget, if it was stopped. SystemUI and the launcher have their own
        // steps.
        val relaunched = stopped.filter { it in wanted && it != ShellCommands.SystemUi && it != launcher }

        return ApplyPlan(
            steps = buildList {
                if (!appsOnly) rendererStep(before.status.newApps, target)?.let(::add)
                if (stopped.isNotEmpty()) {
                    add(ApplyStep.Run(StepKind.StopApps(stopped.size), ShellCommands.forceStopAll(stopped)))
                }
                if (restartLauncher && launcher != null) {
                    add(ApplyStep.Run(StepKind.RestartLauncher, ShellCommands.restartLauncher(launcher)))
                }
                if (relaunched.isNotEmpty()) {
                    // The user started this from Burkan and comes back to it, not to the last app reopened.
                    add(
                        ApplyStep.Run(
                            StepKind.RelaunchApps(relaunched.size),
                            ShellCommands.launchAll(relaunched, front = self),
                        ),
                    )
                }
                if (!appsOnly && inputs.keyboard != null && before.status.keyboard != target) {
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
            restartSystemUi = !appsOnly && before.status.systemUi != target,
            target = target,
        )
    }

    /**
     * The packages no run force-stops, whatever else it does: the keyboards, the wallpaper, what keeps the phone
     * connected, what the user set aside, and Burkan itself.
     */
    fun kept(
        inputMethods: Set<PackageName>,
        keyboard: PackageName?,
        wallpaper: PackageName?,
        userExclusions: Set<PackageName>,
        self: PackageName,
    ): Set<PackageName> = buildSet {
        // Never force-stop an input method: that clears it as the default keyboard.
        addAll(inputMethods)
        if (keyboard != null) add(keyboard)
        if (wallpaper != null) add(wallpaper)
        addAll(FixedExclusions)
        addAll(userExclusions)
        add(self)
    }

    /** Whether a run may force-stop [packageName], given what is [kept]. */
    fun mayStop(packageName: PackageName, kept: Set<PackageName>): Boolean =
        packageName !in kept && MEDIA_PROVIDER_MODULE !in packageName.value
}
