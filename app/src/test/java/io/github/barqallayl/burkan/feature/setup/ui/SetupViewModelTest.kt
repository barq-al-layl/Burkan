package io.github.barqallayl.burkan.feature.setup.ui

import arrow.core.Either
import arrow.core.left
import arrow.core.right
import io.github.barqallayl.burkan.core.model.AppError
import io.github.barqallayl.burkan.core.shell.FakeShellExecutor
import io.github.barqallayl.burkan.core.shell.PackageName
import io.github.barqallayl.burkan.core.shell.ShellCommands
import io.github.barqallayl.burkan.core.shell.ShellExecutor
import io.github.barqallayl.burkan.core.storage.FakeDeviceStateStorage
import io.github.barqallayl.burkan.feature.connection.PairingLauncher
import io.github.barqallayl.burkan.feature.connection.data.FakeAdbClient
import io.github.barqallayl.burkan.feature.connection.data.FakeAdbDiscovery
import io.github.barqallayl.burkan.feature.connection.data.FakeWirelessDebugging
import io.github.barqallayl.burkan.feature.connection.data.PairingRepository
import io.github.barqallayl.burkan.feature.connection.data.PairingStatus
import io.github.barqallayl.burkan.feature.connection.data.ShellAccess
import io.github.barqallayl.burkan.feature.connection.model.ConnectionError
import io.github.barqallayl.burkan.feature.setup.data.SetupChecks
import io.github.barqallayl.burkan.feature.setup.model.SetupError
import io.github.barqallayl.burkan.feature.setup.model.SetupStep
import kotlinx.coroutines.launch
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
    private val grant = ShellCommands.grantWriteSecureSettings(checks.ownPackage)

    private fun viewModel() = SetupViewModel(checks, wirelessDebugging, deviceState, pairing, launcher, shellAccess)

    /** Everything up to pairing done, so the next step is Connect. */
    private fun readyToConnect() {
        checks.notifications = true
        checks.developerOptions = true
        wirelessDebugging.on = true
        deviceState.isPaired.value = true
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
    fun `once paired it connects and grants itself the permission without being asked`() = runTest {
        readyToConnect()

        viewModel().testWithInternalState(this) {
            val creating = runOnCreate()

            val state = awaitState { it.current == SetupStep.Battery }
            assertNull(state.failure)
            assertFalse(state.isConnecting)
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
            awaitState { it.current == SetupStep.Battery }
            assertEquals(2, shellAccess.runs)

            creating.cancel()
            cancelAndIgnoreRemainingItems()
        }
    }

    @Test
    fun `a grant that does not take leaves the permission step failed`() = runTest {
        readyToConnect()
        shell.beforeEach = {}
        shell.reply(grant, stderr = "Security exception\n", exitCode = 255)

        viewModel().testWithInternalState(this) {
            val creating = runOnCreate()

            val failed = awaitState { it.failure != null }
            assertEquals(SetupError.GrantFailed, failed.failure)
            containerHost.refresh()
            val state = awaitState { it.current == SetupStep.Permission }
            assertTrue(SetupStep.Connect in state.done.orEmpty())

            creating.cancel()
            cancelAndIgnoreRemainingItems()
        }
    }

    @Test
    fun `skipping the battery step completes setup`() = runTest {
        readyToConnect()
        wirelessDebugging.permitted = true

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
        override val ownPackage = PackageName.known("io.github.barqallayl.burkan")

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

    /** Runs the block on [shell], or fails before connecting with [failure]. */
    private class FakeShellAccess(private val shell: ShellExecutor) : ShellAccess {
        var failure: AppError? = null
        var runs = 0

        override suspend fun <T> withShell(block: suspend (ShellExecutor) -> T): Either<AppError, T> {
            runs++
            return failure?.left() ?: block(shell).right()
        }
    }
}
