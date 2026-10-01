package io.github.barqallayl.burkan

import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesIntoMap
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.binding
import dev.zacsweers.metrox.viewmodel.ViewModelKey
import io.github.barqallayl.burkan.core.storage.DeviceStateStorage
import io.github.barqallayl.burkan.core.storage.SettingsStorage
import io.github.barqallayl.burkan.designsystem.ColorSpecs
import io.github.barqallayl.burkan.designsystem.PaletteStyles
import io.github.barqallayl.burkan.designsystem.SeedColors
import io.github.barqallayl.burkan.designsystem.ThemeMode
import kotlinx.coroutines.flow.combine
import org.orbitmvi.orbit.OrbitContainerHost
import org.orbitmvi.orbit.viewmodel.orbitContainer

/**
 * Both are null until read, so the first frame is never in the wrong theme or on the wrong screen.
 * [isSetupComplete] chooses the start destination.
 */
@Immutable
data class AppState(val appearance: Appearance? = null, val isSetupComplete: Boolean? = null)

@Immutable
data class Appearance(
    val themeMode: ThemeMode,
    val seedColor: SeedColors,
    val paletteStyle: PaletteStyles,
    val colorSpec: ColorSpecs,
    val textScalePercent: Int,
)

@Inject
@ViewModelKey
@ContributesIntoMap(AppScope::class, binding = binding<ViewModel>())
class AppViewModel(
    private val settings: SettingsStorage,
    private val deviceState: DeviceStateStorage,
) : OrbitContainerHost<AppState, AppState, Nothing>, ViewModel() {

    override val container = orbitContainer<AppState, Nothing>(AppState()) {
        val appearance = combine(
            settings.themeMode,
            settings.seedColor,
            settings.paletteStyle,
            settings.colorSpec,
            settings.textScalePercent,
            ::Appearance,
        )
        combine(appearance, deviceState.isSetupComplete, ::AppState).collect { appState ->
            reduce { appState }
        }
    }
}
