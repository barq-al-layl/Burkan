package io.github.barqallayl.burkan.feature.log.ui

import android.content.res.Resources
import androidx.annotation.StringRes
import io.github.barqallayl.burkan.R
import io.github.barqallayl.burkan.core.model.Renderer
import io.github.barqallayl.burkan.core.model.RendererStatus
import io.github.barqallayl.burkan.core.ui.durationText
import io.github.barqallayl.burkan.feature.apply.model.ApplyKind
import io.github.barqallayl.burkan.feature.apply.model.RunTrigger
import io.github.barqallayl.burkan.feature.log.model.RunLogEntry
import io.github.barqallayl.burkan.feature.log.model.RunResult

/** Names for the parts of a run, shared by Home, the Log and the exported log. */
@get:StringRes
val ApplyKind.label: Int
    get() = when (this) {
        ApplyKind.Light -> R.string.kind_light
        ApplyKind.Full -> R.string.kind_full
        ApplyKind.OpenApps -> R.string.kind_open_apps
        ApplyKind.Revert -> R.string.kind_revert
    }

/** What a run of this kind is called while it is under way. */
@get:StringRes
val ApplyKind.runningLabel: Int
    get() = when (this) {
        ApplyKind.Light -> R.string.apply_running_light_title
        ApplyKind.Full -> R.string.apply_running_full_title
        ApplyKind.OpenApps -> R.string.apply_running_open_apps_title
        ApplyKind.Revert -> R.string.apply_running_revert_title
    }

@get:StringRes
val RunTrigger.label: Int
    get() = when (this) {
        RunTrigger.Manual -> R.string.trigger_manual
        RunTrigger.Boot -> R.string.trigger_boot
        RunTrigger.AtLock -> R.string.trigger_at_lock
        RunTrigger.External -> R.string.trigger_external
    }

@get:StringRes
val RunResult.label: Int
    get() = when (this) {
        RunResult.Succeeded -> R.string.result_succeeded
        RunResult.Failed -> R.string.result_failed
        RunResult.Cancelled -> R.string.result_cancelled
        RunResult.AlreadyApplied -> R.string.result_already_applied
        RunResult.Postponed -> R.string.result_postponed
    }

/**
 * How the run ended, in words. A run that found nothing to change says "already applied" when it was applying
 * Vulkan, and that there was nothing to do otherwise: going back to OpenGL, or restarting open apps.
 */
@get:StringRes
val RunLogEntry.resultLabel: Int
    get() = when {
        result != RunResult.AlreadyApplied -> result.label
        kind == ApplyKind.Light || kind == ApplyKind.Full -> result.label
        else -> R.string.result_nothing_to_do
    }

/** A renderer's name. */
@get:StringRes
val Renderer.label: Int
    get() = when (this) {
        Renderer.Vulkan -> R.string.renderer_vulkan
        Renderer.OpenGL -> R.string.renderer_opengl
        Renderer.Unknown -> R.string.renderer_unknown
    }

/**
 * What the log adds under a run's steps, a line each: what the phone ran on before and after, how long it took to
 * get to the first step, and how many open apps were left behind. Only what the run kept.
 */
fun Resources.runDetailLines(run: RunLogEntry): List<String> = buildList {
    run.preparing?.let { add(getString(R.string.log_preparing, durationText(it))) }
    run.before?.let { add(getString(R.string.log_before, surfacesText(it))) }
    run.after?.let { add(getString(R.string.log_after, surfacesText(it))) }
    run.behind?.let { add(getQuantityString(R.plurals.log_behind, it, it)) }
}

private fun Resources.surfacesText(status: RendererStatus): String = getString(
    R.string.log_surfaces,
    getString(status.newApps.label),
    getString(status.systemUi.label),
    getString(status.launcher.label),
    getString(status.keyboard.label),
)
