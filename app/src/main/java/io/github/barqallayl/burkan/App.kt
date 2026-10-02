package io.github.barqallayl.burkan

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalDensity
import androidx.lifecycle.viewmodel.navigation3.rememberViewModelStoreNavEntryDecorator
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
import androidx.navigation3.ui.NavDisplay
import dev.zacsweers.metrox.viewmodel.LocalMetroViewModelFactory
import dev.zacsweers.metrox.viewmodel.MetroViewModelFactory
import dev.zacsweers.metrox.viewmodel.metroViewModel
import io.github.barqallayl.burkan.core.navigation.HomeRoute
import io.github.barqallayl.burkan.core.navigation.LocalNavigator
import io.github.barqallayl.burkan.core.navigation.SHARED_AXIS_DISTANCE
import io.github.barqallayl.burkan.core.navigation.SetupRoute
import io.github.barqallayl.burkan.core.navigation.appEntryProvider
import io.github.barqallayl.burkan.core.navigation.rememberNavigator
import io.github.barqallayl.burkan.core.navigation.sharedAxisX
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
        val isSetupComplete = state.isSetupComplete ?: return@CompositionLocalProvider

        ProvideTextScale(appearance.textScalePercent) {
            BurkanTheme(
                isDarkTheme = appearance.themeMode.isDark(),
                seedColor = appearance.seedColor.color,
                paletteStyle = appearance.paletteStyle.style,
                specVersion = appearance.colorSpec.version,
            ) {
                AppNavigation(isSetupComplete)
            }
        }
    }
}

/** The one place that decides between setup and home. No screen navigates on finishing or losing setup. */
@Composable
private fun AppNavigation(isSetupComplete: Boolean) {
    val navigator = rememberNavigator(start = if (isSetupComplete) HomeRoute else SetupRoute)
    val distance = with(LocalDensity.current) { SHARED_AXIS_DISTANCE.roundToPx() }
    CompositionLocalProvider(LocalNavigator provides navigator) {
        NavDisplay(
            backStack = navigator.entries,
            onBack = navigator::pop,
            entryDecorators = listOf(
                rememberSaveableStateHolderNavEntryDecorator(),
                rememberViewModelStoreNavEntryDecorator(),
            ),
            transitionSpec = { sharedAxisX(distance, forward = true) },
            popTransitionSpec = { sharedAxisX(distance, forward = false) },
            predictivePopTransitionSpec = { sharedAxisX(distance, forward = false) },
            entryProvider = ::appEntryProvider,
        )
    }
}
