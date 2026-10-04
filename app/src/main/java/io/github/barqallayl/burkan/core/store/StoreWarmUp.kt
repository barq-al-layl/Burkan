package io.github.barqallayl.burkan.core.store

import dev.zacsweers.metro.Inject
import io.github.barqallayl.burkan.feature.log.data.RunLogStore
import io.github.barqallayl.burkan.feature.settings.data.InstalledAppsStore
import io.github.barqallayl.burkan.feature.settings.data.LibraryStore

/**
 * Fills the stores the screens read from. Called once, when the activity is created, so the data is there by the
 * time a screen asks for it. The services never call it: a run after a restart has no use for the list of apps.
 */
@Inject
class StoreWarmUp(
    // Asked for so that they exist: these two start reading as they are created.
    @Suppress("unused") private val settings: SettingsStore,
    @Suppress("unused") private val runLog: RunLogStore,
    private val installedApps: InstalledAppsStore,
    private val libraries: LibraryStore,
) {
    fun start() {
        installedApps.refresh()
        libraries.load()
    }
}
