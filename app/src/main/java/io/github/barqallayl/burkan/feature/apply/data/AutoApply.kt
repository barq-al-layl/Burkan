package io.github.barqallayl.burkan.feature.apply.data

import io.github.barqallayl.burkan.feature.apply.model.isUnattended
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn
import io.github.barqallayl.burkan.core.model.AppError
import io.github.barqallayl.burkan.core.model.AppErrorType
import io.github.barqallayl.burkan.core.model.ShellBackend
import io.github.barqallayl.burkan.core.model.type
import io.github.barqallayl.burkan.core.storage.DeviceStateStorage
import io.github.barqallayl.burkan.core.storage.SettingsStorage
import io.github.barqallayl.burkan.feature.apply.ApplyLauncher
import io.github.barqallayl.burkan.feature.apply.model.ApplyKind
import io.github.barqallayl.burkan.feature.apply.model.AutoApplyState
import io.github.barqallayl.burkan.feature.apply.model.RunTrigger
import io.github.barqallayl.burkan.feature.apply.model.WaitReason
import io.github.barqallayl.burkan.feature.connection.data.WirelessDebugging
import io.github.barqallayl.burkan.feature.connection.model.ConnectionError
import io.github.barqallayl.burkan.feature.log.model.RunResult
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.merge
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlin.time.Duration.Companion.seconds

/** The notifications a run's end can leave behind. */
interface RunAlerts {
    /** A run failed; [error] says why, when known. */
    fun showFailure(error: AppError?)

    /** Wi-Fi is back but the app could not start the run by itself; the notification starts it. */
    fun showReadyToApply()

    /**
     * A run of [kind] asked for from outside the app (its tile, a shortcut, another app) could not start, as Android
     * did not let the app start it from the background. The notification starts it.
     */
    fun showNotStarted(kind: ApplyKind)

    /**
     * The apply after a restart is done, System UI included: Vulkan is active with nothing left to wait for. [behind]
     * open apps started before it and are still on OpenGL.
     */
    fun showApplied(behind: Int)
}

/**
 * The automatic apply after a restart. It starts the run when the phone can connect, and otherwise waits for a Wi-Fi
 * network through [WifiWatch] for the rest of the boot. Never retries in a loop: each network gets one attempt.
 *
 * With Shizuku as the shell the phone has no need of Wi-Fi to begin with: Shizuku is waited for by the run itself, and
 * is not the app's to start.
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
     * Held while the state is read, decided on and written back. The broadcasts come in on other threads than the
     * service's, and two of them deciding at once from the same reading would each undo the other's change.
     */
    private val changing = Mutex()

    /**
     * `BOOT_COMPLETED` arrived. After a restart, whatever the previous boot was waiting for is forgotten and the
     * apply starts. The same boot count as last time means the phone did not restart: see [resumeThisBoot].
     *
     * Android also sends it when an app that was force-stopped is opened again. Until the first one after an install
     * there is no boot count to compare with, so [runEnded] keeps this boot's as well.
     */
    suspend fun onBoot() = changing.withLock { booted() }

    private suspend fun booted() {
        val boot = bootCount.current()
        if (boot != null && boot == storage.lastBoot()) {
            resumeThisBoot()
            return
        }
        if (boot != null) storage.setLastBoot(boot)
        // A restart switches wireless debugging off, so whatever the app had switched on is off already. Still on,
        // this was no restart after all (on a Galaxy S23, a force-stop with no boot count kept yet), and what the app
        // switched on is still its own to switch off.
        if (!wirelessDebugging.isOn()) deviceState.setSwitchedOnWirelessDebugging(false)
        forget()
        if (!isEnabled()) return
        if (needsWifi() && !wirelessDebugging.isOn() && !wirelessDebugging.isWifiConnected()) {
            waitFor(WaitReason.Wifi, triedNetwork = null)
            return
        }
        start()
    }

    /** A Wi-Fi network came up while waiting. [network] is its handle, or null when the system did not say. */
    suspend fun onWifiAvailable(network: Long?) = changing.withLock { wifiCameUp(network) }

    private suspend fun wifiCameUp(network: Long?) {
        val current = storage.state.first()
        if (current.waitingFor == null || !isEnabled()) {
            forget()
            return
        }
        // The network the last attempt failed on would fail the same way again. The watch is used up by having said
        // so; it is started again when it is next due, and tells of whichever network is up then.
        if (network != null && network == current.triedNetwork) return
        storage.set(current.copy(triedNetwork = network))
        start()
    }

    /**
     * The watch for Wi-Fi is due to be started again: Android lets go of it once it has told of one network, which
     * may have been the one already tried. Started again for as long as something is waited for, and stopped for
     * good otherwise.
     */
    suspend fun onWifiWatchDue() = changing.withLock {
        if (storage.state.first().waitingFor != null && isEnabled()) wifiWatch.start() else wifiWatch.stop()
    }

    /** A run has ended. Decides what the automatic apply waits for next, and what the user is told. */
    suspend fun onRunFinished(trigger: RunTrigger, outcome: RunOutcome) =
        changing.withLock { runEnded(trigger, outcome) }

    private suspend fun runEnded(trigger: RunTrigger, outcome: RunOutcome) {
        // A run in a boot whose BOOT_COMPLETED the app never saw, the first after an install: keep the boot now, so
        // that one sent later in it, after a force-stop, is not taken for a restart.
        if (storage.lastBoot() == null) bootCount.current()?.let { storage.setLastBoot(it) }
        val tried = storage.state.first().triedNetwork
        // The apply after a restart works unseen, so it says when it is done. While System UI is still left for
        // the lock, the service's own notification says that, and this waits for the run at the lock.
        val automatic = trigger == RunTrigger.Boot || trigger == RunTrigger.AtLock
        val done = outcome.result == RunResult.Succeeded || outcome.result == RunResult.AlreadyApplied
        if (automatic && done && !outcome.systemUiDeferred) alerts.showApplied(outcome.behind?.size ?: 0)
        when {
            outcome.systemUiDeferred -> {
                wifiWatch.stop()
                storage.set(AutoApplyState(systemUiAtNextLock = true))
            }
            // Restarting the open apps applies nothing: System UI is still left for the lock, or the apply after a
            // restart still waits for Wi-Fi, as before it.
            done && outcome.appsOnly -> Unit
            done -> forget()
            // At the lock adbd has just restarted, and the first try can come too soon for it: the run could not
            // connect, or lost its connection. System UI stays left for the lock, and it is tried again, a few
            // times and no more.
            trigger == RunTrigger.AtLock && outcome.error.isPassing() && retries() < MAX_LOCK_RETRIES ->
                storage.set(AutoApplyState(systemUiAtNextLock = true, lockRetries = retries() + 1))
            // Not the apply after a restart, so there is nothing of it to wait for again: the failure is only said.
            trigger == RunTrigger.Manual || trigger.isUnattended ->
                if (outcome.result == RunResult.Failed) alerts.showFailure(outcome.error)
            outcome.error == ConnectionError.NoWifi -> waitFor(WaitReason.Wifi, tried)
            outcome.error == ConnectionError.WirelessDebuggingRefused -> {
                waitFor(WaitReason.TrustedNetwork, tried ?: wifiWatch.wifiNetwork())
                alerts.showFailure(outcome.error)
            }
            outcome.result == RunResult.Failed -> {
                forget()
                alerts.showFailure(outcome.error)
            }
            else -> forget()
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
     * found the phone unlocked again leaves System UI for the next lock, and the wait starts over, unless [goOn]
     * says otherwise: it is asked once each run's outcome has been dealt with.
     */
    suspend fun restartSystemUiAtLocks(goOn: () -> Boolean = { true }, atLock: suspend () -> RunOutcome?) {
        while (awaitLockWhilePending()) {
            val outcome = atLock() ?: return
            val retries = retries()
            onRunFinished(RunTrigger.AtLock, outcome)
            // Tried again after a pause, not at once: the phone is still locked, and what failed needs a moment.
            if (retries() > retries) delay(LOCK_RETRY_PAUSE)
            if (!goOn()) return
        }
    }

    /** True while System UI is left for the next lock. */
    suspend fun isWaitingForLock(): Boolean = storage.state.first().systemUiAtNextLock

    /** The user cancelled the wait for the lock: System UI stays as it is until they apply. */
    suspend fun cancelSystemUiAtLock() = changing.withLock {
        storage.set(storage.state.first().copy(systemUiAtNextLock = false))
    }

    /** Stops waiting: the user turned the automatic apply off, or setup starts again. */
    suspend fun stopWaiting() = changing.withLock { forget() }

    private suspend fun forget() {
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

    private suspend fun needsWifi(): Boolean = settings.shellBackend.first() == ShellBackend.WirelessDebugging

    private suspend fun isEnabled(): Boolean = settings.applyOnBoot.first() && deviceState.isSetupComplete.first()

    private suspend fun retries(): Int = storage.state.first().lockRetries

    /** A failure of the connection, which a later try may not meet, as against one the phone would give again. */
    private fun AppError?.isPassing(): Boolean = when (this?.type()) {
        AppErrorType.DaemonNotFound, AppErrorType.ConnectFailed, AppErrorType.ConnectionLost,
        AppErrorType.CommandTimedOut,
            -> true

        else -> false
    }

    companion object {
        /** How many times a run at the lock that could not connect is tried again. */
        const val MAX_LOCK_RETRIES = 2
        val LOCK_RETRY_PAUSE = 15.seconds
    }
}
