package io.github.barqallayl.burkan.feature.settings.ui

import io.github.barqallayl.burkan.awaitStateMatching
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
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import org.junit.Test
import org.orbitmvi.orbit.test.testWithInternalState
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class SettingsViewModelTest {

    private val settings = FakeSettingsStorage()
    private val deviceState = FakeDeviceStateStorage(paired = true, setupComplete = true)
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
            containerHost.setTextScalePercent(115).join()
            containerHost.setTurnOffWirelessDebugging(false).join()
            cancelAndIgnoreRemainingItems()
        }

        assertEquals(ThemeMode.Dark, settings.themeMode.value)
        assertEquals(SeedColors.Teal, settings.seedColor.value)
        assertEquals(PaletteStyles.Vibrant, settings.paletteStyle.value)
        assertEquals(115, settings.textScalePercent.value)
        assertFalse(settings.turnOffWirelessDebugging.value)
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
