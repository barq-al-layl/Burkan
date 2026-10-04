package io.github.barqallayl.burkan.feature.log.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ProvideTextStyle
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.PreviewWrapper
import androidx.compose.ui.unit.dp
import com.composables.icons.tabler.Tabler
import com.composables.icons.tabler.outline.AlertCircle
import com.composables.icons.tabler.outline.ChevronDown
import com.composables.icons.tabler.outline.CircleCheck
import com.composables.icons.tabler.outline.History
import com.composables.icons.tabler.outline.Share
import dev.zacsweers.metrox.viewmodel.metroViewModel
import io.github.barqallayl.burkan.R
import io.github.barqallayl.burkan.core.model.AppErrorType
import io.github.barqallayl.burkan.core.navigation.LocalNavigator
import io.github.barqallayl.burkan.core.ui.LocalCurrentYear
import io.github.barqallayl.burkan.core.ui.durationText
import io.github.barqallayl.burkan.core.ui.formatDateTime
import io.github.barqallayl.burkan.designsystem.MonoFontFamily
import io.github.barqallayl.burkan.designsystem.SeedColors
import io.github.barqallayl.burkan.designsystem.TextScale
import io.github.barqallayl.burkan.designsystem.ThemeMode
import io.github.barqallayl.burkan.designsystem.component.BurkanMessage
import io.github.barqallayl.burkan.designsystem.component.BurkanSegment
import io.github.barqallayl.burkan.designsystem.component.BurkanSegmentItem
import io.github.barqallayl.burkan.designsystem.component.GroupGap
import io.github.barqallayl.burkan.designsystem.component.ScreenMargin
import io.github.barqallayl.burkan.designsystem.component.SegmentGap
import io.github.barqallayl.burkan.designsystem.preview.BurkanPreview
import io.github.barqallayl.burkan.designsystem.preview.BurkanPreviewTheme
import io.github.barqallayl.burkan.designsystem.preview.BurkanPreviewWrapper
import io.github.barqallayl.burkan.feature.apply.model.ApplyKind
import io.github.barqallayl.burkan.feature.apply.model.RestoredSetting
import io.github.barqallayl.burkan.feature.apply.model.RunTrigger
import io.github.barqallayl.burkan.feature.apply.model.StepKind
import io.github.barqallayl.burkan.feature.apply.ui.label
import io.github.barqallayl.burkan.feature.apply.ui.text
import io.github.barqallayl.burkan.feature.log.model.LoggedStep
import io.github.barqallayl.burkan.feature.log.model.RunLogEntry
import io.github.barqallayl.burkan.feature.log.model.RunResult
import java.time.ZoneId
import java.time.ZoneOffset
import org.orbitmvi.orbit.compose.collectAsState
import org.orbitmvi.orbit.compose.collectSideEffect
import kotlin.time.Clock
import kotlin.time.Duration.Companion.seconds
import kotlin.time.Instant

@Composable
fun LogScreen() {
    val viewModel = metroViewModel<LogViewModel>()
    val state by viewModel.collectAsState()
    val navigator = LocalNavigator.current
    val context = LocalContext.current
    val zone = ZoneId.systemDefault()
    viewModel.collectSideEffect { effect ->
        when (effect) {
            LogSideEffect.Back -> navigator.pop()
            is LogSideEffect.Share -> context.shareLogFile(effect.runs, zone, Clock.System.now())
        }
    }
    LogContent(
        state = state,
        zone = zone,
        onToggle = viewModel::toggle,
        onShare = viewModel::share,
        onBack = viewModel::back,
    )
}

@Composable
private fun LogContent(
    state: LogState,
    zone: ZoneId,
    onToggle: (RunLogEntry) -> Unit,
    onShare: () -> Unit,
    onBack: () -> Unit,
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.log_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Outlined.ArrowBack, stringResource(R.string.navigate_back))
                    }
                },
                actions = {
                    if (!state.runs.isNullOrEmpty()) {
                        IconButton(onClick = onShare) {
                            Icon(Tabler.Outline.Share, contentDescription = stringResource(R.string.log_share))
                        }
                    }
                },
            )
        },
    ) { innerPadding ->
        val runs = state.runs
        when {
            runs == null -> Box(Modifier.fillMaxSize().padding(innerPadding), contentAlignment = Alignment.Center) {
                LoadingIndicator()
            }
            runs.isEmpty() && state.isUnreadable -> BurkanMessage(
                icon = Tabler.Outline.AlertCircle,
                title = stringResource(R.string.log_unreadable_title),
                text = stringResource(R.string.log_unreadable_text),
                modifier = Modifier.padding(innerPadding),
            )
            runs.isEmpty() -> BurkanMessage(
                icon = Tabler.Outline.History,
                title = stringResource(R.string.log_empty_title),
                text = stringResource(R.string.log_empty_text),
                modifier = Modifier.padding(innerPadding),
            ) {
                FilledTonalButton(onClick = onBack) { Text(stringResource(R.string.log_empty_action)) }
            }
            else -> LazyColumn(
                modifier = Modifier.fillMaxSize().padding(horizontal = ScreenMargin),
                contentPadding = PaddingValues(
                    top = innerPadding.calculateTopPadding(),
                    bottom = innerPadding.calculateBottomPadding() + GroupGap,
                ),
                verticalArrangement = Arrangement.spacedBy(SegmentGap),
            ) {
                itemsIndexed(runs, key = { _, run -> run.startedAt.toEpochMilliseconds() }) { index, run ->
                    RunItem(
                        run = run,
                        index = index,
                        count = runs.size,
                        expanded = run.startedAt in state.expanded,
                        zone = zone,
                        onClick = { onToggle(run) },
                    )
                }
            }
        }
    }
}

/**
 * One run. Its header is the result, then when and how it ran, with the result's icon level with the two lines. A
 * run that recorded steps or an error opens on a tap to show them, and says so with a chevron; a run that changed
 * nothing has nothing to open and does not react.
 */
@Composable
private fun RunItem(run: RunLogEntry, index: Int, count: Int, expanded: Boolean, zone: ZoneId, onClick: () -> Unit) {
    val hasDetail = run.steps.isNotEmpty() || run.error != null
    BurkanSegment(index = index, count = count, onClick = onClick.takeIf { hasDetail }) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 14.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            RunResultIcon(failed = run.result == RunResult.Failed)
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    stringResource(
                        R.string.log_run_title,
                        stringResource(run.kind.label),
                        stringResource(run.result.label),
                    ),
                    style = MaterialTheme.typography.titleMedium,
                )
                Text(
                    stringResource(
                        R.string.log_run_detail,
                        formatDateTime(run.startedAt, zone, LocalCurrentYear.current),
                        stringResource(run.trigger.label),
                        durationText(run.duration),
                    ),
                    style = MaterialTheme.typography.bodyMedium,
                    color = LocalContentColor.current.copy(alpha = DETAIL_ALPHA),
                )
            }
            if (hasDetail) {
                val turn by animateFloatAsState(if (expanded) HALF_TURN else 0f, label = "chevron")
                Icon(
                    Tabler.Outline.ChevronDown,
                    contentDescription = stringResource(if (expanded) R.string.log_collapse else R.string.log_expand),
                    modifier = Modifier.rotate(turn),
                )
            }
        }
        // Under the header, so opening a run moves nothing in it: the card only grows.
        AnimatedVisibility(
            visible = expanded && hasDetail,
            enter = expandVertically(spring(stiffness = Spring.StiffnessMediumLow)) + fadeIn(),
            exit = shrinkVertically(spring(stiffness = Spring.StiffnessMediumLow)) + fadeOut(),
        ) {
            RunSteps(run)
        }
    }
}

@Composable
private fun RunSteps(run: RunLogEntry) {
    // The steps are the log proper, so they are set in the fixed-width face. They line up under the header's text.
    ProvideTextStyle(MaterialTheme.typography.bodyMedium.copy(fontFamily = MonoFontFamily)) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(start = 56.dp, end = 16.dp, bottom = 16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            run.error?.let { Text(stringResource(it.resource), color = MaterialTheme.colorScheme.error) }
            run.steps.forEach { step ->
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.Top) {
                    RunResultIcon(failed = step.error != null, modifier = Modifier.size(20.dp))
                    Column {
                        Text(step.kind.label().text())
                        step.error?.let { Text(stringResource(it.resource), color = MaterialTheme.colorScheme.error) }
                    }
                }
            }
        }
    }
}

private const val DETAIL_ALPHA = 0.74f
private const val HALF_TURN = 180f

/** A run's or a step's result. The words beside it carry the result too: never colour alone. */
@Composable
fun RunResultIcon(failed: Boolean, modifier: Modifier = Modifier) {
    if (failed) {
        Icon(
            Tabler.Outline.AlertCircle,
            contentDescription = stringResource(R.string.result_failed),
            modifier = modifier,
            tint = MaterialTheme.colorScheme.error,
        )
    } else {
        Icon(Tabler.Outline.CircleCheck, contentDescription = null, modifier = modifier)
    }
}

private val sampleRuns = listOf(
    RunLogEntry(
        startedAt = Instant.parse("2026-10-01T09:30:00Z"),
        trigger = RunTrigger.Manual,
        kind = ApplyKind.Full,
        result = RunResult.Failed,
        duration = 73.seconds,
        steps = listOf(
            LoggedStep(StepKind.SetRenderer),
            LoggedStep(StepKind.StopApps(612)),
            LoggedStep(StepKind.RestartSystemUi),
            LoggedStep(StepKind.RestartLauncher),
            LoggedStep(StepKind.RelaunchApps(110), AppErrorType.CommandTimedOut),
            LoggedStep(StepKind.RestoreSetting(RestoredSetting.AutoRotation)),
            LoggedStep(StepKind.RestoreSetting(RestoredSetting.AccessibilityServices)),
        ),
    ),
    RunLogEntry(
        startedAt = Instant.parse("2026-10-01T08:02:00Z"),
        trigger = RunTrigger.Boot,
        kind = ApplyKind.Light,
        result = RunResult.Failed,
        duration = 4.seconds,
        steps = emptyList(),
        error = AppErrorType.NoWifi,
    ),
    // The run after a restart leaves System UI for the next lock, and the run at the lock continues it.
    RunLogEntry(
        startedAt = Instant.parse("2026-09-30T23:10:00Z"),
        trigger = RunTrigger.AtLock,
        kind = ApplyKind.Light,
        result = RunResult.Succeeded,
        duration = 9.seconds,
        steps = listOf(LoggedStep(StepKind.RestartSystemUi)),
    ),
    RunLogEntry(
        startedAt = Instant.parse("2026-09-30T23:05:00Z"),
        trigger = RunTrigger.Boot,
        kind = ApplyKind.Light,
        result = RunResult.Succeeded,
        duration = 9.seconds,
        steps = listOf(
            LoggedStep(StepKind.SetRenderer),
            LoggedStep(StepKind.RestartLauncher),
            LoggedStep(StepKind.RestartKeyboard),
        ),
    ),
    RunLogEntry(
        startedAt = Instant.parse("2026-09-30T22:15:00Z"),
        trigger = RunTrigger.Manual,
        kind = ApplyKind.Light,
        result = RunResult.Succeeded,
        duration = 6.seconds,
        steps = listOf(
            LoggedStep(StepKind.SetRenderer),
            LoggedStep(StepKind.RestartSystemUi),
            LoggedStep(StepKind.RestartLauncher),
            LoggedStep(StepKind.RestartKeyboard),
        ),
    ),
)

@PreviewWrapper(BurkanPreviewWrapper::class)
@BurkanPreview
@Composable
private fun LogPreview() {
    LogContent(LogState(sampleRuns), ZoneOffset.UTC, onToggle = {}, onShare = {}, onBack = {})
}

@PreviewWrapper(BurkanPreviewWrapper::class)
@BurkanPreview
@Composable
private fun LogExpandedPreview() {
    val state = LogState(sampleRuns, expanded = setOf(sampleRuns[0].startedAt, sampleRuns[1].startedAt))
    LogContent(state, ZoneOffset.UTC, onToggle = {}, onShare = {}, onBack = {})
}

@PreviewWrapper(BurkanPreviewWrapper::class)
@BurkanPreview
@Composable
private fun LogLoadingPreview() {
    LogContent(LogState(), ZoneOffset.UTC, onToggle = {}, onShare = {}, onBack = {})
}

@PreviewWrapper(BurkanPreviewWrapper::class)
@BurkanPreview
@Composable
private fun LogUnreadablePreview() {
    LogContent(LogState(emptyList(), isUnreadable = true), ZoneOffset.UTC, onToggle = {}, onShare = {}, onBack = {})
}

@PreviewWrapper(BurkanPreviewWrapper::class)
@BurkanPreview
@Composable
private fun LogEmptyPreview() {
    LogContent(LogState(emptyList()), ZoneOffset.UTC, onToggle = {}, onShare = {}, onBack = {})
}

@BurkanPreview
@Composable
private fun LogExpandedDarkPreview() {
    BurkanPreviewTheme(themeMode = ThemeMode.Dark) {
        val state = LogState(sampleRuns, expanded = setOf(sampleRuns[0].startedAt))
        LogContent(state, ZoneOffset.UTC, onToggle = {}, onShare = {}, onBack = {})
    }
}

@BurkanPreview
@Composable
private fun LogExpandedLargeTextPreview() {
    BurkanPreviewTheme(textScalePercent = TextScale.percentages.last) {
        val state = LogState(sampleRuns, expanded = setOf(sampleRuns[0].startedAt))
        LogContent(state, ZoneOffset.UTC, onToggle = {}, onShare = {}, onBack = {})
    }
}

@BurkanPreview
@Composable
private fun LogTealPreview() {
    BurkanPreviewTheme(seedColor = SeedColors.Teal) {
        LogContent(LogState(sampleRuns), ZoneOffset.UTC, onToggle = {}, onShare = {}, onBack = {})
    }
}
