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
import io.github.barqallayl.burkan.core.store.SettingsStore
import io.github.barqallayl.burkan.feature.connection.PairingLauncher
import io.github.barqallayl.burkan.feature.connection.data.PairingRepository
import io.github.barqallayl.burkan.feature.connection.data.PairingStatus
import io.github.barqallayl.burkan.feature.connection.data.ShellAccess
import io.github.barqallayl.burkan.feature.connection.data.WirelessDebugging
import io.github.barqallayl.burkan.feature.connection.model.ConnectionError
import io.github.barqallayl.burkan.feature.setup.data.SetupChecks
import io.github.barqallayl.burkan.feature.setup.model.SetupError
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
 * [done] is null until the stored progress is read, which is before the screen opens once the store has the phone's
 * state. [isCurrentMet] is whether the current step's own check passes: only that step is checked, and it is done
 * when the user says so, not before. [failure] belongs to the automatic steps, Connect and Permission; pairing
 * failures arrive in [pairing].
 */
@Immutable
data class SetupState(
    val done: Set<SetupStep>? = null,
    val isCurrentMet: Boolean = false,
    /** The current step needs wireless debugging switched on, and the phone is not on Wi-Fi, where it cannot be. */
    val isWifiMissing: Boolean = false,
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
    private val store: SettingsStore,
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
        store.device.value.let { device ->
            val done = device?.let { doneSteps(it.setupStepsDone, it.isPaired) }
            SetupState(
                done = done,
                isCurrentMet = done?.currentStep()?.let { isMet(it, device?.isPaired == true) } ?: false,
                isUntestedModel = !isTestedModel(checks.deviceModel),
            )
        },
    ) {
        // Pairing finishes in the notification, often while the app is in the background.
        combine(pairing.status, deviceState.isPaired, deviceState.setupStepsDone) { status, _, _ -> status }
            .collect { status ->
                reduce { state.copy(pairing = status) }
                refresh()
            }
    }

    /**
     * Re-reads what the system says about the current step, and no other. Called on every resume, since the user
     * does most steps in Settings. An automatic step starts by itself when the user lands on it.
     */
    fun refresh(): Job = intent {
        val paired = deviceState.isPaired.first()
        val stepsDone = deviceState.setupStepsDone.first()
        // A pairing the phone dropped takes the later steps with it: they are gone through again after it.
        if (!paired && stepsDone > SetupStep.Pair.ordinal) {
            deviceState.setSetupStepsDone(SetupStep.Pair.ordinal)
            return@intent
        }
        val done = doneSteps(stepsDone, paired)
        val current = done.currentStep()
        if (current == null) {
            reduce { state.copy(done = done, isCurrentMet = false, failure = null) }
            deviceState.setSetupComplete(true)
            return@intent
        }
        val met = isMet(current, paired)
        // A failure belongs to the step it happened on; once setup has moved elsewhere it no longer applies.
        val failure = state.failure.takeIf { current.isAutomatic && current == state.current }
        val wifiMissing = !met && current.needsWifi && !wirelessDebugging.isWifiConnected()
        reduce { state.copy(done = done, isCurrentMet = met, isWifiMissing = wifiMissing, failure = failure) }
        // After a failure, the user retries; nothing retries in a loop by itself.
        if (current.isAutomatic && !met && failure == null) runStep(current)
    }

    /** What the system says now about one step. Nothing here is self-declared by the user. */
    private fun isMet(step: SetupStep, paired: Boolean): Boolean = when (step) {
        SetupStep.Notifications -> checks.notificationsAllowed()
        SetupStep.DeveloperOptions -> checks.developerOptionsEnabled()
        SetupStep.WirelessDebugging -> wirelessDebugging.isOn()
        SetupStep.Pair -> paired
        SetupStep.Connect -> connected
        SetupStep.Permission -> wirelessDebugging.canSwitch()
        SetupStep.Battery -> checks.batteryExempt()
    }

    /** Connect and Permission are done by the app, not the user. */
    private val SetupStep.isAutomatic: Boolean get() = this == SetupStep.Connect || this == SetupStep.Permission

    /** The steps done in the Wireless debugging screen, whose switch Android offers only on Wi-Fi. */
    private val SetupStep.needsWifi: Boolean get() = this == SetupStep.WirelessDebugging || this == SetupStep.Pair

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
            SetupStep.Connect, SetupStep.Permission -> runStep(step)
            SetupStep.Battery -> postSideEffect(SetupSideEffect.RequestBatteryExemption)
        }
    }

    /** The Done button: the current step is finished if its check still passes, and the next becomes current. */
    fun finishStep() = intent {
        val current = state.current ?: return@intent
        if (isMet(current, deviceState.isPaired.first())) {
            deviceState.setSetupStepsDone(current.ordinal + 1)
        } else {
            refresh()
        }
    }

    fun skipBattery() = intent {
        if (state.current == SetupStep.Battery) deviceState.setSetupStepsDone(SetupStep.Battery.ordinal + 1)
    }

    /**
     * An automatic step's work. Connect proves the shell works; Permission grants the app the permission to switch
     * wireless debugging.
     */
    private fun runStep(step: SetupStep): Job = intent {
        if (!connecting.compareAndSet(false, true)) return@intent
        reduce { state.copy(isConnecting = true, failure = null) }
        val result = shellAccess.withShell { shell ->
            either {
                if (step == SetupStep.Connect) {
                    val echo = shell.run(ShellCommands.echoOk()).bind()
                    ensure(echo.exitCode == 0) { ConnectionError.ConnectFailed }
                    connected = true
                } else {
                    val grant = shell.run(ShellCommands.grantWriteSecureSettings(ownPackage)).bind()
                    ensure(grant.exitCode == 0 && wirelessDebugging.canSwitch()) { SetupError.GrantFailed }
                }
            }
        }.flatten()
        connecting.set(false)
        reduce { state.copy(isConnecting = false, failure = result.leftOrNull()) }
        if (result.isRight()) refresh()
    }
}
