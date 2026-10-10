package io.github.barqallayl.burkan.feature.status.ui

import android.content.Intent
import android.provider.Settings as SystemSettings
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.TextAutoSize
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularWavyProgressIndicator
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
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
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewWrapper
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.LifecycleResumeEffect
import com.composables.icons.tabler.Tabler
import com.composables.icons.tabler.filled.Clock
import com.composables.icons.tabler.filled.Settings
import com.composables.icons.tabler.outline.AlertTriangle
import com.composables.icons.tabler.outline.Apps
import com.composables.icons.tabler.outline.Check
import com.composables.icons.tabler.outline.Checks
import com.composables.icons.tabler.outline.ExclamationMark
import com.composables.icons.tabler.outline.ChevronRight
import com.composables.icons.tabler.outline.CircleCheck
import com.composables.icons.tabler.outline.CircleX
import com.composables.icons.tabler.outline.HelpCircle
import com.composables.icons.tabler.outline.History
import com.composables.icons.tabler.outline.Home
import com.composables.icons.tabler.outline.Hourglass
import com.composables.icons.tabler.outline.Keyboard
import com.composables.icons.tabler.outline.ListSearch
import com.composables.icons.tabler.outline.Lock
import com.composables.icons.tabler.outline.PlugConnected
import com.composables.icons.tabler.outline.Power
import com.composables.icons.tabler.outline.QuestionMark
import com.composables.icons.tabler.outline.Refresh
import com.composables.icons.tabler.outline.Settings
import com.composables.icons.tabler.outline.ShieldCheck
import com.composables.icons.tabler.outline.Stack2
import com.composables.icons.tabler.outline.WifiOff
import com.composables.icons.tabler.outline.X
import dev.zacsweers.metrox.viewmodel.metroViewModel
import io.github.barqallayl.burkan.R
import io.github.barqallayl.burkan.core.model.AppError
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
import io.github.barqallayl.burkan.designsystem.burkanMotion
import io.github.barqallayl.burkan.designsystem.SeedColors
import io.github.barqallayl.burkan.designsystem.preview.LargeFontScale
import io.github.barqallayl.burkan.designsystem.ThemeMode
import io.github.barqallayl.burkan.designsystem.component.listInset
import io.github.barqallayl.burkan.designsystem.component.rememberBurkanAppBar
import io.github.barqallayl.burkan.designsystem.component.listTop
import io.github.barqallayl.burkan.designsystem.component.topBarScroll
import io.github.barqallayl.burkan.designsystem.component.oneUiScrollFade
import io.github.barqallayl.burkan.designsystem.component.BurkanBottomSheet
import io.github.barqallayl.burkan.designsystem.component.ActionHeight
import io.github.barqallayl.burkan.designsystem.component.BurkanButton
import io.github.barqallayl.burkan.designsystem.component.BurkanTonalButton
import io.github.barqallayl.burkan.designsystem.component.burkanButtonShapes
import io.github.barqallayl.burkan.designsystem.component.burkanTonalButtonColors
import io.github.barqallayl.burkan.designsystem.component.glass
import io.github.barqallayl.burkan.designsystem.component.glassSource
import io.github.barqallayl.burkan.designsystem.component.readableWidth
import io.github.barqallayl.burkan.designsystem.component.BurkanConfirm
import io.github.barqallayl.burkan.designsystem.component.BurkanIconBadge
import io.github.barqallayl.burkan.designsystem.component.BurkanIconButton
import io.github.barqallayl.burkan.designsystem.component.BurkanSectionTitle
import io.github.barqallayl.burkan.designsystem.component.BurkanSegment
import io.github.barqallayl.burkan.designsystem.component.BurkanSheetActions
import io.github.barqallayl.burkan.designsystem.component.BurkanSheetHeader
import io.github.barqallayl.burkan.designsystem.component.BurkanSheetPreview
import io.github.barqallayl.burkan.designsystem.component.BurkanTopBar
import io.github.barqallayl.burkan.designsystem.component.BurkanSegmentChoice
import io.github.barqallayl.burkan.designsystem.component.BurkanSegmentItem
import io.github.barqallayl.burkan.designsystem.component.ContentMargin
import io.github.barqallayl.burkan.designsystem.component.GlassRise
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
            onDismissRun = viewModel::dismissRun,
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
    val onDismissRun: () -> Unit = {},
    val onOpenLog: () -> Unit = {},
    val onOpenSettings: () -> Unit = {},
    val onOpenDeveloperOptions: () -> Unit = {},
)

@Composable
private fun HomeContent(state: HomeState, zone: ZoneId, actions: HomeActions) {
    val oneUi = LocalAppStyle.current == AppStyle.OneUi
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
                    // Material asks for filled icons in a bar where there are any; One UI's are drawn in outline.
                    BurkanIconButton(
                        icon = if (oneUi) Tabler.Outline.History else Tabler.Filled.Clock,
                        label = stringResource(R.string.home_open_log),
                        onClick = actions.onOpenLog,
                    )
                    BurkanIconButton(
                        icon = if (oneUi) Tabler.Outline.Settings else Tabler.Filled.Settings,
                        label = stringResource(R.string.home_open_settings),
                        onClick = actions.onOpenSettings,
                    )
                },
            )
        },
        // Material keeps the everyday action at the bottom, in reach of the thumb, where it never moves whatever
        // the state above is. It is the screen's one filled button; the heavier, rarer action is a row of the
        // content. One UI docks nothing: its actions are in the summary card, as Samsung's own apps have them.
        bottomBar = {
            val idle = state.run == ApplyRunState.Idle
            // Material's medium button: its height, the label that goes with it, and its shape when pressed.
            val actionStyle = ButtonDefaults.textStyleFor(ActionHeight)
            // Frosted glass behind the button, coming in gradually above it: what scrolls under the bar blurs
            // away rather than being cut off at a line.
            if (!oneUi) Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .glass(appBar, risingOver = GlassRise)
                    .navigationBarsPadding()
                    .padding(horizontal = ContentMargin)
                    .padding(top = GlassRise, bottom = GroupGap)
                    .readableWidth(),
            ) {
                // While a run is under way the button is the way to stop it, quieter and in the same place and
                // size, so the bar does not change shape.
                AnimatedContent(
                    targetState = idle,
                    transitionSpec = { fadeIn() togetherWith fadeOut() },
                    label = "home action",
                ) { shownIdle ->
                    // A button on its way out no longer does anything: a second tap meant for "Apply now" must
                    // not land on the "Cancel" replacing it.
                    val live = shownIdle == idle
                    val size = Modifier
                        .fillMaxWidth()
                        .heightIn(min = ActionHeight)
                    if (shownIdle) {
                        Button(
                            onClick = { if (live) actions.onApplyNow() },
                            shapes = burkanButtonShapes(ActionHeight),
                            modifier = size,
                        ) {
                            Text(stringResource(R.string.home_apply_now), style = actionStyle)
                        }
                    } else {
                        FilledTonalButton(
                            onClick = { if (live) actions.onCancelRun() },
                            shapes = burkanButtonShapes(ActionHeight),
                            modifier = size,
                            colors = burkanTonalButtonColors(),
                        ) {
                            Text(stringResource(R.string.home_cancel_run), style = actionStyle)
                        }
                    }
                }
            }
        },
    ) { innerPadding ->
        Column(
            // Only the top is kept clear: the content scrolls on under the bar's fade, and its own bottom padding
            // brings the last of it up above the buttons.
            modifier = Modifier
                .fillMaxSize()
                .glassSource(appBar)
                .padding(top = innerPadding.listTop())
                // Material's bar over the buttons already fades the lower edge.
                .oneUiScrollFade(scrollState, bottom = oneUi)
                .verticalScroll(scrollState)
                .padding(top = innerPadding.listInset())
                .padding(horizontal = ScreenMargin)
                .padding(bottom = innerPadding.calculateBottomPadding())
                .readableWidth(),
        ) {
            // The status card already gives this reason when it is why the status cannot be read either.
            val shownAbove = (state.status as? StatusState.Failed)?.error?.type()
            val showError = state.lastRun?.error != shownAbove
            // The status is always first and always the same size, whatever it has to say: checking, a run under
            // way, an answer, or none. Nothing above it comes and goes, so nothing on the screen jumps. What the
            // automatic apply is waiting for comes in under it, opening the room it needs.
            if (oneUi) {
                // One UI's own arrangement: a summary card that carries the action, then labelled lists. While a
                // run is under way, and until the user has seen how it went, its steps take the surfaces' place.
                OneUiSummary(state, actions)
                Notices(state, actions.onOpenDeveloperOptions)
                OneUiProgress(state, actions.onRetry)
                SectionLabel(R.string.home_section_runs)
                Details(state, zone, actions, showError)
            } else {
                // Material's arrangement: the answer, then lists. The surfaces are a list like any other, read
                // down one edge, and what can be done about them closes the screen.
                StatusGroup(state)
                Notices(state, actions.onOpenDeveloperOptions)
                Spacer(Modifier.height(GroupGap))
                Surfaces(state, actions.onRetry)
                Spacer(Modifier.height(GroupGap))
                Details(state, zone, actions, showError)
            }
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
                // System UI is restarted, and the screen locks, only when it is not on Vulkan already. Not
                // knowing, say it: a lock nobody was told about is the worse surprise.
                locks = (state.status as? StatusState.Loaded)?.status?.systemUi != Renderer.Vulkan,
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
    locks: Boolean,
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
            if (locks) {
                stringResource(R.string.home_confirm_text) + " " + stringResource(R.string.home_confirm_lock_text)
            } else {
                stringResource(R.string.home_confirm_text)
            },
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
 * The status as one card that keeps its shape in every state: a badge, a headline and two lines of explanation.
 * While it is read the badge turns. During a run the badge turns, the headline is the run and the explanation its
 * current step. What each surface runs on, and the way to try again when the phone cannot be asked, are in the
 * list under it.
 */
@Composable
private fun StatusGroup(state: HomeState) {
    val status = state.status
    val failed = status as? StatusState.Failed
    val run = state.run as? ApplyRunState.Running
    val headline = (status as? StatusState.Loaded)?.status?.headline()
    val busy = run != null || (headline == null && failed == null)
    BurkanSegment(index = 0, count = 1) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .padding(top = 28.dp, bottom = 20.dp),
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
                    // Material's loading indicator is for a wait of a few seconds, which reading the status and
                    // the light apply are. Restarting every app takes most of a minute: a progress indicator's.
                    if (run?.kind == ApplyKind.Full) {
                        CircularWavyProgressIndicator(
                            modifier = Modifier.size(BADGE_ICON_SIZE),
                            color = tone.colors.content,
                            trackColor = tone.colors.content.copy(alpha = TRACK_ALPHA),
                        )
                    } else {
                        LoadingIndicator(
                            modifier = Modifier.size(BADGE_ICON_SIZE),
                            color = tone.colors.content,
                        )
                    }
                }
            } else {
                BurkanIconBadge(
                    headline?.icon ?: failed?.error.failureIcon(),
                    tone = tone,
                    size = BADGE_SIZE,
                )
            }
            Text(
                when {
                    run != null -> stringResource(run.kind.label)
                    failed != null -> stringResource(failed.error.failureTitle())
                    headline == null -> stringResource(R.string.home_checking)
                    else -> stringResource(headline.title)
                },
                style = MaterialTheme.typography.headlineSmallEmphasized,
                textAlign = TextAlign.Center,
                // The answer changes by itself, as the phone is read and as a run goes on: it is read out when
                // it does, without taking over from whatever is being read.
                modifier = Modifier
                    .padding(top = 8.dp)
                    .semantics { liveRegion = LiveRegionMode.Polite },
            )
            val note = when {
                run != null -> run.phase.label().text()
                failed != null -> stringResource(failed.error.failureText())
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
                modifier = Modifier
                    .padding(horizontal = 8.dp)
                    .semantics { liveRegion = LiveRegionMode.Polite },
            )
        }
    }
}

/** What the surfaces were before the run that is now changing them. */
private const val STALE_ALPHA = 0.5f

/** The track of the progress indicator, as a share of the indicator's own colour. */
private const val TRACK_ALPHA = 0.16f

/** A group's label, with the gap above it that parts the group from the one before. */
@Composable
private fun SectionLabel(text: Int) {
    BurkanSectionTitle(stringResource(text), modifier = Modifier.padding(top = GroupGap - 8.dp))
}

/**
 * The status as One UI heads a screen with it: the answer in a few large words in its tone's colour, two lines of
 * explanation, and the action as a pill, all centred. It keeps this shape in every state. During a run the words
 * are the run and its current step and the pill stops it; when the run has ended the pill says so and puts the
 * screen back. A glow of the state's colour spreads from under the card.
 */
@Composable
private fun OneUiSummary(state: HomeState, actions: HomeActions) {
    val failed = state.status as? StatusState.Failed
    val headline = (state.status as? StatusState.Loaded)?.status?.headline()
    val run = state.run as? ApplyRunState.Running
    val tone = if (run != null) Tone.Neutral else headline?.tone ?: Tone.Neutral
    val accent = MaterialTheme.colorScheme.primary
    // The glow keeps its last colour while it fades out, so it dims away and does not turn grey on the way.
    val glowTarget = when {
        run != null -> accent
        tone == Tone.Good -> StatusGreen
        tone == Tone.HeldUp || tone == Tone.Bad -> StatusOrange
        else -> null
    }
    var lastGlow by remember { mutableStateOf(glowTarget ?: accent) }
    if (glowTarget != null) lastGlow = glowTarget
    val glow by animateColorAsState(lastGlow, tween(GLOW_MILLIS), label = "summary glow")
    val glowStrength by animateFloatAsState(
        if (glowTarget == null) 0f else 1f,
        tween(GLOW_MILLIS),
        label = "summary glow strength",
    )
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .drawBehind {
                if (glowStrength == 0f) return@drawBehind
                // Half an oval under the card's lower edge: the card covers the upper half.
                val center = Offset(size.width / 2, size.height)
                val radius = size.width * GLOW_REACH
                scale(scaleX = 1f, scaleY = GLOW_FLATNESS, pivot = center) {
                    drawCircle(
                        Brush.radialGradient(
                            listOf(glow.copy(alpha = GLOW_ALPHA * glowStrength), Color.Transparent),
                            center = center,
                            radius = radius,
                        ),
                        radius = radius,
                        center = center,
                    )
                }
            },
    ) {
        BurkanSegment(index = 0, count = 1) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp)
                    .padding(top = 28.dp, bottom = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                // One line always: a longer answer is set smaller, so the card never grows a line.
                Text(
                    when {
                        run != null -> stringResource(
                            when (run.kind) {
                                ApplyKind.Light -> R.string.apply_running_light_title
                                ApplyKind.Full -> R.string.apply_running_full_title
                            },
                        )
                        failed != null -> stringResource(failed.error.failureTitle())
                        headline == null -> stringResource(R.string.home_checking)
                        else -> stringResource(headline.title)
                    },
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.SemiBold,
                    textAlign = TextAlign.Center,
                    maxLines = 1,
                    modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite },
                    autoSize = TextAutoSize.StepBased(minFontSize = 18.sp, maxFontSize = 28.sp),
                    // The answer in its tone's colour, as One UI writes the title of a card that reports a state.
                    color = when {
                        run != null -> accent
                        tone == Tone.Good -> statusGreenText
                        tone == Tone.HeldUp -> noticeAccentColor
                        tone == Tone.Bad -> MaterialTheme.colorScheme.error
                        else -> MaterialTheme.colorScheme.onSurface
                    },
                )
                // Always two lines' room, used or not, so a longer or shorter sentence does not move what follows.
                Text(
                    when {
                        run != null -> run.phase.label().text()
                        failed != null -> stringResource(failed.error.failureText())
                        headline == null -> ""
                        headline == Headline.VulkanActive && state.lastRun.isManualLightApply() ->
                            stringResource(R.string.home_open_apps_note)
                        else -> stringResource(headline.text)
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    minLines = 2,
                    maxLines = 2,
                    modifier = Modifier
                        .padding(top = 6.dp)
                        .semantics { liveRegion = LiveRegionMode.Polite },
                )
                SummaryAction(
                    action = when {
                        run != null -> SummaryActions.Cancel
                        state.runSteps.isNotEmpty() -> SummaryActions.Done
                        else -> SummaryActions.Apply
                    },
                    actions = actions,
                    modifier = Modifier.padding(top = 16.dp),
                )
            }
        }
    }
}

private const val GLOW_MILLIS = 500
private const val GLOW_ALPHA = 0.3f

/** How far the glow reaches to each side, as a share of the card's width, and how flat its oval is. */
private const val GLOW_REACH = 0.5f
private const val GLOW_FLATNESS = 0.3f

/** What the summary card's one button does: it starts a run, stops it, or puts away a run that has ended. */
private enum class SummaryActions { Apply, Cancel, Done }

/**
 * The summary card's buttons. At rest they are the two things the app is for, side by side and equally wide, in
 * grey as One UI has a card's own actions. During a run there is one, which stops it; after it one, "Done", in the
 * accent: the one thing left to do. A lone button is only as wide as its word needs.
 */
@Composable
private fun SummaryAction(action: SummaryActions, actions: HomeActions, modifier: Modifier = Modifier) {
    val shape = Modifier
        .height(SUMMARY_ACTION_HEIGHT)
        .widthIn(min = SUMMARY_ACTION_MIN_WIDTH)
    val style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold)
    AnimatedContent(
        targetState = action,
        modifier = modifier,
        transitionSpec = { fadeIn() togetherWith fadeOut() using SizeTransform(clip = false) },
        contentAlignment = Alignment.Center,
        label = "summary action",
    ) { shown ->
        // A button on its way out no longer does anything: a second tap must not land on the one replacing it.
        val live = shown == action
        val grey = ButtonDefaults.filledTonalButtonColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerHighest,
            contentColor = MaterialTheme.colorScheme.onSurface,
        )
        when (shown) {
            SummaryActions.Done -> Button(onClick = { if (live) actions.onDismissRun() }, modifier = shape) {
                Text(stringResource(R.string.home_done), style = style)
            }

            SummaryActions.Cancel -> FilledTonalButton(
                onClick = { if (live) actions.onCancelRun() },
                modifier = shape,
                colors = grey,
            ) {
                Text(stringResource(R.string.home_cancel_run), style = style)
            }

            SummaryActions.Apply -> Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(
                    R.string.home_apply_now to actions.onApplyNow,
                    R.string.home_restart_all to actions.onRestartAll,
                ).forEach { (label, onClick) ->
                    FilledTonalButton(
                        onClick = { if (live) onClick() },
                        modifier = Modifier
                            .weight(1f)
                            .height(SUMMARY_ACTION_HEIGHT),
                        colors = grey,
                        contentPadding = PaddingValues(horizontal = 12.dp),
                    ) {
                        // One line in any font size: the label is set smaller before it is ever cut.
                        Text(
                            stringResource(label),
                            style = style,
                            maxLines = 1,
                            autoSize = TextAutoSize.StepBased(minFontSize = 12.sp, maxFontSize = style.fontSize),
                        )
                    }
                }
            }
        }
    }
}

private val SUMMARY_ACTION_HEIGHT = 52.dp
private val SUMMARY_ACTION_MIN_WIDTH = 160.dp

/**
 * Under the summary: the surfaces, or, from the start of a run until the user has seen how it went, the run's steps.
 * One gives way to the other, with its label, and what is below follows the change in height.
 */
@Composable
private fun OneUiProgress(state: HomeState, onRetry: () -> Unit) {
    AnimatedContent(
        targetState = state.runSteps.isNotEmpty(),
        transitionSpec = {
            fadeIn(tween(SWAP_IN_MILLIS, delayMillis = SWAP_OUT_MILLIS)) togetherWith
                fadeOut(tween(SWAP_OUT_MILLIS)) using SizeTransform(clip = false)
        },
        label = "surfaces or steps",
    ) { steps ->
        Column {
            SectionLabel(if (steps) R.string.home_section_steps else R.string.home_section_surfaces)
            if (steps) RunChecklist(state) else Surfaces(state, onRetry)
        }
    }
}

private const val SWAP_OUT_MILLIS = 90
private const val SWAP_IN_MILLIS = 220

/**
 * A run as a checklist, as One UI shows work under way: one row for each thing the run has done, in order, the last
 * of them turning until it is done too. When a run has failed, the step that failed says so: the one the log
 * records the failure against, or the last when it failed before or after its steps. A run that was cancelled is
 * ticked throughout, because what it had started it also finished or put back; the row under Runs says "Cancelled".
 */
@Composable
private fun RunChecklist(state: HomeState) {
    // The steps are kept while the list gives way to the surfaces, so it does not empty before it has gone.
    var steps by remember { mutableStateOf(state.runSteps) }
    if (state.runSteps.isNotEmpty()) steps = state.runSteps
    val running = state.run is ApplyRunState.Running
    val finished = state.finishedRun
    val failedRow = if (finished?.result == RunResult.Failed) {
        // The log's steps are the rows that are steps, in the same order.
        val failedStep = finished.steps.indexOfFirst { it.error != null }
        val stepRows = steps.indices.filter { steps[it] is RunPhase.Step }
        stepRows.getOrNull(failedStep) ?: steps.lastIndex
    } else {
        -1
    }
    BurkanSegment(index = 0, count = 1) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp)
                .animateContentSize(spring(stiffness = Spring.StiffnessMediumLow)),
        ) {
            steps.forEachIndexed { index, phase ->
                key(index) {
                    ChecklistRow(
                        phase = phase,
                        mark = when {
                            index == failedRow -> StepMarks.Failed
                            index != steps.lastIndex -> StepMarks.Done
                            // Also for the moment between the run ending and its result arriving: no tick
                            // before it is known that the step earned one.
                            running || finished == null -> StepMarks.Working
                            else -> StepMarks.Done
                        },
                    )
                }
            }
        }
    }
}

private enum class StepMarks(val label: Int) {
    Working(R.string.home_step_working),
    Done(R.string.home_step_done),
    Failed(R.string.home_step_failed),
}

@Composable
private fun ChecklistRow(phase: RunPhase, mark: StepMarks) {
    val markLabel = stringResource(mark.label)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = CHECKLIST_ROW_HEIGHT)
            .padding(horizontal = 18.dp, vertical = 6.dp)
            .semantics(mergeDescendants = true) { },
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            phase.icon,
            contentDescription = null,
            modifier = Modifier.size(20.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        // The step by its short name: the longer sentence about the one under way is in the summary card.
        Text(
            if (phase is RunPhase.Step) phase.kind.label().text() else phase.label().text(),
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.weight(1f),
        )
        // The spinner gives way to the mark in place. A glyph for each: never colour alone.
        AnimatedContent(
            targetState = mark,
            modifier = Modifier
                .size(24.dp)
                .semantics { contentDescription = markLabel },
            transitionSpec = { fadeIn() togetherWith fadeOut() },
            contentAlignment = Alignment.Center,
            label = "step mark",
        ) { shown ->
            when (shown) {
                StepMarks.Working -> LoadingIndicator(modifier = Modifier.size(24.dp))
                StepMarks.Done -> Icon(Tabler.Outline.Check, contentDescription = null, tint = statusGreenText)
                StepMarks.Failed -> Icon(
                    Tabler.Outline.AlertTriangle,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.error,
                )
            }
        }
    }
}

private val CHECKLIST_ROW_HEIGHT = 56.dp

private val RunPhase.icon: ImageVector
    get() = when (this) {
        RunPhase.Connecting -> Tabler.Outline.PlugConnected
        RunPhase.Reading -> Tabler.Outline.ListSearch
        RunPhase.Checking -> Tabler.Outline.Checks
        is RunPhase.Step -> when (kind) {
            StepKind.RestartSystemUi -> Tabler.Outline.Stack2
            StepKind.RestartLauncher -> Tabler.Outline.Home
            StepKind.RestartKeyboard -> Tabler.Outline.Keyboard
            is StepKind.RestoreSetting -> Tabler.Outline.Settings
            StepKind.SetRenderer, is StepKind.StopApps, is StepKind.RelaunchApps -> Tabler.Outline.Apps
        }
    }

/**
 * Why the status could not be read, as the card's headline: the cause by name where it is one the user can see
 * and put right, and "cannot connect" only when it is not.
 */
private fun AppError.failureTitle(): Int = when (type()) {
    AppErrorType.NoWifi -> R.string.home_headline_no_wifi
    AppErrorType.WirelessDebuggingOff, AppErrorType.WirelessDebuggingRefused -> R.string.home_headline_debugging_off
    AppErrorType.NotAuthorised -> R.string.home_headline_not_paired
    else -> R.string.home_headline_unknown
}

private fun AppError.failureText(): Int = when (type()) {
    AppErrorType.NoWifi -> R.string.home_no_wifi_text
    else -> R.string.home_unknown_text
}

private fun AppError?.failureIcon(): ImageVector =
    if (this?.type() == AppErrorType.NoWifi) Tabler.Outline.WifiOff else Tabler.Outline.QuestionMark

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
private fun Surfaces(state: HomeState, onRetry: () -> Unit) {
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

/**
 * A surface and what it runs on. One UI writes the renderer under the surface's name, with its solid mark at the
 * end. Material keeps the row to one line: the renderer is the row's trailing text, then a mark so that the three
 * states differ at a glance. [renderer] null is a placeholder while the status is read.
 */
@Composable
private fun SurfaceRow(index: Int, icon: ImageVector, label: Int, renderer: Renderer?) {
    val value = when (renderer) {
        Renderer.Vulkan -> R.string.renderer_vulkan
        Renderer.OpenGL -> R.string.renderer_opengl
        Renderer.Unknown -> R.string.renderer_unknown
        null -> R.string.home_value_checking
    }
    if (LocalAppStyle.current == AppStyle.OneUi) {
        val (mark, tone) = when (renderer) {
            Renderer.Vulkan -> Tabler.Outline.Check to Tone.Good
            // Not a fault: it is how every restart leaves things. It is what wants attention, so it is orange.
            Renderer.OpenGL -> Tabler.Outline.ExclamationMark to Tone.HeldUp
            Renderer.Unknown -> Tabler.Outline.QuestionMark to Tone.Neutral
            null -> Tabler.Outline.Hourglass to Tone.Neutral
        }
        BurkanSegmentItem(
            index = index,
            count = SURFACE_COUNT,
            headline = stringResource(label),
            supporting = stringResource(value),
            leading = { Icon(icon, contentDescription = null) },
            trailing = { StatusMark(mark, tone, 24.dp) },
        )
        return
    }
    val scheme = MaterialTheme.colorScheme
    val (mark, markColor) = when (renderer) {
        Renderer.Vulkan -> Tabler.Outline.CircleCheck to scheme.primary
        Renderer.OpenGL -> Tabler.Outline.CircleX to scheme.error
        Renderer.Unknown -> Tabler.Outline.HelpCircle to scheme.onSurfaceVariant
        null -> Tabler.Outline.Hourglass to scheme.onSurfaceVariant
    }
    BurkanSegmentItem(
        index = index,
        count = SURFACE_COUNT,
        headline = stringResource(label),
        leading = { Icon(icon, contentDescription = null) },
        trailing = {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                TrailingText(stringResource(value))
                Icon(mark, contentDescription = null, modifier = Modifier.size(20.dp), tint = markColor)
            }
        },
    )
}

/** What a row says at its end, in Material: a value that only informs. */
@Composable
private fun TrailingText(text: String) {
    Text(text, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
}

private const val SURFACE_COUNT = 4

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
    val motion = burkanMotion
    AnimatedVisibility(
        visible = visible,
        enter = expandVertically(motion.settle()) + fadeIn(),
        exit = shrinkVertically(motion.settle()) + fadeOut(),
    ) {
        Box(modifier = Modifier.padding(top = GroupGap)) { content() }
    }
}

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
                // A notice comes and goes by itself, so it is read out when it comes.
                modifier = Modifier
                    .weight(1f)
                    .semantics { liveRegion = LiveRegionMode.Polite },
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
            BurkanTonalButton(
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
    val motion = burkanMotion
    val applyOnBoot = state.applyOnBoot
    val oneUi = LocalAppStyle.current == AppStyle.OneUi
    // In Material the heavier of the app's two actions is the last row here; One UI has it in its summary card.
    val restartRow = !oneUi
    val count = 1 + (if (applyOnBoot == null) 0 else 1) + (if (restartRow) 1 else 0)
    // A failed run's reason adds a line to its row: the group grows to take it instead of jumping.
    SegmentedColumn(modifier = Modifier.animateContentSize(motion.settle())) {
        LastRun(state.lastRun, zone, count, actions.onOpenLog, showError)
        if (applyOnBoot != null && oneUi) {
            // One UI writes a setting's value under its name, in the accent.
            BurkanSegmentItem(
                index = 1,
                count = count,
                headline = stringResource(R.string.settings_apply_on_boot),
                supporting = stringResource(if (applyOnBoot) R.string.value_on else R.string.value_off),
                supportingColor = MaterialTheme.colorScheme.primary,
                onClick = actions.onOpenSettings,
                leading = { BurkanIconBadge(Tabler.Outline.Power) },
            )
        } else if (applyOnBoot != null) {
            BurkanSegmentItem(
                index = 1,
                count = count,
                headline = stringResource(R.string.home_after_restart),
                supporting = stringResource(
                    if (applyOnBoot) R.string.home_after_restart_on_text else R.string.home_after_restart_off_text,
                ),
                onClick = actions.onOpenSettings,
                leading = { BurkanIconBadge(Tabler.Outline.Power) },
                // A state, not a control: Material writes it as the row's trailing text.
                trailing = {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        TrailingText(stringResource(if (applyOnBoot) R.string.value_on else R.string.value_off))
                        Icon(Tabler.Outline.ChevronRight, contentDescription = null)
                    }
                },
            )
        }
        if (restartRow) {
            val idle = state.run == ApplyRunState.Idle
            BurkanSegmentItem(
                index = count - 1,
                count = count,
                headline = stringResource(R.string.home_restart_all),
                supporting = stringResource(R.string.home_restart_all_text),
                // Nothing to start while a run is under way: the row stays, dimmed, and does not react.
                modifier = Modifier.alpha(if (idle) 1f else STALE_ALPHA),
                onClick = actions.onRestartAll.takeIf { idle },
                leading = { BurkanIconBadge(Tabler.Outline.Refresh) },
                trailing = { Icon(Tabler.Outline.ChevronRight, contentDescription = null) },
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
    runSteps: List<RunPhase> = emptyList(),
    finishedRun: RunLogEntry? = null,
) = HomeState(
    status = status,
    lastRun = lastRun,
    run = run,
    runSteps = runSteps,
    finishedRun = finishedRun,
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
    BurkanSheetPreview {
        RestartAllSheet(
            scope = RestartScope.All,
            keptCount = 0,
            locks = true,
            onScope = {},
            onConfirm = {},
            onDismiss = {},
        )
    }

@PreviewWrapper(BurkanPreviewWrapper::class)
@BurkanPreview
@Composable
private fun HomeRestartAllSheetLimitedPreview() =
    BurkanSheetPreview {
        RestartAllSheet(
            scope = RestartScope.Recent30,
            keptCount = 3,
            locks = false,
            onScope = {},
            onConfirm = {},
            onDismiss = {},
        )
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
private fun HomeNoWifiPreview() = HomePreviewContent(sample(status = StatusState.Failed(ConnectionError.NoWifi)))

@BurkanPreview
@Composable
private fun HomeNoWifiOneUiPreview() {
    BurkanPreviewTheme(appStyle = AppStyle.OneUi) {
        HomePreviewContent(sample(status = StatusState.Failed(ConnectionError.NoWifi)))
    }
}

private val sampleSteps = listOf(
    RunPhase.Connecting,
    RunPhase.Reading,
    RunPhase.Step(StepKind.SetRenderer),
    RunPhase.Step(StepKind.RestartLauncher),
)

@BurkanPreview
@Composable
private fun HomeActiveOneUiDarkPreview() {
    BurkanPreviewTheme(themeMode = ThemeMode.Dark, appStyle = AppStyle.OneUi) {
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
private fun HomeRunningDarkPreview() {
    BurkanPreviewTheme(themeMode = ThemeMode.Dark) {
        HomePreviewContent(
            sample(
                run = ApplyRunState.Running(ApplyKind.Light, RunTrigger.Manual, sampleSteps.last()),
                runSteps = sampleSteps,
            ),
        )
    }
}

@BurkanPreview
@Composable
private fun HomeRunningOneUiDarkPreview() {
    BurkanPreviewTheme(themeMode = ThemeMode.Dark, appStyle = AppStyle.OneUi) {
        HomePreviewContent(
            sample(
                run = ApplyRunState.Running(ApplyKind.Light, RunTrigger.Manual, sampleSteps.last()),
                runSteps = sampleSteps,
            ),
        )
    }
}

@BurkanPreview
@Composable
private fun HomeRunFinishedOneUiDarkPreview() {
    BurkanPreviewTheme(themeMode = ThemeMode.Dark, appStyle = AppStyle.OneUi) {
        HomePreviewContent(
            sample(
                status = StatusState.Loaded(
                    RendererStatus(Renderer.Vulkan, Renderer.Vulkan, Renderer.Vulkan, Renderer.Vulkan),
                ),
                lastRun = sampleLightRun,
                runSteps = sampleSteps + RunPhase.Step(StepKind.RestartKeyboard) + RunPhase.Checking,
                finishedRun = sampleLightRun,
            ),
        )
    }
}

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
    BurkanPreviewTheme(fontScale = LargeFontScale) {
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
        BurkanSheetPreview {
            RestartAllSheet(
                scope = RestartScope.All,
                keptCount = 3,
                locks = true,
                onScope = {},
                onConfirm = {},
                onDismiss = {},
            )
        }
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

/** A phone held sideways: the content keeps to a width that reads, and the bars still span the screen. */
@Preview(showBackground = true, widthDp = 900, heightDp = 420)
@Composable
private fun HomeActiveSidewaysPreview() {
    BurkanPreviewTheme {
        HomePreviewContent(
            sample(
                status = StatusState.Loaded(
                    RendererStatus(Renderer.Vulkan, Renderer.Vulkan, Renderer.Vulkan, Renderer.Vulkan),
                ),
                lastRun = sampleLightRun,
            ),
        )
    }
}
