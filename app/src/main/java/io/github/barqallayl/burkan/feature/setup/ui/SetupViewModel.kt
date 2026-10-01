package io.github.barqallayl.burkan.feature.setup.ui

import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import arrow.core.flatten
import arrow.core.raise.either
import arrow.core.raise.ensure
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesIntoMap
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.Named
import dev.zacsweers.metro.binding
import dev.zacsweers.metrox.viewmodel.ViewModelKey
import io.github.barqallayl.burkan.core.di.AppBindings
import io.github.barqallayl.burkan.core.model.AppError
import io.github.barqallayl.burkan.core.shell.PackageName
import io.github.barqallayl.burkan.core.shell.ShellCommands
import io.github.barqallayl.burkan.core.storage.DeviceStateStorage
import io.github.barqallayl.burkan.feature.connection.PairingLauncher
import io.github.barqallayl.burkan.feature.connection.data.PairingRepository
import io.github.barqallayl.burkan.feature.connection.data.PairingStatus
import io.github.barqallayl.burkan.feature.connection.data.ShellAccess
import io.github.barqallayl.burkan.feature.connection.data.WirelessDebugging
import io.github.barqallayl.burkan.feature.connection.model.ConnectionError
import io.github.barqallayl.burkan.feature.setup.data.SetupChecks
import io.github.barqallayl.burkan.feature.setup.model.SetupError
import io.github.barqallayl.burkan.feature.setup.model.SetupFacts
import io.github.barqallayl.burkan.feature.setup.model.SetupStep
import io.github.barqallayl.burkan.feature.setup.model.currentStep
import io.github.barqallayl.burkan.feature.setup.model.doneSteps
import io.github.barqallayl.burkan.feature.setup.model.isTestedModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import org.orbitmvi.orbit.OrbitContainerHost
import org.orbitmvi.orbit.viewmodel.orbitContainer
import java.util.concurrent.atomic.AtomicBoolean

/**
 * [done] is null until the first check has run. [failure] belongs to the automatic steps, Connect and Permission;
 * pairing failures arrive in [pairing].
 */
@Immutable
data class SetupState(
    val done: Set<SetupStep>? = null,
    val pairing: PairingStatus = PairingStatus.Idle,
    val isConnecting: Boolean = false,
    val failure: AppError? = null,
    val isUntestedModel: Boolean = false,
) {
    val current: SetupStep? get() = done?.currentStep()
}

sealed interface SetupSideEffect {
    data object RequestNotificationPermission : SetupSideEffect
    data object OpenAboutPhone : SetupSideEffect
    data object OpenDeveloperOptions : SetupSideEffect
    data object RequestBatteryExemption : SetupSideEffect
}

@Inject
@ViewModelKey
@ContributesIntoMap(AppScope::class, binding = binding<ViewModel>())
class SetupViewModel(
    private val checks: SetupChecks,
    private val wirelessDebugging: WirelessDebugging,
    private val deviceState: DeviceStateStorage,
    private val pairing: PairingRepository,
    private val pairingLauncher: PairingLauncher,
    private val shellAccess: ShellAccess,
    @Named(AppBindings.OWN_PACKAGE) private val ownPackage: PackageName,
) : OrbitContainerHost<SetupState, SetupState, SetupSideEffect>, ViewModel() {

    /** A shell command came back in this session. */
    @Volatile
    private var connected = false
    private val connecting = AtomicBoolean(false)

    override val container = orbitContainer<SetupState, SetupSideEffect>(
        SetupState(isUntestedModel = !isTestedModel(checks.deviceModel)),
    ) {
        // Pairing finishes in the notification, often while the app is in the background.
        combine(pairing.status, deviceState.isPaired, deviceState.isBatteryStepSkipped) { status, _, _ -> status }
            .collect { status ->
                reduce { state.copy(pairing = status) }
                refresh()
            }
    }

    /** Re-reads what the system says. Called on every resume, since the user changes most of it in Settings. */
    fun refresh(): Job = intent {
        val facts = SetupFacts(
            notificationsAllowed = checks.notificationsAllowed(),
            developerOptionsEnabled = checks.developerOptionsEnabled(),
            wirelessDebuggingOn = wirelessDebugging.isOn(),
            paired = deviceState.isPaired.first(),
            connected = connected,
            permissionHeld = wirelessDebugging.canSwitch(),
            batteryExempt = checks.batteryExempt(),
            batteryStepSkipped = deviceState.isBatteryStepSkipped.first(),
        )
        val done = facts.doneSteps()
        when (done.currentStep()) {
            null -> {
                reduce { state.copy(done = done) }
                deviceState.setSetupComplete(true)
            }
            SetupStep.Connect, SetupStep.Permission -> {
                reduce { state.copy(done = done) }
                // After a failure, the user retries; nothing retries in a loop by itself.
                if (state.failure == null) connectAndGrant()
            }
            // A failure belongs to Connect or Permission; once setup has moved elsewhere it no longer applies.
            else -> reduce { state.copy(done = done, failure = null) }
        }
    }

    /** The current step's action. */
    fun onAction(step: SetupStep) = intent {
        when (step) {
            SetupStep.Notifications -> postSideEffect(SetupSideEffect.RequestNotificationPermission)
            SetupStep.DeveloperOptions -> postSideEffect(SetupSideEffect.OpenAboutPhone)
            SetupStep.WirelessDebugging -> postSideEffect(SetupSideEffect.OpenDeveloperOptions)
            SetupStep.Pair -> {
                pairingLauncher.start()
                postSideEffect(SetupSideEffect.OpenDeveloperOptions)
            }
            SetupStep.Connect, SetupStep.Permission -> connectAndGrant()
            SetupStep.Battery -> postSideEffect(SetupSideEffect.RequestBatteryExemption)
        }
    }

    fun skipBattery() = intent { deviceState.setBatteryStepSkipped(true) }

    /** Connects, proves the shell works, and grants the app the permission to switch wireless debugging. */
    private fun connectAndGrant(): Job = intent {
        if (!connecting.compareAndSet(false, true)) return@intent
        reduce { state.copy(isConnecting = true, failure = null) }
        val result = shellAccess.withShell { shell ->
            either {
                val echo = shell.run(ShellCommands.echoOk()).bind()
                ensure(echo.exitCode == 0) { ConnectionError.ConnectFailed }
                connected = true
                val grant = shell.run(ShellCommands.grantWriteSecureSettings(ownPackage)).bind()
                ensure(grant.exitCode == 0 && wirelessDebugging.canSwitch()) { SetupError.GrantFailed }
            }
        }.flatten()
        connecting.set(false)
        reduce { state.copy(isConnecting = false, failure = result.leftOrNull()) }
        if (result.isRight()) refresh()
    }
}
