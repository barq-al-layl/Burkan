package io.github.barqallayl.burkan.feature.setup.ui

import android.Manifest
import android.annotation.SuppressLint
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.PreviewWrapper
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import androidx.lifecycle.compose.LifecycleResumeEffect
import com.composables.icons.tabler.Tabler
import com.composables.icons.tabler.outline.AlertCircle
import com.composables.icons.tabler.outline.Check
import com.composables.icons.tabler.outline.InfoCircle
import dev.zacsweers.metrox.viewmodel.metroViewModel
import io.github.barqallayl.burkan.R
import io.github.barqallayl.burkan.core.model.AppError
import io.github.barqallayl.burkan.core.model.messageRes
import io.github.barqallayl.burkan.core.ui.openSettings
import io.github.barqallayl.burkan.designsystem.SeedColors
import io.github.barqallayl.burkan.designsystem.TextScale
import io.github.barqallayl.burkan.designsystem.ThemeMode
import io.github.barqallayl.burkan.designsystem.component.BurkanIconBadge
import io.github.barqallayl.burkan.designsystem.component.BurkanPill
import io.github.barqallayl.burkan.designsystem.component.BurkanSegment
import io.github.barqallayl.burkan.designsystem.component.BurkanSegmentItem
import io.github.barqallayl.burkan.designsystem.component.GroupGap
import io.github.barqallayl.burkan.designsystem.component.noticeColors
import io.github.barqallayl.burkan.designsystem.component.ScreenMargin
import io.github.barqallayl.burkan.designsystem.component.SegmentedColumn
import io.github.barqallayl.burkan.designsystem.component.Tone
import io.github.barqallayl.burkan.designsystem.component.segmentContainerColor
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
    val notificationPermission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {
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
    SetupContent(state = state, onAction = viewModel::onAction, onSkipBattery = viewModel::skipBattery)
}

/** The exemption request is the point of the step: the run after boot must not be held back. */
@SuppressLint("BatteryLife")
private fun batteryExemptionIntent(context: Context): Intent =
    Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS, "package:${context.packageName}".toUri())

@Composable
private fun SetupContent(state: SetupState, onAction: (SetupStep) -> Unit, onSkipBattery: () -> Unit) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.setup_title)) },
                actions = {
                    state.current?.let { current ->
                        BurkanPill(
                            stringResource(R.string.setup_progress, current.ordinal + 1, SetupStep.entries.size),
                            modifier = Modifier.padding(end = ScreenMargin),
                            tone = Tone.Good,
                        )
                    }
                },
            )
        },
    ) { innerPadding ->
        val done = state.done
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = ScreenMargin)
                .padding(bottom = GroupGap),
            verticalArrangement = Arrangement.spacedBy(GroupGap),
        ) {
            if (state.isUntestedModel) UntestedModelNotice()
            if (done == null) {
                Box(Modifier.fillMaxWidth().padding(vertical = 96.dp), contentAlignment = Alignment.Center) {
                    LoadingIndicator()
                }
            } else {
                // The steps are one group, the checklist the user works down. What is done shrinks to one quiet row,
                // so the step to do now is at the top with the ones still to come under it.
                val finished = SetupStep.entries.filter { it in done }
                val remaining = SetupStep.entries.filter { it !in done }
                val rows = remaining.size + if (finished.isEmpty()) 0 else 1
                Column(verticalArrangement = Arrangement.spacedBy(GroupGap)) {
                    ProgressBar(done)
                    SegmentedColumn {
                        if (finished.isNotEmpty()) DoneItem(finished, rows)
                        remaining.forEachIndexed { index, step ->
                            StepItem(
                                index = index + rows - remaining.size,
                                count = rows,
                                step = step,
                                status = if (step == state.current) StepStatus.Current else StepStatus.Waiting,
                                state = state,
                                onAction = { onAction(step) },
                                onSkipBattery = onSkipBattery,
                            )
                        }
                    }
                }
            }
        }
    }
}

/** One bar for each step, filled for the ones that are done: how far along, at a glance. The pill says it in words. */
@Composable
private fun ProgressBar(done: Set<SetupStep>) {
    Row(modifier = Modifier.fillMaxWidth().clearAndSetSemantics { }, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        SetupStep.entries.forEach { step ->
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(4.dp)
                    .background(
                        if (step in done) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceContainerHighest,
                        CircleShape,
                    ),
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
    BurkanSegment(index = 0, count = 1, containerColor = colors.container, contentColor = colors.content) {
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

/** The current step stands out in the scheme's primary colour and carries its instructions and action. */
@Composable
private fun StepItem(
    index: Int,
    count: Int,
    step: SetupStep,
    status: StepStatus,
    state: SetupState,
    onAction: () -> Unit,
    onSkipBattery: () -> Unit,
) {
    val isCurrent = status == StepStatus.Current
    BurkanSegmentItem(
        index = index,
        count = count,
        headline = stringResource(step.title),
        containerColor = if (isCurrent) MaterialTheme.colorScheme.primaryContainer else segmentContainerColor,
        leading = { StepNumber(step.ordinal + 1, isCurrent) },
        // Said on the step itself while it waits; once it is the current one, its own button offers the skip.
        trailing = if (step == SetupStep.Battery && !isCurrent) {
            { BurkanPill(stringResource(R.string.setup_optional)) }
        } else {
            null
        },
        content = if (isCurrent) ({ CurrentStepDetail(step, state, onAction, onSkipBattery) }) else null,
    )
}

/**
 * A step's place in the list, in a round badge: filled for the one to do now, quiet for the ones still to come.
 * Each has a spoken label as well: never colour alone.
 */
@Composable
private fun StepNumber(number: Int, isCurrent: Boolean) {
    val scheme = MaterialTheme.colorScheme
    val label = stringResource(if (isCurrent) R.string.setup_step_current else R.string.setup_step_waiting)
    Box(
        modifier = Modifier
            .size(40.dp)
            .background(if (isCurrent) scheme.primary else scheme.surfaceContainerHighest, CircleShape)
            .clearAndSetSemantics { contentDescription = label },
        contentAlignment = Alignment.Center,
    ) {
        Text(
            number.toString(),
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold,
            color = if (isCurrent) scheme.onPrimary else scheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun CurrentStepDetail(step: SetupStep, state: SetupState, onAction: () -> Unit, onSkipBattery: () -> Unit) {
    Column(
        modifier = Modifier.padding(top = 4.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(stringResource(step.text))
        when (step) {
            SetupStep.Pair -> PairDetail(state.pairing, onAction)
            SetupStep.Connect, SetupStep.Permission -> {
                val failure = state.failure
                when {
                    failure != null -> FailureWithRetry(failure, onAction)
                    else -> Progress(stringResource(R.string.setup_connect_in_progress))
                }
            }
            SetupStep.Battery -> Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = onAction) { Text(stringResource(R.string.setup_battery_action)) }
                TextButton(onClick = onSkipBattery) { Text(stringResource(R.string.setup_battery_skip)) }
            }
            else -> Button(onClick = onAction) { Text(stringResource(step.action)) }
        }
    }
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
    Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
        LoadingIndicator(modifier = Modifier.size(40.dp))
        Text(label)
    }
}

@Composable
private fun FailureWithRetry(error: AppError, onRetry: () -> Unit) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Icon(Tabler.Outline.AlertCircle, contentDescription = null, tint = MaterialTheme.colorScheme.error)
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
) = SetupState(
    done = SetupStep.entries.takeWhile { it != current }.toSet(),
    pairing = pairing,
    isConnecting = isConnecting,
    failure = failure,
    isUntestedModel = isUntestedModel,
)

@Composable
private fun SetupPreviewContent(state: SetupState) {
    SetupContent(state = state, onAction = {}, onSkipBattery = {})
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
private fun SetupUntestedModelPreview() = SetupPreviewContent(sample(SetupStep.Notifications, isUntestedModel = true))

@PreviewWrapper(BurkanPreviewWrapper::class)
@BurkanPreview
@Composable
private fun SetupDeveloperOptionsPreview() = SetupPreviewContent(sample(SetupStep.DeveloperOptions))

@PreviewWrapper(BurkanPreviewWrapper::class)
@BurkanPreview
@Composable
private fun SetupWirelessDebuggingPreview() = SetupPreviewContent(sample(SetupStep.WirelessDebugging))

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
private fun SetupPairInProgressPreview() = SetupPreviewContent(sample(SetupStep.Pair, pairing = PairingStatus.Pairing))

@PreviewWrapper(BurkanPreviewWrapper::class)
@BurkanPreview
@Composable
private fun SetupPairWrongCodePreview() =
    SetupPreviewContent(sample(SetupStep.Pair, pairing = PairingStatus.Failed(PairingError.WrongCode)))

@PreviewWrapper(BurkanPreviewWrapper::class)
@BurkanPreview
@Composable
private fun SetupPairDialogClosedPreview() =
    SetupPreviewContent(sample(SetupStep.Pair, pairing = PairingStatus.Failed(PairingError.DialogClosed)))

@PreviewWrapper(BurkanPreviewWrapper::class)
@BurkanPreview
@Composable
private fun SetupPairTimedOutPreview() =
    SetupPreviewContent(sample(SetupStep.Pair, pairing = PairingStatus.Failed(PairingError.TimedOut)))

@PreviewWrapper(BurkanPreviewWrapper::class)
@BurkanPreview
@Composable
private fun SetupConnectingPreview() = SetupPreviewContent(sample(SetupStep.Connect, isConnecting = true))

@PreviewWrapper(BurkanPreviewWrapper::class)
@BurkanPreview
@Composable
private fun SetupConnectNoWifiPreview() =
    SetupPreviewContent(sample(SetupStep.Connect, failure = ConnectionError.NoWifi))

@PreviewWrapper(BurkanPreviewWrapper::class)
@BurkanPreview
@Composable
private fun SetupConnectRefusedPreview() =
    SetupPreviewContent(sample(SetupStep.Connect, failure = ConnectionError.WirelessDebuggingRefused))

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
private fun SetupPairWrongCodeDarkPreview() {
    BurkanPreviewTheme(themeMode = ThemeMode.Dark) {
        SetupPreviewContent(sample(SetupStep.Pair, pairing = PairingStatus.Failed(PairingError.WrongCode)))
    }
}

@BurkanPreview
@Composable
private fun SetupBatteryLargeTextPreview() {
    BurkanPreviewTheme(textScalePercent = TextScale.percentages.last) {
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
