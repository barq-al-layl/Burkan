package io.github.barqallayl.burkan

import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesIntoMap
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.binding
import dev.zacsweers.metrox.viewmodel.ViewModelKey
import io.github.barqallayl.burkan.core.store.SettingsStore
import io.github.barqallayl.burkan.core.storage.SettingsStorage
import io.github.barqallayl.burkan.designsystem.AppStyle
import io.github.barqallayl.burkan.designsystem.PaletteStyles
import io.github.barqallayl.burkan.designsystem.SeedColors
import io.github.barqallayl.burkan.designsystem.ThemeMode
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filterNotNull
import org.orbitmvi.orbit.OrbitContainerHost
import org.orbitmvi.orbit.viewmodel.orbitContainer

/**
 * Both are null until read, so the first frame is never in the wrong theme or on the wrong screen; once the store
 * has them they are there from the first state. [isSetupComplete] chooses the start destination.
 */
@Immutable
data class AppState(val appearance: Appearance? = null, val isSetupComplete: Boolean? = null)

@Immutable
data class Appearance(
    val themeMode: ThemeMode,
    val seedColor: SeedColors,
    val paletteStyle: PaletteStyles,
    val appStyle: AppStyle = AppStyle.Material,
)

/** The appearance preferences as one value, for the composition root and for Settings. */
fun SettingsStorage.appearance(): Flow<Appearance> =
    combine(themeMode, seedColor, paletteStyle, appStyle, ::Appearance)

@Inject
@ViewModelKey
@ContributesIntoMap(AppScope::class, binding = binding<ViewModel>())
class AppViewModel(
    private val store: SettingsStore,
) : OrbitContainerHost<AppState, AppState, Nothing>, ViewModel() {

    override val container = orbitContainer<AppState, Nothing>(
        AppState(store.settings.value?.appearance, store.device.value?.isSetupComplete),
    ) {
        combine(store.settings.filterNotNull(), store.device.filterNotNull()) { settings, device ->
            AppState(settings.appearance, device.isSetupComplete)
        }.collect { appState -> reduce { appState } }
    }
}
