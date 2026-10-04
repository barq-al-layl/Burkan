package io.github.barqallayl.burkan.feature.status.ui

import android.content.Intent
import android.provider.Settings as SystemSettings
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowRight
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.PreviewWrapper
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.LifecycleResumeEffect
import com.composables.icons.tabler.Tabler
import com.composables.icons.tabler.outline.AlertTriangle
import com.composables.icons.tabler.outline.CircleCheck
import com.composables.icons.tabler.outline.CircleX
import com.composables.icons.tabler.outline.HelpCircle
import com.composables.icons.tabler.outline.History
import com.composables.icons.tabler.outline.Hourglass
import com.composables.icons.tabler.outline.Lock
import com.composables.icons.tabler.outline.Settings
import dev.zacsweers.metrox.viewmodel.metroViewModel
import io.github.barqallayl.burkan.R
import io.github.barqallayl.burkan.core.model.AppErrorType
import io.github.barqallayl.burkan.core.model.Renderer
import io.github.barqallayl.burkan.core.model.RendererStatus
import io.github.barqallayl.burkan.core.model.messageRes
import io.github.barqallayl.burkan.core.navigation.LocalNavigator
import io.github.barqallayl.burkan.core.navigation.LogRoute
import io.github.barqallayl.burkan.core.navigation.SettingsRoute
import io.github.barqallayl.burkan.core.ui.durationText
import io.github.barqallayl.burkan.core.ui.formatDateTime
import io.github.barqallayl.burkan.core.ui.openSettings
import io.github.barqallayl.burkan.designsystem.SeedColors
import io.github.barqallayl.burkan.designsystem.TextScale
import io.github.barqallayl.burkan.designsystem.ThemeMode
import io.github.barqallayl.burkan.designsystem.component.BurkanBottomSheet
import io.github.barqallayl.burkan.designsystem.component.BurkanSectionTitle
import io.github.barqallayl.burkan.designsystem.component.BurkanSegment
import io.github.barqallayl.burkan.designsystem.component.BurkanSegmentChoice
import io.github.barqallayl.burkan.designsystem.component.BurkanSegmentItem
import io.github.barqallayl.burkan.designsystem.component.GroupGap
import io.github.barqallayl.burkan.designsystem.component.ScreenMargin
import io.github.barqallayl.burkan.designsystem.component.SegmentedColumn
import io.github.barqallayl.burkan.designsystem.preview.BurkanPreview
import io.github.barqallayl.burkan.designsystem.preview.BurkanPreviewTheme
import io.github.barqallayl.burkan.designsystem.preview.BurkanPreviewWrapper
import io.github.barqallayl.burkan.feature.apply.data.ApplyRunState
import io.github.barqallayl.burkan.feature.apply.data.RunPhase
import io.github.barqallayl.burkan.feature.apply.model.ApplyKind
import io.github.barqallayl.burkan.feature.apply.model.RestartScope
import io.github.barqallayl.burkan.feature.apply.model.RunTrigger
import io.github.barqallayl.burkan.feature.apply.model.StepKind
import io.github.barqallayl.burkan.feature.apply.model.WaitReason
import io.github.barqallayl.burkan.feature.apply.ui.label
import io.github.barqallayl.burkan.feature.apply.ui.text
import io.github.barqallayl.burkan.feature.connection.model.ConnectionError
import io.github.barqallayl.burkan.feature.log.model.RunLogEntry
import io.github.barqallayl.burkan.feature.log.model.RunResult
import io.github.barqallayl.burkan.feature.log.ui.RunResultIcon
import io.github.barqallayl.burkan.feature.log.ui.label
import io.github.barqallayl.burkan.feature.status.model.Headline
import io.github.barqallayl.burkan.feature.status.model.headline
import java.time.ZoneId
import java.time.ZoneOffset
import org.orbitmvi.orbit.compose.collectAsState
import org.orbitmvi.orbit.compose.collectSideEffect
import kotlin.time.Duration.Companion.seconds
import kotlin.time.Instant

@Composable
fun HomeScreen() {
    val viewModel = metroViewModel<HomeViewModel>()
    val state by viewModel.collectAsState()
    val navigator = LocalNavigator.current
    val context = LocalContext.current
    LifecycleResumeEffect(viewModel) {
        viewModel.onResume()
        onPauseOrDispose { viewModel.onPause() }
    }
    viewModel.collectSideEffect { effect ->
        when (effect) {
            HomeSideEffect.OpenLog -> navigator.push(LogRoute)
            HomeSideEffect.OpenSettings -> navigator.push(SettingsRoute)
            HomeSideEffect.OpenDeveloperOptions ->
                context.openSettings(Intent(SystemSettings.ACTION_APPLICATION_DEVELOPMENT_SETTINGS))
        }
    }
    HomeContent(
        state = state,
        zone = ZoneId.systemDefault(),
        actions = HomeActions(
            onRetry = viewModel::refresh,
            onApplyNow = viewModel::applyNow,
            onConfirmApply = viewModel::confirmApply,
            onDismissApply = viewModel::dismissApply,
            onRestartAll = viewModel::requestRestartAll,
            onRestartScope = viewModel::chooseRestartScope,
            onConfirmRestartAll = viewModel::confirmRestartAll,
            onDismissRestartAll = viewModel::dismissRestartAll,
            onCancelRun = viewModel::cancelRun,
            onOpenLog = viewModel::openLog,
            onOpenSettings = viewModel::openSettings,
            onOpenDeveloperOptions = viewModel::openDeveloperOptions,
        ),
    )
}

private class HomeActions(
    val onRetry: () -> Unit = {},
    val onApplyNow: () -> Unit = {},
    val onConfirmApply: () -> Unit = {},
    val onDismissApply: () -> Unit = {},
    val onRestartAll: () -> Unit = {},
    val onRestartScope: (RestartScope) -> Unit = {},
    val onConfirmRestartAll: () -> Unit = {},
    val onDismissRestartAll: () -> Unit = {},
    val onCancelRun: () -> Unit = {},
    val onOpenLog: () -> Unit = {},
    val onOpenSettings: () -> Unit = {},
    val onOpenDeveloperOptions: () -> Unit = {},
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
                .padding(horizontal = ScreenMargin)
                .padding(bottom = GroupGap),
            verticalArrangement = Arrangement.spacedBy(GroupGap),
        ) {
            state.waitingFor?.let { WaitingNotice(it, actions.onOpenDeveloperOptions) }
            if (state.systemUiAtNextLock) NextLockNotice()
            val run = state.run
            // The run first: it is what is happening, and the status below says it is about to change.
            if (run is ApplyRunState.Running) RunningSegment(run, actions.onCancelRun)
            StatusGroup(state, actions.onRetry)
            LastRun(state.lastRun, zone, actions.onOpenLog)
            val idle = run == ApplyRunState.Idle
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = actions.onApplyNow, enabled = idle, modifier = Modifier.fillMaxWidth()) {
                    Text(stringResource(R.string.home_apply_now))
                }
                OutlinedButton(onClick = actions.onRestartAll, enabled = idle, modifier = Modifier.fillMaxWidth()) {
                    Text(stringResource(R.string.home_restart_all))
                }
            }
        }
    }
    if (state.isConfirmingApply) {
        AlertDialog(
            onDismissRequest = actions.onDismissApply,
            title = { Text(stringResource(R.string.home_confirm_apply_title)) },
            text = { Text(stringResource(R.string.home_confirm_apply_text)) },
            confirmButton = {
                TextButton(onClick = actions.onConfirmApply) {
                    Text(stringResource(R.string.home_confirm_apply_action))
                }
            },
            dismissButton = {
                TextButton(onClick = actions.onDismissApply) { Text(stringResource(R.string.home_confirm_dismiss)) }
            },
        )
    }
    if (state.isConfirmingFullApply) {
        BurkanBottomSheet(onDismiss = actions.onDismissRestartAll) { hide ->
            RestartAllSheet(
                scope = state.restartScope,
                onScope = actions.onRestartScope,
                onConfirm = { hide(actions.onConfirmRestartAll) },
                onDismiss = { hide(actions.onDismissRestartAll) },
            )
        }
    }
}

/**
 * Asks before the full apply, and how far it should go: every app, as it opens, or only the most recent few.
 */
@Composable
private fun RestartAllSheet(
    scope: RestartScope,
    onScope: (RestartScope) -> Unit,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    Column(modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState())) {
        Text(
            stringResource(R.string.home_confirm_title),
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(horizontal = 24.dp),
        )
        Text(
            stringResource(R.string.home_confirm_text),
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.padding(horizontal = 24.dp).padding(top = 8.dp),
        )
        Column(modifier = Modifier.padding(ScreenMargin)) {
            BurkanSectionTitle(stringResource(R.string.home_confirm_scope))
            SegmentedColumn(modifier = Modifier.selectableGroup()) {
                RestartScope.entries.forEachIndexed { index, option ->
                    BurkanSegmentChoice(
                        index = index,
                        count = RestartScope.entries.size,
                        text = option.label(),
                        selected = option == scope,
                        onClick = { onScope(option) },
                    )
                }
            }
            Text(
                stringResource(R.string.home_confirm_scope_text),
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(horizontal = 16.dp).padding(top = 8.dp),
            )
            Row(modifier = Modifier.padding(top = GroupGap), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedButton(onClick = onDismiss, modifier = Modifier.weight(1f)) {
                    Text(stringResource(R.string.home_confirm_dismiss))
                }
                Button(onClick = onConfirm, modifier = Modifier.weight(1f)) {
                    Text(stringResource(R.string.home_confirm_action))
                }
            }
        }
    }
}

@Composable
private fun RestartScope.label(): String {
    val limit = limit ?: return stringResource(R.string.restart_scope_all)
    return stringResource(R.string.restart_scope_recent, limit)
}

/**
 * The status as one group: a headline segment, then a segment per surface. A status that could not be read is a
 * single segment saying why, with a way to try again.
 */
@Composable
private fun StatusGroup(state: HomeState, onRetry: () -> Unit) {
    val status = state.status
    if (status is StatusState.Failed) {
        BurkanSegment(index = 0, count = 1) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                HeadlineRow(Tabler.Outline.HelpCircle, R.string.home_headline_unknown)
                Text(stringResource(status.error.messageRes()))
                Button(onClick = onRetry, enabled = !state.isRefreshing) {
                    Text(stringResource(R.string.home_retry))
                }
            }
        }
        return
    }
    // Null while the status is read: the rows stay in place with placeholders, so nothing moves when it arrives.
    val renderers = (status as? StatusState.Loaded)?.status
    SegmentedColumn {
        BurkanSegment(index = 0, count = STATUS_SEGMENTS, containerColor = MaterialTheme.colorScheme.primaryContainer) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                when {
                    renderers == null -> Row(
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        LoadingIndicator(modifier = Modifier.size(24.dp))
                        Text(stringResource(R.string.home_checking), style = MaterialTheme.typography.titleLarge)
                    }
                    state.run is ApplyRunState.Running -> {
                        // What is shown was read before the run, which is changing it now.
                        HeadlineRow(Tabler.Outline.Hourglass, R.string.home_headline_changing)
                        Text(stringResource(R.string.home_changing_note))
                    }
                    else -> {
                        val headline = renderers.headline()
                        HeadlineRow(headline)
                        if (headline == Headline.VulkanActive && state.lastRun.isManualLightApply()) {
                            Text(stringResource(R.string.home_open_apps_note))
                        }
                    }
                }
            }
        }
        RendererSegment(1, R.string.home_row_new_apps, renderers?.newApps)
        RendererSegment(2, R.string.home_row_system_ui, renderers?.systemUi)
        RendererSegment(3, R.string.home_row_launcher, renderers?.launcher)
        RendererSegment(4, R.string.home_row_keyboard, renderers?.keyboard)
    }
}

/** The headline, then one segment for each of the four surfaces. */
private const val STATUS_SEGMENTS = 5

/** After a manual light apply, apps that were already open are still on OpenGL; the status must not hide that. */
private fun RunLogEntry?.isManualLightApply(): Boolean =
    this != null && kind == ApplyKind.Light && trigger == RunTrigger.Manual && result == RunResult.Succeeded

@Composable
private fun HeadlineRow(headline: Headline) {
    // An icon and words for each: never colour alone.
    when (headline) {
        Headline.VulkanActive -> HeadlineRow(Tabler.Outline.CircleCheck, R.string.home_headline_active)
        Headline.NotApplied -> HeadlineRow(Tabler.Outline.CircleX, R.string.home_headline_not_applied)
        Headline.PartlyApplied -> HeadlineRow(Tabler.Outline.AlertTriangle, R.string.home_headline_partly)
        Headline.Unknown -> HeadlineRow(Tabler.Outline.HelpCircle, R.string.home_headline_unknown)
    }
}

@Composable
private fun HeadlineRow(icon: ImageVector, text: Int) {
    Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, contentDescription = null)
        Text(stringResource(text), style = MaterialTheme.typography.titleLarge)
    }
}

/**
 * A surface and its renderer, with an icon as well as the word so the three differ at a glance. [renderer] null is
 * a placeholder while the status is read.
 */
@Composable
private fun RendererSegment(index: Int, label: Int, renderer: Renderer?) {
    val (icon, value) = when (renderer) {
        Renderer.Vulkan -> Tabler.Outline.CircleCheck to R.string.renderer_vulkan
        Renderer.OpenGL -> Tabler.Outline.CircleX to R.string.renderer_opengl
        Renderer.Unknown -> Tabler.Outline.HelpCircle to R.string.renderer_unknown
        null -> Tabler.Outline.Hourglass to R.string.home_value_checking
    }
    BurkanSegment(index = index, count = STATUS_SEGMENTS) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(stringResource(label), style = MaterialTheme.typography.titleMedium)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(icon, contentDescription = null, modifier = Modifier.size(20.dp))
                Text(stringResource(value))
            }
        }
    }
}

/** Something the automatic apply is waiting for. It stands apart from the status in the scheme's secondary colour. */
@Composable
private fun Notice(icon: ImageVector, title: Int, text: Int, action: (@Composable () -> Unit)? = null) {
    BurkanSegmentItem(
        index = 0,
        count = 1,
        headline = stringResource(title),
        supporting = stringResource(text),
        containerColor = MaterialTheme.colorScheme.secondaryContainer,
        verticalAlignment = Alignment.Top,
        leading = { Icon(icon, contentDescription = null) },
        content = action?.let { { it() } },
    )
}

@Composable
private fun WaitingNotice(reason: WaitReason, onOpenDeveloperOptions: () -> Unit) {
    when (reason) {
        WaitReason.Wifi ->
            Notice(Tabler.Outline.Hourglass, R.string.home_waiting_wifi_title, R.string.home_waiting_wifi_text)
        WaitReason.TrustedNetwork -> Notice(
            Tabler.Outline.Hourglass,
            R.string.home_waiting_network_title,
            R.string.home_waiting_network_text,
        ) {
            TextButton(onClick = onOpenDeveloperOptions) {
                Text(stringResource(R.string.home_waiting_open_developer_options))
            }
        }
    }
}

@Composable
private fun NextLockNotice() {
    Notice(Tabler.Outline.Lock, R.string.home_next_lock_title, R.string.home_next_lock_text)
}

@Composable
private fun RunningSegment(run: ApplyRunState.Running, onCancel: () -> Unit) {
    BurkanSegmentItem(
        index = 0,
        count = 1,
        headline = stringResource(run.kind.label),
        supporting = run.phase.label().text(),
        leading = { LoadingIndicator(modifier = Modifier.size(40.dp)) },
        trailing = { TextButton(onClick = onCancel) { Text(stringResource(R.string.home_cancel_run)) } },
    )
}

/** The newest run, as the log shows it. It opens the log. */
@Composable
private fun LastRun(run: RunLogEntry?, zone: ZoneId, onOpenLog: () -> Unit) {
    Column {
        BurkanSectionTitle(stringResource(R.string.home_last_run))
        if (run == null) {
            BurkanSegmentItem(
                index = 0,
                count = 1,
                headline = stringResource(R.string.home_no_runs),
                leading = { Icon(Tabler.Outline.History, contentDescription = null) },
            )
            return@Column
        }
        BurkanSegmentItem(
            index = 0,
            count = 1,
            headline = stringResource(
                R.string.log_run_title,
                stringResource(run.kind.label),
                stringResource(run.result.label),
            ),
            supporting = stringResource(
                R.string.log_run_detail,
                formatDateTime(run.startedAt, zone),
                stringResource(run.trigger.label),
                durationText(run.duration),
            ),
            onClick = onOpenLog,
            leading = { RunResultIcon(failed = run.result == RunResult.Failed) },
            trailing = { Icon(Icons.AutoMirrored.Outlined.KeyboardArrowRight, contentDescription = null) },
        ) {
            run.error?.let { Text(stringResource(it.resource), color = MaterialTheme.colorScheme.error) }
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
    waitingFor: WaitReason? = null,
) = HomeState(
    status = status,
    lastRun = lastRun,
    run = run,
    isConfirmingFullApply = confirming,
    waitingFor = waitingFor,
)

@Composable
private fun HomePreviewContent(state: HomeState) {
    HomeContent(state = state, zone = ZoneOffset.UTC, actions = HomeActions())
}

@PreviewWrapper(BurkanPreviewWrapper::class)
@BurkanPreview
@Composable
private fun HomeRestartAllSheetPreview() =
    RestartAllSheet(scope = RestartScope.All, onScope = {}, onConfirm = {}, onDismiss = {})

@PreviewWrapper(BurkanPreviewWrapper::class)
@BurkanPreview
@Composable
private fun HomeRestartAllSheetLimitedPreview() =
    RestartAllSheet(scope = RestartScope.Recent50, onScope = {}, onConfirm = {}, onDismiss = {})

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
private fun HomeWaitingForWifiPreview() = HomePreviewContent(
    sample(status = StatusState.Failed(ConnectionError.NoWifi), waitingFor = WaitReason.Wifi),
)

@PreviewWrapper(BurkanPreviewWrapper::class)
@BurkanPreview
@Composable
private fun HomeWaitingForTrustedNetworkPreview() = HomePreviewContent(
    sample(
        status = StatusState.Failed(ConnectionError.WirelessDebuggingRefused),
        lastRun = sampleLightRun.copy(
            trigger = RunTrigger.Boot,
            result = RunResult.Failed,
            error = AppErrorType.WirelessDebuggingRefused,
        ),
        waitingFor = WaitReason.TrustedNetwork,
    ),
)

@PreviewWrapper(BurkanPreviewWrapper::class)
@BurkanPreview
@Composable
private fun HomeSystemUiAtNextLockPreview() = HomePreviewContent(
    sample(
        status = StatusState.Loaded(
            RendererStatus(Renderer.Vulkan, Renderer.OpenGL, Renderer.Vulkan, Renderer.Vulkan),
        ),
        lastRun = sampleLightRun.copy(trigger = RunTrigger.Boot),
    ).copy(systemUiAtNextLock = true),
)

@PreviewWrapper(BurkanPreviewWrapper::class)
@BurkanPreview
@Composable
private fun HomeConfirmApplyPreview() = HomePreviewContent(sample().copy(isConfirmingApply = true))

@PreviewWrapper(BurkanPreviewWrapper::class)
@BurkanPreview
@Composable
private fun HomeRestartingSystemUiPreview() = HomePreviewContent(
    sample(
        run = ApplyRunState.Running(ApplyKind.Light, RunTrigger.Manual, RunPhase.Step(StepKind.RestartSystemUi)),
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

@BurkanPreview
@Composable
private fun HomeRunningLargeTextPreview() {
    BurkanPreviewTheme(textScalePercent = TextScale.percentages.last) {
        HomePreviewContent(
            sample(
                run = ApplyRunState.Running(ApplyKind.Light, RunTrigger.Boot, RunPhase.Step(StepKind.RestartSystemUi)),
                lastRun = sampleLightRun,
                waitingFor = WaitReason.Wifi,
            ),
        )
    }
}

@BurkanPreview
@Composable
private fun HomePartlyAppliedTealPreview() {
    BurkanPreviewTheme(seedColor = SeedColors.Teal) {
        HomePreviewContent(
            sample(
                status = StatusState.Loaded(
                    RendererStatus(Renderer.Vulkan, Renderer.Vulkan, Renderer.OpenGL, Renderer.Unknown),
                ),
                lastRun = sampleLightRun,
            ),
        )
    }
}
