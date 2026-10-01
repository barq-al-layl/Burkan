package io.github.barqallayl.burkan.feature.settings.ui

import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesIntoMap
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.binding
import dev.zacsweers.metrox.viewmodel.ViewModelKey
import io.github.barqallayl.burkan.core.shell.PackageName
import io.github.barqallayl.burkan.core.storage.SettingsStorage
import io.github.barqallayl.burkan.feature.apply.data.FullApplyPlan
import io.github.barqallayl.burkan.feature.settings.data.InstalledApps
import io.github.barqallayl.burkan.feature.settings.model.ExcludableApp
import kotlinx.coroutines.flow.first
import org.orbitmvi.orbit.OrbitContainerHost
import org.orbitmvi.orbit.viewmodel.orbitContainer

/** [apps] is null until the installed apps have been listed. */
@Immutable
data class ExclusionsState(
    val apps: List<ExcludableApp>? = null,
    val excluded: Set<PackageName> = emptySet(),
    val fixed: List<PackageName> = FullApplyPlan.FixedExclusions,
)

sealed interface ExclusionsSideEffect {
    data object Back : ExclusionsSideEffect
}

@Inject
@ViewModelKey
@ContributesIntoMap(AppScope::class, binding = binding<ViewModel>())
class ExclusionsViewModel(
    private val settings: SettingsStorage,
    private val installedApps: InstalledApps,
) : OrbitContainerHost<ExclusionsState, ExclusionsState, ExclusionsSideEffect>, ViewModel() {

    override val container = orbitContainer<ExclusionsState, ExclusionsSideEffect>(ExclusionsState()) {
        val installed = installedApps.launchable()
        // An app excluded earlier and since uninstalled stays listed, so it can be removed.
        val missing = settings.userExclusions.first()
            .filter { excluded -> installed.none { it.packageName == excluded } }
            .map { ExcludableApp(it, label = null) }
        val apps = (installed + missing).sortedWith(
            compareBy<ExcludableApp> { (it.label ?: it.packageName.value).lowercase() }.thenBy { it.packageName.value },
        )
        reduce { state.copy(apps = apps) }
        settings.userExclusions.collect { excluded -> reduce { state.copy(excluded = excluded) } }
    }

    fun toggle(app: PackageName) = intent {
        val excluded = state.excluded
        settings.setUserExclusions(if (app in excluded) excluded - app else excluded + app)
    }

    fun back() = intent { postSideEffect(ExclusionsSideEffect.Back) }
}
