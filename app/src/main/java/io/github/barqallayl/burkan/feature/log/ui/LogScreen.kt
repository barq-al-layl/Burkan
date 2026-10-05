package io.github.barqallayl.burkan.feature.log.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ProvideTextStyle
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.pluralStringResource
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
import io.github.barqallayl.burkan.designsystem.AppStyle
import io.github.barqallayl.burkan.designsystem.MonoFontFamily
import io.github.barqallayl.burkan.designsystem.SeedColors
import io.github.barqallayl.burkan.designsystem.preview.LargeFontScale
import io.github.barqallayl.burkan.designsystem.ThemeMode
import io.github.barqallayl.burkan.designsystem.component.scrolledPx
import io.github.barqallayl.burkan.designsystem.component.listInset
import io.github.barqallayl.burkan.designsystem.component.rememberBurkanAppBar
import io.github.barqallayl.burkan.designsystem.component.listTop
import io.github.barqallayl.burkan.designsystem.component.topBarScroll
import io.github.barqallayl.burkan.designsystem.component.oneUiScrollFade
import io.github.barqallayl.burkan.designsystem.component.BurkanTopBar
import io.github.barqallayl.burkan.designsystem.component.BurkanMessage
import io.github.barqallayl.burkan.designsystem.component.BurkanIconButton
import io.github.barqallayl.burkan.designsystem.component.BurkanSegment
import io.github.barqallayl.burkan.designsystem.component.GroupGap
import io.github.barqallayl.burkan.designsystem.component.ScreenMargin
import io.github.barqallayl.burkan.designsystem.component.SegmentGap
import io.github.barqallayl.burkan.designsystem.component.bleedsToScreenEdges
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
    var filter by rememberSaveable { mutableStateOf(LogFilter.All) }
    LogContent(
        state = state,
        zone = zone,
        filter = filter,
        onFilter = { filter = it },
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
    filter: LogFilter = LogFilter.All,
    onFilter: (LogFilter) -> Unit = {},
) {
    val appBar = rememberBurkanAppBar()
    val listState = rememberLazyListState()
    Scaffold(
        modifier = Modifier.topBarScroll(appBar),
        topBar = {
            BurkanTopBar(
                onBack = onBack,
                appBar = appBar,
                contentScroll = { listState.scrolledPx() },
                // How many runs there are, under the title: the list is long and this is its size.
                title = {
                    Column {
                        Text(stringResource(R.string.log_title))
                        val count = state.runs?.size ?: 0
                        if (count > 0) {
                            Text(
                                pluralStringResource(R.plurals.log_run_count, count, count),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                },
                actions = {
                    if (!state.runs.isNullOrEmpty()) {
                        BurkanIconButton(
                            icon = Tabler.Outline.Share,
                            label = stringResource(R.string.log_share),
                            onClick = onShare,
                        )
                    }
                },
            )
        },
    ) { innerPadding ->
        val runs = state.runs
        when {
            runs == null -> Box(
                Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentAlignment = Alignment.Center,
            ) {
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

            else -> {
                val found = remember(runs, filter) { runs.filter { filter.accepts(it.result) } }
                val barInset = innerPadding.listInset()
                LazyColumn(
                    // The margin is content padding, so the row of chips can run to the screen's edges. The bar's
                    // height is kept clear here instead, so the fade starts under it.
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(top = innerPadding.listTop())
                        .oneUiScrollFade(listState),
                    state = listState,
                    contentPadding = PaddingValues(
                        start = ScreenMargin,
                        end = ScreenMargin,
                        bottom = innerPadding.calculateBottomPadding() + GroupGap,
                    ),
                    verticalArrangement = Arrangement.spacedBy(SegmentGap),
                ) {
                    // In One UI the list runs under the bar: this keeps its start below it, and measures the scroll for it.
                    if (barInset > 0.dp) item(key = "bar") { Spacer(Modifier.height(barInset)) }
                    // Only when there is something to tell apart: a log of one kind of result needs no filter.
                    val present =
                        LogFilter.entries.filter { option -> runs.any { option.accepts(it.result) } }
                    if (present.size > 2) {
                        item(key = "filters") { Filters(runs, present, filter, onFilter) }
                    }
                    itemsIndexed(
                        found,
                        key = { _, run -> run.startedAt.toEpochMilliseconds() },
                    ) { index, run ->
                        RunItem(
                            run = run,
                            index = index,
                            count = found.size,
                            expanded = run.startedAt in state.expanded,
                            zone = zone,
                            onClick = { onToggle(run) },
                        )
                    }
                }
            }
        }
    }
}

/** Which runs the log shows, by how they ended. */
enum class LogFilter {
    All, Failed, Succeeded, Other;

    fun accepts(result: RunResult): Boolean = when (this) {
        All -> true
        Failed -> result == RunResult.Failed
        Succeeded -> result == RunResult.Succeeded || result == RunResult.AlreadyApplied
        Other -> result == RunResult.Postponed || result == RunResult.Cancelled
    }
}

private val LogFilter.label: Int
    get() = when (this) {
        LogFilter.All -> R.string.log_filter_all
        LogFilter.Failed -> R.string.result_failed
        LogFilter.Succeeded -> R.string.result_succeeded
        LogFilter.Other -> R.string.log_filter_other
    }

/** One chip for each kind of result the log holds, with how many runs ended that way. */
@Composable
private fun Filters(
    runs: List<RunLogEntry>,
    present: List<LogFilter>,
    selected: LogFilter,
    onSelect: (LogFilter) -> Unit,
) {
    Row(
        modifier = Modifier
            .bleedsToScreenEdges()
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = ScreenMargin)
            .padding(bottom = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        present.forEach { option ->
            val count = runs.count { option.accepts(it.result) }
            FilterChip(
                selected = option == selected,
                onClick = { onSelect(option) },
                label = {
                    Text(
                        stringResource(
                            R.string.filter_with_count,
                            stringResource(option.label),
                            count,
                        ),
                    )
                },
                shape = CircleShape,
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                    selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer,
                ),
            )
        }
    }
}

/**
 * One run. Its header is the result, then when and how it ran, with the result's icon level with the two lines. A
 * run that recorded steps or an error opens on a tap to show them, and says so with a chevron; a run that changed
 * nothing has nothing to open and does not react.
 */
@Composable
private fun RunItem(
    run: RunLogEntry,
    index: Int,
    count: Int,
    expanded: Boolean,
    zone: ZoneId,
    onClick: () -> Unit,
) {
    val hasDetail = run.steps.isNotEmpty() || run.error != null
    BurkanSegment(index = index, count = count, onClick = onClick.takeIf { hasDetail }) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            RunResultBadge(run.result)
            RunSummary(run, zone, modifier = Modifier.weight(1f))
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
    // The steps are the log proper: set in the fixed-width face, on a panel of the screen's own colour set into
    // the card.
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 12.dp, end = 12.dp, bottom = 12.dp),
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surface,
    ) {
        ProvideTextStyle(MaterialTheme.typography.bodyMedium.copy(fontFamily = MonoFontFamily)) {
            Column(
                modifier = Modifier.padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                run.error?.let {
                    Text(
                        stringResource(it.resource),
                        color = MaterialTheme.colorScheme.error,
                    )
                }
                run.steps.forEach { step ->
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.Top,
                    ) {
                        RunResultIcon(failed = step.error != null, modifier = Modifier.size(20.dp))
                        Column {
                            Text(step.kind.label().text())
                            step.error?.let {
                                Text(
                                    stringResource(it.resource),
                                    color = MaterialTheme.colorScheme.error,
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

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
        Icon(
            Tabler.Outline.CircleCheck,
            contentDescription = null,
            modifier = modifier,
            tint = MaterialTheme.colorScheme.primary,
        )
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
    val state =
        LogState(sampleRuns, expanded = setOf(sampleRuns[0].startedAt, sampleRuns[1].startedAt))
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
    LogContent(
        LogState(emptyList(), isUnreadable = true),
        ZoneOffset.UTC,
        onToggle = {},
        onShare = {},
        onBack = {},
    )
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
    BurkanPreviewTheme(fontScale = LargeFontScale) {
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

@BurkanPreview
@Composable
private fun LogExpandedOneUiPreview() {
    BurkanPreviewTheme(appStyle = AppStyle.OneUi) {
        val state = LogState(sampleRuns, expanded = setOf(sampleRuns[0].startedAt))
        LogContent(state, ZoneOffset.UTC, onToggle = {}, onShare = {}, onBack = {})
    }
}
