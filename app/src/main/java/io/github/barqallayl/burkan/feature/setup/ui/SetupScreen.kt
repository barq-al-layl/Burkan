package io.github.barqallayl.burkan.feature.setup.ui

import android.Manifest
import android.annotation.SuppressLint
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.FiniteAnimationSpec
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ProvideTextStyle
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.contentColorFor
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.PreviewWrapper
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import androidx.lifecycle.compose.LifecycleResumeEffect
import com.composables.icons.tabler.Tabler
import com.composables.icons.tabler.outline.AlertCircle
import com.composables.icons.tabler.outline.Check
import com.composables.icons.tabler.outline.InfoCircle
import com.composables.icons.tabler.outline.WifiOff
import dev.zacsweers.metrox.viewmodel.metroViewModel
import io.github.barqallayl.burkan.R
import io.github.barqallayl.burkan.core.model.AppError
import io.github.barqallayl.burkan.core.model.messageRes
import io.github.barqallayl.burkan.core.ui.openSettings
import io.github.barqallayl.burkan.designsystem.AppStyle
import io.github.barqallayl.burkan.designsystem.LocalAppStyle
import io.github.barqallayl.burkan.designsystem.SeedColors
import io.github.barqallayl.burkan.designsystem.preview.LargeFontScale
import io.github.barqallayl.burkan.designsystem.ThemeMode
import io.github.barqallayl.burkan.designsystem.component.listInset
import io.github.barqallayl.burkan.designsystem.component.rememberBurkanAppBar
import io.github.barqallayl.burkan.designsystem.component.listTop
import io.github.barqallayl.burkan.designsystem.component.topBarScroll
import io.github.barqallayl.burkan.designsystem.component.oneUiScrollFade
import io.github.barqallayl.burkan.designsystem.component.BurkanTopBar
import io.github.barqallayl.burkan.designsystem.component.BurkanIconBadge
import io.github.barqallayl.burkan.designsystem.component.BurkanPill
import io.github.barqallayl.burkan.designsystem.component.BurkanSegment
import io.github.barqallayl.burkan.designsystem.component.BurkanSegmentItem
import io.github.barqallayl.burkan.designsystem.component.GroupGap
import io.github.barqallayl.burkan.designsystem.component.noticeColors
import io.github.barqallayl.burkan.designsystem.component.ScreenMargin
import io.github.barqallayl.burkan.designsystem.component.SegmentGap
import io.github.barqallayl.burkan.designsystem.component.Tone
import io.github.barqallayl.burkan.designsystem.component.segmentContainerColor
import io.github.barqallayl.burkan.designsystem.component.segmentedShape
import io.github.barqallayl.burkan.designsystem.preview.BurkanPreview
import io.github.barqallayl.burkan.designsystem.preview.BurkanPreviewTheme
import io.github.barqallayl.burkan.designsystem.preview.BurkanPreviewWrapper
import io.github.barqallayl.burkan.feature.connection.data.PairingStatus
import io.github.barqallayl.burkan.feature.connection.model.ConnectionError
import io.github.barqallayl.burkan.feature.connection.model.PairingError
import io.github.barqallayl.burkan.feature.setup.model.SetupError
import io.github.barqallayl.burkan.feature.setup.model.SetupStep
import org.orbitmvi.orbit.compose.collectAsState
import org.orbitmvi.orbit.compose.collectSideEffect

@Composable
fun SetupScreen() {
    val viewModel = metroViewModel<SetupViewModel>()
    val state by viewModel.collectAsState()
    val context = LocalContext.current
    val notificationPermission =
        rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {
            viewModel.refresh()
        }
    // Most steps are done in Settings, so every return to the app re-checks them.
    LifecycleResumeEffect(viewModel) {
        viewModel.refresh()
        onPauseOrDispose { }
    }
    viewModel.collectSideEffect { effect ->
        when (effect) {
            SetupSideEffect.RequestNotificationPermission ->
                notificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)

            SetupSideEffect.OpenAboutPhone -> context.openSettings(Intent(Settings.ACTION_DEVICE_INFO_SETTINGS))
            SetupSideEffect.OpenDeveloperOptions ->
                context.openSettings(Intent(Settings.ACTION_APPLICATION_DEVELOPMENT_SETTINGS))
            // A dialog over this screen rather than a screen of Settings, so it stays in the app's own window.
            SetupSideEffect.RequestBatteryExemption -> try {
                context.startActivity(batteryExemptionIntent(context))
            } catch (_: ActivityNotFoundException) {
                context.openSettings(Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS))
            }
        }
    }
    SetupContent(
        state = state,
        onAction = viewModel::onAction,
        onDone = viewModel::finishStep,
        onSkipBattery = viewModel::skipBattery,
    )
}

/** The exemption request is the point of the step: the run after boot must not be held back. */
@SuppressLint("BatteryLife")
private fun batteryExemptionIntent(context: Context): Intent =
    Intent(
        Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS,
        "package:${context.packageName}".toUri(),
    )

@Composable
private fun SetupContent(
    state: SetupState,
    onAction: (SetupStep) -> Unit,
    onDone: () -> Unit,
    onSkipBattery: () -> Unit,
) {
    val appBar = rememberBurkanAppBar()
    val scrollState = rememberScrollState()
    Scaffold(
        modifier = Modifier.topBarScroll(appBar),
        topBar = {
            BurkanTopBar(
                appBar = appBar,
                contentScroll = { scrollState.value },
                title = { Text(stringResource(R.string.setup_title)) },
                actions = {
                    // The count rolls up to the next number as a step is finished.
                    AnimatedContent(
                        targetState = state.current,
                        transitionSpec = {
                            (slideInVertically(move()) { it / 2 } + fadeIn(fadeInSpec)) togetherWith
                                (slideOutVertically(move()) { -it / 2 } + fadeOut(fadeOutSpec))
                        },
                        label = "setup progress",
                    ) { current ->
                        if (current != null) {
                            BurkanPill(
                                stringResource(
                                    R.string.setup_progress,
                                    current.ordinal + 1,
                                    SetupStep.entries.size,
                                ),
                                modifier = Modifier.padding(end = ScreenMargin),
                                tone = Tone.Good,
                            )
                        }
                    }
                },
            )
        },
    ) { innerPadding ->
        val done = state.done
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(
                    top = innerPadding.listTop(),
                    bottom = innerPadding.calculateBottomPadding(),
                )
                .oneUiScrollFade(scrollState)
                .verticalScroll(scrollState)
                .padding(top = innerPadding.listInset())

                .padding(horizontal = ScreenMargin)
                .padding(bottom = GroupGap),
            verticalArrangement = Arrangement.spacedBy(GroupGap),
        ) {
            if (state.isUntestedModel) UntestedModelNotice()
            if (done == null) {
                Box(
                    Modifier
                        .fillMaxWidth()
                        .padding(vertical = 96.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    LoadingIndicator()
                }
            } else {
                // The steps are one group, the checklist the user works down. What is done shrinks to one quiet row,
                // so the step to do now is at the top with the ones still to come under it.
                val finished = SetupStep.entries.filter { it in done }
                val firstStepRow = if (finished.isEmpty()) 0 else 1
                val rows = SetupStep.entries.size - finished.size + firstStepRow
                // The row of finished steps keeps its last words while it closes.
                var shownFinished by remember { mutableStateOf(finished) }
                if (finished.isNotEmpty()) shownFinished = finished
                Column(verticalArrangement = Arrangement.spacedBy(GroupGap)) {
                    ProgressBar(done)
                    // Every step has its place here, shown or not, so one that is finished closes where it stood
                    // while the row of finished steps opens above it and the next step opens under it. The group
                    // is cut to its own outline, so its corners stay round whichever row is at its edge mid-change.
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(segmentedShape(0, 1)),
                    ) {
                        SlidingRow(visible = finished.isNotEmpty(), gap = 0.dp) { DoneItem(shownFinished, rows) }
                        SetupStep.entries.forEach { step ->
                            key(step) {
                                val visible = step !in done
                                val index = firstStepRow + SetupStep.entries.count { it !in done && it < step }
                                // A step that has just been finished closes as it last was, with its tick and its
                                // button, not as an emptied row. Its buttons no longer do anything: a second tap
                                // on Done must not land on the step that follows.
                                var shown by remember { mutableStateOf(state) }
                                if (visible) shown = state
                                SlidingRow(visible = visible, gap = if (index == 0) 0.dp else SegmentGap) {
                                    StepItem(
                                        index = index,
                                        count = rows,
                                        step = step,
                                        status = if (step == shown.current) StepStatus.Current else StepStatus.Waiting,
                                        state = shown,
                                        onAction = { if (visible) onAction(step) },
                                        onDone = { if (visible) onDone() },
                                        onSkipBattery = { if (visible) onSkipBattery() },
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * A row of the checklist that opens and closes its own room, with the gap above it, so the others slide. Its top
 * edge stays where it is and its lower edge moves. What is in it fades along with the room, not ahead of it: a row
 * that emptied first would leave a hole in the list while it closed.
 */
@Composable
private fun ColumnScope.SlidingRow(visible: Boolean, gap: Dp, content: @Composable () -> Unit) {
    AnimatedVisibility(
        visible = visible,
        enter = expandVertically(move(), expandFrom = Alignment.Top) + fadeIn(move()),
        exit = shrinkVertically(move(), shrinkTowards = Alignment.Top) + fadeOut(move()),
    ) {
        Box(modifier = Modifier.padding(top = gap)) { content() }
    }
}

/**
 * One pace and one curve for everything on this screen that moves or changes colour, so the parts of a change
 * start and arrive together: quick off the mark and long in settling, without a bounce.
 */
private fun <T> move(): FiniteAnimationSpec<T> = tween(MOVE_MILLIS, easing = MoveEasing)

private const val MOVE_MILLIS = 420
private val MoveEasing = CubicBezierEasing(0.22f, 0.25f, 0f, 1f)

/** What leaves fades at once; what arrives waits for it to have gone. */
private val fadeOutSpec = tween<Float>(FADE_OUT_MILLIS)
private val fadeInSpec = tween<Float>(FADE_IN_MILLIS, delayMillis = FADE_OUT_MILLIS)

private const val FADE_OUT_MILLIS = 110
private const val FADE_IN_MILLIS = 260

/**
 * One bar for each step, filled for the ones that are done: how far along, at a glance. The pill says it in words.
 * A bar fills as its step is finished.
 */
@Composable
private fun ProgressBar(done: Set<SetupStep>) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clearAndSetSemantics { },
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        SetupStep.entries.forEach { step ->
            val color by animateColorAsState(
                if (step in done) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceContainerHighest,
                animationSpec = move(),
                label = "progress bar",
            )
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(4.dp)
                    .background(color, CircleShape),
            )
        }
    }
}

private enum class StepStatus { Current, Waiting }

/** Every finished step in one row: how many, and which. */
@Composable
private fun DoneItem(finished: List<SetupStep>, count: Int) {
    val names = finished.map { stringResource(it.title) }
    BurkanSegmentItem(
        index = 0,
        count = count,
        headline = pluralStringResource(R.plurals.setup_steps_done, finished.size, finished.size),
        supporting = names.joinToString(separator = stringResource(R.string.list_separator)),
        leading = {
            BurkanIconBadge(
                Tabler.Outline.Check,
                tone = Tone.Good,
                contentDescription = stringResource(R.string.setup_step_done),
            )
        },
    )
}

@Composable
private fun UntestedModelNotice() {
    val colors = noticeColors
    BurkanSegment(
        index = 0,
        count = 1,
        containerColor = colors.container,
        contentColor = colors.content,
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(Tabler.Outline.InfoCircle, contentDescription = null)
            Text(stringResource(R.string.setup_untested_model))
        }
    }
}

/**
 * A step of the checklist: its number, its name beside it, and, for the current one, its instructions and action
 * under the name. The current step stands out in the scheme's primary colour. A step that becomes current takes
 * that colour and opens its instructions under a name that stays where it was.
 */
@Composable
private fun StepItem(
    index: Int,
    count: Int,
    step: SetupStep,
    status: StepStatus,
    state: SetupState,
    onAction: () -> Unit,
    onDone: () -> Unit,
    onSkipBattery: () -> Unit,
) {
    val isCurrent = status == StepStatus.Current
    val targetContainer = if (isCurrent) MaterialTheme.colorScheme.primaryContainer else segmentContainerColor
    val container by animateColorAsState(targetContainer, move(), label = "step container")
    val content by animateColorAsState(contentColorFor(targetContainer), move(), label = "step content")
    BurkanSegment(index = index, count = count, containerColor = container, contentColor = content) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            StepNumber(step.ordinal + 1, isCurrent)
            Column(modifier = Modifier.weight(1f)) {
                // The name is level with the number, whether or not anything is open under it.
                Box(modifier = Modifier.heightIn(min = STEP_NUMBER_SIZE), contentAlignment = Alignment.CenterStart) {
                    Text(stringResource(step.title), style = MaterialTheme.typography.titleMedium)
                }
                AnimatedVisibility(
                    visible = isCurrent,
                    enter = expandVertically(move(), expandFrom = Alignment.Top) + fadeIn(fadeInSpec),
                    exit = shrinkVertically(move(), shrinkTowards = Alignment.Top) + fadeOut(fadeOutSpec),
                ) {
                    ProvideTextStyle(MaterialTheme.typography.bodyMedium) {
                        Box(modifier = Modifier.padding(bottom = 6.dp)) {
                            CurrentStepDetail(step, state, onAction, onDone, onSkipBattery)
                        }
                    }
                }
            }
            // Said on the step itself while it waits; once it is the current one, its own button offers the skip.
            if (step == SetupStep.Battery) {
                AnimatedVisibility(visible = !isCurrent, enter = fadeIn(fadeInSpec), exit = fadeOut(fadeOutSpec)) {
                    Box(modifier = Modifier.heightIn(min = STEP_NUMBER_SIZE), contentAlignment = Alignment.Center) {
                        BurkanPill(stringResource(R.string.setup_optional))
                    }
                }
            }
        }
    }
}

private val STEP_NUMBER_SIZE = 40.dp

/**
 * A step's place in the list, in a round badge: filled for the one to do now, quiet for the ones still to come.
 * Each has a spoken label as well: never colour alone.
 */
@Composable
private fun StepNumber(number: Int, isCurrent: Boolean) {
    val scheme = MaterialTheme.colorScheme
    val label =
        stringResource(if (isCurrent) R.string.setup_step_current else R.string.setup_step_waiting)
    val fill by animateColorAsState(
        if (isCurrent) scheme.primary else scheme.surfaceContainerHighest,
        move(),
        label = "step number",
    )
    val ink by animateColorAsState(
        if (isCurrent) scheme.onPrimary else scheme.onSurfaceVariant,
        move(),
        label = "step number text",
    )
    Box(
        modifier = Modifier
            .size(STEP_NUMBER_SIZE)
            .background(fill, CircleShape)
            .clearAndSetSemantics { contentDescription = label },
        contentAlignment = Alignment.Center,
    ) {
        Text(
            number.toString(),
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold,
            color = ink,
        )
    }
}

@Composable
private fun CurrentStepDetail(
    step: SetupStep,
    state: SetupState,
    onAction: () -> Unit,
    onDone: () -> Unit,
    onSkipBattery: () -> Unit,
) {
    Column(
        modifier = Modifier.padding(top = 4.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Column {
            Text(stringResource(step.text))
            // Said before the user goes to Settings and finds the switch greyed out.
            AnimatedVisibility(
                visible = state.isWifiMissing,
                enter = expandVertically(move(), expandFrom = Alignment.Top) + fadeIn(fadeInSpec),
                exit = shrinkVertically(move(), shrinkTowards = Alignment.Top) + fadeOut(fadeOutSpec),
            ) {
                Row(modifier = Modifier.padding(top = 12.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(Tabler.Outline.WifiOff, contentDescription = null)
                    Text(stringResource(R.string.setup_no_wifi), fontWeight = FontWeight.SemiBold)
                }
            }
        }
        // What the step shows under its instructions changes as it goes: its action, then work under way or a
        // failure, then what the check found. Each gives way to the next, and the row follows its height.
        AnimatedContent(
            targetState = state.detail(step),
            transitionSpec = {
                fadeIn(fadeInSpec) togetherWith fadeOut(fadeOutSpec) using SizeTransform(clip = false) { _, _ -> move() }
            },
            label = "step detail",
        ) { detail ->
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                when (detail) {
                    StepDetail.Met -> Met(step, onDone)
                    is StepDetail.Pairing -> PairDetail(detail.status, onAction)
                    is StepDetail.Failed -> FailureWithRetry(detail.error, onAction)
                    StepDetail.Working -> Progress(
                        stringResource(
                            if (step == SetupStep.Connect) {
                                R.string.setup_connect_in_progress
                            } else {
                                R.string.setup_permission_in_progress
                            },
                        ),
                    )

                    StepDetail.Action -> if (step == SetupStep.Battery) {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Button(onClick = onAction) { Text(stringResource(R.string.setup_battery_action)) }
                            // One UI does not put a flat button beside a contained one.
                            if (LocalAppStyle.current == AppStyle.OneUi) {
                                FilledTonalButton(onClick = onSkipBattery) {
                                    Text(stringResource(R.string.setup_battery_skip))
                                }
                            } else {
                                TextButton(onClick = onSkipBattery) { Text(stringResource(R.string.setup_battery_skip)) }
                            }
                        }
                    } else {
                        Button(onClick = onAction) { Text(stringResource(step.action)) }
                    }
                }
            }
        }
    }
}

/** What the current step shows under its instructions. It carries what it shows, so it can still draw as it leaves. */
private sealed interface StepDetail {
    /** The step's check passes. It is still the user who says it is done and moves on. */
    data object Met : StepDetail
    data object Action : StepDetail
    data class Pairing(val status: PairingStatus) : StepDetail
    data object Working : StepDetail
    data class Failed(val error: AppError) : StepDetail
}

private fun SetupState.detail(step: SetupStep): StepDetail = when {
    isCurrentMet -> StepDetail.Met
    step == SetupStep.Pair -> StepDetail.Pairing(pairing)
    step == SetupStep.Connect || step == SetupStep.Permission -> failure?.let(StepDetail::Failed) ?: StepDetail.Working
    else -> StepDetail.Action
}

/** What the check found, in words and with a tick, and the button that finishes the step. */
@Composable
private fun Met(step: SetupStep, onDone: () -> Unit) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
        Icon(Tabler.Outline.Check, contentDescription = null)
        Text(stringResource(step.met), fontWeight = FontWeight.SemiBold)
    }
    Button(onClick = onDone) { Text(stringResource(R.string.setup_done_action)) }
}

@Composable
private fun PairDetail(pairing: PairingStatus, onAction: () -> Unit) {
    when (pairing) {
        PairingStatus.Pairing, PairingStatus.Paired -> Progress(stringResource(R.string.setup_pair_in_progress))
        is PairingStatus.Failed -> FailureWithRetry(pairing.error, onAction)
        PairingStatus.WaitingForCode -> {
            Text(stringResource(R.string.setup_pair_waiting))
            Button(onClick = onAction) { Text(stringResource(R.string.setup_wireless_debugging_action)) }
        }

        PairingStatus.Idle -> Button(onClick = onAction) { Text(stringResource(R.string.setup_pair_action)) }
    }
}

@Composable
private fun Progress(label: String) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        LoadingIndicator(modifier = Modifier.size(40.dp))
        Text(label)
    }
}

@Composable
private fun FailureWithRetry(error: AppError, onRetry: () -> Unit) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Icon(
            Tabler.Outline.AlertCircle,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.error,
        )
        Text(stringResource(error.messageRes()), color = MaterialTheme.colorScheme.error)
    }
    Button(onClick = onRetry) { Text(stringResource(R.string.setup_try_again)) }
}

private val SetupStep.title: Int
    get() = when (this) {
        SetupStep.Notifications -> R.string.setup_notifications_title
        SetupStep.DeveloperOptions -> R.string.setup_developer_options_title
        SetupStep.WirelessDebugging -> R.string.setup_wireless_debugging_title
        SetupStep.Pair -> R.string.setup_pair_title
        SetupStep.Connect -> R.string.setup_connect_title
        SetupStep.Permission -> R.string.setup_permission_title
        SetupStep.Battery -> R.string.setup_battery_title
    }

private val SetupStep.text: Int
    get() = when (this) {
        SetupStep.Notifications -> R.string.setup_notifications_text
        SetupStep.DeveloperOptions -> R.string.setup_developer_options_text
        SetupStep.WirelessDebugging -> R.string.setup_wireless_debugging_text
        SetupStep.Pair -> R.string.setup_pair_text
        SetupStep.Connect -> R.string.setup_connect_text
        SetupStep.Permission -> R.string.setup_permission_text
        SetupStep.Battery -> R.string.setup_battery_text
    }

/** What a step's check found when it passes. */
private val SetupStep.met: Int
    get() = when (this) {
        SetupStep.Notifications -> R.string.setup_notifications_met
        SetupStep.DeveloperOptions -> R.string.setup_developer_options_met
        SetupStep.WirelessDebugging -> R.string.setup_wireless_debugging_met
        SetupStep.Pair -> R.string.setup_pair_met
        SetupStep.Connect -> R.string.setup_connect_met
        SetupStep.Permission -> R.string.setup_permission_met
        SetupStep.Battery -> R.string.setup_battery_met
    }

/** The single action of a step that has one button; Pair, Connect, Permission and Battery build their own. */
private val SetupStep.action: Int
    get() = when (this) {
        SetupStep.Notifications -> R.string.setup_notifications_action
        SetupStep.DeveloperOptions -> R.string.setup_developer_options_action
        else -> R.string.setup_wireless_debugging_action
    }

private fun sample(
    current: SetupStep,
    pairing: PairingStatus = PairingStatus.Idle,
    isConnecting: Boolean = false,
    failure: AppError? = null,
    isUntestedModel: Boolean = false,
    isMet: Boolean = false,
    isWifiMissing: Boolean = false,
) = SetupState(
    done = SetupStep.entries.takeWhile { it != current }.toSet(),
    isCurrentMet = isMet,
    isWifiMissing = isWifiMissing,
    pairing = pairing,
    isConnecting = isConnecting,
    failure = failure,
    isUntestedModel = isUntestedModel,
)

@Composable
private fun SetupPreviewContent(state: SetupState) {
    SetupContent(state = state, onAction = {}, onDone = {}, onSkipBattery = {})
}

@PreviewWrapper(BurkanPreviewWrapper::class)
@BurkanPreview
@Composable
private fun SetupLoadingPreview() = SetupPreviewContent(SetupState())

@PreviewWrapper(BurkanPreviewWrapper::class)
@BurkanPreview
@Composable
private fun SetupNotificationsPreview() = SetupPreviewContent(sample(SetupStep.Notifications))

@PreviewWrapper(BurkanPreviewWrapper::class)
@BurkanPreview
@Composable
private fun SetupNotificationsMetPreview() = SetupPreviewContent(sample(SetupStep.Notifications, isMet = true))

@PreviewWrapper(BurkanPreviewWrapper::class)
@BurkanPreview
@Composable
private fun SetupConnectMetPreview() = SetupPreviewContent(sample(SetupStep.Connect, isMet = true))

@PreviewWrapper(BurkanPreviewWrapper::class)
@BurkanPreview
@Composable
private fun SetupWirelessDebuggingNoWifiPreview() =
    SetupPreviewContent(sample(SetupStep.WirelessDebugging, isWifiMissing = true))

@PreviewWrapper(BurkanPreviewWrapper::class)
@BurkanPreview
@Composable
private fun SetupUntestedModelPreview() =
    SetupPreviewContent(sample(SetupStep.Notifications, isUntestedModel = true))

@PreviewWrapper(BurkanPreviewWrapper::class)
@BurkanPreview
@Composable
private fun SetupDeveloperOptionsPreview() = SetupPreviewContent(sample(SetupStep.DeveloperOptions))

@PreviewWrapper(BurkanPreviewWrapper::class)
@BurkanPreview
@Composable
private fun SetupWirelessDebuggingPreview() =
    SetupPreviewContent(sample(SetupStep.WirelessDebugging))

@PreviewWrapper(BurkanPreviewWrapper::class)
@BurkanPreview
@Composable
private fun SetupPairPreview() = SetupPreviewContent(sample(SetupStep.Pair))

@PreviewWrapper(BurkanPreviewWrapper::class)
@BurkanPreview
@Composable
private fun SetupPairWaitingPreview() =
    SetupPreviewContent(sample(SetupStep.Pair, pairing = PairingStatus.WaitingForCode))

@PreviewWrapper(BurkanPreviewWrapper::class)
@BurkanPreview
@Composable
private fun SetupPairInProgressPreview() =
    SetupPreviewContent(sample(SetupStep.Pair, pairing = PairingStatus.Pairing))

@PreviewWrapper(BurkanPreviewWrapper::class)
@BurkanPreview
@Composable
private fun SetupPairWrongCodePreview() =
    SetupPreviewContent(
        sample(
            SetupStep.Pair,
            pairing = PairingStatus.Failed(PairingError.WrongCode),
        ),
    )

@PreviewWrapper(BurkanPreviewWrapper::class)
@BurkanPreview
@Composable
private fun SetupPairDialogClosedPreview() =
    SetupPreviewContent(
        sample(
            SetupStep.Pair,
            pairing = PairingStatus.Failed(PairingError.DialogClosed),
        ),
    )

@PreviewWrapper(BurkanPreviewWrapper::class)
@BurkanPreview
@Composable
private fun SetupPairTimedOutPreview() =
    SetupPreviewContent(
        sample(
            SetupStep.Pair,
            pairing = PairingStatus.Failed(PairingError.TimedOut),
        ),
    )

@PreviewWrapper(BurkanPreviewWrapper::class)
@BurkanPreview
@Composable
private fun SetupConnectingPreview() =
    SetupPreviewContent(sample(SetupStep.Connect, isConnecting = true))

@PreviewWrapper(BurkanPreviewWrapper::class)
@BurkanPreview
@Composable
private fun SetupConnectNoWifiPreview() =
    SetupPreviewContent(sample(SetupStep.Connect, failure = ConnectionError.NoWifi))

@PreviewWrapper(BurkanPreviewWrapper::class)
@BurkanPreview
@Composable
private fun SetupConnectRefusedPreview() =
    SetupPreviewContent(
        sample(
            SetupStep.Connect,
            failure = ConnectionError.WirelessDebuggingRefused,
        ),
    )

@PreviewWrapper(BurkanPreviewWrapper::class)
@BurkanPreview
@Composable
private fun SetupPermissionFailedPreview() =
    SetupPreviewContent(sample(SetupStep.Permission, failure = SetupError.GrantFailed))

@PreviewWrapper(BurkanPreviewWrapper::class)
@BurkanPreview
@Composable
private fun SetupBatteryPreview() = SetupPreviewContent(sample(SetupStep.Battery))

@BurkanPreview
@Composable
private fun SetupPairDarkPreview() {
    BurkanPreviewTheme(themeMode = ThemeMode.Dark) { SetupPreviewContent(sample(SetupStep.Pair)) }
}

@BurkanPreview
@Composable
private fun SetupPairOneUiDarkPreview() {
    BurkanPreviewTheme(themeMode = ThemeMode.Dark, appStyle = AppStyle.OneUi) {
        SetupPreviewContent(sample(SetupStep.Pair))
    }
}

@BurkanPreview
@Composable
private fun SetupPairWrongCodeDarkPreview() {
    BurkanPreviewTheme(themeMode = ThemeMode.Dark) {
        SetupPreviewContent(
            sample(
                SetupStep.Pair,
                pairing = PairingStatus.Failed(PairingError.WrongCode),
            ),
        )
    }
}

@BurkanPreview
@Composable
private fun SetupBatteryLargeTextPreview() {
    BurkanPreviewTheme(fontScale = LargeFontScale) {
        SetupPreviewContent(sample(SetupStep.Battery, isUntestedModel = true))
    }
}

@BurkanPreview
@Composable
private fun SetupConnectTealPreview() {
    BurkanPreviewTheme(seedColor = SeedColors.Teal) {
        SetupPreviewContent(sample(SetupStep.Connect))
    }
}
