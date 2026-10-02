package io.github.barqallayl.burkan.feature.settings.ui

import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import com.mikepenz.aboutlibraries.Libs
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesIntoMap
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.binding
import dev.zacsweers.metrox.viewmodel.ViewModelKey
import io.github.barqallayl.burkan.core.model.AppError
import io.github.barqallayl.burkan.feature.settings.data.LibraryCatalogue
import org.orbitmvi.orbit.OrbitContainerHost
import org.orbitmvi.orbit.viewmodel.orbitContainer

/** [libraries] is null until read, and stays null when [error] says it could not be. */
@Immutable
data class LicencesState(val libraries: Libs? = null, val error: AppError? = null)

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
        val read = catalogue.read()
        reduce { state.copy(libraries = read.getOrNull(), error = read.leftOrNull()) }
    }

    fun back() = intent { postSideEffect(LicencesSideEffect.Back) }
}
