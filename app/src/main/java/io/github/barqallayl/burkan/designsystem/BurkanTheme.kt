package io.github.barqallayl.burkan.designsystem

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialExpressiveTheme
import androidx.compose.material3.Typography
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.materialkolor.PaletteStyle
import com.materialkolor.dynamiccolor.ColorSpec
import com.materialkolor.material3.DynamicMaterialExpressiveTheme
import com.materialkolor.material3.rememberDynamicColorScheme

/**
 * The app's theme, in one of its two styles.
 *
 * As [AppStyle.Material] it is Material 3 Expressive with a scheme generated from [seedColor] under Material's 2026
 * colour spec, and Material's type scale set in Roboto. As [AppStyle.OneUi] it follows One UI 8.5: the accents
 * still come from [seedColor], as the phone's own colour palette does, but the surfaces are One UI's greys and the
 * type is the system's font. Every appearance choice arrives as a parameter; shapes are Material's own.
 */
@Composable
fun BurkanTheme(
    isDarkTheme: Boolean,
    seedColor: Color,
    paletteStyle: PaletteStyle,
    appStyle: AppStyle = AppStyle.Material,
    content: @Composable () -> Unit,
) {
    CompositionLocalProvider(LocalAppStyle provides appStyle) {
        when (appStyle) {
            AppStyle.Material -> {
                val typography = remember { materialTypography() }
                DynamicMaterialExpressiveTheme(
                    seedColor = seedColor,
                    isDark = isDarkTheme,
                    style = paletteStyle,
                    specVersion = ColorSpec.SpecVersion.SPEC_2026,
                    typography = typography,
                    content = content,
                )
            }

            AppStyle.OneUi -> {
                // The accent keeps the seed's own hue, as One UI's colour palette does; the palette styles are
                // Material's idea and are not offered here.
                val accents = rememberDynamicColorScheme(
                    seedColor = seedColor,
                    isDark = isDarkTheme,
                    style = PaletteStyle.Fidelity,
                    specVersion = ColorSpec.SpecVersion.SPEC_2026,
                )
                val colors = remember(accents, isDarkTheme, seedColor) {
                    accents.withOneUiSurfaces(
                        isDarkTheme,
                        seedColor,
                    )
                }
                MaterialExpressiveTheme(
                    colorScheme = colors,
                    typography = OneUiTypography,
                    content = content,
                )
            }
        }
    }
}

/**
 * One UI's surfaces and its single [accent] laid over a generated scheme, as of One UI 8.5: a light grey screen with white cards, or a
 * black screen with near-black cards, a lighter grey for what floats over them, and a hairline between rows.
 */
private fun ColorScheme.withOneUiSurfaces(isDark: Boolean, accent: Color): ColorScheme {
    val screen = if (isDark) Color(0xFF000000) else Color(0xFFF2F2F6)
    val card = if (isDark) Color(0xFF1A1A1C) else Color(0xFFFCFCFF)
    val raised = if (isDark) Color(0xFF2C2C2F) else Color(0xFFE8E8EE)
    val text = if (isDark) Color(0xFFFAFAFF) else Color(0xFF1B1B1F)
    val quietText = if (isDark) Color(0xFF9C9CA3) else Color(0xFF6E6E76)
    return copy(
        // One accent, the same in both themes, as Samsung's is: whatever is filled with the accent is this colour,
        // and whatever is only marked with it is a tint of it. The generated scheme would give a pale primary in
        // the dark theme and a deep one for its containers, which read as two or three different blues.
        primary = accent,
        onPrimary = Color.White,
        primaryContainer = accent.copy(alpha = if (isDark) 0.24f else 0.14f).compositeOver(card),
        onPrimaryContainer = primary,
        inversePrimary = accent,
        background = screen,
        onBackground = text,
        surface = screen,
        onSurface = text,
        onSurfaceVariant = quietText,
        surfaceVariant = raised,
        surfaceContainerLowest = card,
        surfaceContainerLow = card,
        surfaceContainer = card,
        surfaceContainerHigh = raised,
        surfaceContainerHighest = raised,
        surfaceBright = card,
        surfaceDim = screen,
        outlineVariant = if (isDark) Color(0xFF303034) else Color(0xFFE2E2E8),
    )
}

/**
 * Material's type scale in the system's own font, which on a Galaxy is Samsung's, reweighted the way One UI sets
 * its screens: a row's name is regular at 18 sp, a group's label small and semibold, and the titles of screens and
 * cards bold.
 */
private val OneUiTypography: Typography = Typography().let { base ->
    base.copy(
        headlineMedium = base.headlineMedium.copy(fontWeight = FontWeight.Bold),
        headlineSmall = base.headlineSmall.copy(fontWeight = FontWeight.Bold),
        titleLarge = base.titleLarge.copy(fontWeight = FontWeight.Bold),
        titleMedium = base.titleMedium.copy(
            fontWeight = FontWeight.Normal,
            fontSize = 18.sp,
            letterSpacing = 0.sp,
        ),
        titleSmall = base.titleSmall.copy(fontWeight = FontWeight.SemiBold, fontSize = 14.sp),
    )
}
