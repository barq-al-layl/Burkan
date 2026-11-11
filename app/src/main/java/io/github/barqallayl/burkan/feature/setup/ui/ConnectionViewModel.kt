package io.github.barqallayl.burkan.feature.setup.ui

import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesIntoMap
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.binding
import dev.zacsweers.metrox.viewmodel.ViewModelKey
import io.github.barqallayl.burkan.core.model.ShellBackend
import io.github.barqallayl.burkan.core.storage.SettingsStorage
import io.github.barqallayl.burkan.core.store.SettingsStore
import io.github.barqallayl.burkan.feature.setup.data.ShellBackendSwitcher
import org.orbitmvi.orbit.OrbitContainerHost
import org.orbitmvi.orbit.viewmodel.orbitContainer

/** The connection [selected] on the screen, which starts as the one in use: wireless debugging on a fresh install. */
@Immutable
data class ConnectionState(val selected: ShellBackend = SettingsStorage.Defaults.shellBackend)

sealed interface ConnectionSideEffect {
    data object StartSetup : ConnectionSideEffect
}

@Inject
@ViewModelKey
@ContributesIntoMap(AppScope::class, binding = binding<ViewModel>())
class ConnectionViewModel(
    store: SettingsStore,
    private val switcher: ShellBackendSwitcher,
) : OrbitContainerHost<ConnectionState, ConnectionState, ConnectionSideEffect>, ViewModel() {

    override val container = orbitContainer<ConnectionState, ConnectionSideEffect>(
        ConnectionState(store.settings.value?.shellBackend ?: SettingsStorage.Defaults.shellBackend),
    )

    fun select(backend: ShellBackend) = intent { reduce { state.copy(selected = backend) } }

    /** Continue: the chosen connection is the one Setup's steps are for. Nothing is stored until now. */
    fun confirm() = intent {
        switcher.choose(state.selected)
        postSideEffect(ConnectionSideEffect.StartSetup)
    }
}
