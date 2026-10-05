package io.github.barqallayl.burkan.feature.settings.ui

import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesIntoMap
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.Named
import dev.zacsweers.metro.binding
import dev.zacsweers.metrox.viewmodel.ViewModelKey
import io.github.barqallayl.burkan.Appearance
import io.github.barqallayl.burkan.core.di.AppBindings
import io.github.barqallayl.burkan.core.storage.DeviceStateStorage
import io.github.barqallayl.burkan.core.storage.SettingsStorage
import io.github.barqallayl.burkan.core.store.SettingsStore
import io.github.barqallayl.burkan.core.store.UserSettings
import io.github.barqallayl.burkan.designsystem.AppFont
import io.github.barqallayl.burkan.designsystem.AppStyle
import io.github.barqallayl.burkan.designsystem.PaletteStyles
import io.github.barqallayl.burkan.designsystem.SeedColors
import io.github.barqallayl.burkan.designsystem.ThemeMode
import io.github.barqallayl.burkan.feature.apply.data.AutoApply
import io.github.barqallayl.burkan.feature.settings.model.About
import kotlinx.coroutines.flow.filterNotNull
import org.orbitmvi.orbit.OrbitContainerHost
import org.orbitmvi.orbit.viewmodel.orbitContainer

@Immutable
data class SettingsValues(
    val applyOnBoot: Boolean,
    val turnOffWirelessDebugging: Boolean,
    val exclusionCount: Int,
    val appearance: Appearance,
)

/** The sheets and the dialog Settings can show; one at a time. */
enum class SettingsDialog {
    Style,
    ThemeMode,
    SeedColor,
    PaletteStyle,
    Font,
    TextSize,
    RedoSetup,
}

/** [values] is null until the settings have been read, which the store has usually done before the screen opens. */
@Immutable
data class SettingsState(
    val values: SettingsValues? = null,
    val dialog: SettingsDialog? = null,
    val version: String = "",
)

sealed interface SettingsSideEffect {
    data object Back : SettingsSideEffect
    data object OpenExclusions : SettingsSideEffect
    data object OpenLicences : SettingsSideEffect
    data class OpenUrl(val url: String) : SettingsSideEffect
}

@Inject
@ViewModelKey
@ContributesIntoMap(AppScope::class, binding = binding<ViewModel>())
class SettingsViewModel(
    private val settings: SettingsStorage,
    private val store: SettingsStore,
    private val deviceState: DeviceStateStorage,
    private val autoApply: AutoApply,
    @Named(AppBindings.APP_VERSION) version: String,
) : OrbitContainerHost<SettingsState, SettingsState, SettingsSideEffect>, ViewModel() {

    override val container = orbitContainer<SettingsState, SettingsSideEffect>(
        SettingsState(values = store.settings.value?.toValues(), version = version),
    ) {
        store.settings.filterNotNull()
            .collect { settings -> reduce { state.copy(values = settings.toValues()) } }
    }

    private fun UserSettings.toValues() =
        SettingsValues(applyOnBoot, turnOffWirelessDebugging, exclusions.size, appearance)

    fun setApplyOnBoot(enabled: Boolean) = intent {
        settings.setApplyOnBoot(enabled)
        // Turned off, nothing should still be waiting to run.
        if (!enabled) autoApply.stopWaiting()
    }

    fun setTurnOffWirelessDebugging(enabled: Boolean) =
        intent { settings.setTurnOffWirelessDebugging(enabled) }

    fun openExclusions() = intent { postSideEffect(SettingsSideEffect.OpenExclusions) }

    fun show(dialog: SettingsDialog) = intent { reduce { state.copy(dialog = dialog) } }

    fun dismissDialog() = intent { reduce { state.copy(dialog = null) } }

    // The appearance sheets stay open after a choice: the app changes behind them, so the choice can be compared.

    fun setAppStyle(style: AppStyle) = intent { settings.setAppStyle(style) }

    fun setThemeMode(mode: ThemeMode) = intent { settings.setThemeMode(mode) }

    fun setSeedColor(color: SeedColors) = intent { settings.setSeedColor(color) }

    fun setPaletteStyle(style: PaletteStyles) = intent { settings.setPaletteStyle(style) }

    fun setAppFont(font: AppFont) = intent { settings.setAppFont(font) }

    /** The text size is chosen on a sample and saved from its sheet, which then closes. */
    fun setTextScalePercent(percent: Int) = intent {
        reduce { state.copy(dialog = null) }
        settings.setTextScalePercent(percent)
    }

    /** Forgets the pairing and leaves setup unfinished; `App.kt` then shows Setup. */
    fun confirmRedoSetup() = intent {
        reduce { state.copy(dialog = null) }
        autoApply.stopWaiting()
        deviceState.setPaired(false)
        deviceState.setSetupComplete(false)
    }

    fun openSource() = intent { postSideEffect(SettingsSideEffect.OpenUrl(About.SOURCE_URL)) }

    fun openLicence() = intent { postSideEffect(SettingsSideEffect.OpenUrl(About.LICENCE_URL)) }

    fun openLicences() = intent { postSideEffect(SettingsSideEffect.OpenLicences) }

    fun back() = intent { postSideEffect(SettingsSideEffect.Back) }
}
