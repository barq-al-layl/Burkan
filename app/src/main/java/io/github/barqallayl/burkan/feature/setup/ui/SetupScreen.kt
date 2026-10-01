package io.github.barqallayl.burkan.feature.setup.ui

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.PreviewWrapper
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import androidx.lifecycle.compose.LifecycleResumeEffect
import com.composables.icons.tabler.Tabler
import com.composables.icons.tabler.outline.AlertCircle
import com.composables.icons.tabler.outline.Circle
import com.composables.icons.tabler.outline.CircleCheck
import com.composables.icons.tabler.outline.CircleDot
import com.composables.icons.tabler.outline.InfoCircle
import dev.zacsweers.metrox.viewmodel.metroViewModel
import io.github.barqallayl.burkan.R
import io.github.barqallayl.burkan.core.model.AppError
import io.github.barqallayl.burkan.core.model.messageRes
import io.github.barqallayl.burkan.core.ui.openSettings
import io.github.barqallayl.burkan.designsystem.TextScale
import io.github.barqallayl.burkan.designsystem.ThemeMode
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
            SetupSideEffect.RequestBatteryExemption -> context.openSettings(batteryExemptionIntent(context))
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
    Scaffold(topBar = { TopAppBar(title = { Text(stringResource(R.string.setup_title)) }) }) { innerPadding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = innerPadding,
        ) {
            if (state.isUntestedModel) {
                item { UntestedModelNotice() }
            }
            val done = state.done
            if (done != null) {
                items(SetupStep.entries) { step ->
                    StepItem(
                        step = step,
                        status = when (step) {
                            in done -> StepStatus.Done
                            state.current -> StepStatus.Current
                            else -> StepStatus.Waiting
                        },
                        state = state,
                        onAction = { onAction(step) },
                        onSkipBattery = onSkipBattery,
                    )
                }
            }
        }
    }
}

private enum class StepStatus { Done, Current, Waiting }

@Composable
private fun UntestedModelNotice() {
    Card(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp)) {
        Row(
            modifier = Modifier.padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(Tabler.Outline.InfoCircle, contentDescription = null)
            Text(stringResource(R.string.setup_untested_model))
        }
    }
}

@Composable
private fun StepItem(
    step: SetupStep,
    status: StepStatus,
    state: SetupState,
    onAction: () -> Unit,
    onSkipBattery: () -> Unit,
) {
    ListItem(
        leadingContent = { StatusIcon(status) },
        supportingContent = if (status == StepStatus.Current) {
            { CurrentStepDetail(step, state, onAction, onSkipBattery) }
        } else {
            null
        },
        verticalAlignment = Alignment.Top,
    ) {
        Text(stringResource(step.title))
    }
}

@Composable
private fun StatusIcon(status: StepStatus) {
    // Each status has its own shape and a spoken label: never colour alone.
    val (icon: ImageVector, label: Int) = when (status) {
        StepStatus.Done -> Tabler.Outline.CircleCheck to R.string.setup_step_done
        StepStatus.Current -> Tabler.Outline.CircleDot to R.string.setup_step_current
        StepStatus.Waiting -> Tabler.Outline.Circle to R.string.setup_step_waiting
    }
    Icon(icon, contentDescription = stringResource(label))
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
