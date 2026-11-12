package io.github.barqallayl.burkan.feature.setup.ui

import android.Manifest
import android.annotation.SuppressLint
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.provider.Settings
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
import androidx.compose.material3.Icon
import io.github.barqallayl.burkan.designsystem.component.BurkanLoadingIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ProvideTextStyle
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
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
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.tooling.preview.PreviewWrapper
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import androidx.lifecycle.compose.LifecycleResumeEffect
import com.composables.icons.tabler.Tabler
import com.composables.icons.tabler.outline.AlertCircle
import com.composables.icons.tabler.outline.Check
import com.composables.icons.tabler.outline.Devices
import com.composables.icons.tabler.outline.WifiOff
import dev.zacsweers.metrox.viewmodel.metroViewModel
import io.github.barqallayl.burkan.R
import io.github.barqallayl.burkan.core.model.AppError
import io.github.barqallayl.burkan.core.model.ShellBackend
import io.github.barqallayl.burkan.core.model.messageRes
import io.github.barqallayl.burkan.core.ui.buildNumberIntent
import io.github.barqallayl.burkan.core.ui.openSettings
import io.github.barqallayl.burkan.core.ui.wirelessDebuggingIntent
import io.github.barqallayl.burkan.designsystem.AppStyle
import io.github.barqallayl.burkan.designsystem.LocalAppStyle
import io.github.barqallayl.burkan.designsystem.SeedColors
import io.github.barqallayl.burkan.designsystem.ThemeMode
import io.github.barqallayl.burkan.designsystem.burkanMotion
import io.github.barqallayl.burkan.designsystem.component.BurkanButton
import io.github.barqallayl.burkan.designsystem.component.BurkanIconBadge
import io.github.barqallayl.burkan.designsystem.component.BurkanPill
import io.github.barqallayl.burkan.designsystem.component.BurkanSegment
import io.github.barqallayl.burkan.designsystem.component.BurkanSegmentItem
import io.github.barqallayl.burkan.designsystem.component.BurkanTextButton
import io.github.barqallayl.burkan.designsystem.component.BurkanTonalButton
import io.github.barqallayl.burkan.designsystem.component.BurkanTopBar
import io.github.barqallayl.burkan.designsystem.component.GroupGap
import io.github.barqallayl.burkan.designsystem.component.ScreenMargin
import io.github.barqallayl.burkan.designsystem.component.SegmentGap
import io.github.barqallayl.burkan.designsystem.component.Tone
import io.github.barqallayl.burkan.designsystem.component.glassSource
import io.github.barqallayl.burkan.designsystem.component.listInset
import io.github.barqallayl.burkan.designsystem.component.listTop
import io.github.barqallayl.burkan.designsystem.component.oneUiScrollFade
import io.github.barqallayl.burkan.designsystem.component.readableWidth
import io.github.barqallayl.burkan.designsystem.component.rememberBurkanAppBar
import io.github.barqallayl.burkan.designsystem.component.segmentContainerColor
import io.github.barqallayl.burkan.designsystem.component.segmentedShape
import io.github.barqallayl.burkan.designsystem.component.topBarScroll
import io.github.barqallayl.burkan.designsystem.emphasis
import io.github.barqallayl.burkan.designsystem.preview.BurkanPreview
import io.github.barqallayl.burkan.designsystem.preview.BurkanPreviewTheme
import io.github.barqallayl.burkan.designsystem.preview.BurkanPreviewWrapper
import io.github.barqallayl.burkan.designsystem.preview.LargeFontScale
import io.github.barqallayl.burkan.feature.connection.data.PairingStatus
import io.github.barqallayl.burkan.feature.connection.data.shizukuAppPackage
import io.github.barqallayl.burkan.feature.connection.model.ConnectionError
import io.github.barqallayl.burkan.feature.connection.model.PairingError
import io.github.barqallayl.burkan.feature.connection.rememberLocalNetworkRequest
import io.github.barqallayl.burkan.feature.setup.model.SetupError
import io.github.barqallayl.burkan.feature.setup.model.SetupStep
import io.github.barqallayl.burkan.feature.setup.model.movesOnByItself
import io.github.barqallayl.burkan.core.navigation.LocalNavigator
import io.github.barqallayl.burkan.core.ui.rememberPermissionRequest
import org.orbitmvi.orbit.compose.collectAsState
import org.orbitmvi.orbit.compose.collectSideEffect
import io.github.barqallayl.burkan.feature.settings.ui.openIfPossible
import io.github.barqallayl.burkan.feature.settings.model.About
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.foundation.layout.offset

@Composable
fun SetupScreen() {
    val viewModel = metroViewModel<SetupViewModel>()
    val state by viewModel.collectAsState()
    val context = LocalContext.current
    val navigator = LocalNavigator.current
    // Refused twice, Android no longer asks: the app's notification settings open instead, and the step is checked
    // again on the way back.
    val requestNotifications = rememberPermissionRequest(
        permission = Manifest.permission.POST_NOTIFICATIONS,
        settingsIntent = { notificationSettingsIntent(it) },
        onResult = { viewModel.refresh() },
    )
    val requestLocalNetwork = rememberLocalNetworkRequest(viewModel::onLocalNetworkResult)
    // Most steps are done in Settings, so every return to the app re-checks them.
    LifecycleResumeEffect(viewModel) {
        viewModel.refresh()
        onPauseOrDispose { }
    }
    viewModel.collectSideEffect { effect ->
        when (effect) {
            SetupSideEffect.RequestNotificationPermission -> requestNotifications()

            SetupSideEffect.RequestLocalNetworkPermission -> requestLocalNetwork()

            SetupSideEffect.OpenAboutPhone -> context.openSettings(buildNumberIntent())
            SetupSideEffect.OpenDeveloperOptions -> context.openSettings(wirelessDebuggingIntent())
            SetupSideEffect.OpenShizuku -> openShizuku(context)
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
        // Back to the choice of connection or to Welcome while getting started; after "Redo setup" there is nothing
        // behind Setup, and no arrow.
        onBack = if (navigator.canPop) navigator::pop else null,
        onAction = viewModel::onAction,
        onDone = viewModel::finishStep,
        onSkipBattery = viewModel::skipBattery,
        onSwitchBackend = viewModel::switchShellBackend,
        onApplyFirst = viewModel::applyFirst,
        onSkipFirstApply = viewModel::skipFirstApply,
    )
}

/** Shizuku's own app, or where to get it when the phone has none. */
private fun openShizuku(context: Context) {
    val intent = shizukuAppPackage(context.packageManager)
        ?.let { context.packageManager.getLaunchIntentForPackage(it.value) }
        ?: Intent(Intent.ACTION_VIEW, SHIZUKU_DOWNLOAD_URL.toUri())
    try {
        context.startActivity(intent)
    } catch (_: ActivityNotFoundException) {
        // A phone with nothing to open the page with: the step stays as it is.
    }
}

private const val SHIZUKU_DOWNLOAD_URL = "https://shizuku.rikka.app/download/"

/** This app's page of notification settings, where notifications can be allowed once Android stops asking. */
private fun notificationSettingsIntent(context: Context): Intent =
    Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)

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
    onSwitchBackend: () -> Unit,
    onApplyFirst: () -> Unit,
    onSkipFirstApply: () -> Unit,
    onBack: (() -> Unit)? = null,
) {
    val appBar = rememberBurkanAppBar()
    val scrollState = rememberScrollState()
    Scaffold(
        modifier = Modifier.topBarScroll(appBar),
        topBar = {
            BurkanTopBar(
                appBar = appBar,
                contentScroll = { scrollState.value },
                onBack = onBack,
                title = { Text(stringResource(R.string.setup_title)) },
            )
        },
    ) { innerPadding ->
        val done = state.done
        Column(
            modifier = Modifier
                .fillMaxSize()
                .glassSource(appBar)
                .padding(
                    top = innerPadding.listTop(),
                    bottom = innerPadding.calculateBottomPadding(),
                )
                .oneUiScrollFade(scrollState)
                .verticalScroll(scrollState)
                .padding(top = innerPadding.listInset())

                .padding(horizontal = ScreenMargin)
                .padding(bottom = GroupGap)
                .readableWidth(),
            verticalArrangement = Arrangement.spacedBy(GroupGap),
        ) {
            // Said once, with the intro, before anything is asked for; the report link stays in Settings › About.
            if (state.isUntestedModel && state.done?.isEmpty() == true) OtherModelNotice()
            if (done == null) {
                Box(
                    Modifier
                        .fillMaxWidth()
                        .padding(vertical = 96.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    BurkanLoadingIndicator()
                }
            } else {
                // The steps are one group, the checklist the user works down. What is done shrinks to one quiet row,
                // so the step to do now is at the top with the ones still to come under it.
                val steps = state.steps
                val finished = steps.filter { it in done }
                val firstStepRow = if (finished.isEmpty()) 0 else 1
                val rows = steps.size - finished.size + firstStepRow
                // The row of finished steps keeps its last words while it closes.
                var shownFinished by remember { mutableStateOf(finished) }
                if (finished.isNotEmpty()) shownFinished = finished
                Column(verticalArrangement = Arrangement.spacedBy(GroupGap)) {
                    // Said once, before anything is asked for: what all this is in aid of.
                    if (finished.isEmpty()) {
                        Text(
                            stringResource(R.string.setup_intro),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            // Level with the rows' own text, as a group's title is.
                            modifier = Modifier.padding(horizontal = 16.dp),
                        )
                    }
                    ProgressBar(steps, done)
                    // Every step has its place here, shown or not, so one that is finished closes where it stood
                    // while the row of finished steps opens above it and the next step opens under it. The group
                    // is cut to its own outline, so its corners stay round whichever row is at its edge mid-change.
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(segmentedShape(0, 1)),
                    ) {
                        SlidingRow(visible = finished.isNotEmpty(), gap = 0.dp) { DoneItem(shownFinished, rows) }
                        steps.forEach { step ->
                            key(step) {
                                val visible = step !in done
                                val index =
                                    firstStepRow + steps.count { it !in done && steps.indexOf(it) < steps.indexOf(step) }
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
                // Every step done: what is left is to apply, now or later.
                if (state.current == null && state.isFirstApplyPending) {
                    FirstApply(onApply = onApplyFirst, onLater = onSkipFirstApply)
                }
                // The connection is a choice, and setup does not reach Settings, so it can be made, and made again,
                // here, for as long as there are steps to go.
                if (state.current != null) BurkanTextButton(
                    onClick = onSwitchBackend,
                    modifier = Modifier.align(Alignment.CenterHorizontally),
                ) {
                    Text(
                        stringResource(
                            if (state.backend == ShellBackend.Shizuku) {
                                R.string.setup_use_wireless_debugging_instead
                            } else {
                                R.string.setup_use_shizuku_instead
                            },
                        ),
                    )
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
    val motion = burkanMotion
    AnimatedVisibility(
        visible = visible,
        enter = expandVertically(motion.move(), expandFrom = Alignment.Top) + fadeIn(motion.tint()),
        exit = shrinkVertically(motion.move(), shrinkTowards = Alignment.Top) + fadeOut(motion.tint()),
    ) {
        Box(modifier = Modifier.padding(top = gap)) { content() }
    }
}

/**
 * One bar for each step, filled for the ones that are done: how far along, at a glance. The pill says it in words.
 * A bar fills as its step is finished.
 */
@Composable
private fun ProgressBar(steps: List<SetupStep>, done: Set<SetupStep>) {
    val motion = burkanMotion
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clearAndSetSemantics { },
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        steps.forEach { step ->
            val color by animateColorAsState(
                if (step in done) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceContainerHighest,
                animationSpec = motion.move(),
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

/** Every finished step in one row, by count: the steps are named where they were done, and in the bar above. */
@Composable
private fun DoneItem(finished: List<SetupStep>, count: Int) {
    BurkanSegmentItem(
        index = 0,
        count = count,
        headline = pluralStringResource(R.plurals.setup_steps_done, finished.size, finished.size),
        leading = {
            BurkanIconBadge(
                Tabler.Outline.Check,
                tone = Tone.Good,
                contentDescription = stringResource(R.string.setup_step_done),
            )
        },
    )
}

/**
 * For a phone outside the S23 family: an invitation to try it and say how it went, in the screen's own quiet colours,
 * not a warning. Owners of other phones are who the report link is for.
 */
@Composable
private fun OtherModelNotice() {
    val uriHandler = LocalUriHandler.current
    BurkanSegment(index = 0, count = 1) {
        Row(
            modifier = Modifier.padding(start = 16.dp, top = 16.dp, end = 16.dp, bottom = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Icon(
                Tabler.Outline.Devices,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
            )
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    stringResource(R.string.setup_other_model),
                    style = MaterialTheme.typography.bodyMedium,
                )
                BurkanTextButton(
                    onClick = { uriHandler.openIfPossible(About.REPORT_URL) },
                    // Level with the text above it, not with the button's own padding.
                    modifier = Modifier.offset(x = (-12).dp),
                ) {
                    Text(stringResource(R.string.setup_report_problem))
                }
            }
        }
    }
}

/**
 * The end of setup: Burkan applies by itself from the next restart, and can apply now. Said before the tap, since
 * applying restarts System UI and locks the screen once.
 */
@Composable
private fun FirstApply(onApply: () -> Unit, onLater: () -> Unit) {
    BurkanSegment(
        index = 0,
        count = 1,
        containerColor = MaterialTheme.colorScheme.primaryContainer,
        contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                stringResource(R.string.setup_first_apply_title),
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.semantics { heading() },
            )
            Text(stringResource(R.string.setup_first_apply_text), style = MaterialTheme.typography.bodyMedium)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                BurkanButton(onClick = onApply) { Text(stringResource(R.string.setup_first_apply_action)) }
                // One UI does not put a flat button beside a contained one.
                if (LocalAppStyle.current == AppStyle.OneUi) {
                    BurkanTonalButton(onClick = onLater) { Text(stringResource(R.string.setup_first_apply_later)) }
                } else {
                    BurkanTextButton(onClick = onLater) { Text(stringResource(R.string.setup_first_apply_later)) }
                }
            }
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
    val motion = burkanMotion
    val isCurrent = status == StepStatus.Current
    val targetContainer = if (isCurrent) MaterialTheme.colorScheme.primaryContainer else segmentContainerColor
    val container by animateColorAsState(targetContainer, motion.tint(), label = "step container")
    val content by animateColorAsState(contentColorFor(targetContainer), motion.tint(), label = "step content")
    BurkanSegment(index = index, count = count, containerColor = container, contentColor = content) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 10.dp)
                // A step still to come is read as one thing, its name and that it is not yet done. The current
                // one has buttons under its name, which are stops of their own.
                .then(if (isCurrent) Modifier else Modifier.semantics(mergeDescendants = true) { }),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            StepNumber(state.steps.indexOf(step) + 1, isCurrent)
            Column(modifier = Modifier.weight(1f)) {
                // The name is level with the number, whether or not anything is open under it.
                Box(modifier = Modifier.heightIn(min = STEP_NUMBER_SIZE), contentAlignment = Alignment.CenterStart) {
                    Text(stringResource(step.title), style = MaterialTheme.typography.titleMedium)
                }
                AnimatedVisibility(
                    visible = isCurrent,
                    enter = expandVertically(motion.move(), expandFrom = Alignment.Top) + fadeIn(motion.appear()),
                    exit = shrinkVertically(motion.move(), shrinkTowards = Alignment.Top) + fadeOut(motion.vanish()),
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
                AnimatedVisibility(
                    visible = !isCurrent,
                    enter = fadeIn(motion.appear()),
                    exit = fadeOut(motion.vanish()),
                ) {
                    Box(modifier = Modifier.heightIn(min = STEP_NUMBER_SIZE), contentAlignment = Alignment.Center) {
                        BurkanPill(stringResource(R.string.setup_recommended))
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
    val motion = burkanMotion
    val scheme = MaterialTheme.colorScheme
    val label =
        stringResource(if (isCurrent) R.string.setup_step_current else R.string.setup_step_waiting)
    val fill by animateColorAsState(
        if (isCurrent) scheme.primary else scheme.surfaceContainerHighest,
        motion.move(),
        label = "step number",
    )
    val ink by animateColorAsState(
        if (isCurrent) scheme.onPrimary else scheme.onSurfaceVariant,
        motion.move(),
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
            style = emphasis(MaterialTheme.typography.titleSmall, MaterialTheme.typography.titleSmallEmphasized),
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
    val motion = burkanMotion
    Column(
        modifier = Modifier.padding(top = 4.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Column {
            step.text(state.backend)?.let { Text(stringResource(it)) }
            // Said before the user goes to Settings and finds the switch greyed out.
            AnimatedVisibility(
                visible = state.isWifiMissing,
                enter = expandVertically(motion.move(), expandFrom = Alignment.Top) + fadeIn(motion.appear()),
                exit = shrinkVertically(motion.move(), shrinkTowards = Alignment.Top) + fadeOut(motion.vanish()),
            ) {
                Row(modifier = Modifier.padding(top = 12.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(Tabler.Outline.WifiOff, contentDescription = null)
                    Text(stringResource(R.string.setup_no_wifi), style = emphasizedBody)
                }
            }
        }
        // What the step shows under its instructions changes as it goes: its action, then work under way or a
        // failure, then what the check found. Each gives way to the next, and the row follows its height.
        AnimatedContent(
            targetState = state.detail(step),
            transitionSpec = {
                fadeIn(motion.appear()) togetherWith fadeOut(motion.vanish()) using SizeTransform(clip = false) { _, _ -> motion.move() }
            },
            label = "step detail",
        ) { detail ->
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                when (detail) {
                    StepDetail.Met -> Met(step, onDone.takeUnless { step.movesOnByItself })
                    is StepDetail.Pairing -> PairDetail(detail.status, onAction)
                    is StepDetail.Failed -> FailureWithRetry(detail.error, onAction)
                    StepDetail.Working -> Progress(stringResource(R.string.setup_finishing_in_progress))

                    StepDetail.Action -> if (step == SetupStep.Battery) {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            BurkanButton(onClick = onAction) {
                                Text(stringResource(R.string.setup_battery_action))
                            }
                            // One UI does not put a flat button beside a contained one.
                            if (LocalAppStyle.current == AppStyle.OneUi) {
                                BurkanTonalButton(onClick = onSkipBattery) {
                                    Text(stringResource(R.string.setup_battery_skip))
                                }
                            } else {
                                BurkanTextButton(onClick = onSkipBattery) {
                                    Text(stringResource(R.string.setup_battery_skip))
                                }
                            }
                        }
                    } else {
                        BurkanButton(onClick = onAction) { Text(stringResource(step.action(state))) }
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
    step == SetupStep.Finishing -> failure?.let(StepDetail::Failed) ?: StepDetail.Working
    else -> StepDetail.Action
}

/**
 * What the check found, in words and with a tick, and the button that finishes the step. A step that moves on by
 * itself has no button: [onDone] is null, and the tick shows for the moment before the next step opens.
 */
@Composable
private fun Met(step: SetupStep, onDone: (() -> Unit)?) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
        Icon(Tabler.Outline.Check, contentDescription = null)
        Text(stringResource(step.met), style = emphasizedBody)
    }
    if (onDone != null) BurkanButton(onClick = onDone) { Text(stringResource(R.string.setup_done_action)) }
}

/**
 * Pairing is two things to do, one after the other, and the user does both outside the app: open the pairing dialog in
 * Wireless debugging, then type its code into the notification. Each is its own line, and the one to do now has the
 * button or the wait under it. A pairing that failed says why and starts again from the first.
 */
@Composable
private fun PairDetail(pairing: PairingStatus, onAction: () -> Unit) {
    if (pairing is PairingStatus.Failed) {
        FailureWithRetry(pairing.error, onAction)
        return
    }
    val opened = pairing != PairingStatus.Idle
    PairAction(number = 1, text = stringResource(R.string.setup_pair_open_text), isDone = opened) {
        if (!opened) {
            BurkanButton(onClick = onAction) { Text(stringResource(R.string.setup_pair_open_action)) }
        } else if (pairing == PairingStatus.WaitingForCode) {
            // Back in the app with the dialog closed: it is opened again from here, under the action it repeats.
            BurkanTonalButton(onClick = onAction) { Text(stringResource(R.string.setup_pair_open_action)) }
        }
    }
    PairAction(number = 2, text = stringResource(R.string.setup_pair_type_text), isDone = false) {
        when (pairing) {
            PairingStatus.WaitingForCode -> Progress(stringResource(R.string.setup_pair_waiting))

            PairingStatus.Pairing, PairingStatus.Paired -> Progress(stringResource(R.string.setup_pair_in_progress))
            else -> Unit
        }
    }
}

/** One of pairing's two actions: its number, or a tick once done, beside what to do, and under it what it offers. */
@Composable
private fun PairAction(number: Int, text: String, isDone: Boolean, content: @Composable ColumnScope.() -> Unit) {
    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        Box(modifier = Modifier.size(20.dp), contentAlignment = Alignment.Center) {
            if (isDone) {
                Icon(Tabler.Outline.Check, contentDescription = stringResource(R.string.setup_step_done))
            } else {
                Text(
                    number.toString(),
                    style = emphasis(
                        MaterialTheme.typography.labelLarge,
                        MaterialTheme.typography.labelLargeEmphasized,
                    ),
                )
            }
        }
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(text)
            content()
        }
    }
}

@Composable
private fun Progress(label: String) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        BurkanLoadingIndicator(modifier = Modifier.size(40.dp))
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
    BurkanButton(onClick = onRetry) { Text(stringResource(R.string.setup_try_again)) }
}

/** The step's own text, set heavier: what the check found, or what stands in the step's way. */
private val emphasizedBody: TextStyle
    @Composable
    get() = emphasis(MaterialTheme.typography.bodyMedium, MaterialTheme.typography.bodyMediumEmphasized)

private val SetupStep.title: Int
    get() = when (this) {
        SetupStep.Notifications -> R.string.setup_notifications_title
        SetupStep.DeveloperOptions -> R.string.setup_developer_options_title
        SetupStep.WirelessDebugging -> R.string.setup_wireless_debugging_title
        SetupStep.Pair -> R.string.setup_pair_title
        SetupStep.Finishing -> R.string.setup_finishing_title
        SetupStep.Battery -> R.string.setup_battery_title
        SetupStep.Shizuku -> R.string.setup_shizuku_title
    }

/** What a step asks, which for Finishing is told differently when Shizuku is the connection. */
private fun SetupStep.text(backend: ShellBackend): Int? = when (this) {
    // Pairing, which Shizuku does without, is what the notification is first needed for.
    SetupStep.Notifications ->
        if (backend == ShellBackend.Shizuku) R.string.setup_notifications_shizuku_text else R.string.setup_notifications_text
    SetupStep.DeveloperOptions -> R.string.setup_developer_options_text
    SetupStep.WirelessDebugging -> R.string.setup_wireless_debugging_text
    // Pairing's two actions carry its instructions.
    SetupStep.Pair -> null
    SetupStep.Finishing ->
        if (backend == ShellBackend.Shizuku) R.string.setup_finishing_shizuku_text else R.string.setup_finishing_text
    SetupStep.Battery -> R.string.setup_battery_text
    SetupStep.Shizuku -> R.string.setup_shizuku_text
}

/** What a step's check found when it passes. */
private val SetupStep.met: Int
    get() = when (this) {
        SetupStep.Notifications -> R.string.setup_notifications_met
        SetupStep.DeveloperOptions -> R.string.setup_developer_options_met
        SetupStep.WirelessDebugging -> R.string.setup_wireless_debugging_met
        SetupStep.Pair -> R.string.setup_pair_met
        SetupStep.Finishing -> R.string.setup_finishing_met
        SetupStep.Battery -> R.string.setup_battery_met
        SetupStep.Shizuku -> R.string.setup_shizuku_met
    }

/** The single action of a step that has one button; Pair, Finishing and Battery build their own. */
private fun SetupStep.action(state: SetupState): Int = when (this) {
    SetupStep.Notifications -> R.string.setup_notifications_action
    SetupStep.DeveloperOptions -> R.string.setup_developer_options_action
    // Shizuku is started in its own app; once it runs, what is left is to allow Burkan. A phone without it is
    // sent to where it comes from, and the button says so.
    SetupStep.Shizuku -> when {
        state.isShizukuRunning -> R.string.setup_shizuku_allow_action
        state.isShizukuInstalled -> R.string.setup_shizuku_open_action
        else -> R.string.setup_shizuku_get_action
    }
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
    SetupContent(
        state = state,
        onAction = {},
        onDone = {},
        onSkipBattery = {},
        onSwitchBackend = {},
        onApplyFirst = {},
        onSkipFirstApply = {},
    )
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

/** With Shizuku, the first step says what notifications are for without a pairing. */
@PreviewWrapper(BurkanPreviewWrapper::class)
@BurkanPreview
@Composable
private fun SetupNotificationsShizukuPreview() =
    SetupPreviewContent(sample(SetupStep.Notifications).copy(backend = ShellBackend.Shizuku))

@PreviewWrapper(BurkanPreviewWrapper::class)
@BurkanPreview
@Composable
private fun SetupFinishingMetPreview() = SetupPreviewContent(sample(SetupStep.Finishing, isMet = true))

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
private fun SetupFinishingPreview() =
    SetupPreviewContent(sample(SetupStep.Finishing, isConnecting = true))

@PreviewWrapper(BurkanPreviewWrapper::class)
@BurkanPreview
@Composable
private fun SetupFinishingNoWifiPreview() =
    SetupPreviewContent(sample(SetupStep.Finishing, failure = ConnectionError.NoWifi))

@PreviewWrapper(BurkanPreviewWrapper::class)
@BurkanPreview
@Composable
private fun SetupFinishingRefusedPreview() =
    SetupPreviewContent(
        sample(
            SetupStep.Finishing,
            failure = ConnectionError.WirelessDebuggingRefused,
        ),
    )

@PreviewWrapper(BurkanPreviewWrapper::class)
@BurkanPreview
@Composable
private fun SetupGrantFailedPreview() =
    SetupPreviewContent(sample(SetupStep.Finishing, failure = SetupError.GrantFailed))

@PreviewWrapper(BurkanPreviewWrapper::class)
@BurkanPreview
@Composable
private fun SetupFirstApplyPreview() =
    SetupPreviewContent(
        SetupState(
            done = SetupStep.entries.filter { it != SetupStep.Shizuku }.toSet(),
            isFirstApplyPending = true,
        ),
    )

@BurkanPreview
@Composable
private fun SetupFirstApplyOneUiDarkPreview() {
    BurkanPreviewTheme(themeMode = ThemeMode.Dark, appStyle = AppStyle.OneUi) {
        SetupPreviewContent(
            SetupState(
                done = SetupStep.entries.filter { it != SetupStep.Shizuku }.toSet(),
                isFirstApplyPending = true,
            ),
        )
    }
}

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
private fun SetupFinishingTealPreview() {
    BurkanPreviewTheme(seedColor = SeedColors.Teal) {
        SetupPreviewContent(sample(SetupStep.Finishing))
    }
}
