package io.github.barqallayl.burkan.core.navigation

import androidx.navigation3.runtime.NavEntry
import io.github.barqallayl.burkan.feature.log.ui.LogScreen
import io.github.barqallayl.burkan.feature.settings.ui.ExclusionsScreen
import io.github.barqallayl.burkan.feature.settings.ui.LicencesScreen
import io.github.barqallayl.burkan.feature.settings.ui.SettingsScreen
import io.github.barqallayl.burkan.feature.setup.ui.SetupScreen
import io.github.barqallayl.burkan.feature.status.ui.HomeScreen

/** The one entry provider: every route to its screen. The `when` is exhaustive, so a new route must be added here. */
fun appEntryProvider(route: Route): NavEntry<Route> = NavEntry(route) { key ->
    when (key) {
        SetupRoute -> SetupScreen()
        HomeRoute -> HomeScreen()
        SettingsRoute -> SettingsScreen()
        LogRoute -> LogScreen()
        ExclusionsRoute -> ExclusionsScreen()
        LicencesRoute -> LicencesScreen()
    }
}
