package io.github.barqallayl.burkan.feature.settings.ui

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.ImageBitmap
import androidx.lifecycle.ViewModel
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesIntoMap
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.binding
import dev.zacsweers.metrox.viewmodel.ViewModelKey
import io.github.barqallayl.burkan.core.model.AppError
import io.github.barqallayl.burkan.core.shell.PackageName
import io.github.barqallayl.burkan.core.storage.SettingsStorage
import io.github.barqallayl.burkan.feature.apply.data.FullApplyPlan
import io.github.barqallayl.burkan.feature.settings.data.AppIcons
import io.github.barqallayl.burkan.feature.settings.data.InstalledApps
import io.github.barqallayl.burkan.feature.settings.model.ExcludableApp
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.first
import org.orbitmvi.orbit.OrbitContainerHost
import org.orbitmvi.orbit.viewmodel.orbitContainer

/** The installed apps the user can choose from. */
sealed interface AppList {
    data object Loading : AppList
    data class Loaded(val apps: List<ExcludableApp>) : AppList
    data class Failed(val error: AppError) : AppList
}

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
    private val installedApps: InstalledApps,
    private val icons: AppIcons,
) : OrbitContainerHost<ExclusionsState, ExclusionsState, ExclusionsSideEffect>, ViewModel() {

    override val container = orbitContainer<ExclusionsState, ExclusionsSideEffect>(ExclusionsState()) {
        val fixed = installedApps.describe(FullApplyPlan.FixedExclusions)
        reduce { state.copy(fixed = fixed) }
        list().join()
        settings.userExclusions.collect { excluded -> reduce { state.copy(excluded = excluded) } }
    }

    /** Lists the installed apps, again after a failure. */
    fun list(): Job = intent {
        reduce { state.copy(apps = AppList.Loading) }
        val apps = installedApps.launchable().fold(
            ifLeft = { AppList.Failed(it) },
            ifRight = { installed ->
                // An app excluded earlier and since uninstalled stays listed, so it can be removed.
                val missing = settings.userExclusions.first()
                    .filter { excluded -> installed.none { it.packageName == excluded } }
                    .map { ExcludableApp(it, label = null) }
                AppList.Loaded(
                    (installed + missing).sortedWith(
                        compareBy<ExcludableApp> { (it.label ?: it.packageName.value).lowercase() }
                            .thenBy { it.packageName.value },
                    ),
                )
            },
        )
        reduce { state.copy(apps = apps) }
    }

    fun toggle(app: PackageName) = intent {
        val excluded = state.excluded
        settings.setUserExclusions(if (app in excluded) excluded - app else excluded + app)
    }

    /** An app's icon, loaded when its row is shown. */
    suspend fun icon(packageName: PackageName): ImageBitmap? = icons.icon(packageName)

    fun back() = intent { postSideEffect(ExclusionsSideEffect.Back) }
}
