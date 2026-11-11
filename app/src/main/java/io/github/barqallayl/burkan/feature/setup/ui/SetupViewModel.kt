package io.github.barqallayl.burkan.feature.setup.ui

import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import arrow.core.flatten
import arrow.core.left
import co.touchlab.kermit.Logger
import io.github.barqallayl.burkan.core.model.UnexpectedError
import kotlinx.coroutines.CancellationException
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
import io.github.barqallayl.burkan.core.model.ShellBackend
import io.github.barqallayl.burkan.core.shell.PackageName
import io.github.barqallayl.burkan.core.shell.ShellCommands
import io.github.barqallayl.burkan.core.storage.DeviceStateStorage
import io.github.barqallayl.burkan.core.storage.SettingsStorage
import io.github.barqallayl.burkan.core.store.SettingsStore
import io.github.barqallayl.burkan.feature.apply.ApplyLauncher
import io.github.barqallayl.burkan.feature.apply.model.ApplyKind
import io.github.barqallayl.burkan.feature.connection.PairingLauncher
import io.github.barqallayl.burkan.feature.connection.data.AdbDiscovery
import io.github.barqallayl.burkan.feature.connection.data.PairingRepository
import io.github.barqallayl.burkan.feature.connection.data.PairingStatus
import io.github.barqallayl.burkan.feature.connection.data.ShellAccess
import io.github.barqallayl.burkan.feature.connection.data.ShizukuClient
import io.github.barqallayl.burkan.feature.connection.data.WirelessDebugging
import io.github.barqallayl.burkan.feature.connection.model.ConnectionError
import io.github.barqallayl.burkan.feature.setup.data.SetupChecks
import io.github.barqallayl.burkan.feature.setup.data.ShellBackendSwitcher
import io.github.barqallayl.burkan.feature.setup.model.SETUP_STEPS_VERSION
import io.github.barqallayl.burkan.feature.setup.model.SetupError
import io.github.barqallayl.burkan.feature.setup.model.SetupStep
import io.github.barqallayl.burkan.feature.setup.model.currentStep
import io.github.barqallayl.burkan.feature.setup.model.doneSteps
import io.github.barqallayl.burkan.feature.setup.model.isTestedModel
import io.github.barqallayl.burkan.feature.setup.model.movesOnByItself
import io.github.barqallayl.burkan.feature.setup.model.stepsDoneNow
import io.github.barqallayl.burkan.feature.setup.model.stepsFor
import kotlinx.coroutines.delay
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import org.orbitmvi.orbit.OrbitContainerHost
import org.orbitmvi.orbit.viewmodel.orbitContainer
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.time.Duration.Companion.milliseconds

/**
 * [done] is null until the stored progress is read, which is before the screen opens once the store has the phone's
 * state. [isCurrentMet] is whether the current step's own check passes: only that step is checked, and it is done
 * when the user says so, not before, except for the steps that move on by themselves. [failure] belongs to the
 * automatic step, Finishing; pairing failures arrive in [pairing]. The checklist is the one for [backend]. Once every
 * step is done, [isFirstApplyPending] offers the First apply.
 */
@Immutable
data class SetupState(
    val done: Set<SetupStep>? = null,
    val backend: ShellBackend = SettingsStorage.Defaults.shellBackend,
    val isCurrentMet: Boolean = false,
    /** The current step is Shizuku's, and Shizuku is running, so what is left for it is to allow Burkan. */
    val isShizukuRunning: Boolean = false,
    /** False on a phone with neither Shizuku's app nor Shizuku running: the step then offers to get it. */
    val isShizukuInstalled: Boolean = true,
    /** The current step needs wireless debugging switched on, and the phone is not on Wi-Fi, where it cannot be. */
    val isWifiMissing: Boolean = false,
    val pairing: PairingStatus = PairingStatus.Idle,
    val isConnecting: Boolean = false,
    val failure: AppError? = null,
    val isUntestedModel: Boolean = false,
    val isFirstApplyPending: Boolean = false,
) {
    val steps: List<SetupStep> get() = stepsFor(backend)
    val current: SetupStep? get() = done?.currentStep(steps)
}

sealed interface SetupSideEffect {
    data object RequestNotificationPermission : SetupSideEffect
    data object RequestLocalNetworkPermission : SetupSideEffect
    data object OpenAboutPhone : SetupSideEffect
    data object OpenDeveloperOptions : SetupSideEffect
    data object OpenShizuku : SetupSideEffect
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
    private val discovery: AdbDiscovery,
    private val shizuku: ShizukuClient,
    private val shellBackendSwitcher: ShellBackendSwitcher,
    private val launcher: ApplyLauncher,
    @Named(AppBindings.OWN_PACKAGE) private val ownPackage: PackageName,
) : OrbitContainerHost<SetupState, SetupState, SetupSideEffect>, ViewModel() {

    /** A shell command came back in this session. */
    @Volatile
    private var connected = false
    private val connecting = AtomicBoolean(false)

    override val container = orbitContainer<SetupState, SetupSideEffect>(
        store.device.value.let { device ->
            val backend = store.settings.value?.shellBackend ?: SettingsStorage.Defaults.shellBackend
            val steps = stepsFor(backend)
            val done = device?.let { doneSteps(steps, it.setupStepsDone, it.isPaired) }
            SetupState(
                done = done,
                backend = backend,
                isCurrentMet = done?.currentStep(steps)?.let { isMet(it, device.isPaired) } ?: false,
                isUntestedModel = !isTestedModel(checks.deviceModel),
                isFirstApplyPending = device?.isFirstApplyPending ?: false,
            )
        },
    ) {
        // Pairing finishes in the notification, often while the app is in the background.
        combine(
            pairing.status,
            deviceState.isPaired,
            deviceState.setupStepsDone,
            store.settings.filterNotNull().map { it.shellBackend },
            deviceState.isFirstApplyPending,
        ) { status, _, _, _, _ -> status }
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
        val backend = store.settings.filterNotNull().first().shellBackend
        val steps = stepsFor(backend)
        val paired = deviceState.isPaired.first()
        val stored = deviceState.setupStepsDone.first()
        // A count kept by an older version, which had more steps, is put in today's terms once.
        val version = deviceState.setupStepsVersion.first()
        val stepsDone = stepsDoneNow(stored, version, backend)
        if (version < SETUP_STEPS_VERSION) {
            deviceState.setSetupStepsDone(stepsDone, SETUP_STEPS_VERSION)
            return@intent
        }
        // A pairing the phone dropped takes the later steps with it: they are gone through again after it. Shizuku
        // pairs with nothing.
        val pairAt = steps.indexOf(SetupStep.Pair)
        if (pairAt >= 0 && !paired && stepsDone > pairAt) {
            deviceState.setSetupStepsDone(pairAt)
            return@intent
        }
        val done = doneSteps(steps, stepsDone, paired)
        val current = done.currentStep(steps)
        if (current == null) {
            // The First apply is offered when setup is completed, not each time the screen comes back.
            val completing = !deviceState.isSetupComplete.first()
            if (completing) deviceState.setFirstApplyPending(true)
            val pending = deviceState.isFirstApplyPending.first()
            reduce {
                state.copy(
                    backend = backend,
                    done = done,
                    isCurrentMet = false,
                    failure = null,
                    isFirstApplyPending = pending,
                )
            }
            deviceState.setSetupComplete(true)
            return@intent
        }
        val met = isMet(current, paired)
        // A failure belongs to the step it happened on; once setup has moved elsewhere it no longer applies.
        val failure = state.failure.takeIf { current.isAutomatic && current == state.current }
        val wifiMissing = !met && current.needsWifi && !wirelessDebugging.isWifiConnected()
        val shizukuRunning = current == SetupStep.Shizuku && shizuku.isRunning()
        val shizukuInstalled = current != SetupStep.Shizuku || shizukuRunning || shizuku.appPackage() != null
        reduce {
            state.copy(
                backend = backend,
                done = done,
                isCurrentMet = met,
                isWifiMissing = wifiMissing,
                isShizukuRunning = shizukuRunning,
                isShizukuInstalled = shizukuInstalled,
                failure = failure,
            )
        }
        // After a failure, the user retries; nothing retries in a loop by itself.
        if (current.isAutomatic && !met && failure == null) finish()
        // A step with nothing to read once it is done shows its tick for a moment, then gives way to the next.
        if (met && current.movesOnByItself) {
            delay(MOVE_ON_PAUSE)
            if (state.current == current && isMet(current, deviceState.isPaired.first())) {
                deviceState.setSetupStepsDone(steps.indexOf(current) + 1)
            }
        }
    }

    /** What the system says now about one step. Nothing here is self-declared by the user. */
    private fun isMet(step: SetupStep, paired: Boolean): Boolean = when (step) {
        SetupStep.Notifications -> checks.notificationsAllowed()
        SetupStep.DeveloperOptions -> checks.developerOptionsEnabled()
        SetupStep.WirelessDebugging -> wirelessDebugging.isOn()
        SetupStep.Pair -> paired
        SetupStep.Finishing -> connected && wirelessDebugging.canSwitch()
        SetupStep.Battery -> checks.batteryExempt()
        SetupStep.Shizuku -> shizuku.isAllowed()
    }

    /** Finishing is done by the app, not the user. */
    private val SetupStep.isAutomatic: Boolean get() = this == SetupStep.Finishing

    /** The steps done in the Wireless debugging screen, whose switch Android offers only on Wi-Fi. */
    private val SetupStep.needsWifi: Boolean get() = this == SetupStep.WirelessDebugging || this == SetupStep.Pair

    /** The current step's action. */
    fun onAction(step: SetupStep) = intent {
        when (step) {
            SetupStep.Notifications -> postSideEffect(SetupSideEffect.RequestNotificationPermission)
            SetupStep.DeveloperOptions -> postSideEffect(SetupSideEffect.OpenAboutPhone)
            SetupStep.WirelessDebugging -> postSideEffect(SetupSideEffect.OpenDeveloperOptions)
            // Without the permission, looking for the pairing dialog would bring up the system's device picker,
            // which closes the dialog.
            SetupStep.Pair -> if (discovery.isAllowed()) {
                pairingLauncher.start()
                postSideEffect(SetupSideEffect.OpenDeveloperOptions)
            } else {
                postSideEffect(SetupSideEffect.RequestLocalNetworkPermission)
            }
            // Shizuku has to be running before it can be asked for anything: starting it is its own app's work.
            SetupStep.Shizuku -> if (shizuku.isRunning()) {
                shizuku.requestPermission()
            } else {
                postSideEffect(SetupSideEffect.OpenShizuku)
            }
            SetupStep.Finishing -> finish()
            SetupStep.Battery -> postSideEffect(SetupSideEffect.RequestBatteryExemption)
        }
    }

    /** The Done button: the current step is finished if its check still passes, and the next becomes current. */
    fun finishStep() = intent {
        val current = state.current ?: return@intent
        if (isMet(current, deviceState.isPaired.first())) {
            deviceState.setSetupStepsDone(state.steps.indexOf(current) + 1)
        } else {
            refresh()
        }
    }

    /** The answer to the permission Pair asked for: with it, pairing goes on as if it had been there all along. */
    fun onLocalNetworkResult(granted: Boolean) = intent {
        if (granted && state.current == SetupStep.Pair) onAction(SetupStep.Pair)
    }

    /**
     * "Use Shizuku instead", or, from Shizuku's steps, "use wireless debugging instead": the other checklist takes over,
     * from the steps the two share. Without it, a choice made by mistake could be undone only from Settings, which
     * setup does not reach.
     */
    fun switchShellBackend() = intent {
        shellBackendSwitcher.switch()
    }

    /**
     * The First apply: the same apply as Home's Apply now. Home then opens on the run, as getting started is over.
     * System UI's restart locks the screen once, which the screen says before this is tapped.
     */
    fun applyFirst() = intent {
        launcher.start(ApplyKind.Light)
        deviceState.setFirstApplyPending(false)
    }

    /** The First apply put off: Home opens as it is, with Apply now there for later. */
    fun skipFirstApply() = intent {
        deviceState.setFirstApplyPending(false)
    }

    fun skipBattery() = intent {
        if (state.current == SetupStep.Battery) deviceState.setSetupStepsDone(state.steps.indexOf(SetupStep.Battery) + 1)
    }

    /**
     * Finishing's work: a command that proves the shell works, then the grant of the permission to switch wireless
     * debugging, which also lets the app put back what a full apply changed when its shell is gone.
     */
    private fun finish(): Job = intent {
        if (!connecting.compareAndSet(false, true)) return@intent
        reduce { state.copy(isConnecting = true, failure = null) }
        val result = try {
            shellAccess.withShell { shell ->
                either {
                    val echo = shell.run(ShellCommands.echoOk()).bind()
                    ensure(echo.exitCode == 0) { ConnectionError.ConnectFailed }
                    connected = true
                    if (!wirelessDebugging.canSwitch()) {
                        val grant = shell.run(ShellCommands.grantWriteSecureSettings(ownPackage)).bind()
                        ensure(grant.exitCode == 0 && wirelessDebugging.canSwitch()) { SetupError.GrantFailed }
                    }
                }
            }.flatten()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            // Said on the step, where the user can try again: left to fly, the step would never run again.
            Logger.e(e) { "The setup step failed unexpectedly" }
            UnexpectedError.left()
        } finally {
            connecting.set(false)
        }
        reduce { state.copy(isConnecting = false, failure = result.leftOrNull()) }
        if (result.isRight()) refresh()
    }

    private companion object {
        /** How long a step that moves on by itself shows its tick first. */
        val MOVE_ON_PAUSE = 900.milliseconds
    }
}
