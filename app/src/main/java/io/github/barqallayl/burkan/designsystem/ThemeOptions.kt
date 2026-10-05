package io.github.barqallayl.burkan.designsystem

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.colorResource
import com.materialkolor.PaletteStyle

/**
 * The choices behind the appearance preferences. Each is stored by its entry name, so renaming an entry resets
 * what users chose.
 */
enum class ThemeMode {
    FollowSystem,
    Light,
    Dark,
}

@Composable
fun ThemeMode.isDark(): Boolean = when (this) {
    ThemeMode.FollowSystem -> isSystemInDarkTheme()
    ThemeMode.Light -> false
    ThemeMode.Dark -> true
}

/**
 * The colours the scheme can be generated from. [Wallpaper] has none of its own: it is the accent Android takes
 * from the user's wallpaper, read when the theme is built.
 */
enum class SeedColors(val color: Color?) {
    Wallpaper(null),
    Blue(Color(0xFF1E88E5)),
    Indigo(Color(0xFF3949AB)),
    Purple(Color(0xFF8E24AA)),
    Pink(Color(0xFFD81B60)),
    Red(Color(0xFFE53935)),
    Orange(Color(0xFFFB8C00)),
    Green(Color(0xFF43A047)),
    Teal(Color(0xFF00897B)),
    ;

    companion object {
        val Default: SeedColors = Wallpaper
    }
}

/** The colour to generate the scheme from: the entry's own, or the system's wallpaper accent. */
@Composable
@ReadOnlyComposable
fun SeedColors.resolved(): Color = color ?: colorResource(android.R.color.system_accent1_500)

/** Every MaterialKolor palette style, under names this app owns so a library rename cannot reset a choice. */
enum class PaletteStyles(val style: PaletteStyle) {
    TonalSpot(PaletteStyle.TonalSpot),
    Neutral(PaletteStyle.Neutral),
    Vibrant(PaletteStyle.Vibrant),
    Expressive(PaletteStyle.Expressive),
    Rainbow(PaletteStyle.Rainbow),
    FruitSalad(PaletteStyle.FruitSalad),
    Monochrome(PaletteStyle.Monochrome),
    Fidelity(PaletteStyle.Fidelity),
    Content(PaletteStyle.Content),

    /** The variant the 2026 spec added. */
    Cmf(PaletteStyle.Cmf()),
}

/** Text size, as a percentage of the system font scale. */
object TextScale {
    val percentages: IntProgression = 85..130 step 5
}
