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
import io.github.barqallayl.burkan.designsystem.ColorSpecs
import io.github.barqallayl.burkan.designsystem.PaletteStyles
import io.github.barqallayl.burkan.designsystem.SeedColors
import io.github.barqallayl.burkan.designsystem.ThemeMode
import io.github.barqallayl.burkan.feature.apply.data.AutoApply
import io.github.barqallayl.burkan.feature.settings.model.About
import kotlinx.coroutines.flow.combine
import org.orbitmvi.orbit.OrbitContainerHost
import org.orbitmvi.orbit.viewmodel.orbitContainer

@Immutable
data class SettingsValues(
    val applyOnBoot: Boolean,
    val turnOffWirelessDebugging: Boolean,
    val exclusionCount: Int,
    val appearance: Appearance,
)

/** The dialogs Settings can show; one at a time. */
enum class SettingsDialog {
    ThemeMode,
    SeedColor,
    PaletteStyle,
    ColorSpec,
    RedoSetup,
}

/** [values] is null until the settings have been read. */
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
    private val deviceState: DeviceStateStorage,
    private val autoApply: AutoApply,
    @Named(AppBindings.APP_VERSION) version: String,
) : OrbitContainerHost<SettingsState, SettingsState, SettingsSideEffect>, ViewModel() {

    override val container = orbitContainer<SettingsState, SettingsSideEffect>(SettingsState(version = version)) {
        val appearance = combine(
            settings.themeMode,
            settings.seedColor,
            settings.paletteStyle,
            settings.colorSpec,
            settings.textScalePercent,
            ::Appearance,
        )
        combine(
            settings.applyOnBoot,
            settings.turnOffWirelessDebugging,
            settings.userExclusions,
            appearance,
        ) { applyOnBoot, turnOff, exclusions, look ->
            SettingsValues(applyOnBoot, turnOff, exclusions.size, look)
        }.collect { values -> reduce { state.copy(values = values) } }
    }

    fun setApplyOnBoot(enabled: Boolean) = intent {
        settings.setApplyOnBoot(enabled)
        // Turned off, nothing should still be waiting to run.
        if (!enabled) autoApply.stopWaiting()
    }

    fun setTurnOffWirelessDebugging(enabled: Boolean) = intent { settings.setTurnOffWirelessDebugging(enabled) }

    fun openExclusions() = intent { postSideEffect(SettingsSideEffect.OpenExclusions) }

    fun show(dialog: SettingsDialog) = intent { reduce { state.copy(dialog = dialog) } }

    fun dismissDialog() = intent { reduce { state.copy(dialog = null) } }

    fun setThemeMode(mode: ThemeMode) = intent {
        reduce { state.copy(dialog = null) }
        settings.setThemeMode(mode)
    }

    fun setSeedColor(color: SeedColors) = intent {
        reduce { state.copy(dialog = null) }
        settings.setSeedColor(color)
    }

    fun setPaletteStyle(style: PaletteStyles) = intent {
        reduce { state.copy(dialog = null) }
        settings.setPaletteStyle(style)
    }

    fun setColorSpec(spec: ColorSpecs) = intent {
        reduce { state.copy(dialog = null) }
        settings.setColorSpec(spec)
    }

    fun setTextScalePercent(percent: Int) = intent { settings.setTextScalePercent(percent) }

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
