package io.github.barqallayl.burkan

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.lifecycle.viewmodel.navigation3.rememberViewModelStoreNavEntryDecorator
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
import androidx.navigation3.ui.NavDisplay
import dev.zacsweers.metrox.viewmodel.LocalMetroViewModelFactory
import dev.zacsweers.metrox.viewmodel.MetroViewModelFactory
import dev.zacsweers.metrox.viewmodel.metroViewModel
import io.github.barqallayl.burkan.core.navigation.LocalNavigator
import io.github.barqallayl.burkan.core.navigation.SetupRoute
import io.github.barqallayl.burkan.core.navigation.appEntryProvider
import io.github.barqallayl.burkan.core.navigation.rememberNavigator
import io.github.barqallayl.burkan.designsystem.BurkanTheme
import io.github.barqallayl.burkan.designsystem.ProvideTextScale
import io.github.barqallayl.burkan.designsystem.isDark
import org.orbitmvi.orbit.compose.collectAsState

/** The composition root: appearance, text size and navigation, each applied once, here. */
@Composable
fun App(viewModelFactory: MetroViewModelFactory) {
    CompositionLocalProvider(LocalMetroViewModelFactory provides viewModelFactory) {
        val viewModel = metroViewModel<AppViewModel>()
        val state by viewModel.collectAsState()
        val appearance = state.appearance ?: return@CompositionLocalProvider

        ProvideTextScale(appearance.textScalePercent) {
            BurkanTheme(
                isDarkTheme = appearance.themeMode.isDark(),
                seedColor = appearance.seedColor.color,
                paletteStyle = appearance.paletteStyle.style,
                specVersion = appearance.colorSpec.version,
            ) {
                AppNavigation()
            }
        }
    }
}

@Composable
private fun AppNavigation() {
    // Setup is never complete until M2 builds it; then this chooses between SetupRoute and HomeRoute.
    val navigator = rememberNavigator(start = SetupRoute)
    CompositionLocalProvider(LocalNavigator provides navigator) {
        NavDisplay(
            backStack = navigator.entries,
            onBack = navigator::pop,
            entryDecorators = listOf(
                rememberSaveableStateHolderNavEntryDecorator(),
                rememberViewModelStoreNavEntryDecorator(),
            ),
            entryProvider = ::appEntryProvider,
        )
    }
}
