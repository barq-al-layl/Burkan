package io.github.barqallayl.burkan.feature.apply.data

import io.github.barqallayl.burkan.core.model.AppError
import io.github.barqallayl.burkan.core.shell.FakeShellExecutor
import io.github.barqallayl.burkan.core.shell.ShellCommands
import io.github.barqallayl.burkan.core.shell.fixture
import io.github.barqallayl.burkan.core.shell.ShellError
import io.github.barqallayl.burkan.core.storage.FakeDeviceStateStorage
import io.github.barqallayl.burkan.core.storage.FakeSettingsStorage
import io.github.barqallayl.burkan.feature.apply.FakeApplyLauncher
import io.github.barqallayl.burkan.feature.apply.FakeBootCount
import io.github.barqallayl.burkan.feature.apply.FakeAutoApplyStorage
import io.github.barqallayl.burkan.feature.apply.FakeLockEvents
import io.github.barqallayl.burkan.feature.apply.FakeRunAlerts
import io.github.barqallayl.burkan.feature.apply.FakeSystemUiRestarts
import io.github.barqallayl.burkan.feature.apply.FakeWifiWatch
import io.github.barqallayl.burkan.feature.apply.model.ApplyKind
import io.github.barqallayl.burkan.feature.apply.model.AutoApplyState
import io.github.barqallayl.burkan.feature.apply.model.RunTrigger
import io.github.barqallayl.burkan.feature.apply.model.WaitReason
import io.github.barqallayl.burkan.feature.connection.data.FakeShellAccess
import io.github.barqallayl.burkan.feature.connection.data.FakeWirelessDebugging
import io.github.barqallayl.burkan.feature.connection.model.ConnectionError
import io.github.barqallayl.burkan.feature.log.data.FakeRunLogStorage
import io.github.barqallayl.burkan.feature.log.model.RunResult
import kotlinx.coroutines.async
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.seconds

class AutoApplyTest {

    private val settings = FakeSettingsStorage()
    private val deviceState = FakeDeviceStateStorage(paired = true, setupComplete = true)
    private val storage = FakeAutoApplyStorage()
    private val wirelessDebugging = FakeWirelessDebugging()
    private val wifiWatch = FakeWifiWatch(active = HOME_NETWORK)
    private val launcher = FakeApplyLauncher()
    private val alerts = FakeRunAlerts()
    private val lockEvents = FakeLockEvents()
    private val bootCount = FakeBootCount()
    private val autoApply =
        AutoApply(settings, deviceState, storage, wirelessDebugging, wifiWatch, launcher, alerts, lockEvents, bootCount)

    @Test
    fun `a restart on Wi-Fi starts the run`() = runTest {
        autoApply.onBoot()

        assertEquals(1, launcher.automaticStarts)
        assertEquals(AutoApplyState(), storage.state.value)
        assertFalse(wifiWatch.watching)
    }

    @Test
    fun `BOOT_COMPLETED again in the same boot is not a restart, and applies nothing`() = runTest {
        autoApply.onBoot()
        assertEquals(1, launcher.automaticStarts)

        // Installing from Android Studio force-stops the app; opening it again sends BOOT_COMPLETED, twice here.
        autoApply.onBoot()
        autoApply.onBoot()

        assertEquals(1, launcher.automaticStarts)
    }

    @Test
    fun `a real restart after that applies again`() = runTest {
        autoApply.onBoot()
        bootCount.count = 2

        autoApply.onBoot()

        assertEquals(2, launcher.automaticStarts)
    }

    @Test
    fun `BOOT_COMPLETED again in the same boot picks the wait for Wi-Fi up again`() = runTest {
        wirelessDebugging.wifi = false
        autoApply.onBoot()
        // The force-stop cancelled the network callback.
        wifiWatch.watching = false

        autoApply.onBoot()

        assertTrue(wifiWatch.watching)
        assertEquals(AutoApplyState(WaitReason.Wifi), storage.state.value)
        assertEquals(0, launcher.automaticStarts)
    }

    @Test
    fun `BOOT_COMPLETED again in the same boot waits for the lock again, if System UI was left for it`() = runTest {
        autoApply.onBoot()
        autoApply.onRunFinished(RunTrigger.Boot, RunOutcome(RunResult.Succeeded, systemUiDeferred = true))

        autoApply.onBoot()

        assertEquals(1, launcher.lockWaits)
        assertTrue(storage.state.value.systemUiAtNextLock)
    }

    @Test
    fun `a boot count the system does not give is taken as a restart`() = runTest {
        bootCount.count = null

        autoApply.onBoot()
        autoApply.onBoot()

        assertEquals(2, launcher.automaticStarts)
    }

    @Test
    fun `a restart with the automatic apply off does nothing`() = runTest {
        settings.applyOnBoot.value = false

        autoApply.onBoot()

        assertEquals(0, launcher.automaticStarts)
        assertFalse(wifiWatch.watching)
    }

    @Test
    fun `a restart before setup is complete does nothing`() = runTest {
        deviceState.isSetupComplete.value = false

        autoApply.onBoot()

        assertEquals(0, launcher.automaticStarts)
    }

    @Test
    fun `a restart without Wi-Fi waits for it instead of starting`() = runTest {
        wirelessDebugging.wifi = false

        autoApply.onBoot()

        assertEquals(0, launcher.automaticStarts)
        assertEquals(AutoApplyState(WaitReason.Wifi), storage.state.value)
        assertTrue(wifiWatch.watching)
    }

    @Test
    fun `a restart forgets what the previous boot waited for`() = runTest {
        storage.state.value = AutoApplyState(WaitReason.TrustedNetwork, triedNetwork = OTHER_NETWORK)

        autoApply.onBoot()

        assertEquals(AutoApplyState(), storage.state.value)
        assertEquals(1, launcher.automaticStarts)
    }

    @Test
    fun `Wi-Fi coming up while waiting starts the run on that network`() = runTest {
        storage.state.value = AutoApplyState(WaitReason.Wifi)

        autoApply.onWifiAvailable(HOME_NETWORK)

        assertEquals(1, launcher.automaticStarts)
        assertEquals(AutoApplyState(WaitReason.Wifi, triedNetwork = HOME_NETWORK), storage.state.value)
    }

    @Test
    fun `the network an attempt already failed on is not tried again`() = runTest {
        storage.state.value = AutoApplyState(WaitReason.TrustedNetwork, triedNetwork = HOME_NETWORK)

        autoApply.onWifiAvailable(HOME_NETWORK)
        assertEquals(0, launcher.automaticStarts)

        autoApply.onWifiAvailable(OTHER_NETWORK)
        assertEquals(1, launcher.automaticStarts)
    }

    @Test
    fun `Wi-Fi coming up with nothing to wait for stops the watch`() = runTest {
        wifiWatch.watching = true

        autoApply.onWifiAvailable(HOME_NETWORK)

        assertEquals(0, launcher.automaticStarts)
        assertFalse(wifiWatch.watching)
    }

    @Test
    fun `Wi-Fi coming up after the automatic apply was turned off stops waiting`() = runTest {
        storage.state.value = AutoApplyState(WaitReason.Wifi)
        wifiWatch.watching = true
        settings.applyOnBoot.value = false

        autoApply.onWifiAvailable(HOME_NETWORK)

        assertEquals(0, launcher.automaticStarts)
        assertEquals(AutoApplyState(), storage.state.value)
        assertFalse(wifiWatch.watching)
    }

    @Test
    fun `a run the system will not start from the background becomes a notification`() = runTest {
        storage.state.value = AutoApplyState(WaitReason.Wifi)
        launcher.backgroundStartAllowed = false

        autoApply.onWifiAvailable(HOME_NETWORK)

        assertEquals(1, alerts.readyToApply)
    }

    @Test
    fun `a run that succeeds or finds Vulkan in place ends the wait`() = runTest {
        listOf(RunResult.Succeeded, RunResult.AlreadyApplied).forEach { result ->
            storage.state.value = AutoApplyState(WaitReason.Wifi, triedNetwork = HOME_NETWORK)
            wifiWatch.watching = true

            autoApply.onRunFinished(RunTrigger.Boot, RunOutcome(result))

            assertEquals(AutoApplyState(), storage.state.value, "$result")
            assertFalse(wifiWatch.watching)
        }
        assertEquals(emptyList(), alerts.failures)
    }

    @Test
    fun `a manual run that succeeds also ends the wait`() = runTest {
        storage.state.value = AutoApplyState(WaitReason.TrustedNetwork, triedNetwork = HOME_NETWORK)

        autoApply.onRunFinished(RunTrigger.Manual, RunOutcome(RunResult.Succeeded))

        assertEquals(AutoApplyState(), storage.state.value)
    }

    @Test
    fun `a manual run that fails is reported and leaves the wait alone`() = runTest {
        val waiting = AutoApplyState(WaitReason.Wifi)
        storage.state.value = waiting

        autoApply.onRunFinished(RunTrigger.Manual, RunOutcome(RunResult.Failed, ConnectionError.NoWifi))

        assertEquals(listOf<AppError?>(ConnectionError.NoWifi), alerts.failures)
        assertEquals(waiting, storage.state.value)
    }

    @Test
    fun `losing Wi-Fi before connecting waits for it, quietly`() = runTest {
        autoApply.onRunFinished(RunTrigger.Boot, RunOutcome(RunResult.Failed, ConnectionError.NoWifi))

        assertEquals(AutoApplyState(WaitReason.Wifi), storage.state.value)
        assertTrue(wifiWatch.watching)
        assertEquals(emptyList(), alerts.failures)
    }

    @Test
    fun `a network Android will not allow is explained, and remembered so it is not retried`() = runTest {
        autoApply.onRunFinished(
            RunTrigger.Boot,
            RunOutcome(RunResult.Failed, ConnectionError.WirelessDebuggingRefused),
        )

        assertEquals(AutoApplyState(WaitReason.TrustedNetwork, triedNetwork = HOME_NETWORK), storage.state.value)
        assertTrue(wifiWatch.watching)
        assertEquals(listOf<AppError?>(ConnectionError.WirelessDebuggingRefused), alerts.failures)
    }

    @Test
    fun `any other failure is reported once and nothing waits`() = runTest {
        wifiWatch.watching = true

        autoApply.onRunFinished(RunTrigger.Boot, RunOutcome(RunResult.Failed, ShellError.ConnectionLost))

        assertEquals(listOf<AppError?>(ShellError.ConnectionLost), alerts.failures)
        assertEquals(AutoApplyState(), storage.state.value)
        assertFalse(wifiWatch.watching)
    }

    @Test
    fun `a run that left System UI for later waits for the next lock`() = runTest {
        storage.state.value = AutoApplyState(WaitReason.Wifi, triedNetwork = HOME_NETWORK)
        wifiWatch.watching = true

        autoApply.onRunFinished(RunTrigger.Boot, RunOutcome(RunResult.Succeeded, systemUiDeferred = true))

        assertEquals(AutoApplyState(systemUiAtNextLock = true), storage.state.value)
        assertFalse(wifiWatch.watching)
        assertTrue(autoApply.isWaitingForLock())
    }

    @Test
    fun `the wait ends when the phone locks`() = runTest {
        storage.state.value = AutoApplyState(systemUiAtNextLock = true)

        val waiting = async { autoApply.awaitLockWhilePending() }
        runCurrent()
        assertFalse(waiting.isCompleted)
        lockEvents.lock()

        assertTrue(waiting.await())
    }

    @Test
    fun `the wait ends without restarting once something else settled System UI`() = runTest {
        storage.state.value = AutoApplyState(systemUiAtNextLock = true)

        val waiting = async { autoApply.awaitLockWhilePending() }
        runCurrent()
        // A manual apply restarted System UI meanwhile.
        autoApply.onRunFinished(RunTrigger.Manual, RunOutcome(RunResult.Succeeded))

        assertFalse(waiting.await())
    }

    @Test
    fun `with nothing left for the lock there is nothing to wait for`() = runTest {
        assertFalse(autoApply.awaitLockWhilePending())
    }

    @Test
    fun `an unlock during the wait at the lock waits for the next lock, which restarts System UI`() = runTest {
        val shell = FakeShellExecutor().apply {
            reply(ShellCommands.getRenderer(), stdout = "skiavk\n")
            reply(ShellCommands.gfxInfo(ShellCommands.SystemUi), stdout = fixture("gfxinfo-opengl.txt"))
        }
        val access = FakeShellAccess(shell)
        val log = FakeRunLogStorage()
        val cooldown = SystemUiCooldown(FakeSystemUiRestarts(), FixedClock)
        val controller = ApplyController(access, log, settings, cooldown, lockEvents, FixedClock, FixtureDevice.Self)
        storage.state.value = AutoApplyState(systemUiAtNextLock = true)

        val waiting = async {
            autoApply.restartSystemUiAtLocks { controller.run(ApplyKind.Light, RunTrigger.AtLock) }
        }
        runCurrent()
        lockEvents.lock()
        // The user unlocks again while adbd settles.
        advanceTimeBy(1.seconds)
        lockEvents.locked = false
        advanceTimeBy(ApplyController.ADBD_SETTLE)
        runCurrent()

        assertTrue(shell.lines.none { it.startsWith("am crash") }, "System UI was not restarted in front of the user")
        assertEquals(1, access.releases)
        assertTrue(storage.state.value.systemUiAtNextLock)
        assertFalse(waiting.isCompleted, "waiting for the next lock")

        shell.thenReply(ShellCommands.gfxInfo(ShellCommands.SystemUi), stdout = fixture("gfxinfo-vulkan.txt"))
        lockEvents.lock()
        waiting.await()

        assertEquals(1, shell.lines.count { it == "am crash com.android.systemui" })
        assertFalse(storage.state.value.systemUiAtNextLock)
        assertEquals(
            listOf(RunResult.Succeeded, RunResult.Postponed),
            log.runs.value.map { it.result },
        )
        assertTrue(log.runs.value.all { it.trigger == RunTrigger.AtLock })
    }

    @Test
    fun `cancelling the wait forgets that System UI was left for the lock`() = runTest {
        storage.state.value = AutoApplyState(systemUiAtNextLock = true)

        autoApply.cancelSystemUiAtLock()

        assertFalse(autoApply.isWaitingForLock())
    }

    @Test
    fun `a restart forgets System UI left for the previous boot's lock`() = runTest {
        storage.state.value = AutoApplyState(systemUiAtNextLock = true)

        autoApply.onBoot()

        assertFalse(storage.state.value.systemUiAtNextLock)
    }

    private companion object {
        const val HOME_NETWORK = 100L
        const val OTHER_NETWORK = 200L
    }
}
