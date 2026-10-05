package io.github.barqallayl.burkan.feature.status.ui

import android.content.Intent
import android.provider.Settings as SystemSettings
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.PreviewWrapper
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.LifecycleResumeEffect
import com.composables.icons.tabler.Tabler
import com.composables.icons.tabler.outline.AlertTriangle
import com.composables.icons.tabler.outline.Apps
import com.composables.icons.tabler.outline.Check
import com.composables.icons.tabler.outline.ChevronRight
import com.composables.icons.tabler.outline.CircleCheck
import com.composables.icons.tabler.outline.CircleX
import com.composables.icons.tabler.outline.HelpCircle
import com.composables.icons.tabler.outline.History
import com.composables.icons.tabler.outline.Home
import com.composables.icons.tabler.outline.Hourglass
import com.composables.icons.tabler.outline.InfoCircle
import com.composables.icons.tabler.outline.Keyboard
import com.composables.icons.tabler.outline.Lock
import com.composables.icons.tabler.outline.Power
import com.composables.icons.tabler.outline.QuestionMark
import com.composables.icons.tabler.outline.Refresh
import com.composables.icons.tabler.outline.Settings
import com.composables.icons.tabler.outline.ShieldCheck
import com.composables.icons.tabler.outline.Stack2
import com.composables.icons.tabler.outline.X
import dev.zacsweers.metrox.viewmodel.metroViewModel
import io.github.barqallayl.burkan.R
import io.github.barqallayl.burkan.core.model.AppErrorType
import io.github.barqallayl.burkan.core.model.Renderer
import io.github.barqallayl.burkan.core.model.RendererStatus
import io.github.barqallayl.burkan.core.model.messageRes
import io.github.barqallayl.burkan.core.model.type
import io.github.barqallayl.burkan.core.navigation.LocalNavigator
import io.github.barqallayl.burkan.core.navigation.LogRoute
import io.github.barqallayl.burkan.core.navigation.SettingsRoute
import io.github.barqallayl.burkan.core.ui.openSettings
import io.github.barqallayl.burkan.designsystem.AppStyle
import io.github.barqallayl.burkan.designsystem.LocalAppStyle
import io.github.barqallayl.burkan.designsystem.SeedColors
import io.github.barqallayl.burkan.designsystem.TextScale
import io.github.barqallayl.burkan.designsystem.ThemeMode
import io.github.barqallayl.burkan.designsystem.component.listInset
import io.github.barqallayl.burkan.designsystem.component.rememberBurkanAppBar
import io.github.barqallayl.burkan.designsystem.component.listTop
import io.github.barqallayl.burkan.designsystem.component.topBarScroll
import io.github.barqallayl.burkan.designsystem.component.oneUiScrollFade
import io.github.barqallayl.burkan.designsystem.component.BurkanBottomSheet
import io.github.barqallayl.burkan.designsystem.component.BurkanConfirm
import io.github.barqallayl.burkan.designsystem.component.BurkanIconBadge
import io.github.barqallayl.burkan.designsystem.component.BurkanPill
import io.github.barqallayl.burkan.designsystem.component.BurkanSegment
import io.github.barqallayl.burkan.designsystem.component.BurkanSheetActions
import io.github.barqallayl.burkan.designsystem.component.BurkanSheetHeader
import io.github.barqallayl.burkan.designsystem.component.BurkanTopBar
import io.github.barqallayl.burkan.designsystem.component.BurkanSegmentChoice
import io.github.barqallayl.burkan.designsystem.component.BurkanSegmentItem
import io.github.barqallayl.burkan.designsystem.component.ContentMargin
import io.github.barqallayl.burkan.designsystem.component.GroupGap
import io.github.barqallayl.burkan.designsystem.component.ScreenMargin
import io.github.barqallayl.burkan.designsystem.component.SegmentedColumn
import io.github.barqallayl.burkan.designsystem.component.Tone
import io.github.barqallayl.burkan.designsystem.component.colors
import io.github.barqallayl.burkan.designsystem.component.noticeColors
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
import io.github.barqallayl.burkan.feature.log.ui.RunResultBadge
import io.github.barqallayl.burkan.feature.log.ui.RunSummary
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
    // In One UI the name starts large, in the upper part of the screen, and shrinks into the bar on scrolling.
    val appBar = rememberBurkanAppBar()
    val scrollState = rememberScrollState()
    Scaffold(
        modifier = Modifier.topBarScroll(appBar),
        topBar = {
            BurkanTopBar(
                appBar = appBar,
                contentScroll = { scrollState.value },
                title = { Text(stringResource(R.string.app_name)) },
                actions = {
                    IconButton(onClick = actions.onOpenLog) {
                        Icon(
                            Tabler.Outline.History,
                            contentDescription = stringResource(R.string.home_open_log),
                        )
                    }
                    IconButton(onClick = actions.onOpenSettings) {
                        Icon(
                            Tabler.Outline.Settings,
                            contentDescription = stringResource(R.string.home_open_settings),
                        )
                    }
                },
            )
        },
        // The two actions stay at the bottom, in reach of the thumb, and never move as cards come and go above.
        bottomBar = {
            val idle = state.run == ApplyRunState.Idle
            // One UI sets a contained button's label heavier than a row's name.
            val actionStyle = if (LocalAppStyle.current == AppStyle.OneUi) {
                MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold)
            } else {
                MaterialTheme.typography.titleMedium
            }
            // Solid behind the buttons, fading out above them: what scrolls under the bar dims away rather than
            // being cut off at a line.
            val surface = MaterialTheme.colorScheme.surface
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .drawBehind {
                        val fade = BAR_FADE.toPx()
                        drawRect(
                            Brush.verticalGradient(
                                listOf(Color.Transparent, surface),
                                startY = 0f,
                                endY = fade,
                            ),
                            size = size.copy(height = fade),
                        )
                        drawRect(
                            surface,
                            topLeft = Offset(0f, fade),
                            size = size.copy(height = size.height - fade),
                        )
                    }
                    .navigationBarsPadding()
                    .padding(horizontal = ContentMargin)
                    .padding(top = BAR_FADE, bottom = GroupGap),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                // The two things the app is for. The everyday one is filled; the heavier, rarer one is quiet under it.
                Button(
                    onClick = actions.onApplyNow,
                    enabled = idle,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(ACTION_HEIGHT),
                ) {
                    Text(stringResource(R.string.home_apply_now), style = actionStyle)
                }
                FilledTonalButton(
                    onClick = actions.onRestartAll,
                    enabled = idle,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(ACTION_HEIGHT),
                    colors = ButtonDefaults.filledTonalButtonColors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainerHighest,
                        contentColor = MaterialTheme.colorScheme.onSurface,
                    ),
                ) {
                    Text(stringResource(R.string.home_restart_all), style = actionStyle)
                }
            }
        },
    ) { innerPadding ->
        Column(
            // Only the top is kept clear: the content scrolls on under the bar's fade, and its own bottom padding
            // brings the last of it up above the buttons.
            modifier = Modifier
                .fillMaxSize()
                .padding(top = innerPadding.listTop())
                // The bar over the buttons already fades the lower edge.
                .oneUiScrollFade(scrollState, bottom = false)
                .verticalScroll(scrollState)
                .padding(top = innerPadding.listInset())

                .padding(horizontal = ScreenMargin)
                .padding(bottom = innerPadding.calculateBottomPadding()),
            verticalArrangement = Arrangement.spacedBy(GroupGap),
        ) {
            state.waitingFor?.let { WaitingNotice(it, actions.onOpenDeveloperOptions) }
            if (state.systemUiAtNextLock) NextLockNotice()
            val run = state.run
            // The run first: it is what is happening, and the status below says it is about to change.
            if (run is ApplyRunState.Running) RunningSegment(run, actions.onCancelRun)
            StatusGroup(state, actions.onRetry)
            // The status card already gives this reason when it is why the status cannot be read either.
            val shownAbove = (state.status as? StatusState.Failed)?.error?.type()
            Details(state, zone, actions, showError = state.lastRun?.error != shownAbove)
            ActionsNote()
        }
    }
    if (state.isConfirmingApply) {
        BurkanConfirm(
            title = stringResource(R.string.home_confirm_apply_title),
            text = stringResource(R.string.home_confirm_apply_text),
            confirm = stringResource(R.string.home_confirm_apply_action),
            dismiss = stringResource(R.string.home_confirm_dismiss),
            onConfirm = actions.onConfirmApply,
            onDismiss = actions.onDismissApply,
        )
    }
    if (state.isConfirmingFullApply) {
        BurkanBottomSheet(onDismiss = actions.onDismissRestartAll) { hide ->
            RestartAllSheet(
                scope = state.restartScope,
                keptCount = state.keptCount,
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
    keptCount: Int,
    onScope: (RestartScope) -> Unit,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = ScreenMargin),
        verticalArrangement = Arrangement.spacedBy(GroupGap),
    ) {
        BurkanSheetHeader(
            stringResource(R.string.home_confirm_title),
            modifier = Modifier.padding(horizontal = 8.dp),
            icon = Tabler.Outline.Refresh,
        )
        Text(
            stringResource(R.string.home_confirm_text),
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.padding(horizontal = 8.dp),
        )
        // What the run leaves alone, said before it starts.
        if (keptCount > 0) {
            NoticeCard(Tabler.Outline.ShieldCheck, centred = true) {
                Text(
                    pluralStringResource(R.plurals.home_confirm_kept, keptCount, keptCount),
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
        }
        Column {
            SegmentedColumn(modifier = Modifier.selectableGroup()) {
                RestartScope.entries.forEachIndexed { index, option ->
                    BurkanSegmentChoice(
                        index = index,
                        count = RestartScope.entries.size,
                        text = option.label(),
                        supporting = stringResource(
                            if (option.limit == null) R.string.restart_scope_all_text else R.string.restart_scope_recent_text,
                        ),
                        selected = option == scope,
                        onClick = { onScope(option) },
                    )
                }
            }
            Text(
                stringResource(R.string.home_confirm_scope_text),
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier
                    .padding(horizontal = 16.dp)
                    .padding(top = 8.dp),
            )
        }
        BurkanSheetActions(
            modifier = Modifier.padding(bottom = 8.dp),
            dismiss = stringResource(R.string.home_confirm_dismiss),
            onDismiss = onDismiss,
            confirm = stringResource(R.string.home_confirm_action),
            onConfirm = onConfirm,
            confirmIcon = Tabler.Outline.Refresh,
        )
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
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                HeadlineRow(Tabler.Outline.HelpCircle, R.string.home_headline_unknown)
                Text(stringResource(status.error.messageRes()))
                Button(onClick = onRetry, enabled = !state.isRefreshing) {
                    Text(stringResource(R.string.home_retry))
                }
            }
        }
        return
    }
    // Null while the status is read: the card keeps its shape with placeholders, so nothing moves when it arrives.
    val renderers = (status as? StatusState.Loaded)?.status
    val running = state.run is ApplyRunState.Running
    val headline = renderers?.headline()
    BurkanSegment(index = 0, count = 1) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .padding(top = 28.dp, bottom = 16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            // The answer first, large enough to read at a glance: a badge in the answer's tone, then the words.
            val tone = if (running) Tone.Neutral else headline?.tone ?: Tone.Neutral
            if (headline == null) {
                Box(
                    modifier = Modifier
                        .size(BADGE_SIZE)
                        .background(tone.colors.container, CircleShape),
                    contentAlignment = Alignment.Center,
                ) {
                    LoadingIndicator(
                        modifier = Modifier.size(BADGE_ICON_SIZE),
                        color = tone.colors.content,
                    )
                }
            } else {
                BurkanIconBadge(
                    if (running) Tabler.Outline.Hourglass else headline.icon,
                    tone = tone,
                    size = BADGE_SIZE,
                )
            }
            Text(
                stringResource(
                    when {
                        headline == null -> R.string.home_checking
                        running -> R.string.home_headline_changing
                        else -> headline.title
                    },
                ),
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.SemiBold,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 8.dp),
            )
            val note = when {
                headline == null -> null
                // What is shown was read before the run, which is changing it now.
                running -> R.string.home_changing_note
                headline == Headline.VulkanActive && state.lastRun.isManualLightApply() -> R.string.home_open_apps_note
                else -> headline.text
            }
            if (note != null) {
                Text(
                    stringResource(note),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(horizontal = 8.dp),
                )
            }
            // Each surface by name, two to a row: which of them the headline is about.
            Column(
                modifier = Modifier.padding(top = 16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    SurfaceCell(
                        Tabler.Outline.Apps,
                        R.string.home_row_new_apps,
                        renderers?.newApps,
                        Modifier.weight(1f),
                    )
                    SurfaceCell(
                        Tabler.Outline.Stack2,
                        R.string.home_row_system_ui,
                        renderers?.systemUi,
                        Modifier.weight(1f),
                    )
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    SurfaceCell(
                        Tabler.Outline.Home,
                        R.string.home_row_launcher,
                        renderers?.launcher,
                        Modifier.weight(1f),
                    )
                    SurfaceCell(
                        Tabler.Outline.Keyboard,
                        R.string.home_row_keyboard,
                        renderers?.keyboard,
                        Modifier.weight(1f),
                    )
                }
            }
        }
    }
}

private val ACTION_HEIGHT = 56.dp

/** How far above the buttons the bar fades from nothing to the screen's colour. */
private val BAR_FADE = 24.dp
private val BADGE_SIZE = 72.dp
private val BADGE_ICON_SIZE = 40.dp

/** After a manual light apply, apps that were already open are still on OpenGL; the status must not hide that. */
private fun RunLogEntry?.isManualLightApply(): Boolean =
    this != null && kind == ApplyKind.Light && trigger == RunTrigger.Manual && result == RunResult.Succeeded

// An icon and words for each: never colour alone.
private val Headline.icon: ImageVector
    get() = when (this) {
        Headline.VulkanActive -> Tabler.Outline.Check
        Headline.NotApplied -> Tabler.Outline.X
        Headline.PartlyApplied -> Tabler.Outline.AlertTriangle
        Headline.Unknown -> Tabler.Outline.QuestionMark
    }

private val Headline.tone: Tone
    get() = when (this) {
        Headline.VulkanActive -> Tone.Good
        Headline.PartlyApplied -> Tone.HeldUp
        Headline.NotApplied, Headline.Unknown -> Tone.Neutral
    }

private val Headline.title: Int
    get() = when (this) {
        Headline.VulkanActive -> R.string.home_headline_active
        Headline.NotApplied -> R.string.home_headline_not_applied
        Headline.PartlyApplied -> R.string.home_headline_partly
        Headline.Unknown -> R.string.home_headline_unknown
    }

private val Headline.text: Int
    get() = when (this) {
        Headline.VulkanActive -> R.string.home_active_text
        Headline.NotApplied -> R.string.home_not_applied_text
        Headline.PartlyApplied -> R.string.home_partly_text
        Headline.Unknown -> R.string.home_unknown_text
    }

@Composable
private fun HeadlineRow(icon: ImageVector, text: Int) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(icon, contentDescription = null)
        Text(stringResource(text), style = MaterialTheme.typography.titleLarge)
    }
}

/**
 * A surface and its renderer, in a cell of the status card: the surface's own icon, its name over the renderer, and
 * a mark at the end so the three states differ at a glance. [renderer] null is a placeholder while the status is
 * read.
 */
@Composable
private fun SurfaceCell(
    icon: ImageVector,
    label: Int,
    renderer: Renderer?,
    modifier: Modifier = Modifier,
) {
    val scheme = MaterialTheme.colorScheme
    val (mark, value, markColor) = when (renderer) {
        Renderer.Vulkan -> Triple(
            Tabler.Outline.CircleCheck,
            R.string.renderer_vulkan,
            scheme.primary,
        )

        Renderer.OpenGL -> Triple(Tabler.Outline.CircleX, R.string.renderer_opengl, scheme.error)
        Renderer.Unknown -> Triple(
            Tabler.Outline.HelpCircle,
            R.string.renderer_unknown,
            scheme.onSurfaceVariant,
        )

        null -> Triple(
            Tabler.Outline.Hourglass,
            R.string.home_value_checking,
            scheme.onSurfaceVariant,
        )
    }
    // The screen's own colour, so the cells read as set into the card.
    Surface(
        modifier = modifier,
        shape = MaterialTheme.shapes.medium,
        color = scheme.surface,
        contentColor = scheme.onSurface,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                icon,
                contentDescription = null,
                modifier = Modifier.size(20.dp),
                tint = scheme.onSurfaceVariant,
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    stringResource(label),
                    style = MaterialTheme.typography.labelMedium,
                    color = scheme.onSurfaceVariant,
                )
                Text(stringResource(value), style = MaterialTheme.typography.titleSmall)
            }
            Icon(mark, contentDescription = null, modifier = Modifier.size(18.dp), tint = markColor)
        }
    }
}

/**
 * A card in the "held up" treatment: an icon in a badge of its own tint, then whatever it has to say. It stands
 * apart from the status in the notice's amber.
 */
@Composable
private fun NoticeCard(
    icon: ImageVector,
    centred: Boolean = false,
    content: @Composable ColumnScope.() -> Unit,
) {
    val colors = noticeColors
    BurkanSegment(
        index = 0,
        count = 1,
        containerColor = colors.container,
        contentColor = colors.content,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            verticalAlignment = if (centred) Alignment.CenterVertically else Alignment.Top,
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .background(colors.content.copy(alpha = BADGE_TINT), CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Icon(icon, contentDescription = null, modifier = Modifier.size(22.dp))
            }
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp),
                content = content,
            )
        }
    }
}

private const val BADGE_TINT = 0.14f

/** Something the automatic apply is waiting for. */
@Composable
private fun Notice(
    icon: ImageVector,
    title: Int,
    text: Int,
    action: (@Composable () -> Unit)? = null,
) {
    NoticeCard(icon) {
        Text(stringResource(title), style = MaterialTheme.typography.titleMedium)
        Text(
            stringResource(text),
            style = MaterialTheme.typography.bodyMedium,
            color = LocalContentColor.current.copy(alpha = NOTICE_TEXT_ALPHA),
        )
        if (action != null) Box(Modifier.padding(top = 8.dp)) { action() }
    }
}

private const val NOTICE_TEXT_ALPHA = 0.8f

@Composable
private fun WaitingNotice(reason: WaitReason, onOpenDeveloperOptions: () -> Unit) {
    when (reason) {
        WaitReason.Wifi ->
            Notice(
                Tabler.Outline.Hourglass,
                R.string.home_waiting_wifi_title,
                R.string.home_waiting_wifi_text,
            )

        WaitReason.TrustedNetwork -> Notice(
            Tabler.Outline.Hourglass,
            R.string.home_waiting_network_title,
            R.string.home_waiting_network_text,
        ) {
            // A pill in the notice's own tint: the scheme's button colours are not made to sit on amber.
            val colors = noticeColors
            FilledTonalButton(
                onClick = onOpenDeveloperOptions,
                colors = ButtonDefaults.filledTonalButtonColors(
                    containerColor = colors.content.copy(alpha = BADGE_TINT),
                    contentColor = colors.content,
                ),
            ) {
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
        // One UI does not put a flat button on a screen that has contained ones.
        trailing = {
            if (LocalAppStyle.current == AppStyle.OneUi) {
                FilledTonalButton(onClick = onCancel) { Text(stringResource(R.string.home_cancel_run)) }
            } else {
                TextButton(onClick = onCancel) { Text(stringResource(R.string.home_cancel_run)) }
            }
        },
    )
}

/**
 * What stands behind the status, as one group: the newest run, which opens the log, and whether Vulkan is applied
 * by itself after a restart, which opens Settings.
 */
@Composable
private fun Details(state: HomeState, zone: ZoneId, actions: HomeActions, showError: Boolean) {
    val applyOnBoot = state.applyOnBoot
    val count = if (applyOnBoot == null) 1 else 2
    SegmentedColumn {
        LastRun(state.lastRun, zone, count, actions.onOpenLog, showError)
        if (applyOnBoot != null) {
            BurkanSegmentItem(
                index = 1,
                count = count,
                headline = stringResource(R.string.home_after_restart),
                supporting = stringResource(
                    if (applyOnBoot) R.string.home_after_restart_on_text else R.string.home_after_restart_off_text,
                ),
                onClick = actions.onOpenSettings,
                leading = { BurkanIconBadge(Tabler.Outline.Power) },
                trailing = {
                    BurkanPill(
                        stringResource(if (applyOnBoot) R.string.value_on else R.string.value_off),
                        tone = if (applyOnBoot) Tone.Good else Tone.Neutral,
                    )
                },
            )
        }
    }
}

/** The newest run, as the log shows it. It opens the log. */
@Composable
private fun LastRun(
    run: RunLogEntry?,
    zone: ZoneId,
    count: Int,
    onOpenLog: () -> Unit,
    showError: Boolean,
) {
    if (run == null) {
        BurkanSegmentItem(
            index = 0,
            count = count,
            headline = stringResource(R.string.home_last_run),
            supporting = stringResource(R.string.home_no_runs),
            leading = { BurkanIconBadge(Tabler.Outline.History) },
        )
        return
    }
    BurkanSegment(index = 0, count = count, onClick = onOpenLog) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            RunResultBadge(run.result)
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                RunSummary(run, zone)
                // Only a run that failed has more to say.
                run.error?.takeIf { showError }?.let { error ->
                    Text(
                        stringResource(error.resource),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.error,
                    )
                }
            }
            Icon(
                Tabler.Outline.ChevronRight,
                contentDescription = stringResource(R.string.home_open_log),
            )
        }
    }
}

/** What the two buttons do, in a line each: it is the one thing on this screen that is not a state. */
@Composable
private fun ActionsNote() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            Tabler.Outline.InfoCircle,
            contentDescription = null,
            modifier = Modifier.size(20.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            stringResource(R.string.home_actions_note),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
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
    applyOnBoot = true,
)

@Composable
private fun HomePreviewContent(state: HomeState) {
    HomeContent(state = state, zone = ZoneOffset.UTC, actions = HomeActions())
}

@PreviewWrapper(BurkanPreviewWrapper::class)
@BurkanPreview
@Composable
private fun HomeRestartAllSheetPreview() =
    RestartAllSheet(
        scope = RestartScope.All,
        keptCount = 0,
        onScope = {},
        onConfirm = {},
        onDismiss = {},
    )

@PreviewWrapper(BurkanPreviewWrapper::class)
@BurkanPreview
@Composable
private fun HomeRestartAllSheetLimitedPreview() =
    RestartAllSheet(
        scope = RestartScope.Recent30,
        keptCount = 3,
        onScope = {},
        onConfirm = {},
        onDismiss = {},
    )

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
        run = ApplyRunState.Running(
            ApplyKind.Full,
            RunTrigger.Manual,
            RunPhase.Step(StepKind.StopApps(612)),
        ),
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
        run = ApplyRunState.Running(
            ApplyKind.Light,
            RunTrigger.Manual,
            RunPhase.Step(StepKind.RestartSystemUi),
        ),
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
                    RendererStatus(
                        Renderer.Vulkan,
                        Renderer.Vulkan,
                        Renderer.Vulkan,
                        Renderer.Vulkan,
                    ),
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
                run = ApplyRunState.Running(
                    ApplyKind.Light,
                    RunTrigger.Boot,
                    RunPhase.Step(StepKind.RestartSystemUi),
                ),
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
                    RendererStatus(
                        Renderer.Vulkan,
                        Renderer.Vulkan,
                        Renderer.OpenGL,
                        Renderer.Unknown,
                    ),
                ),
                lastRun = sampleLightRun,
            ),
        )
    }
}

@BurkanPreview
@Composable
private fun HomeActiveOneUiPreview() {
    BurkanPreviewTheme(appStyle = AppStyle.OneUi) {
        HomePreviewContent(
            sample(
                status = StatusState.Loaded(
                    RendererStatus(
                        Renderer.Vulkan,
                        Renderer.Vulkan,
                        Renderer.Vulkan,
                        Renderer.Vulkan,
                    ),
                ),
                lastRun = sampleLightRun,
            ),
        )
    }
}

@BurkanPreview
@Composable
private fun HomePartlyAppliedOneUiDarkPreview() {
    BurkanPreviewTheme(themeMode = ThemeMode.Dark, appStyle = AppStyle.OneUi) {
        HomePreviewContent(
            sample(
                status = StatusState.Loaded(
                    RendererStatus(
                        Renderer.Vulkan,
                        Renderer.OpenGL,
                        Renderer.Vulkan,
                        Renderer.Vulkan,
                    ),
                ),
                lastRun = sampleLightRun.copy(trigger = RunTrigger.Boot),
            ).copy(systemUiAtNextLock = true),
        )
    }
}

@BurkanPreview
@Composable
private fun HomeRestartAllSheetOneUiPreview() {
    BurkanPreviewTheme(appStyle = AppStyle.OneUi) {
        RestartAllSheet(
            scope = RestartScope.All,
            keptCount = 3,
            onScope = {},
            onConfirm = {},
            onDismiss = {},
        )
    }
}
