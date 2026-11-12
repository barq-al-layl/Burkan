package io.github.barqallayl.burkan.feature.settings.ui

import io.github.barqallayl.burkan.awaitStateMatching
import io.github.barqallayl.burkan.core.model.ShellBackend
import io.github.barqallayl.burkan.core.shell.PackageName
import io.github.barqallayl.burkan.core.storage.FakeDeviceStateStorage
import io.github.barqallayl.burkan.core.storage.FakeSettingsStorage
import io.github.barqallayl.burkan.core.store.SettingsStore
import io.github.barqallayl.burkan.designsystem.PaletteStyles
import io.github.barqallayl.burkan.designsystem.SeedColors
import io.github.barqallayl.burkan.designsystem.ThemeMode
import io.github.barqallayl.burkan.feature.apply.FakeApplyLauncher
import io.github.barqallayl.burkan.feature.apply.FakeAutoApplyStorage
import io.github.barqallayl.burkan.feature.apply.FakeBootCount
import io.github.barqallayl.burkan.feature.apply.FakeLockEvents
import io.github.barqallayl.burkan.feature.apply.FakeRunAlerts
import io.github.barqallayl.burkan.feature.apply.FakeWifiWatch
import io.github.barqallayl.burkan.feature.apply.data.AutoApply
import io.github.barqallayl.burkan.feature.apply.model.AutoApplyState
import io.github.barqallayl.burkan.feature.apply.model.WaitReason
import io.github.barqallayl.burkan.feature.connection.data.FakeWirelessDebugging
import io.github.barqallayl.burkan.feature.settings.model.About
import io.github.barqallayl.burkan.feature.setup.data.ShellBackendSwitcher
import io.github.barqallayl.burkan.feature.setup.model.SHARED_STEPS
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import org.junit.Test
import org.orbitmvi.orbit.test.testWithInternalState
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class SettingsViewModelTest {

    private val settings = FakeSettingsStorage()
    private val deviceState = FakeDeviceStateStorage(paired = true, setupStepsDone = 7, setupComplete = true)
    private val autoApplyStorage = FakeAutoApplyStorage()
    private val wifiWatch = FakeWifiWatch()
    private val autoApply = AutoApply(
        settings,
        deviceState,
        autoApplyStorage,
        FakeWirelessDebugging(),
        wifiWatch,
        FakeApplyLauncher(),
        FakeRunAlerts(),
        FakeLockEvents(),
        FakeBootCount(),
    )

    private fun TestScope.viewModel() = SettingsViewModel(
        settings,
        SettingsStore(settings, deviceState, backgroundScope),
        deviceState,
        autoApply,
        ShellBackendSwitcher(settings, deviceState),
        version = "1.0",
    )

    @Test
    fun `the screen shows the stored settings and follows them`() = runTest {
        viewModel().testWithInternalState(this) {
            val reading = runOnCreate()
            val first = awaitStateMatching { it.values != null }
            assertEquals(true, first.values?.applyOnBoot)
            assertEquals(0, first.values?.exclusionCount)
            assertEquals("1.0", first.version)

            settings.userExclusions.value = setOf(PackageName.known("com.example.notes"))

            assertEquals(1, awaitStateMatching { it.values?.exclusionCount != 0 }.values?.exclusionCount)
            reading.cancel()
            cancelAndIgnoreRemainingItems()
        }
    }

    @Test
    fun `turning the automatic apply off stops any wait`() = runTest {
        autoApplyStorage.state.value = AutoApplyState(WaitReason.Wifi)
        wifiWatch.watching = true

        viewModel().testWithInternalState(this) {
            containerHost.setApplyOnBoot(false).join()
            cancelAndIgnoreRemainingItems()
        }

        assertFalse(settings.applyOnBoot.value)
        assertEquals(AutoApplyState(), autoApplyStorage.state.value)
        assertFalse(wifiWatch.watching)
    }

    @Test
    fun `turning it back on only stores it`() = runTest {
        settings.applyOnBoot.value = false
        autoApplyStorage.state.value = AutoApplyState(WaitReason.Wifi)

        viewModel().testWithInternalState(this) {
            containerHost.setApplyOnBoot(true).join()
            cancelAndIgnoreRemainingItems()
        }

        assertTrue(settings.applyOnBoot.value)
        assertEquals(AutoApplyState(WaitReason.Wifi), autoApplyStorage.state.value)
    }

    @Test
    fun `every appearance choice is stored, and leaves its sheet open`() = runTest {
        viewModel().testWithInternalState(this) {
            containerHost.show(SettingsDialog.ThemeMode).join()
            containerHost.setThemeMode(ThemeMode.Dark).join()
            assertEquals(SettingsDialog.ThemeMode, containerHost.container.stateFlow.value.dialog)
            containerHost.setSeedColor(SeedColors.Teal).join()
            containerHost.setPaletteStyle(PaletteStyles.Vibrant).join()
            containerHost.setTurnOffWirelessDebugging(false).join()
            containerHost.setAllowExternalTriggers(false).join()
            cancelAndIgnoreRemainingItems()
        }

        assertEquals(ThemeMode.Dark, settings.themeMode.value)
        assertEquals(SeedColors.Teal, settings.seedColor.value)
        assertEquals(PaletteStyles.Vibrant, settings.paletteStyle.value)
        assertFalse(settings.turnOffWirelessDebugging.value)
        assertFalse(settings.allowExternalTriggers.value)
    }

    @Test
    fun `redoing setup asks first, then forgets the pairing`() = runTest {
        viewModel().testWithInternalState(this) {
            containerHost.show(SettingsDialog.RedoSetup).join()
            assertTrue(deviceState.isSetupComplete.value, "nothing changes before the answer")

            containerHost.confirmRedoSetup().join()
            cancelAndIgnoreRemainingItems()
        }

        assertFalse(deviceState.isPaired.value)
        assertFalse(deviceState.isSetupComplete.value)
        assertEquals(0, deviceState.setupStepsDone.value)
    }

    @Test
    fun `switching to Shizuku asks first, then keeps only the setup steps both ways share`() = runTest {
        viewModel().testWithInternalState(this) {
            containerHost.show(SettingsDialog.SwitchShellBackend).join()
            assertEquals(ShellBackend.WirelessDebugging, settings.shellBackend.value, "nothing changes before the answer")
            assertTrue(deviceState.isSetupComplete.value)

            containerHost.confirmSwitchShellBackend().join()
            cancelAndIgnoreRemainingItems()
        }

        assertEquals(ShellBackend.Shizuku, settings.shellBackend.value)
        assertEquals(SHARED_STEPS, deviceState.setupStepsDone.value)
        assertFalse(deviceState.isSetupComplete.value, "so that App shows Setup, for the steps Shizuku has")
        assertTrue(deviceState.isPaired.value, "the pairing is kept, for the way back")
    }

    @Test
    fun `switching back to wireless debugging goes the same way`() = runTest {
        settings.shellBackend.value = ShellBackend.Shizuku
        deviceState.setupStepsDone.value = 5

        viewModel().testWithInternalState(this) {
            containerHost.confirmSwitchShellBackend().join()
            cancelAndIgnoreRemainingItems()
        }

        assertEquals(ShellBackend.WirelessDebugging, settings.shellBackend.value)
        assertEquals(SHARED_STEPS, deviceState.setupStepsDone.value)
        assertFalse(deviceState.isSetupComplete.value)
    }

    @Test
    fun `switching stops any wait for the way it leaves`() = runTest {
        autoApplyStorage.state.value = AutoApplyState(WaitReason.Wifi)
        wifiWatch.watching = true

        viewModel().testWithInternalState(this) {
            containerHost.confirmSwitchShellBackend().join()
            cancelAndIgnoreRemainingItems()
        }

        assertEquals(AutoApplyState(), autoApplyStorage.state.value)
        assertFalse(wifiWatch.watching)
    }

    @Test
    fun `a step done before the shared ones is not kept`() = runTest {
        deviceState.setupStepsDone.value = 0

        viewModel().testWithInternalState(this) {
            containerHost.confirmSwitchShellBackend().join()
            cancelAndIgnoreRemainingItems()
        }

        assertEquals(0, deviceState.setupStepsDone.value, "switching never counts a step as done")
    }

    @Test
    fun `dismissing the question changes nothing`() = runTest {
        viewModel().testWithInternalState(this) {
            containerHost.show(SettingsDialog.RedoSetup).join()
            containerHost.dismissDialog().join()
            cancelAndIgnoreRemainingItems()
        }

        assertTrue(deviceState.isPaired.value)
        assertTrue(deviceState.isSetupComplete.value)
    }

    @Test
    fun `the source link opens the repository`() = runTest {
        viewModel().testWithInternalState(this) {
            containerHost.openSource()
            expectSideEffect(SettingsSideEffect.OpenUrl(About.SOURCE_URL))
        }
    }

    @Test
    fun `reporting a problem opens the issue form in the repository`() = runTest {
        viewModel().testWithInternalState(this) {
            containerHost.reportProblem()
            expectSideEffect(SettingsSideEffect.OpenUrl(About.REPORT_URL))
        }
    }

    @Test
    fun `the open-source licences open on their own screen`() = runTest {
        viewModel().testWithInternalState(this) {
            containerHost.openLicences()
            expectSideEffect(SettingsSideEffect.OpenLicences)
        }
    }

    @Test
    fun `the licence link opens the licence in the repository`() = runTest {
        viewModel().testWithInternalState(this) {
            containerHost.openLicence()
            expectSideEffect(SettingsSideEffect.OpenUrl(About.LICENCE_URL))
        }
    }
}
