package io.github.barqallayl.burkan

import io.github.barqallayl.burkan.core.storage.SettingsStorage
import io.github.barqallayl.burkan.designsystem.ColorSpecs
import io.github.barqallayl.burkan.designsystem.PaletteStyles
import io.github.barqallayl.burkan.designsystem.SeedColors
import io.github.barqallayl.burkan.designsystem.ThemeMode
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import org.junit.Test
import org.orbitmvi.orbit.test.testWithInternalState

class AppViewModelTest {

    private val settings = FakeSettingsStorage()

    private val defaultAppearance = Appearance(
        themeMode = SettingsStorage.Defaults.themeMode,
        seedColor = SettingsStorage.Defaults.seedColor,
        paletteStyle = SettingsStorage.Defaults.paletteStyle,
        colorSpec = SettingsStorage.Defaults.colorSpec,
        textScalePercent = SettingsStorage.Defaults.TEXT_SCALE_PERCENT,
    )

    @Test
    fun `appearance is unknown until the preferences are read`() = runTest {
        AppViewModel(settings).testWithInternalState(this) {
            // The initial state, with no appearance, is checked on entry.
            val reading = runOnCreate()

            expectInternalState(AppState(appearance = defaultAppearance))
            reading.cancel()
        }
    }

    @Test
    fun `a change to any preference reaches the appearance`() = runTest {
        AppViewModel(settings).testWithInternalState(this) {
            val reading = runOnCreate()
            expectInternalState(AppState(appearance = defaultAppearance))

            var expected = defaultAppearance
            settings.themeMode.value = ThemeMode.Dark
            expected = expected.copy(themeMode = ThemeMode.Dark)
            expectInternalState(AppState(expected))

            settings.seedColor.value = SeedColors.Orange
            expected = expected.copy(seedColor = SeedColors.Orange)
            expectInternalState(AppState(expected))

            settings.paletteStyle.value = PaletteStyles.Vibrant
            expected = expected.copy(paletteStyle = PaletteStyles.Vibrant)
            expectInternalState(AppState(expected))

            settings.colorSpec.value = ColorSpecs.Spec2021
            expected = expected.copy(colorSpec = ColorSpecs.Spec2021)
            expectInternalState(AppState(expected))

            settings.textScalePercent.value = 120
            expected = expected.copy(textScalePercent = 120)
            expectInternalState(AppState(expected))
            reading.cancel()
        }
    }

    private class FakeSettingsStorage : SettingsStorage {
        override val themeMode = MutableStateFlow(SettingsStorage.Defaults.themeMode)
        override val seedColor = MutableStateFlow(SettingsStorage.Defaults.seedColor)
        override val paletteStyle = MutableStateFlow(SettingsStorage.Defaults.paletteStyle)
        override val colorSpec = MutableStateFlow(SettingsStorage.Defaults.colorSpec)
        override val textScalePercent = MutableStateFlow(SettingsStorage.Defaults.TEXT_SCALE_PERCENT)
    }
}
