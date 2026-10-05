package io.github.barqallayl.burkan.designsystem

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import io.github.barqallayl.burkan.R

/**
 * The app's typefaces. There is no choosing among them: the Material style is set in Roboto, which the app carries
 * so that it looks the same on every phone, the One UI style in the phone's own font, and the log's steps in a
 * fixed-width face in both. All the bundled files are under the SIL Open Font License.
 */
val RobotoFontFamily: FontFamily by lazy { variableFontFamily(R.font.roboto) }

/** The log's typeface: fixed width, whichever style the rest of the app is in. */
val MonoFontFamily: FontFamily by lazy { variableFontFamily(R.font.jetbrains_mono) }

/** One variable file serves every weight: each entry asks the file's weight axis for its own value. */
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

/** Material's own type scale, set in Roboto. Sizes, weights and line heights stay Material's. */
fun materialTypography(): Typography {
    val base = Typography()
    fun TextStyle.inRoboto() = copy(fontFamily = RobotoFontFamily)
    return base.copy(
        displayLarge = base.displayLarge.inRoboto(),
        displayMedium = base.displayMedium.inRoboto(),
        displaySmall = base.displaySmall.inRoboto(),
        headlineLarge = base.headlineLarge.inRoboto(),
        headlineMedium = base.headlineMedium.inRoboto(),
        headlineSmall = base.headlineSmall.inRoboto(),
        titleLarge = base.titleLarge.inRoboto(),
        titleMedium = base.titleMedium.inRoboto(),
        titleSmall = base.titleSmall.inRoboto(),
        bodyLarge = base.bodyLarge.inRoboto(),
        bodyMedium = base.bodyMedium.inRoboto(),
        bodySmall = base.bodySmall.inRoboto(),
        labelLarge = base.labelLarge.inRoboto(),
        labelMedium = base.labelMedium.inRoboto(),
        labelSmall = base.labelSmall.inRoboto(),
        displayLargeEmphasized = base.displayLargeEmphasized.inRoboto(),
        displayMediumEmphasized = base.displayMediumEmphasized.inRoboto(),
        displaySmallEmphasized = base.displaySmallEmphasized.inRoboto(),
        headlineLargeEmphasized = base.headlineLargeEmphasized.inRoboto(),
        headlineMediumEmphasized = base.headlineMediumEmphasized.inRoboto(),
        headlineSmallEmphasized = base.headlineSmallEmphasized.inRoboto(),
        titleLargeEmphasized = base.titleLargeEmphasized.inRoboto(),
        titleMediumEmphasized = base.titleMediumEmphasized.inRoboto(),
        titleSmallEmphasized = base.titleSmallEmphasized.inRoboto(),
        bodyLargeEmphasized = base.bodyLargeEmphasized.inRoboto(),
        bodyMediumEmphasized = base.bodyMediumEmphasized.inRoboto(),
        bodySmallEmphasized = base.bodySmallEmphasized.inRoboto(),
        labelLargeEmphasized = base.labelLargeEmphasized.inRoboto(),
        labelMediumEmphasized = base.labelMediumEmphasized.inRoboto(),
        labelSmallEmphasized = base.labelSmallEmphasized.inRoboto(),
    )
}
