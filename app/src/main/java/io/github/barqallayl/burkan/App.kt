package io.github.barqallayl.burkan

import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.lifecycle.viewmodel.navigation3.rememberViewModelStoreNavEntryDecorator
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
import androidx.navigation3.ui.NavDisplay
import dev.zacsweers.metrox.viewmodel.LocalMetroViewModelFactory
import dev.zacsweers.metrox.viewmodel.MetroViewModelFactory
import dev.zacsweers.metrox.viewmodel.metroViewModel
import io.github.barqallayl.burkan.core.navigation.HomeRoute
import io.github.barqallayl.burkan.core.navigation.LocalNavigator
import io.github.barqallayl.burkan.core.navigation.SetupRoute
import io.github.barqallayl.burkan.core.navigation.appEntryProvider
import io.github.barqallayl.burkan.core.navigation.rememberNavigator
import io.github.barqallayl.burkan.core.navigation.predictiveBackTransition
import io.github.barqallayl.burkan.core.navigation.slideTransition
import io.github.barqallayl.burkan.designsystem.ProvideTextScale
import io.github.barqallayl.burkan.designsystem.BurkanTheme
import io.github.barqallayl.burkan.designsystem.isDark
import org.orbitmvi.orbit.compose.collectAsState

/**
 * The composition root: appearance, text size and navigation, each applied once, here. [onThemeChange] is told
 * whether the app is dark, at the start and whenever that changes.
 */
@Composable
fun App(viewModelFactory: MetroViewModelFactory, onThemeChange: (isDark: Boolean) -> Unit) {
    CompositionLocalProvider(LocalMetroViewModelFactory provides viewModelFactory) {
        val viewModel = metroViewModel<AppViewModel>()
        val state by viewModel.collectAsState()
        val appearance = state.appearance ?: return@CompositionLocalProvider
        val isSetupComplete = state.isSetupComplete ?: return@CompositionLocalProvider

        val isDarkTheme = appearance.themeMode.isDark()
        // The system bars' icons are the activity's to colour, and it only knows the system's own theme.
        LaunchedEffect(isDarkTheme) { onThemeChange(isDarkTheme) }
        ProvideTextScale(appearance.textScalePercent) {
            BurkanTheme(
                isDarkTheme = isDarkTheme,
                seedColor = appearance.seedColor.color,
                paletteStyle = appearance.paletteStyle.style,
                appFont = appearance.appFont,
            ) {
                Surface { AppNavigation(isSetupComplete) }
            }
        }
    }
}

/** The one place that decides between setup and home. No screen navigates on finishing or losing setup. */
@Composable
private fun AppNavigation(isSetupComplete: Boolean) {
    val navigator = rememberNavigator(start = if (isSetupComplete) HomeRoute else SetupRoute)
    CompositionLocalProvider(LocalNavigator provides navigator) {
        NavDisplay(
            backStack = navigator.entries,
            onBack = navigator::pop,
            entryDecorators = listOf(
                rememberSaveableStateHolderNavEntryDecorator(),
                rememberViewModelStoreNavEntryDecorator(),
            ),
            transitionSpec = { slideTransition(forward = true) },
            popTransitionSpec = { slideTransition(forward = false) },
            predictivePopTransitionSpec = { predictiveBackTransition() },
            entryProvider = ::appEntryProvider,
        )
    }
}
