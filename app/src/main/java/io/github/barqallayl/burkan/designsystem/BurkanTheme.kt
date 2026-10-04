package io.github.barqallayl.burkan.designsystem

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import com.materialkolor.material3.DynamicMaterialExpressiveTheme
import com.materialkolor.PaletteStyle
import com.materialkolor.dynamiccolor.ColorSpec

/**
 * Material 3 Expressive with a scheme generated from [seedColor] under Material's 2026 colour spec, and Material's
 * type scale set in [appFont]. Every appearance choice arrives as a parameter; shapes are Material's own.
 */
@Composable
fun BurkanTheme(
    isDarkTheme: Boolean,
    seedColor: Color,
    paletteStyle: PaletteStyle,
    appFont: AppFont,
    content: @Composable () -> Unit,
) {
    val typography = remember(appFont) { burkanTypography(appFont) }
    DynamicMaterialExpressiveTheme(
        seedColor = seedColor,
        isDark = isDarkTheme,
        style = paletteStyle,
        specVersion = ColorSpec.SpecVersion.SPEC_2026,
        typography = typography,
        content = content,
    )
}

/**
 * Multiplies the system font scale by [percent] / 100 for [content].
 *
 * The scaled density is linear, so above the system's own scale it does not follow Android's non-linear curve for
 * very large text.
 */
@Composable
fun ProvideTextScale(percent: Int, content: @Composable () -> Unit) {
    val density = LocalDensity.current
    CompositionLocalProvider(
        LocalDensity provides Density(density = density.density, fontScale = density.fontScale * percent / 100f),
        content = content,
    )
}
