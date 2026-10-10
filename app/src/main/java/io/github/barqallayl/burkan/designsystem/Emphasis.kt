package io.github.barqallayl.burkan.designsystem

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight

/**
 * The style of text that stands out from the text around it: a title, the chosen one of several. In the Material
 * style it is [emphasized], the twin Material's type scale has for [base], so no weight is set by hand. One UI has
 * no such scale, and sets [base] in [oneUiWeight].
 */
@Composable
@ReadOnlyComposable
fun emphasis(base: TextStyle, emphasized: TextStyle, oneUiWeight: FontWeight = FontWeight.SemiBold): TextStyle =
    if (LocalAppStyle.current == AppStyle.OneUi) base.copy(fontWeight = oneUiWeight) else emphasized
