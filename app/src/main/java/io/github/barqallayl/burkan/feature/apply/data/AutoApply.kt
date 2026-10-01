package io.github.barqallayl.burkan.feature.apply.data

import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn
import io.github.barqallayl.burkan.core.model.AppError
import io.github.barqallayl.burkan.core.storage.DeviceStateStorage
import io.github.barqallayl.burkan.core.storage.SettingsStorage
import io.github.barqallayl.burkan.feature.apply.ApplyLauncher
import io.github.barqallayl.burkan.feature.apply.model.AutoApplyState
import io.github.barqallayl.burkan.feature.apply.model.RunTrigger
import io.github.barqallayl.burkan.feature.apply.model.WaitReason
import io.github.barqallayl.burkan.feature.connection.data.WirelessDebugging
import io.github.barqallayl.burkan.feature.connection.model.ConnectionError
import io.github.barqallayl.burkan.feature.log.model.RunResult
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first

/** The notifications a run's end can leave behind. */
interface RunAlerts {
    /** A run failed; [error] says why, when known. */
    fun showFailure(error: AppError?)

    /** Wi-Fi is back but the app could not start the run by itself; the notification starts it. */
    fun showReadyToApply()
}

/**
 * The automatic apply after a restart. It starts the run when the phone can connect, and otherwise waits for a Wi-Fi
 * network through [WifiWatch] for the rest of the boot. Never retries in a loop: each network gets one attempt.
 */
@Inject
@SingleIn(AppScope::class)
class AutoApply(
    private val settings: SettingsStorage,
    private val deviceState: DeviceStateStorage,
    private val storage: AutoApplyStorage,
    private val wirelessDebugging: WirelessDebugging,
    private val wifiWatch: WifiWatch,
    private val launcher: ApplyLauncher,
    private val alerts: RunAlerts,
) {

    val state: Flow<AutoApplyState> = storage.state

    /** The phone has restarted. Whatever the previous boot was waiting for is forgotten. */
    suspend fun onBoot() {
        stopWaiting()
        if (!isEnabled()) return
        if (!wirelessDebugging.isOn() && !wirelessDebugging.isWifiConnected()) {
            waitFor(WaitReason.Wifi, triedNetwork = null)
            return
        }
        start()
    }

    /** A Wi-Fi network came up while waiting. [network] is its handle, or null when the system did not say. */
    suspend fun onWifiAvailable(network: Long?) {
        val current = storage.state.first()
        if (current.waitingFor == null || !isEnabled()) {
            stopWaiting()
            return
        }
        // The network the last attempt failed on would fail the same way again.
        if (network != null && network == current.triedNetwork) return
        storage.set(current.copy(triedNetwork = network))
        start()
    }

    /** A run has ended. Decides what the automatic apply waits for next, and what the user is told. */
    suspend fun onRunFinished(trigger: RunTrigger, outcome: RunOutcome) {
        val tried = storage.state.first().triedNetwork
        when {
            outcome.result == RunResult.Succeeded || outcome.result == RunResult.AlreadyApplied -> stopWaiting()
            trigger == RunTrigger.Manual -> if (outcome.result == RunResult.Failed) alerts.showFailure(outcome.error)
            outcome.error == ConnectionError.NoWifi -> waitFor(WaitReason.Wifi, tried)
            outcome.error == ConnectionError.WirelessDebuggingRefused -> {
                waitFor(WaitReason.TrustedNetwork, tried ?: wifiWatch.activeNetwork())
                alerts.showFailure(outcome.error)
            }
            outcome.result == RunResult.Failed -> {
                stopWaiting()
                alerts.showFailure(outcome.error)
            }
            else -> stopWaiting()
        }
    }

    /** Stops waiting: the user turned the automatic apply off, or setup starts again. */
    suspend fun stopWaiting() {
        wifiWatch.stop()
        storage.set(AutoApplyState())
    }

    private suspend fun waitFor(reason: WaitReason, triedNetwork: Long?) {
        storage.set(AutoApplyState(reason, triedNetwork))
        wifiWatch.start()
    }

    private fun start() {
        if (!launcher.startAutomatic()) alerts.showReadyToApply()
    }

    private suspend fun isEnabled(): Boolean = settings.applyOnBoot.first() && deviceState.isSetupComplete.first()
}
