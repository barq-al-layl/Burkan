package io.github.barqallayl.burkan.feature.settings.data

import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.Named
import dev.zacsweers.metro.SingleIn
import io.github.barqallayl.burkan.core.di.AppBindings
import io.github.barqallayl.burkan.core.storage.SettingsStorage
import io.github.barqallayl.burkan.feature.apply.data.FullApplyPlan
import io.github.barqallayl.burkan.feature.settings.model.AppList
import io.github.barqallayl.burkan.feature.settings.model.ExcludableApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/**
 * The apps the user can keep out of a full apply. Listing a few hundred apps with their names takes a moment, so
 * it is done ahead of the screen that shows them and the answer is kept.
 */
@Inject
@SingleIn(AppScope::class)
class InstalledAppsStore(
    private val installedApps: InstalledApps,
    private val settings: SettingsStorage,
    @Named(AppBindings.APP_SCOPE) private val scope: CoroutineScope,
) {
    private val _apps = MutableStateFlow<AppList>(AppList.Loading)
    val apps: StateFlow<AppList> = _apps.asStateFlow()

    private val _fixed = MutableStateFlow<List<ExcludableApp>?>(null)

    /** The packages never restarted, labelled where installed; null until it is known which are. */
    val fixed: StateFlow<List<ExcludableApp>?> = _fixed.asStateFlow()

    private var refreshing: Job? = null

    /**
     * Lists the apps again: one may have been installed or removed since. A list already here stays in place until
     * the new one replaces it; only a failure goes back to loading.
     */
    fun refresh(): Job {
        refreshing?.takeIf { it.isActive }?.let { return it }
        return scope.launch {
            if (_apps.value is AppList.Failed) _apps.value = AppList.Loading
            _fixed.value = installedApps.describe(FullApplyPlan.FixedExclusions)
            _apps.value = installedApps.launchable().fold(
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
        }.also { refreshing = it }
    }
}
