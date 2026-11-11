package io.github.barqallayl.burkan

import io.github.barqallayl.burkan.core.storage.FakeDeviceStateStorage
import io.github.barqallayl.burkan.core.storage.FakeSettingsStorage
import io.github.barqallayl.burkan.core.storage.SettingsStorage
import io.github.barqallayl.burkan.core.store.DeviceState
import io.github.barqallayl.burkan.core.store.SettingsStore
import io.github.barqallayl.burkan.designsystem.PaletteStyles
import io.github.barqallayl.burkan.designsystem.SeedColors
import io.github.barqallayl.burkan.designsystem.ThemeMode
import kotlinx.coroutines.test.runTest
import org.junit.Test
import org.orbitmvi.orbit.test.testWithInternalState
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class AppViewModelTest {

    private val settings = FakeSettingsStorage()
    private val deviceState = FakeDeviceStateStorage()

    /** A fresh install: getting started, on Welcome. */
    private val fresh = Opening(isGettingStarted = true, showsWelcome = true)

    private val defaultAppearance = Appearance(
        themeMode = SettingsStorage.Defaults.themeMode,
        seedColor = SettingsStorage.Defaults.seedColor,
        paletteStyle = SettingsStorage.Defaults.paletteStyle,
    )

    @Test
    fun `finishing setup reaches the app, which then starts at Home`() = runTest {
        deviceState.isWelcomeSeen.value = true
        deviceState.setupStepsDone.value = 6
        AppViewModel(SettingsStore(settings, deviceState, backgroundScope)).testWithInternalState(this) {
            val reading = runOnCreate()
            expectInternalState(
                AppState(defaultAppearance, Opening(isGettingStarted = true, showsWelcome = false)),
            )

            deviceState.isSetupComplete.value = true

            expectInternalState(
                AppState(
                    appearance = defaultAppearance,
                    opening = Opening(isGettingStarted = false, showsWelcome = false),
                ),
            )
            reading.cancel()
        }
    }

    @Test
    fun `appearance and setup are unknown until read`() = runTest {
        AppViewModel(SettingsStore(settings, deviceState, backgroundScope)).testWithInternalState(this) {
            // The initial state, with no appearance, is checked on entry.
            val reading = runOnCreate()

            expectInternalState(AppState(appearance = defaultAppearance, opening = fresh))
            reading.cancel()
        }
    }

    @Test
    fun `a change to any preference reaches the appearance`() = runTest {
        AppViewModel(SettingsStore(settings, deviceState, backgroundScope)).testWithInternalState(this) {
            val reading = runOnCreate()
            expectInternalState(AppState(appearance = defaultAppearance, opening = fresh))

            var expected = defaultAppearance
            settings.themeMode.value = ThemeMode.Dark
            expected = expected.copy(themeMode = ThemeMode.Dark)
            expectInternalState(AppState(expected, opening = fresh))

            settings.seedColor.value = SeedColors.Orange
            expected = expected.copy(seedColor = SeedColors.Orange)
            expectInternalState(AppState(expected, opening = fresh))

            settings.paletteStyle.value = PaletteStyles.Vibrant
            expected = expected.copy(paletteStyle = PaletteStyles.Vibrant)
            expectInternalState(AppState(expected, opening = fresh))

            reading.cancel()
        }
    }

    @Test
    fun `a fresh install opens on Welcome, and nothing else does`() {
        val fresh = DeviceState(isPaired = false, setupStepsDone = 0, isSetupComplete = false, isWelcomeSeen = false)
        assertTrue(fresh.opening().showsWelcome)
        assertFalse(fresh.copy(isWelcomeSeen = true).opening().showsWelcome, "left Welcome already")
        assertFalse(fresh.copy(setupStepsDone = 2).opening().showsWelcome, "updated part way through setup")
        assertFalse(fresh.copy(isSetupComplete = true).opening().showsWelcome, "updated with setup done")
    }

    @Test
    fun `getting started lasts until the First apply is taken or put off`() {
        val done = DeviceState(isPaired = true, setupStepsDone = 6, isSetupComplete = true, isWelcomeSeen = true)
        assertTrue(done.copy(isFirstApplyPending = true).opening().isGettingStarted)
        assertFalse(done.opening().isGettingStarted)
        assertTrue(done.copy(isSetupComplete = false).opening().isGettingStarted)
    }
}
