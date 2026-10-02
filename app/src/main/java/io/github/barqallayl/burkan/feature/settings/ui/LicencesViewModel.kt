package io.github.barqallayl.burkan.feature.settings.ui

import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import com.mikepenz.aboutlibraries.Libs
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesIntoMap
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.binding
import dev.zacsweers.metrox.viewmodel.ViewModelKey
import io.github.barqallayl.burkan.feature.settings.data.LibraryCatalogue
import org.orbitmvi.orbit.OrbitContainerHost
import org.orbitmvi.orbit.viewmodel.orbitContainer

/** [libraries] is null until read. */
@Immutable
data class LicencesState(val libraries: Libs? = null)

sealed interface LicencesSideEffect {
    data object Back : LicencesSideEffect
}

@Inject
@ViewModelKey
@ContributesIntoMap(AppScope::class, binding = binding<ViewModel>())
class LicencesViewModel(
    private val catalogue: LibraryCatalogue,
) : OrbitContainerHost<LicencesState, LicencesState, LicencesSideEffect>, ViewModel() {

    override val container = orbitContainer<LicencesState, LicencesSideEffect>(LicencesState()) {
        val libraries = catalogue.read()
        reduce { state.copy(libraries = libraries) }
    }

    fun back() = intent { postSideEffect(LicencesSideEffect.Back) }
}
