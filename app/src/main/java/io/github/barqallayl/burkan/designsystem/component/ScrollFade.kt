package io.github.barqallayl.burkan.designsystem.component

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.gestures.ScrollableState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import io.github.barqallayl.burkan.designsystem.AppStyle
import io.github.barqallayl.burkan.designsystem.LocalAppStyle

/**
 * In One UI, fades the top and bottom edges of a scrolling list wherever content continues past them, so rows
 * dissolve into the screen instead of being cut off at a line. An edge with nothing beyond it is left sharp, and
 * the fade comes and goes as the list reaches its ends. Nothing in the Material style.
 *
 * Put it ahead of the scrolling modifier, on the part of the screen the list is seen through. [bottom] is off for
 * a screen that already fades its lower edge some other way.
 */
fun Modifier.oneUiScrollFade(
    state: ScrollableState,
    // Deeper at the top: the list runs up under the status bar there, and is gone by the screen's edge.
    topHeight: Dp = 72.dp,
    height: Dp = 32.dp,
    bottom: Boolean = true,
): Modifier = composed {
    if (LocalAppStyle.current != AppStyle.OneUi) return@composed this
    val fadeAtTop by animateFloatAsState(
        if (state.canScrollBackward) 1f else 0f,
        label = "scrollFadeTop",
    )
    val fadeAtBottom by animateFloatAsState(
        if (bottom && state.canScrollForward) 1f else 0f,
        label = "scrollFadeBottom",
    )
    this
        // Its own layer, so the fade rubs out the list alone and the screen's colour shows through.
        .graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen }
        .drawWithContent {
            drawContent()
            val fade = height.toPx().coerceAtMost(size.height / 2f)
            if (fade <= 0f) return@drawWithContent
            val topFade = topHeight.toPx().coerceAtMost(size.height / 2f)
            if (fadeAtTop > 0f) {
                drawRect(
                    brush = Brush.verticalGradient(
                        0f to Color.Black.copy(alpha = 1f - fadeAtTop),
                        1f to Color.Black,
                        startY = 0f,
                        endY = topFade,
                    ),
                    blendMode = BlendMode.DstIn,
                )
            }
            if (fadeAtBottom > 0f) {
                drawRect(
                    brush = Brush.verticalGradient(
                        0f to Color.Black,
                        1f to Color.Black.copy(alpha = 1f - fadeAtBottom),
                        startY = size.height - fade,
                        endY = size.height,
                    ),
                    blendMode = BlendMode.DstIn,
                )
            }
        }
}
