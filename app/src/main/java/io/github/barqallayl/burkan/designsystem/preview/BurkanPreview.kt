package io.github.barqallayl.burkan.designsystem.preview

import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewWrapperProvider
import io.github.barqallayl.burkan.core.storage.SettingsStorage
import io.github.barqallayl.burkan.core.ui.LocalCurrentYear
import io.github.barqallayl.burkan.designsystem.AppFont
import io.github.barqallayl.burkan.designsystem.BurkanTheme
import io.github.barqallayl.burkan.designsystem.PaletteStyles
import io.github.barqallayl.burkan.designsystem.ProvideTextScale
import io.github.barqallayl.burkan.designsystem.SeedColors
import io.github.barqallayl.burkan.designsystem.resolved
import io.github.barqallayl.burkan.designsystem.ThemeMode
import io.github.barqallayl.burkan.designsystem.isDark

/**
 * The app's preview. Every preview is also a screenshot test.
 *
 * Pair it with `@PreviewWrapper(BurkanPreviewWrapper::class)` to render in the app's theme with the default
 * preferences. A preview that needs dark, large text or another seed leaves the wrapper off and wraps its body in
 * [BurkanPreviewTheme] instead.
 */
@Preview(showBackground = true)
annotation class BurkanPreview

/** Applies [BurkanPreviewTheme] with the default preferences, in Android Studio and in screenshot tests. */
class BurkanPreviewWrapper : PreviewWrapperProvider {
    @Composable
    override fun Wrap(content: @Composable () -> Unit) {
        BurkanPreviewTheme(content = content)
    }
}

/** The app's theme for a preview, applied the way `App.kt` applies it. Defaults are those of a fresh install. */
@Composable
fun BurkanPreviewTheme(
    themeMode: ThemeMode = SettingsStorage.Defaults.themeMode,
    // A fixed colour: the wallpaper's accent is whatever the machine drawing the preview has.
    seedColor: SeedColors = SeedColors.Blue,
    paletteStyle: PaletteStyles = SettingsStorage.Defaults.paletteStyle,
    appFont: AppFont = SettingsStorage.Defaults.appFont,
    textScalePercent: Int = SettingsStorage.Defaults.TEXT_SCALE_PERCENT,
    content: @Composable () -> Unit,
) {
    // The sample runs are from this year, and stay written that way whenever the screenshots are recorded.
    CompositionLocalProvider(LocalCurrentYear provides PREVIEW_YEAR) {
        ProvideTextScale(textScalePercent) {
            BurkanTheme(
                isDarkTheme = themeMode.isDark(),
                seedColor = seedColor.resolved(),
                paletteStyle = paletteStyle.style,
                appFont = appFont,
            ) {
                Surface(content = content)
            }
        }
    }
}

private const val PREVIEW_YEAR = 2026
