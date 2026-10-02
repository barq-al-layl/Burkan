package io.github.barqallayl.burkan.core.navigation

import androidx.compose.animation.ContentTransform
import androidx.compose.animation.core.FastOutLinearInEasing
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.VisibilityThreshold
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp

/**
 * The shared-axis X transition every destination change uses. Both screens move [SHARED_AXIS_DISTANCE] along the
 * direction of travel, the new one arriving from +[distancePx] and the old one leaving to -[distancePx]; going back
 * reverses the signs. The old screen fades out quickly, then the new one fades in.
 *
 * The new screen fades in from 0.8 rather than 0: from 0 there is a stretch where neither screen is drawn and the
 * background flashes through.
 */
fun sharedAxisX(distancePx: Int, forward: Boolean): ContentTransform {
    val sign = if (forward) 1 else -1
    val enter = slideInHorizontally(slideSpec) { sign * distancePx } + fadeIn(fadeInSpec, initialAlpha = FADE_IN_FROM)
    val exit = slideOutHorizontally(slideSpec) { -sign * distancePx } + fadeOut(fadeOutSpec)
    return enter togetherWith exit
}

/** How far both screens move. */
val SHARED_AXIS_DISTANCE = 30.dp

private val slideSpec = spring(
    dampingRatio = Spring.DampingRatioMediumBouncy,
    stiffness = Spring.StiffnessMediumLow,
    visibilityThreshold = IntOffset.VisibilityThreshold,
)

/** The fades take 300 ms together: out over the first 105, in over the remaining 195. */
private const val FADE_OUT_MILLIS = 105
private const val FADE_IN_MILLIS = 195
private const val FADE_IN_FROM = 0.8f

private val fadeOutSpec = tween<Float>(durationMillis = FADE_OUT_MILLIS, easing = FastOutLinearInEasing)
private val fadeInSpec = tween<Float>(
    durationMillis = FADE_IN_MILLIS,
    delayMillis = FADE_OUT_MILLIS,
    easing = LinearOutSlowInEasing,
)
