package io.github.barqallayl.burkan.feature.setup.ui

import io.github.barqallayl.burkan.core.model.ShellBackend
import io.github.barqallayl.burkan.core.shell.FakeShellExecutor
import io.github.barqallayl.burkan.core.shell.PackageName
import io.github.barqallayl.burkan.core.shell.ShellCommands
import io.github.barqallayl.burkan.core.storage.FakeDeviceStateStorage
import io.github.barqallayl.burkan.core.storage.FakeSettingsStorage
import io.github.barqallayl.burkan.core.store.SettingsStore
import io.github.barqallayl.burkan.feature.apply.FakeApplyLauncher
import io.github.barqallayl.burkan.feature.apply.model.ApplyKind
import io.github.barqallayl.burkan.feature.connection.PairingLauncher
import io.github.barqallayl.burkan.feature.connection.data.FakeAdbClient
import io.github.barqallayl.burkan.feature.connection.data.FakeAdbDiscovery
import io.github.barqallayl.burkan.feature.connection.data.FakeShellAccess
import io.github.barqallayl.burkan.feature.connection.data.FakeShizukuClient
import io.github.barqallayl.burkan.feature.connection.data.FakeWirelessDebugging
import io.github.barqallayl.burkan.feature.connection.data.PairingRepository
import io.github.barqallayl.burkan.feature.connection.data.PairingStatus
import io.github.barqallayl.burkan.feature.connection.model.ConnectionError
import io.github.barqallayl.burkan.feature.setup.data.SetupChecks
import io.github.barqallayl.burkan.feature.setup.data.ShellBackendSwitcher
import io.github.barqallayl.burkan.feature.setup.model.SetupError
import io.github.barqallayl.burkan.feature.setup.model.SetupStep
import io.github.barqallayl.burkan.feature.setup.model.stepsFor
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import org.junit.Test
import org.orbitmvi.orbit.test.Item
import org.orbitmvi.orbit.test.OrbitScopedTestContextInternal
import org.orbitmvi.orbit.test.testWithInternalState
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

private typealias SetupTest = OrbitScopedTestContextInternal<SetupState, SetupState, SetupSideEffect, SetupViewModel>

class SetupViewModelTest {

    private val checks = FakeSetupChecks()
    private val wirelessDebugging = FakeWirelessDebugging(permitted = false)
    private val deviceState = FakeDeviceStateStorage()
    private val settings = FakeSettingsStorage()
    private val shizuku = FakeShizukuClient()
    private val discovery = FakeAdbDiscovery()
    private val pairing = PairingRepository(FakeAdbClient(), discovery, deviceState)
    private val launcher = FakePairingLauncher()
    private val applyLauncher = FakeApplyLauncher()
    private val shell = FakeShellExecutor()
    private val shellAccess = FakeShellAccess(shell)
    private val ownPackage = PackageName.known("io.github.barqallayl.burkan")
    private val grant = ShellCommands.grantWriteSecureSettings(ownPackage)

    private fun TestScope.viewModel() = SetupViewModel(
        checks,
        wirelessDebugging,
        deviceState,
        SettingsStore(settings, deviceState, backgroundScope),
        pairing,
        launcher,
        shellAccess,
        discovery,
        shizuku,
        ShellBackendSwitcher(settings, deviceState),
        applyLauncher,
        ownPackage,
    )

    /** Everything up to pairing finished by the user, so the current step is Finishing. */
    private fun readyToConnect() {
        wirelessDebugging.on = true
        deviceState.isPaired.value = true
        deviceState.setupStepsDone.value = SetupStep.Finishing.ordinal
        // pm grant succeeding gives the app the permission.
        shell.beforeEach = { if (it == grant) wirelessDebugging.permitted = true }
    }

    @Test
    fun `a fresh phone starts at notifications`() = runTest {
        viewModel().testWithInternalState(this) {
            val creating = runOnCreate()

            val state = awaitState { it.done != null }
            assertEquals(SetupStep.Notifications, state.current)
            assertFalse(state.isUntestedModel)

            creating.cancel()
            cancelAndIgnoreRemainingItems()
        }
    }

    @Test
    fun `a step done in Settings waits for the user to say it is done`() = runTest {
        deviceState.setupStepsDone.value = SetupStep.DeveloperOptions.ordinal
        checks.developerOptions = true
        // Further down the list, and not looked at until its turn.
        wirelessDebugging.on = true

        viewModel().testWithInternalState(this) {
            val creating = runOnCreate()

            val met = awaitState { it.isCurrentMet }
            assertEquals(SetupStep.DeveloperOptions, met.current)
            assertEquals(1, deviceState.setupStepsDone.value, "not finished without Done")

            containerHost.finishStep()
            awaitState { it.current == SetupStep.WirelessDebugging && it.isCurrentMet }
            assertEquals(2, deviceState.setupStepsDone.value)

            creating.cancel()
            cancelAndIgnoreRemainingItems()
        }
    }

    @Test
    fun `notifications, once allowed, move on by themselves`() = runTest {
        checks.notifications = true

        viewModel().testWithInternalState(this) {
            val creating = runOnCreate()

            awaitState { it.current == SetupStep.Notifications && it.isCurrentMet }
            awaitState { it.current == SetupStep.DeveloperOptions }
            assertEquals(1, deviceState.setupStepsDone.value)

            creating.cancel()
            cancelAndIgnoreRemainingItems()
        }
    }

    @Test
    fun `Done does nothing while the step's check fails`() = runTest {
        viewModel().testWithInternalState(this) {
            val creating = runOnCreate()
            awaitState { it.done != null }

            containerHost.finishStep().join()

            assertEquals(0, deviceState.setupStepsDone.value)

            creating.cancel()
            cancelAndIgnoreRemainingItems()
        }
    }

    @Test
    fun `nothing connects before the user reaches the connect step`() = runTest {
        readyToConnect()
        deviceState.setupStepsDone.value = SetupStep.Pair.ordinal

        viewModel().testWithInternalState(this) {
            val creating = runOnCreate()

            val state = awaitState { it.isCurrentMet }
            assertEquals(SetupStep.Pair, state.current)
            assertEquals(0, shellAccess.runs)

            creating.cancel()
            cancelAndIgnoreRemainingItems()
        }
    }

    @Test
    fun `without Wi-Fi the wireless debugging step says so`() = runTest {
        wirelessDebugging.wifi = false
        deviceState.setupStepsDone.value = SetupStep.WirelessDebugging.ordinal

        viewModel().testWithInternalState(this) {
            val creating = runOnCreate()

            val state = awaitState { it.isWifiMissing }
            assertEquals(SetupStep.WirelessDebugging, state.current)

            creating.cancel()
            cancelAndIgnoreRemainingItems()
        }
    }

    @Test
    fun `another model gets the notice and is not blocked`() = runTest {
        checks.deviceModel = "SM-A556B"

        viewModel().testWithInternalState(this) {
            val creating = runOnCreate()

            val state = awaitState { it.done != null }
            assertTrue(state.isUntestedModel)
            assertEquals(SetupStep.Notifications, state.current)

            creating.cancel()
            cancelAndIgnoreRemainingItems()
        }
    }

    @Test
    fun `each manual step asks for its own screen`() = runTest {
        viewModel().testWithInternalState(this) {
            val creating = runOnCreate()
            awaitState { it.done != null }

            containerHost.onAction(SetupStep.Notifications)
            assertEquals(SetupSideEffect.RequestNotificationPermission, awaitSideEffect())
            containerHost.onAction(SetupStep.DeveloperOptions)
            assertEquals(SetupSideEffect.OpenAboutPhone, awaitSideEffect())
            containerHost.onAction(SetupStep.WirelessDebugging)
            assertEquals(SetupSideEffect.OpenDeveloperOptions, awaitSideEffect())
            containerHost.onAction(SetupStep.Battery)
            assertEquals(SetupSideEffect.RequestBatteryExemption, awaitSideEffect())

            creating.cancel()
            cancelAndIgnoreRemainingItems()
        }
    }

    @Test
    fun `pairing without the local network permission asks for it first`() = runTest {
        discovery.allowed = false
        deviceState.setupStepsDone.value = SetupStep.Pair.ordinal
        viewModel().testWithInternalState(this) {
            val creating = runOnCreate()
            awaitState { it.current == SetupStep.Pair }

            containerHost.onAction(SetupStep.Pair)
            assertEquals(SetupSideEffect.RequestLocalNetworkPermission, awaitSideEffect())
            assertEquals(0, launcher.starts)

            discovery.allowed = true
            containerHost.onLocalNetworkResult(granted = true)
            assertEquals(SetupSideEffect.OpenDeveloperOptions, awaitSideEffect())
            assertEquals(1, launcher.starts)

            creating.cancel()
            cancelAndIgnoreRemainingItems()
        }
    }

    @Test
    fun `pairing starts the notification and opens Developer options`() = runTest {
        viewModel().testWithInternalState(this) {
            val creating = runOnCreate()
            awaitState { it.done != null }

            containerHost.onAction(SetupStep.Pair)

            assertEquals(SetupSideEffect.OpenDeveloperOptions, awaitSideEffect())
            assertEquals(1, launcher.starts)

            creating.cancel()
            cancelAndIgnoreRemainingItems()
        }
    }

    @Test
    fun `finishing setup connects and grants the permission by itself, then moves on`() = runTest {
        readyToConnect()

        viewModel().testWithInternalState(this) {
            val creating = runOnCreate()

            val state = awaitState { it.current == SetupStep.Finishing && it.isCurrentMet }
            assertNull(state.failure)
            assertFalse(state.isConnecting)
            assertEquals(listOf("echo ok", grant.line), shell.lines)

            awaitState { it.current == SetupStep.Battery }
            assertEquals(SetupStep.Battery.ordinal, deviceState.setupStepsDone.value)

            creating.cancel()
            cancelAndIgnoreRemainingItems()
        }
    }

    @Test
    fun `a failed connection waits for the user to retry`() = runTest {
        readyToConnect()
        shellAccess.failure = ConnectionError.NoWifi

        viewModel().testWithInternalState(this) {
            val creating = runOnCreate()

            val failed = awaitState { it.failure != null }
            assertEquals(ConnectionError.NoWifi, failed.failure)
            assertEquals(SetupStep.Finishing, failed.current)

            // Returning to the app does not retry by itself.
            containerHost.refresh().join()
            assertEquals(1, shellAccess.runs)

            shellAccess.failure = null
            containerHost.onAction(SetupStep.Finishing)
            awaitState { it.current == SetupStep.Finishing && it.isCurrentMet }
            assertEquals(2, shellAccess.runs)

            creating.cancel()
            cancelAndIgnoreRemainingItems()
        }
    }

    @Test
    fun `a grant that does not take leaves finishing setup failed`() = runTest {
        readyToConnect()
        shell.beforeEach = {}
        shell.reply(grant, stderr = "Security exception\n", exitCode = 255)

        viewModel().testWithInternalState(this) {
            val creating = runOnCreate()

            val failed = awaitState { it.failure != null }
            assertEquals(SetupError.GrantFailed, failed.failure)
            assertEquals(SetupStep.Finishing, failed.current)
            assertFalse(failed.isCurrentMet)

            creating.cancel()
            cancelAndIgnoreRemainingItems()
        }
    }

    @Test
    fun `skipping the battery step completes setup`() = runTest {
        readyToConnect()
        deviceState.setupStepsDone.value = SetupStep.Battery.ordinal

        viewModel().testWithInternalState(this) {
            val creating = runOnCreate()
            awaitState { it.current == SetupStep.Battery }

            containerHost.skipBattery()

            awaitState { it.done != null && it.current == null && it.isFirstApplyPending }
            assertTrue(deviceState.isSetupComplete.value)

            creating.cancel()
            cancelAndIgnoreRemainingItems()
        }
    }

    @Test
    fun `the First apply runs Apply now and ends getting started`() = runTest {
        deviceState.setupStepsDone.value = stepsFor(ShellBackend.WirelessDebugging).size
        deviceState.isPaired.value = true

        viewModel().testWithInternalState(this) {
            val creating = runOnCreate()
            awaitState { it.isFirstApplyPending }

            containerHost.applyFirst().join()

            assertEquals(listOf(ApplyKind.Light), applyLauncher.started)
            assertFalse(deviceState.isFirstApplyPending.value)

            creating.cancel()
            cancelAndIgnoreRemainingItems()
        }
    }

    @Test
    fun `the First apply can be put off, and is then not offered again`() = runTest {
        deviceState.setupStepsDone.value = stepsFor(ShellBackend.WirelessDebugging).size
        deviceState.isPaired.value = true

        viewModel().testWithInternalState(this) {
            val creating = runOnCreate()
            awaitState { it.isFirstApplyPending }

            containerHost.skipFirstApply().join()
            containerHost.refresh().join()

            assertTrue(applyLauncher.started.isEmpty())
            assertFalse(deviceState.isFirstApplyPending.value)
            assertTrue(deviceState.isSetupComplete.value)

            creating.cancel()
            cancelAndIgnoreRemainingItems()
        }
    }

    @Test
    fun `progress counted in the old steps lands on finishing setup, or keeps its place`() = runTest {
        // Version 1 had Connect at 4 and Permission at 5: someone who had connected now finishes setup.
        readyToConnect()
        deviceState.setupStepsVersion.value = 1
        deviceState.setupStepsDone.value = 5

        viewModel().testWithInternalState(this) {
            val creating = runOnCreate()

            awaitState { it.current == SetupStep.Finishing }
            assertEquals(2, deviceState.setupStepsVersion.value)

            creating.cancel()
            cancelAndIgnoreRemainingItems()
        }
    }

    @Test
    fun `finishing the last step completes setup`() = runTest {
        readyToConnect()
        deviceState.setupStepsDone.value = SetupStep.Battery.ordinal
        checks.battery = true

        viewModel().testWithInternalState(this) {
            val creating = runOnCreate()
            awaitState { it.current == SetupStep.Battery && it.isCurrentMet }
            assertFalse(deviceState.isSetupComplete.value)

            containerHost.finishStep()

            awaitState { it.done != null && it.current == null }
            assertTrue(deviceState.isSetupComplete.value)

            creating.cancel()
            cancelAndIgnoreRemainingItems()
        }
    }

    @Test
    fun `a pairing the phone dropped sends setup back to pairing`() = runTest {
        readyToConnect()
        deviceState.setupStepsDone.value = SetupStep.Battery.ordinal

        viewModel().testWithInternalState(this) {
            val creating = runOnCreate()
            awaitState { it.current == SetupStep.Battery }

            deviceState.isPaired.value = false

            awaitState { it.current == SetupStep.Pair }
            assertEquals(SetupStep.Pair.ordinal, deviceState.setupStepsDone.value)

            creating.cancel()
            cancelAndIgnoreRemainingItems()
        }
    }

    @Test
    fun `the pairing status reaches the screen`() = runTest {
        viewModel().testWithInternalState(this) {
            val creating = runOnCreate()
            awaitState { it.done != null }

            this@runTest.backgroundScope.launch { pairing.runSession() }

            awaitState { it.pairing == PairingStatus.WaitingForCode }

            creating.cancel()
            cancelAndIgnoreRemainingItems()
        }
    }

    @Test
    fun `with Shizuku the checklist has no Developer options, wireless debugging or pairing`() = runTest {
        settings.shellBackend.value = ShellBackend.Shizuku
        deviceState.setupStepsDone.value = 1

        viewModel().testWithInternalState(this) {
            val creating = runOnCreate()

            val state = awaitState { it.current == SetupStep.Shizuku }
            assertEquals(stepsFor(ShellBackend.Shizuku), state.steps)
            assertEquals(setOf(SetupStep.Notifications), state.done)

            creating.cancel()
            cancelAndIgnoreRemainingItems()
        }
    }

    @Test
    fun `the Shizuku step is met once Shizuku allows Burkan, and not before`() = runTest {
        settings.shellBackend.value = ShellBackend.Shizuku
        deviceState.setupStepsDone.value = 1
        shizuku.allowed = false

        viewModel().testWithInternalState(this) {
            val creating = runOnCreate()

            val waiting = awaitState { it.current == SetupStep.Shizuku && it.isShizukuRunning }
            assertFalse(waiting.isCurrentMet)

            shizuku.allowed = true
            containerHost.refresh()
            awaitState { it.current == SetupStep.Shizuku && it.isCurrentMet }

            creating.cancel()
            cancelAndIgnoreRemainingItems()
        }
    }

    @Test
    fun `the Shizuku step opens Shizuku until it runs, and then asks it to allow Burkan`() = runTest {
        settings.shellBackend.value = ShellBackend.Shizuku
        deviceState.setupStepsDone.value = 1
        shizuku.running = false

        viewModel().testWithInternalState(this) {
            val creating = runOnCreate()
            val state = awaitState { it.current == SetupStep.Shizuku }
            assertFalse(state.isShizukuRunning)

            containerHost.onAction(SetupStep.Shizuku)
            assertEquals(SetupSideEffect.OpenShizuku, awaitSideEffect())
            assertEquals(0, shizuku.permissionRequests)

            shizuku.running = true
            containerHost.onAction(SetupStep.Shizuku).join()
            assertEquals(1, shizuku.permissionRequests)

            creating.cancel()
            cancelAndIgnoreRemainingItems()
        }
    }

    @Test
    fun `with Shizuku the steps are counted in its own list, and setup completes without a pairing`() = runTest {
        settings.shellBackend.value = ShellBackend.Shizuku
        deviceState.setupStepsDone.value = stepsFor(ShellBackend.Shizuku).indexOf(SetupStep.Battery)

        viewModel().testWithInternalState(this) {
            val creating = runOnCreate()
            awaitState { it.current == SetupStep.Battery }

            containerHost.skipBattery()

            awaitState { it.done != null && it.current == null }
            assertEquals(stepsFor(ShellBackend.Shizuku).size, deviceState.setupStepsDone.value)
            assertTrue(deviceState.isSetupComplete.value)

            creating.cancel()
            cancelAndIgnoreRemainingItems()
        }
    }

    @Test
    fun `Use Shizuku instead swaps the checklist for Shizuku's, from the steps they share`() = runTest {
        deviceState.setupStepsDone.value = 3

        viewModel().testWithInternalState(this) {
            val creating = runOnCreate()
            awaitState { it.current == SetupStep.Pair }

            containerHost.switchShellBackend()

            val state = awaitState { it.backend == ShellBackend.Shizuku && it.current == SetupStep.Shizuku }
            assertEquals(stepsFor(ShellBackend.Shizuku), state.steps)
            assertEquals(ShellBackend.Shizuku, settings.shellBackend.value)
            assertEquals(1, deviceState.setupStepsDone.value)
            assertFalse(deviceState.isSetupComplete.value)

            creating.cancel()
            cancelAndIgnoreRemainingItems()
        }
    }

    @Test
    fun `from Shizuku's steps, wireless debugging can be chosen again`() = runTest {
        settings.shellBackend.value = ShellBackend.Shizuku
        deviceState.setupStepsDone.value = 1

        viewModel().testWithInternalState(this) {
            val creating = runOnCreate()
            awaitState { it.current == SetupStep.Shizuku }

            containerHost.switchShellBackend()

            awaitState { it.backend == ShellBackend.WirelessDebugging && it.current == SetupStep.DeveloperOptions }
            assertEquals(ShellBackend.WirelessDebugging, settings.shellBackend.value)

            creating.cancel()
            cancelAndIgnoreRemainingItems()
        }
    }

    /** The next state that matches, skipping the states and side effects on the way. */
    private suspend fun SetupTest.awaitState(
        matches: (SetupState) -> Boolean,
    ): SetupState {
        while (true) {
            val item = awaitItem()
            if (item is Item.StateItem && matches(item.value)) return item.value
        }
    }

    private class FakeSetupChecks : SetupChecks {
        var notifications = false
        var developerOptions = false
        var battery = false
        override var deviceModel = "SM-S911B"

        override fun notificationsAllowed() = notifications
        override fun developerOptionsEnabled() = developerOptions
        override fun batteryExempt() = battery
    }

    private class FakePairingLauncher : PairingLauncher {
        var starts = 0
        override fun start() {
            starts++
        }
    }
}
