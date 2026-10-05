package io.github.barqallayl.burkan.feature.status.ui

import android.content.Intent
import android.provider.Settings as SystemSettings
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.PreviewWrapper
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.Dp
import androidx.lifecycle.compose.LifecycleResumeEffect
import com.composables.icons.tabler.Tabler
import com.composables.icons.tabler.outline.AlertTriangle
import com.composables.icons.tabler.outline.Apps
import com.composables.icons.tabler.outline.Check
import com.composables.icons.tabler.outline.ExclamationMark
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
import io.github.barqallayl.burkan.designsystem.component.BurkanIconButton
import io.github.barqallayl.burkan.designsystem.component.BurkanPill
import io.github.barqallayl.burkan.designsystem.component.BurkanSectionTitle
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
import io.github.barqallayl.burkan.designsystem.component.noticeAccentColor
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
                    BurkanIconButton(
                        icon = Tabler.Outline.History,
                        label = stringResource(R.string.home_open_log),
                        onClick = actions.onOpenLog,
                    )
                    BurkanIconButton(
                        icon = Tabler.Outline.Settings,
                        label = stringResource(R.string.home_open_settings),
                        onClick = actions.onOpenSettings,
                    )
                },
            )
        },
        // The two actions stay at the bottom, in reach of the thumb, and never move whatever the state above is.
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
                // While a run is under way the quiet button is the way to stop it: the same place and size, so the
                // bar does not change shape, and not the button that was just tapped to start it.
                FilledTonalButton(
                    onClick = if (idle) actions.onRestartAll else actions.onCancelRun,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(ACTION_HEIGHT),
                    colors = ButtonDefaults.filledTonalButtonColors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainerHighest,
                        contentColor = MaterialTheme.colorScheme.onSurface,
                    ),
                ) {
                    Text(
                        stringResource(if (idle) R.string.home_restart_all else R.string.home_cancel_run),
                        style = actionStyle,
                    )
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
        ) {
            // The status card already gives this reason when it is why the status cannot be read either.
            val shownAbove = (state.status as? StatusState.Failed)?.error?.type()
            val showError = state.lastRun?.error != shownAbove
            // The status is always first and always the same size, whatever it has to say: checking, a run under
            // way, an answer, or none. Nothing above it comes and goes, so nothing on the screen jumps. What the
            // automatic apply is waiting for comes in under it, opening the room it needs.
            if (LocalAppStyle.current == AppStyle.OneUi) {
                // One UI's own arrangement: a summary card, then labelled lists, as its Settings screens have.
                OneUiSummary(state)
                Notices(state, actions.onOpenDeveloperOptions)
                SectionLabel(R.string.home_section_surfaces)
                OneUiSurfaces(state, actions.onRetry)
                SectionLabel(R.string.home_section_runs)
                Details(state, zone, actions, showError)
            } else {
                StatusGroup(state, actions.onRetry)
                Notices(state, actions.onOpenDeveloperOptions)
                Spacer(Modifier.height(GroupGap))
                Details(state, zone, actions, showError)
            }
            Spacer(Modifier.height(GroupGap))
            ActionsNote()
            Spacer(Modifier.height(GroupGap))
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
 * The status as one card that keeps its shape in every state: a badge, a headline, two lines of explanation, and
 * the four surfaces. While it is read the badge turns and the surfaces are placeholders. During a run the badge
 * turns, the headline is the run and the explanation its current step. When the phone cannot be asked, the reason
 * and the way to try again take the surfaces' place, which is room the card already has.
 */
@Composable
private fun StatusGroup(state: HomeState, onRetry: () -> Unit) {
    val status = state.status
    val failed = status as? StatusState.Failed
    // Null while the status is read, and when it could not be: the surfaces are placeholders then.
    val renderers = (status as? StatusState.Loaded)?.status
    val run = state.run as? ApplyRunState.Running
    val headline = renderers?.headline()
    val busy = run != null || (headline == null && failed == null)
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
            val tone = if (busy) Tone.Neutral else headline?.tone ?: Tone.Neutral
            if (busy) {
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
                    headline?.icon ?: Tabler.Outline.QuestionMark,
                    tone = tone,
                    size = BADGE_SIZE,
                )
            }
            Text(
                when {
                    run != null -> stringResource(run.kind.label)
                    failed != null -> stringResource(R.string.home_headline_unknown)
                    headline == null -> stringResource(R.string.home_checking)
                    else -> stringResource(headline.title)
                },
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.SemiBold,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 8.dp),
            )
            val note = when {
                run != null -> run.phase.label().text()
                failed != null -> stringResource(R.string.home_unknown_text)
                headline == null -> ""
                headline == Headline.VulkanActive && state.lastRun.isManualLightApply() ->
                    stringResource(R.string.home_open_apps_note)
                else -> stringResource(headline.text)
            }
            // Always two lines' room, used or not, so a longer or shorter sentence does not move what is under it.
            Text(
                note,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                minLines = 2,
                modifier = Modifier.padding(horizontal = 8.dp),
            )
            Box(modifier = Modifier.padding(top = 16.dp), contentAlignment = Alignment.Center) {
                // Each surface by name, two to a row: which of them the headline is about. They keep their room
                // when there is nothing to show in it, and are dimmed while a run is changing them.
                SurfaceGrid(
                    renderers = renderers,
                    modifier = Modifier
                        .alpha(if (failed != null) 0f else if (run != null) STALE_ALPHA else 1f)
                        .then(if (failed != null) Modifier.clearAndSetSemantics { } else Modifier),
                )
                if (failed != null) {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Text(
                            stringResource(failed.error.messageRes()),
                            style = MaterialTheme.typography.bodyMedium,
                            textAlign = TextAlign.Center,
                        )
                        Button(onClick = onRetry, enabled = !state.isRefreshing) {
                            Text(stringResource(R.string.home_retry))
                        }
                    }
                }
            }
        }
    }
}

/** What the surfaces were before the run that is now changing them. */
private const val STALE_ALPHA = 0.5f

/** A group's label, with the gap above it that parts the group from the one before. */
@Composable
private fun SectionLabel(text: Int) {
    BurkanSectionTitle(stringResource(text), modifier = Modifier.padding(top = GroupGap - 8.dp))
}

/**
 * The status as One UI heads a screen with it: a badge in the answer's tone, the answer beside it in that tone's
 * colour, and two lines of explanation. It keeps this shape while the status is read and during a run, when the
 * badge turns and the words are the run and its current step.
 */
@Composable
private fun OneUiSummary(state: HomeState) {
    val failed = state.status as? StatusState.Failed
    val headline = (state.status as? StatusState.Loaded)?.status?.headline()
    val run = state.run as? ApplyRunState.Running
    val busy = run != null || (headline == null && failed == null)
    val tone = if (busy) Tone.Neutral else headline?.tone ?: Tone.Neutral
    BurkanSegment(index = 0, count = 1) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 18.dp, vertical = 20.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (busy) {
                Box(
                    modifier = Modifier
                        .size(SUMMARY_BADGE_SIZE)
                        .background(tone.colors.container, CircleShape),
                    contentAlignment = Alignment.Center,
                ) {
                    LoadingIndicator(modifier = Modifier.size(30.dp), color = tone.colors.content)
                }
            } else {
                StatusMark(headline?.icon ?: Tabler.Outline.QuestionMark, tone, SUMMARY_BADGE_SIZE)
            }
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    when {
                        run != null -> stringResource(run.kind.label)
                        failed != null -> stringResource(R.string.home_headline_unknown)
                        headline == null -> stringResource(R.string.home_checking)
                        else -> stringResource(headline.title)
                    },
                    style = MaterialTheme.typography.titleLarge,
                    // The answer in its tone's colour, as One UI writes the title of a card that reports a state.
                    color = when (tone) {
                        Tone.Good -> statusGreenText
                        Tone.HeldUp -> noticeAccentColor
                        Tone.Bad -> MaterialTheme.colorScheme.error
                        Tone.Neutral -> MaterialTheme.colorScheme.onSurface
                    },
                )
                // Always two lines' room, used or not, so a longer or shorter sentence does not move what follows.
                Text(
                    when {
                        run != null -> run.phase.label().text()
                        failed != null -> stringResource(R.string.home_unknown_text)
                        headline == null -> ""
                        headline == Headline.VulkanActive && state.lastRun.isManualLightApply() ->
                            stringResource(R.string.home_open_apps_note)
                        else -> stringResource(headline.text)
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    minLines = 2,
                )
            }
        }
    }
}

private val SUMMARY_BADGE_SIZE = 52.dp

/**
 * One UI's own colours for a state: green for what is as it should be, orange for what needs attention. They are
 * fixed, whatever accent the user has chosen, so a state never reads as a mere highlight.
 */
private val StatusGreen = Color(0xFF2DB84D)
private val StatusOrange = Color(0xFFFF9A0A)

/** Green as a title's colour: deeper on a light card, so it is still read easily. */
private val statusGreenText: Color
    @Composable
    @ReadOnlyComposable
    get() = if (MaterialTheme.colorScheme.surface.luminance() < 0.5f) Color(0xFF4CD471) else Color(0xFF1B8A3A)

/**
 * A state as One UI marks it: a solid green or orange disc with a dark glyph. A state that is neither, unknown or
 * still being read, keeps the quiet grey badge. The glyph differs with the state too: never colour alone.
 */
@Composable
private fun StatusMark(icon: ImageVector, tone: Tone, size: Dp) {
    val fill = when (tone) {
        Tone.Good -> StatusGreen
        Tone.HeldUp, Tone.Bad -> StatusOrange
        Tone.Neutral -> null
    }
    if (fill == null) {
        BurkanIconBadge(icon, tone = Tone.Neutral, size = size)
        return
    }
    Box(modifier = Modifier.size(size).background(fill, CircleShape), contentAlignment = Alignment.Center) {
        Icon(icon, contentDescription = null, modifier = Modifier.size(size * MARK_GLYPH_SHARE), tint = MarkGlyph)
    }
}

private const val MARK_GLYPH_SHARE = 0.6f
private val MarkGlyph = Color(0xFF10131A)

/**
 * The four surfaces as a list, each with its renderer under its name and a mark at the end. The rows keep their
 * room in every state: placeholders while the status is read, dimmed during a run, and when the phone cannot be
 * asked, the reason and the way to try again in their place.
 */
@Composable
private fun OneUiSurfaces(state: HomeState, onRetry: () -> Unit) {
    val failed = state.status as? StatusState.Failed
    val renderers = (state.status as? StatusState.Loaded)?.status
    val running = state.run is ApplyRunState.Running
    Box(contentAlignment = Alignment.Center) {
        // When the phone cannot be asked the rows are laid out for their size alone, and one plain card of that
        // size carries the reason instead.
        SegmentedColumn(
            modifier = Modifier
                .alpha(if (failed != null) 0f else if (running) STALE_ALPHA else 1f)
                .then(if (failed != null) Modifier.clearAndSetSemantics { } else Modifier),
        ) {
            SurfaceRow(0, Tabler.Outline.Apps, R.string.home_row_new_apps, renderers?.newApps)
            SurfaceRow(1, Tabler.Outline.Stack2, R.string.home_row_system_ui, renderers?.systemUi)
            SurfaceRow(2, Tabler.Outline.Home, R.string.home_row_launcher, renderers?.launcher)
            SurfaceRow(3, Tabler.Outline.Keyboard, R.string.home_row_keyboard, renderers?.keyboard)
        }
        if (failed != null) {
            BurkanSegment(index = 0, count = 1, modifier = Modifier.matchParentSize()) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 24.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp, Alignment.CenterVertically),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text(
                        stringResource(failed.error.messageRes()),
                        style = MaterialTheme.typography.bodyMedium,
                        textAlign = TextAlign.Center,
                    )
                    Button(onClick = onRetry, enabled = !state.isRefreshing) {
                        Text(stringResource(R.string.home_retry))
                    }
                }
            }
        }
    }
}

@Composable
private fun SurfaceRow(index: Int, icon: ImageVector, label: Int, renderer: Renderer?) {
    val (mark, value, tone) = when (renderer) {
        Renderer.Vulkan -> Triple(Tabler.Outline.Check, R.string.renderer_vulkan, Tone.Good)
        // Not a fault: it is how every restart leaves things. It is what wants attention, so it is orange.
        Renderer.OpenGL -> Triple(Tabler.Outline.ExclamationMark, R.string.renderer_opengl, Tone.HeldUp)
        Renderer.Unknown -> Triple(Tabler.Outline.QuestionMark, R.string.renderer_unknown, Tone.Neutral)
        null -> Triple(Tabler.Outline.Hourglass, R.string.home_value_checking, Tone.Neutral)
    }
    BurkanSegmentItem(
        index = index,
        count = SURFACE_COUNT,
        headline = stringResource(label),
        supporting = stringResource(value),
        leading = { Icon(icon, contentDescription = null) },
        trailing = { StatusMark(mark, tone, 24.dp) },
    )
}

private const val SURFACE_COUNT = 4

@Composable
private fun SurfaceGrid(renderers: RendererStatus?, modifier: Modifier = Modifier) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            SurfaceCell(Tabler.Outline.Apps, R.string.home_row_new_apps, renderers?.newApps, Modifier.weight(1f))
            SurfaceCell(Tabler.Outline.Stack2, R.string.home_row_system_ui, renderers?.systemUi, Modifier.weight(1f))
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            SurfaceCell(Tabler.Outline.Home, R.string.home_row_launcher, renderers?.launcher, Modifier.weight(1f))
            SurfaceCell(Tabler.Outline.Keyboard, R.string.home_row_keyboard, renderers?.keyboard, Modifier.weight(1f))
        }
    }
}

/**
 * What the automatic apply is waiting for, if anything. Each notice opens and closes its own room, with the gap
 * above it, so the cards under it slide instead of jumping.
 */
@Composable
private fun ColumnScope.Notices(state: HomeState, onOpenDeveloperOptions: () -> Unit) {
    // The last reason is kept while its notice closes, so it does not empty before it has gone.
    var lastWait by remember { mutableStateOf(state.waitingFor) }
    if (state.waitingFor != null) lastWait = state.waitingFor
    SlidingNotice(visible = state.waitingFor != null) {
        lastWait?.let { WaitingNotice(it, onOpenDeveloperOptions) }
    }
    SlidingNotice(visible = state.systemUiAtNextLock) { NextLockNotice() }
}

@Composable
private fun ColumnScope.SlidingNotice(visible: Boolean, content: @Composable () -> Unit) {
    AnimatedVisibility(
        visible = visible,
        enter = expandVertically(spring(stiffness = Spring.StiffnessMediumLow)) + fadeIn(),
        exit = shrinkVertically(spring(stiffness = Spring.StiffnessMediumLow)) + fadeOut(),
    ) {
        Box(modifier = Modifier.padding(top = GroupGap)) { content() }
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

/**
 * What stands behind the status, as one group: the newest run, which opens the log, and whether Vulkan is applied
 * by itself after a restart, which opens Settings.
 */
@Composable
private fun Details(state: HomeState, zone: ZoneId, actions: HomeActions, showError: Boolean) {
    val applyOnBoot = state.applyOnBoot
    val count = if (applyOnBoot == null) 1 else 2
    // A failed run's reason adds a line to its row: the group grows to take it instead of jumping.
    SegmentedColumn(modifier = Modifier.animateContentSize()) {
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
        RestartAllSheet(scope = RestartScope.All, keptCount = 3, onScope = {}, onConfirm = {}, onDismiss = {})
    }
}

@BurkanPreview
@Composable
private fun HomeRunningOneUiPreview() {
    BurkanPreviewTheme(appStyle = AppStyle.OneUi) {
        HomePreviewContent(
            sample(
                run = ApplyRunState.Running(ApplyKind.Full, RunTrigger.Manual, RunPhase.Step(StepKind.StopApps(612))),
                lastRun = sampleLightRun,
            ),
        )
    }
}

@BurkanPreview
@Composable
private fun HomeCannotConnectOneUiDarkPreview() {
    BurkanPreviewTheme(themeMode = ThemeMode.Dark, appStyle = AppStyle.OneUi) {
        HomePreviewContent(
            sample(
                status = StatusState.Failed(ConnectionError.WirelessDebuggingRefused),
                lastRun = sampleLightRun.copy(result = RunResult.Failed, error = AppErrorType.NoWifi),
            ),
        )
    }
}
