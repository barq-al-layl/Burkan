package io.github.barqallayl.burkan.designsystem

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
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
 * The look the whole app is drawn in. [Material] is Material 3 Expressive, as the app was built. [OneUi] follows
 * Samsung's One UI, so the app sits beside the phone's own: a grey screen with white cards, rows parted by hairlines,
 * and the system's font.
 */
enum class AppStyle {
    Material,
    OneUi,
    ;

    companion object {
        /**
         * The style a phone gets until its user chooses one: One UI on a Samsung, where it sits beside the phone's
         * own apps, and Material anywhere else. [manufacturer] is the phone's own name for its maker.
         */
        fun defaultFor(manufacturer: String?): AppStyle =
            if (manufacturer.equals(SAMSUNG, ignoreCase = true)) OneUi else Material

        private const val SAMSUNG = "samsung"
    }
}

/** The style the shared components draw themselves in. The theme provides it. */
val LocalAppStyle = staticCompositionLocalOf { AppStyle.Material }

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
