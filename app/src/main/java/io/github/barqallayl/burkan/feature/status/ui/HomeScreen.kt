package io.github.barqallayl.burkan.feature.status.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.PreviewWrapper
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.LifecycleResumeEffect
import com.composables.icons.tabler.Tabler
import com.composables.icons.tabler.outline.AlertTriangle
import com.composables.icons.tabler.outline.CircleCheck
import com.composables.icons.tabler.outline.CircleX
import com.composables.icons.tabler.outline.HelpCircle
import com.composables.icons.tabler.outline.History
import com.composables.icons.tabler.outline.Settings
import dev.zacsweers.metrox.viewmodel.metroViewModel
import io.github.barqallayl.burkan.R
import io.github.barqallayl.burkan.core.model.AppErrorType
import io.github.barqallayl.burkan.core.model.Renderer
import io.github.barqallayl.burkan.core.model.messageRes
import io.github.barqallayl.burkan.core.navigation.LocalNavigator
import io.github.barqallayl.burkan.core.navigation.LogRoute
import io.github.barqallayl.burkan.core.navigation.SettingsRoute
import io.github.barqallayl.burkan.core.ui.durationText
import io.github.barqallayl.burkan.core.ui.formatDateTime
import io.github.barqallayl.burkan.designsystem.ThemeMode
import io.github.barqallayl.burkan.designsystem.preview.BurkanPreview
import io.github.barqallayl.burkan.designsystem.preview.BurkanPreviewTheme
import io.github.barqallayl.burkan.designsystem.preview.BurkanPreviewWrapper
import io.github.barqallayl.burkan.feature.apply.data.ApplyRunState
import io.github.barqallayl.burkan.feature.apply.data.RunPhase
import io.github.barqallayl.burkan.feature.apply.model.ApplyKind
import io.github.barqallayl.burkan.feature.apply.model.RunTrigger
import io.github.barqallayl.burkan.feature.apply.model.StepKind
import io.github.barqallayl.burkan.feature.apply.ui.label
import io.github.barqallayl.burkan.feature.apply.ui.text
import io.github.barqallayl.burkan.feature.connection.model.ConnectionError
import io.github.barqallayl.burkan.feature.log.model.RunLogEntry
import io.github.barqallayl.burkan.feature.log.model.RunResult
import io.github.barqallayl.burkan.feature.log.ui.label
import io.github.barqallayl.burkan.feature.status.model.Headline
import io.github.barqallayl.burkan.feature.status.model.RendererStatus
import io.github.barqallayl.burkan.feature.status.model.headline
import org.orbitmvi.orbit.compose.collectAsState
import org.orbitmvi.orbit.compose.collectSideEffect
import java.time.ZoneId
import java.time.ZoneOffset
import kotlin.time.Duration.Companion.seconds
import kotlin.time.Instant

@Composable
fun HomeScreen() {
    val viewModel = metroViewModel<HomeViewModel>()
    val state by viewModel.collectAsState()
    val navigator = LocalNavigator.current
    LifecycleResumeEffect(viewModel) {
        viewModel.refresh()
        onPauseOrDispose { }
    }
    viewModel.collectSideEffect { effect ->
        when (effect) {
            HomeSideEffect.OpenLog -> navigator.push(LogRoute)
            HomeSideEffect.OpenSettings -> navigator.push(SettingsRoute)
        }
    }
    HomeContent(
        state = state,
        zone = ZoneId.systemDefault(),
        actions = HomeActions(
            onRetry = viewModel::refresh,
            onApplyNow = viewModel::applyNow,
            onRestartAll = viewModel::requestRestartAll,
            onConfirmRestartAll = viewModel::confirmRestartAll,
            onDismissRestartAll = viewModel::dismissRestartAll,
            onCancelRun = viewModel::cancelRun,
            onOpenLog = viewModel::openLog,
            onOpenSettings = viewModel::openSettings,
        ),
    )
}

private class HomeActions(
    val onRetry: () -> Unit = {},
    val onApplyNow: () -> Unit = {},
    val onRestartAll: () -> Unit = {},
    val onConfirmRestartAll: () -> Unit = {},
    val onDismissRestartAll: () -> Unit = {},
    val onCancelRun: () -> Unit = {},
    val onOpenLog: () -> Unit = {},
    val onOpenSettings: () -> Unit = {},
)

@Composable
private fun HomeContent(state: HomeState, zone: ZoneId, actions: HomeActions) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.app_name)) },
                actions = {
                    IconButton(onClick = actions.onOpenLog) {
                        Icon(Tabler.Outline.History, contentDescription = stringResource(R.string.home_open_log))
                    }
                    IconButton(onClick = actions.onOpenSettings) {
                        Icon(Tabler.Outline.Settings, contentDescription = stringResource(R.string.home_open_settings))
                    }
                },
            )
        },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            StatusCard(state, actions.onRetry)
            val run = state.run
            if (run is ApplyRunState.Running) RunningCard(run, actions.onCancelRun)
            LastRun(state.lastRun, zone)
            val idle = run == ApplyRunState.Idle
            Button(onClick = actions.onApplyNow, enabled = idle, modifier = Modifier.fillMaxWidth()) {
                Text(stringResource(R.string.home_apply_now))
            }
            OutlinedButton(onClick = actions.onRestartAll, enabled = idle, modifier = Modifier.fillMaxWidth()) {
                Text(stringResource(R.string.home_restart_all))
            }
        }
    }
    if (state.isConfirmingFullApply) {
        AlertDialog(
            onDismissRequest = actions.onDismissRestartAll,
            title = { Text(stringResource(R.string.home_confirm_title)) },
            text = { Text(stringResource(R.string.home_confirm_text)) },
            confirmButton = {
                TextButton(onClick = actions.onConfirmRestartAll) {
                    Text(stringResource(R.string.home_confirm_action))
                }
            },
            dismissButton = {
                TextButton(onClick = actions.onDismissRestartAll) {
                    Text(stringResource(R.string.home_confirm_dismiss))
                }
            },
        )
    }
}

@Composable
private fun StatusCard(state: HomeState, onRetry: () -> Unit) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            when (val status = state.status) {
                StatusState.Loading -> Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    LoadingIndicator(modifier = Modifier.size(40.dp))
                    Text(stringResource(R.string.home_checking))
                }
                is StatusState.Failed -> {
                    HeadlineRow(Headline.Unknown)
                    Text(stringResource(status.error.messageRes()))
                    Button(onClick = onRetry, enabled = !state.isRefreshing) {
                        Text(stringResource(R.string.home_retry))
                    }
                }
                is StatusState.Loaded -> {
                    val headline = status.status.headline()
                    HeadlineRow(headline)
                    if (headline == Headline.VulkanActive && state.lastRun.isManualLightApply()) {
                        Text(stringResource(R.string.home_open_apps_note))
                    }
                    HorizontalDivider()
                    RendererRow(R.string.home_row_new_apps, status.status.newApps)
                    RendererRow(R.string.home_row_system_ui, status.status.systemUi)
                    RendererRow(R.string.home_row_launcher, status.status.launcher)
                    RendererRow(R.string.home_row_keyboard, status.status.keyboard)
                }
            }
        }
    }
}

/** After a manual light apply, apps that were already open are still on OpenGL; the status must not hide that. */
private fun RunLogEntry?.isManualLightApply(): Boolean =
    this != null && kind == ApplyKind.Light && trigger == RunTrigger.Manual && result == RunResult.Succeeded

@Composable
private fun HeadlineRow(headline: Headline) {
    // An icon and words for each: never colour alone.
    val (icon: ImageVector, text: Int) = when (headline) {
        Headline.VulkanActive -> Tabler.Outline.CircleCheck to R.string.home_headline_active
        Headline.NotApplied -> Tabler.Outline.CircleX to R.string.home_headline_not_applied
        Headline.PartlyApplied -> Tabler.Outline.AlertTriangle to R.string.home_headline_partly
        Headline.Unknown -> Tabler.Outline.HelpCircle to R.string.home_headline_unknown
    }
    Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, contentDescription = null)
        Text(stringResource(text), style = MaterialTheme.typography.titleLarge)
    }
}

@Composable
private fun RendererRow(label: Int, renderer: Renderer) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(stringResource(label))
        Text(
            stringResource(
                when (renderer) {
                    Renderer.Vulkan -> R.string.renderer_vulkan
                    Renderer.OpenGL -> R.string.renderer_opengl
                    Renderer.Unknown -> R.string.renderer_unknown
                },
            ),
        )
    }
}

@Composable
private fun RunningCard(run: ApplyRunState.Running, onCancel: () -> Unit) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            LoadingIndicator(modifier = Modifier.size(40.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(stringResource(run.kind.label), style = MaterialTheme.typography.titleMedium)
                Text(run.phase.label().text())
            }
            TextButton(onClick = onCancel) { Text(stringResource(R.string.home_cancel_run)) }
        }
    }
}

@Composable
private fun LastRun(run: RunLogEntry?, zone: ZoneId) {
    Column {
        Text(stringResource(R.string.home_last_run), style = MaterialTheme.typography.titleMedium)
        if (run == null) {
            Text(stringResource(R.string.home_no_runs))
        } else {
            Text(
                stringResource(
                    R.string.home_last_run_value,
                    formatDateTime(run.startedAt, zone),
                    stringResource(run.trigger.label),
                    stringResource(run.result.label),
                ),
            )
            run.error?.let { Text(stringResource(it.resource)) }
            Text(durationText(run.duration))
        }
    }
}

private val sampleTime = Instant.parse("2026-10-01T09:30:00Z")

private val sampleLightRun = RunLogEntry(
    startedAt = sampleTime,
    trigger = RunTrigger.Manual,
    kind = ApplyKind.Light,
    result = RunResult.Succeeded,
    duration = 6.seconds,
    steps = emptyList(),
)

private fun sample(
    status: StatusState = StatusState.Loaded(
        RendererStatus(Renderer.OpenGL, Renderer.OpenGL, Renderer.OpenGL, Renderer.OpenGL),
    ),
    lastRun: RunLogEntry? = null,
    run: ApplyRunState = ApplyRunState.Idle,
    confirming: Boolean = false,
) = HomeState(status = status, lastRun = lastRun, run = run, isConfirmingFullApply = confirming)

@Composable
private fun HomePreviewContent(state: HomeState) {
    HomeContent(state = state, zone = ZoneOffset.UTC, actions = HomeActions())
}

@PreviewWrapper(BurkanPreviewWrapper::class)
@BurkanPreview
@Composable
private fun HomeNotAppliedPreview() = HomePreviewContent(sample())

@PreviewWrapper(BurkanPreviewWrapper::class)
@BurkanPreview
@Composable
private fun HomeActivePreview() = HomePreviewContent(
    sample(
        status = StatusState.Loaded(
            RendererStatus(Renderer.Vulkan, Renderer.Vulkan, Renderer.Vulkan, Renderer.Vulkan),
        ),
        lastRun = sampleLightRun,
    ),
)

@PreviewWrapper(BurkanPreviewWrapper::class)
@BurkanPreview
@Composable
private fun HomePartlyAppliedPreview() = HomePreviewContent(
    sample(
        status = StatusState.Loaded(
            RendererStatus(Renderer.Vulkan, Renderer.Vulkan, Renderer.OpenGL, Renderer.Unknown),
        ),
        lastRun = sampleLightRun.copy(trigger = RunTrigger.Boot, result = RunResult.Failed),
    ),
)

@PreviewWrapper(BurkanPreviewWrapper::class)
@BurkanPreview
@Composable
private fun HomeLoadingPreview() = HomePreviewContent(sample(status = StatusState.Loading))

@PreviewWrapper(BurkanPreviewWrapper::class)
@BurkanPreview
@Composable
private fun HomeCannotConnectPreview() = HomePreviewContent(
    sample(
        status = StatusState.Failed(ConnectionError.WirelessDebuggingRefused),
        lastRun = sampleLightRun.copy(result = RunResult.Failed, error = AppErrorType.NoWifi),
    ),
)

@PreviewWrapper(BurkanPreviewWrapper::class)
@BurkanPreview
@Composable
private fun HomeRunningPreview() = HomePreviewContent(
    sample(
        run = ApplyRunState.Running(ApplyKind.Full, RunTrigger.Manual, RunPhase.Step(StepKind.StopApps(612))),
        lastRun = sampleLightRun,
    ),
)

@PreviewWrapper(BurkanPreviewWrapper::class)
@BurkanPreview
@Composable
private fun HomeConfirmRestartAllPreview() = HomePreviewContent(sample(confirming = true))

@BurkanPreview
@Composable
private fun HomeActiveDarkPreview() {
    BurkanPreviewTheme(themeMode = ThemeMode.Dark) {
        HomePreviewContent(
            sample(
                status = StatusState.Loaded(
                    RendererStatus(Renderer.Vulkan, Renderer.Vulkan, Renderer.Vulkan, Renderer.Vulkan),
                ),
                lastRun = sampleLightRun.copy(kind = ApplyKind.Full, duration = 73.seconds),
            ),
        )
    }
}
