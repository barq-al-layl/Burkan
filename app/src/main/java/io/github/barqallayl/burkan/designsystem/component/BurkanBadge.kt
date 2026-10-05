package io.github.barqallayl.burkan.designsystem.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * What a badge or a pill says about the thing it marks. Each has a fill and a text colour of its own, and whatever
 * carries one also carries an icon or a word: never colour alone.
 */
enum class Tone { Neutral, Good, HeldUp, Bad }

@Immutable
data class ToneColors(val container: Color, val content: Color)

val Tone.colors: ToneColors
    @Composable
    @ReadOnlyComposable
    get() = when (this) {
        Tone.Neutral -> ToneColors(
            MaterialTheme.colorScheme.surfaceContainerHighest,
            MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Tone.Good -> ToneColors(MaterialTheme.colorScheme.primaryContainer, MaterialTheme.colorScheme.onPrimaryContainer)
        Tone.HeldUp -> noticeColors.let { ToneColors(it.container, it.content) }
        Tone.Bad -> ToneColors(MaterialTheme.colorScheme.errorContainer, MaterialTheme.colorScheme.onErrorContainer)
    }

/** An icon in a round, tinted badge: the leading mark of a row, or, larger, of a card. */
@Composable
fun BurkanIconBadge(
    icon: ImageVector,
    modifier: Modifier = Modifier,
    tone: Tone = Tone.Neutral,
    size: Dp = 40.dp,
    contentDescription: String? = null,
) {
    val colors = tone.colors
    Box(modifier = modifier.size(size).background(colors.container, CircleShape), contentAlignment = Alignment.Center) {
        Icon(icon, contentDescription = contentDescription, modifier = Modifier.size(size * ICON_SHARE), tint = colors.content)
    }
}

/** A word or a number in a small pill: a count, a value, a result. */
@Composable
fun BurkanPill(text: String, modifier: Modifier = Modifier, tone: Tone = Tone.Neutral, icon: ImageVector? = null) {
    val colors = tone.colors
    Row(
        modifier = modifier.background(colors.container, CircleShape).padding(horizontal = 10.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (icon != null) Icon(icon, contentDescription = null, modifier = Modifier.size(14.dp), tint = colors.content)
        Text(text, style = MaterialTheme.typography.labelMedium, color = colors.content, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

/** The icon fills a little over half of its badge. */
private const val ICON_SHARE = 0.55f
