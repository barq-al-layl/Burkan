package io.github.barqallayl.burkan.feature.setup.ui

import io.github.barqallayl.burkan.core.shell.FakeShellExecutor
import io.github.barqallayl.burkan.core.shell.PackageName
import io.github.barqallayl.burkan.core.shell.ShellCommands
import io.github.barqallayl.burkan.core.storage.FakeDeviceStateStorage
import io.github.barqallayl.burkan.core.storage.FakeSettingsStorage
import io.github.barqallayl.burkan.core.store.SettingsStore
import io.github.barqallayl.burkan.feature.connection.PairingLauncher
import io.github.barqallayl.burkan.feature.connection.data.FakeAdbClient
import io.github.barqallayl.burkan.feature.connection.data.FakeAdbDiscovery
import io.github.barqallayl.burkan.feature.connection.data.FakeShellAccess
import io.github.barqallayl.burkan.feature.connection.data.FakeWirelessDebugging
import io.github.barqallayl.burkan.feature.connection.data.PairingRepository
import io.github.barqallayl.burkan.feature.connection.data.PairingStatus
import io.github.barqallayl.burkan.feature.connection.model.ConnectionError
import io.github.barqallayl.burkan.feature.setup.data.SetupChecks
import io.github.barqallayl.burkan.feature.setup.model.SetupError
import io.github.barqallayl.burkan.feature.setup.model.SetupStep
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
    private val pairing = PairingRepository(FakeAdbClient(), FakeAdbDiscovery(), deviceState)
    private val launcher = FakePairingLauncher()
    private val shell = FakeShellExecutor()
    private val shellAccess = FakeShellAccess(shell)
    private val ownPackage = PackageName.known("io.github.barqallayl.burkan")
    private val grant = ShellCommands.grantWriteSecureSettings(ownPackage)

    private fun TestScope.viewModel() = SetupViewModel(
        checks,
        wirelessDebugging,
        deviceState,
        SettingsStore(FakeSettingsStorage(), deviceState, backgroundScope),
        pairing,
        launcher,
        shellAccess,
        ownPackage,
    )

    /** Everything up to pairing finished by the user, so the current step is Connect. */
    private fun readyToConnect() {
        wirelessDebugging.on = true
        deviceState.isPaired.value = true
        deviceState.setupStepsDone.value = SetupStep.Connect.ordinal
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
    fun `a step the phone already satisfies waits for the user to say it is done`() = runTest {
        checks.notifications = true
        // Further down the list, and not looked at until its turn.
        checks.developerOptions = true

        viewModel().testWithInternalState(this) {
            val creating = runOnCreate()

            val met = awaitState { it.isCurrentMet }
            assertEquals(SetupStep.Notifications, met.current)

            containerHost.finishStep()
            awaitState { it.current == SetupStep.DeveloperOptions && it.isCurrentMet }
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
    fun `it connects on reaching the connect step, and grants the permission on reaching that one`() = runTest {
        readyToConnect()

        viewModel().testWithInternalState(this) {
            val creating = runOnCreate()

            val state = awaitState { it.current == SetupStep.Connect && it.isCurrentMet }
            assertNull(state.failure)
            assertFalse(state.isConnecting)
            assertEquals(listOf("echo ok"), shell.lines)

            containerHost.finishStep()
            awaitState { it.current == SetupStep.Permission && it.isCurrentMet }
            assertEquals(listOf("echo ok", grant.line), shell.lines)

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
            assertEquals(SetupStep.Connect, failed.current)

            // Returning to the app does not retry by itself.
            containerHost.refresh().join()
            assertEquals(1, shellAccess.runs)

            shellAccess.failure = null
            containerHost.onAction(SetupStep.Connect)
            awaitState { it.current == SetupStep.Connect && it.isCurrentMet }
            assertEquals(2, shellAccess.runs)

            creating.cancel()
            cancelAndIgnoreRemainingItems()
        }
    }

    @Test
    fun `a grant that does not take leaves the permission step failed`() = runTest {
        readyToConnect()
        deviceState.setupStepsDone.value = SetupStep.Permission.ordinal
        shell.beforeEach = {}
        shell.reply(grant, stderr = "Security exception\n", exitCode = 255)

        viewModel().testWithInternalState(this) {
            val creating = runOnCreate()

            val failed = awaitState { it.failure != null }
            assertEquals(SetupError.GrantFailed, failed.failure)
            assertEquals(SetupStep.Permission, failed.current)
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

            awaitState { it.done != null && it.current == null }
            assertTrue(deviceState.isSetupComplete.value)

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
