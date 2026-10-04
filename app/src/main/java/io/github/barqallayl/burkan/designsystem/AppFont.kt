package io.github.barqallayl.burkan.designsystem

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import io.github.barqallayl.burkan.R

/**
 * The typefaces the app can be set in. Each is stored by its entry name, so renaming an entry resets what users
 * chose. [displayName] is the typeface's own name, which is not translated; [System] has none and is labelled by
 * the screen that lists it.
 */
enum class AppFont(val displayName: String?) {
    /** The phone's own font. */
    System(null),
    Poppins("Poppins"),
    GoogleSansFlex("Google Sans Flex"),
    Figtree("Figtree"),
    SpaceGrotesk("Space Grotesk"),
    Geist("Geist"),
    Onest("Onest"),
    ;

    /** Null for [System]: Material's default typography already uses the phone's font. */
    val family: FontFamily?
        get() = when (this) {
            System -> null
            Poppins -> Fonts.poppins
            GoogleSansFlex -> Fonts.googleSansFlex
            Figtree -> Fonts.figtree
            SpaceGrotesk -> Fonts.spaceGrotesk
            Geist -> Fonts.geist
            Onest -> Fonts.onest
        }
}

/** The log's typeface: fixed width, whatever [AppFont] the rest of the app is set in. */
val MonoFontFamily: FontFamily get() = Fonts.jetBrainsMono

/** The bundled font files as families. All are under the SIL Open Font License. */
private object Fonts {
    val googleSansFlex by lazy { variableFontFamily(R.font.google_sans_flex) }
    val figtree by lazy { variableFontFamily(R.font.figtree) }
    val spaceGrotesk by lazy { variableFontFamily(R.font.space_grotesk) }
    val geist by lazy { variableFontFamily(R.font.geist) }
    val onest by lazy { variableFontFamily(R.font.onest) }
    val jetBrainsMono by lazy { variableFontFamily(R.font.jetbrains_mono) }

    /** Poppins has no variable file: one file per weight, and the heavier weights fall back to bold. */
    val poppins by lazy {
        FontFamily(
            Font(R.font.poppins_light, FontWeight.Light),
            Font(R.font.poppins_regular, FontWeight.Normal),
            Font(R.font.poppins_medium, FontWeight.Medium),
            Font(R.font.poppins_semibold, FontWeight.SemiBold),
            Font(R.font.poppins_bold, FontWeight.Bold),
        )
    }

    /**
     * One variable file serves every weight: each entry asks the file's weight axis for its own value. Space
     * Grotesk's axis stops at bold, so its heaviest entry is drawn as bold.
     */
    private fun variableFontFamily(resource: Int): FontFamily = FontFamily(
        VariableWeights.map { weight ->
            Font(
                resId = resource,
                weight = weight,
                variationSettings = FontVariation.Settings(FontVariation.weight(weight.weight)),
            )
        },
    )

    private val VariableWeights = listOf(
        FontWeight.Light,
        FontWeight.Normal,
        FontWeight.Medium,
        FontWeight.SemiBold,
        FontWeight.Bold,
        FontWeight.ExtraBold,
    )
}

/** Material's own type scale, set in [font]. Sizes, weights and line heights stay Material's. */
fun burkanTypography(font: AppFont): Typography {
    val family = font.family ?: return Typography()
    val base = Typography()
    fun TextStyle.inFont() = copy(fontFamily = family)
    return base.copy(
        displayLarge = base.displayLarge.inFont(),
        displayMedium = base.displayMedium.inFont(),
        displaySmall = base.displaySmall.inFont(),
        headlineLarge = base.headlineLarge.inFont(),
        headlineMedium = base.headlineMedium.inFont(),
        headlineSmall = base.headlineSmall.inFont(),
        titleLarge = base.titleLarge.inFont(),
        titleMedium = base.titleMedium.inFont(),
        titleSmall = base.titleSmall.inFont(),
        bodyLarge = base.bodyLarge.inFont(),
        bodyMedium = base.bodyMedium.inFont(),
        bodySmall = base.bodySmall.inFont(),
        labelLarge = base.labelLarge.inFont(),
        labelMedium = base.labelMedium.inFont(),
        labelSmall = base.labelSmall.inFont(),
        displayLargeEmphasized = base.displayLargeEmphasized.inFont(),
        displayMediumEmphasized = base.displayMediumEmphasized.inFont(),
        displaySmallEmphasized = base.displaySmallEmphasized.inFont(),
        headlineLargeEmphasized = base.headlineLargeEmphasized.inFont(),
        headlineMediumEmphasized = base.headlineMediumEmphasized.inFont(),
        headlineSmallEmphasized = base.headlineSmallEmphasized.inFont(),
        titleLargeEmphasized = base.titleLargeEmphasized.inFont(),
        titleMediumEmphasized = base.titleMediumEmphasized.inFont(),
        titleSmallEmphasized = base.titleSmallEmphasized.inFont(),
        bodyLargeEmphasized = base.bodyLargeEmphasized.inFont(),
        bodyMediumEmphasized = base.bodyMediumEmphasized.inFont(),
        bodySmallEmphasized = base.bodySmallEmphasized.inFont(),
        labelLargeEmphasized = base.labelLargeEmphasized.inFont(),
        labelMediumEmphasized = base.labelMediumEmphasized.inFont(),
        labelSmallEmphasized = base.labelSmallEmphasized.inFont(),
    )
}
