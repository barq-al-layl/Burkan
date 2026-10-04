package io.github.barqallayl.burkan.core.navigation

import androidx.compose.animation.ContentTransform
import androidx.compose.animation.core.VisibilityThreshold
import androidx.compose.animation.core.spring
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.ui.unit.IntOffset

/**
 * Every change of screen: the two screens slide the full width, side by side, the new one arriving from the
 * direction of travel. Going back reverses the direction, whether by the back button or by the back gesture, which
 * drags the two screens along with the finger. Nothing fades or scales, so the move reads as one sheet of paper
 * pushing the other away.
 */
fun slideTransition(forward: Boolean): ContentTransform {
    val sign = if (forward) 1 else -1
    return slideInHorizontally(slideSpec) { width -> sign * width } togetherWith
        slideOutHorizontally(slideSpec) { width -> -sign * width }
}

/** The slide rides this spring: stiff and barely underdamped, so a screen lands with a snap. */
private const val DAMPING = 0.85f
private const val STIFFNESS = 420f

private val slideSpec = spring(
    dampingRatio = DAMPING,
    stiffness = STIFFNESS,
    visibilityThreshold = IntOffset.VisibilityThreshold,
)
