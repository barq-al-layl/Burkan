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
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.merge

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
    private val lockEvents: LockEvents,
    private val bootCount: BootCount,
) {

    val state: Flow<AutoApplyState> = storage.state

    /**
     * `BOOT_COMPLETED` arrived. After a restart, whatever the previous boot was waiting for is forgotten and the
     * apply starts. The same boot count as last time means the phone did not restart: see [resumeThisBoot].
     */
    suspend fun onBoot() {
        val boot = bootCount.current()
        if (boot != null && boot == storage.lastBoot()) {
            resumeThisBoot()
            return
        }
        if (boot != null) storage.setLastBoot(boot)
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
            outcome.systemUiDeferred -> {
                wifiWatch.stop()
                storage.set(AutoApplyState(systemUiAtNextLock = true))
            }
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

    /**
     * Waits for the phone to lock while System UI is still left for then. True when it locked with System UI still to
     * restart; false at once when nothing is waiting, or as soon as something else (a manual apply, the automatic
     * apply turned off) settled it.
     */
    suspend fun awaitLockWhilePending(): Boolean {
        if (!storage.state.first().systemUiAtNextLock) return false
        return merge(
            lockEvents.locks.map { true },
            storage.state.filter { !it.systemUiAtNextLock }.map { false },
        ).first()
    }

    /**
     * Restarts System UI at the next lock, for as long as it is left for then. [atLock] runs the apply once the phone
     * has locked, and returns null when it could not start because another run holds the app. A run at the lock that
     * found the phone unlocked again leaves System UI for the next lock, and the wait starts over.
     */
    suspend fun restartSystemUiAtLocks(atLock: suspend () -> RunOutcome?) {
        while (awaitLockWhilePending()) {
            val outcome = atLock() ?: return
            onRunFinished(RunTrigger.AtLock, outcome)
        }
    }

    /** True while System UI is left for the next lock. */
    suspend fun isWaitingForLock(): Boolean = storage.state.first().systemUiAtNextLock

    /** The user cancelled the wait for the lock: System UI stays as it is until they apply. */
    suspend fun cancelSystemUiAtLock() {
        storage.set(storage.state.first().copy(systemUiAtNextLock = false))
    }

    /** Stops waiting: the user turned the automatic apply off, or setup starts again. */
    suspend fun stopWaiting() {
        wifiWatch.stop()
        storage.set(AutoApplyState())
    }

    /**
     * `BOOT_COMPLETED` again within one boot. Since Android 15 the system sends it to an app taken out of the stopped
     * state, which a force-stop leaves it in (an install from Android Studio does one), so that the app can register
     * again the pending intents the force-stop cancelled. Nothing has restarted, so nothing is applied: only what
     * this boot is still waiting for is picked up again.
     */
    private suspend fun resumeThisBoot() {
        if (!isEnabled()) return
        val state = storage.state.first()
        if (state.waitingFor != null) wifiWatch.start()
        if (state.systemUiAtNextLock) launcher.awaitLock()
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
