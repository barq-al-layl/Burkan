package io.github.barqallayl.burkan

import androidx.compose.animation.ContentTransform
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
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
import io.github.barqallayl.burkan.core.navigation.oneUiTransition
import io.github.barqallayl.burkan.core.navigation.slideTransition
import io.github.barqallayl.burkan.designsystem.AppStyle
import io.github.barqallayl.burkan.designsystem.BurkanTheme
import io.github.barqallayl.burkan.designsystem.LocalAppStyle
import io.github.barqallayl.burkan.designsystem.PaletteStyles
import io.github.barqallayl.burkan.designsystem.SeedColors
import io.github.barqallayl.burkan.designsystem.resolved
import io.github.barqallayl.burkan.designsystem.ThemeReveal
import io.github.barqallayl.burkan.designsystem.isDark
import io.github.barqallayl.burkan.designsystem.recordsRevealOrigin
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
        // The colours and the style change in a circle spreading from the tap.
        ThemeReveal(
            ThemeColours(isDarkTheme, appearance.seedColor, appearance.paletteStyle, appearance.appStyle),
        ) { colours ->
            BurkanTheme(
                isDarkTheme = colours.isDark,
                seedColor = colours.seedColor.resolved(),
                paletteStyle = colours.paletteStyle.style,
                appStyle = colours.appStyle,
            ) {
                Surface(modifier = Modifier.recordsRevealOrigin()) { AppNavigation(isSetupComplete) }
            }
        }
    }
}

/** What the colour scheme is generated from. */
private data class ThemeColours(
    val isDark: Boolean,
    val seedColor: SeedColors,
    val paletteStyle: PaletteStyles,
    val appStyle: AppStyle,
)

/** The one place that decides between setup and home. No screen navigates on finishing or losing setup. */
@Composable
private fun AppNavigation(isSetupComplete: Boolean) {
    // Each style changes screen its own way.
    val style = LocalAppStyle.current
    val navigator = rememberNavigator(start = if (isSetupComplete) HomeRoute else SetupRoute)
    CompositionLocalProvider(LocalNavigator provides navigator) {
        NavDisplay(
            backStack = navigator.entries,
            onBack = navigator::pop,
            entryDecorators = listOf(
                rememberSaveableStateHolderNavEntryDecorator(),
                rememberViewModelStoreNavEntryDecorator(),
            ),
            transitionSpec = { screenTransition(style, forward = true) },
            popTransitionSpec = { screenTransition(style, forward = false) },
            predictivePopTransitionSpec = { screenTransition(style, forward = false) },
            entryProvider = ::appEntryProvider,
        )
    }
}

private fun screenTransition(style: AppStyle, forward: Boolean): ContentTransform = when (style) {
    AppStyle.Material -> slideTransition(forward)
    AppStyle.OneUi -> oneUiTransition(forward)
}
