package io.github.barqallayl.burkan.feature.welcome.ui

import io.github.barqallayl.burkan.awaitStateMatching
import io.github.barqallayl.burkan.core.model.ShellBackend
import io.github.barqallayl.burkan.core.storage.FakeDeviceStateStorage
import io.github.barqallayl.burkan.core.storage.FakeSettingsStorage
import io.github.barqallayl.burkan.core.store.SettingsStore
import io.github.barqallayl.burkan.feature.connection.data.FakeShizukuClient
import io.github.barqallayl.burkan.feature.connection.data.FakeWirelessDebugging
import io.github.barqallayl.burkan.feature.setup.data.ShellBackendSwitcher
import io.github.barqallayl.burkan.feature.setup.ui.ConnectionSideEffect
import io.github.barqallayl.burkan.feature.setup.ui.ConnectionViewModel
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import org.junit.Test
import org.orbitmvi.orbit.test.testWithInternalState
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class WelcomeViewModelTest {

    private val deviceState = FakeDeviceStateStorage()
    private val settings = FakeSettingsStorage()
    private val wirelessDebugging = FakeWirelessDebugging()
    private val shizukuApp = FakeShizukuClient().app
    private val shizuku = FakeShizukuClient().apply { app = null }

    private fun viewModel() = WelcomeViewModel(deviceState, wirelessDebugging, shizuku)

    private fun TestScope.connectionViewModel() = ConnectionViewModel(
        SettingsStore(settings, deviceState, backgroundScope),
        ShellBackendSwitcher(settings, deviceState),
        deviceState,
    )

    @Test
    fun `off Wi-Fi without Shizuku, the line shows, and goes when Wi-Fi comes up`() = runTest {
        wirelessDebugging.wifi = false

        viewModel().testWithInternalState(this) {
            val watching = runOnCreate()
            awaitStateMatching { it.showsNoWifi }

            wirelessDebugging.wifi = true
            awaitStateMatching { it.isOnWifi && !it.showsNoWifi }

            watching.cancel()
            cancelAndIgnoreRemainingItems()
        }
    }

    @Test
    fun `with Shizuku's app, the line does not show off Wi-Fi`() = runTest {
        wirelessDebugging.wifi = false
        shizuku.app = shizukuApp

        viewModel().testWithInternalState(this) {
            val watching = runOnCreate()
            val state = awaitStateMatching { it.hasShizuku && !it.isOnWifi }
            assertFalse(state.showsNoWifi)

            watching.cancel()
            cancelAndIgnoreRemainingItems()
        }
    }

    @Test
    fun `without Shizuku, Get started leaves Welcome for good`() = runTest {
        viewModel().testWithInternalState(this) {
            containerHost.getStarted()

            assertEquals(WelcomeSideEffect.StartSetup, awaitSideEffect())
            assertTrue(deviceState.isWelcomeSeen.value)
        }
    }

    @Test
    fun `with Shizuku, Welcome is left only once the connection is chosen`() = runTest {
        shizuku.app = shizukuApp

        viewModel().testWithInternalState(this) {
            containerHost.getStarted()

            assertEquals(WelcomeSideEffect.ChooseConnection, awaitSideEffect())
            assertFalse(deviceState.isWelcomeSeen.value, "backing out of the choice opens Welcome again")
        }
        connectionViewModel().testWithInternalState(this) {
            containerHost.select(ShellBackend.Shizuku)
            awaitStateMatching { it.selected == ShellBackend.Shizuku }
            containerHost.confirm()

            assertEquals(ConnectionSideEffect.StartSetup, awaitSideEffect())
            assertTrue(deviceState.isWelcomeSeen.value)
            assertEquals(ShellBackend.Shizuku, settings.shellBackend.value)
        }
    }
}
