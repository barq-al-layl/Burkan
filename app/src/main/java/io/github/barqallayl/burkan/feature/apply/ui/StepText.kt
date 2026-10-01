package io.github.barqallayl.burkan.feature.apply.ui

import android.content.res.Resources
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import io.github.barqallayl.burkan.R
import io.github.barqallayl.burkan.feature.apply.data.RunPhase
import io.github.barqallayl.burkan.feature.apply.model.RestoredSetting
import io.github.barqallayl.burkan.feature.apply.model.StepKind

/** A step or phase in words: a string, or a plural with its count. Shared by the notification, Home and the Log. */
class StepLabel(val resource: Int, val count: Int? = null)

fun StepKind.label(): StepLabel = when (this) {
    StepKind.SetRenderer -> StepLabel(R.string.step_set_renderer)
    is StepKind.StopApps -> StepLabel(R.plurals.step_stop_apps, count)
    StepKind.RestartSystemUi -> StepLabel(R.string.step_restart_system_ui)
    StepKind.RestartLauncher -> StepLabel(R.string.step_restart_launcher)
    is StepKind.RelaunchApps -> StepLabel(R.plurals.step_relaunch_apps, count)
    StepKind.RestartKeyboard -> StepLabel(R.string.step_restart_keyboard)
    is StepKind.RestoreSetting -> StepLabel(
        when (setting) {
            RestoredSetting.AutoRotation -> R.string.step_restore_auto_rotation
            RestoredSetting.AccessibilityServices -> R.string.step_restore_accessibility
            RestoredSetting.EdgeEnabled -> R.string.step_restore_edge_enabled
            RestoredSetting.EdgePanels -> R.string.step_restore_edge_panels
        },
    )
}

fun RunPhase.label(): StepLabel = when (this) {
    RunPhase.Connecting -> StepLabel(R.string.phase_connecting)
    RunPhase.Reading -> StepLabel(R.string.phase_reading)
    is RunPhase.Step -> kind.label()
}

fun Resources.text(label: StepLabel): String =
    label.count?.let { getQuantityString(label.resource, it, it) } ?: getString(label.resource)

@Composable
fun StepLabel.text(): String = count?.let { pluralStringResource(resource, it, it) } ?: stringResource(resource)
