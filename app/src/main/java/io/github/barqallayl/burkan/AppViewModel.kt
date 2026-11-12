package io.github.barqallayl.burkan

import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesIntoMap
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.binding
import dev.zacsweers.metrox.viewmodel.ViewModelKey
import io.github.barqallayl.burkan.core.store.DeviceState
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
 * has them they are there from the first state. [opening] chooses the start destination.
 */
@Immutable
data class AppState(val appearance: Appearance? = null, val opening: Opening? = null)

/** Where the app opens: getting started, which ends after the First apply, or Home. */
@Immutable
data class Opening(
    /** Welcome, Setup or the First apply: the screens a phone goes through before Home. */
    val isGettingStarted: Boolean,
    /** A fresh install that has not yet left Welcome. */
    val showsWelcome: Boolean,
    /** Every setup step is done; while still getting started, the First apply is on offer. */
    val isSetupComplete: Boolean = false,
)

/** Where a phone in [this] state opens. Welcome is for a fresh install alone: not an update, not "Redo setup". */
fun DeviceState.opening(): Opening = Opening(
    isGettingStarted = !isSetupComplete || isFirstApplyPending,
    showsWelcome = !isWelcomeSeen && !isSetupComplete && setupStepsDone == 0,
    isSetupComplete = isSetupComplete,
)

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
        AppState(store.settings.value?.appearance, store.device.value?.opening()),
    ) {
        combine(store.settings.filterNotNull(), store.device.filterNotNull()) { settings, device ->
            AppState(settings.appearance, device.opening())
        }.collect { appState -> reduce { appState } }
    }
}
