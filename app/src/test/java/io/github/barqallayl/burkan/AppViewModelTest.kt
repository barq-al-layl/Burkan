package io.github.barqallayl.burkan

import io.github.barqallayl.burkan.core.storage.FakeDeviceStateStorage
import io.github.barqallayl.burkan.core.storage.FakeSettingsStorage
import io.github.barqallayl.burkan.core.storage.SettingsStorage
import io.github.barqallayl.burkan.designsystem.ColorSpecs
import io.github.barqallayl.burkan.designsystem.PaletteStyles
import io.github.barqallayl.burkan.designsystem.SeedColors
import io.github.barqallayl.burkan.designsystem.ThemeMode
import kotlinx.coroutines.test.runTest
import org.junit.Test
import org.orbitmvi.orbit.test.testWithInternalState

class AppViewModelTest {

    private val settings = FakeSettingsStorage()
    private val deviceState = FakeDeviceStateStorage()

    private val defaultAppearance = Appearance(
        themeMode = SettingsStorage.Defaults.themeMode,
        seedColor = SettingsStorage.Defaults.seedColor,
        paletteStyle = SettingsStorage.Defaults.paletteStyle,
        colorSpec = SettingsStorage.Defaults.colorSpec,
        textScalePercent = SettingsStorage.Defaults.TEXT_SCALE_PERCENT,
    )

    @Test
    fun `finishing setup reaches the app, which then starts at Home`() = runTest {
        AppViewModel(settings, deviceState).testWithInternalState(this) {
            val reading = runOnCreate()
            expectInternalState(AppState(appearance = defaultAppearance, isSetupComplete = false))

            deviceState.isSetupComplete.value = true

            expectInternalState(AppState(appearance = defaultAppearance, isSetupComplete = true))
            reading.cancel()
        }
    }

    @Test
    fun `appearance and setup are unknown until read`() = runTest {
        AppViewModel(settings, deviceState).testWithInternalState(this) {
            // The initial state, with no appearance, is checked on entry.
            val reading = runOnCreate()

            expectInternalState(AppState(appearance = defaultAppearance, isSetupComplete = false))
            reading.cancel()
        }
    }

    @Test
    fun `a change to any preference reaches the appearance`() = runTest {
        AppViewModel(settings, deviceState).testWithInternalState(this) {
            val reading = runOnCreate()
            expectInternalState(AppState(appearance = defaultAppearance, isSetupComplete = false))

            var expected = defaultAppearance
            settings.themeMode.value = ThemeMode.Dark
            expected = expected.copy(themeMode = ThemeMode.Dark)
            expectInternalState(AppState(expected, isSetupComplete = false))

            settings.seedColor.value = SeedColors.Orange
            expected = expected.copy(seedColor = SeedColors.Orange)
            expectInternalState(AppState(expected, isSetupComplete = false))

            settings.paletteStyle.value = PaletteStyles.Vibrant
            expected = expected.copy(paletteStyle = PaletteStyles.Vibrant)
            expectInternalState(AppState(expected, isSetupComplete = false))

            settings.colorSpec.value = ColorSpecs.Spec2021
            expected = expected.copy(colorSpec = ColorSpecs.Spec2021)
            expectInternalState(AppState(expected, isSetupComplete = false))

            settings.textScalePercent.value = 120
            expected = expected.copy(textScalePercent = 120)
            expectInternalState(AppState(expected, isSetupComplete = false))
            reading.cancel()
        }
    }
}
