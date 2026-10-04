package io.github.barqallayl.burkan.feature.settings.ui

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.ImageBitmap
import androidx.lifecycle.ViewModel
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesIntoMap
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.binding
import dev.zacsweers.metrox.viewmodel.ViewModelKey
import io.github.barqallayl.burkan.core.shell.PackageName
import io.github.barqallayl.burkan.core.storage.SettingsStorage
import io.github.barqallayl.burkan.core.store.SettingsStore
import io.github.barqallayl.burkan.feature.settings.data.AppIcons
import io.github.barqallayl.burkan.feature.settings.data.InstalledAppsStore
import io.github.barqallayl.burkan.feature.settings.model.AppList
import io.github.barqallayl.burkan.feature.settings.model.ExcludableApp
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filterNotNull
import org.orbitmvi.orbit.OrbitContainerHost
import org.orbitmvi.orbit.viewmodel.orbitContainer

/** [fixed] are the packages never restarted, labelled where installed; null until it is known which are. */
@Immutable
data class ExclusionsState(
    val apps: AppList = AppList.Loading,
    val excluded: Set<PackageName> = emptySet(),
    val fixed: List<ExcludableApp>? = null,
)

sealed interface ExclusionsSideEffect {
    data object Back : ExclusionsSideEffect
}

@Inject
@ViewModelKey
@ContributesIntoMap(AppScope::class, binding = binding<ViewModel>())
class ExclusionsViewModel(
    private val settings: SettingsStorage,
    private val store: SettingsStore,
    private val installedApps: InstalledAppsStore,
    private val icons: AppIcons,
) : OrbitContainerHost<ExclusionsState, ExclusionsState, ExclusionsSideEffect>, ViewModel() {

    override val container = orbitContainer<ExclusionsState, ExclusionsSideEffect>(
        ExclusionsState(
            apps = installedApps.apps.value,
            excluded = store.settings.value?.exclusions.orEmpty(),
            fixed = installedApps.fixed.value,
        ),
    ) {
        // What the store holds is shown at once; an app installed or removed since arrives a moment later.
        installedApps.refresh()
        combine(installedApps.apps, installedApps.fixed, store.settings.filterNotNull()) { apps, fixed, settings ->
            Triple(apps, fixed, settings.exclusions)
        }.collect { (apps, fixed, excluded) -> reduce { state.copy(apps = apps, fixed = fixed, excluded = excluded) } }
    }

    /** Lists the installed apps again, after a failure. */
    fun list() = intent { installedApps.refresh() }

    fun toggle(app: PackageName) = intent {
        val excluded = state.excluded
        settings.setUserExclusions(if (app in excluded) excluded - app else excluded + app)
    }

    /** An app's icon if it is already at hand. */
    fun cachedIcon(packageName: PackageName): ImageBitmap? = icons.cached(packageName)

    /** An app's icon, loaded when its row is shown. */
    suspend fun icon(packageName: PackageName): ImageBitmap? = icons.icon(packageName)

    fun back() = intent { postSideEffect(ExclusionsSideEffect.Back) }
}
