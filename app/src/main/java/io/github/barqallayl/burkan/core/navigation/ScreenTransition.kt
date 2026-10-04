package io.github.barqallayl.burkan.core.navigation

import androidx.compose.animation.ContentTransform
import androidx.compose.animation.core.VisibilityThreshold
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.ui.unit.IntOffset

/**
 * Going to a screen, and going back with the back button: the two screens slide the full width, side by side, the
 * new one arriving from the direction of travel. Going back reverses the direction. Nothing fades or scales, so the
 * move reads as one sheet of paper pushing the other away.
 */
fun slideTransition(forward: Boolean): ContentTransform {
    val sign = if (forward) 1 else -1
    return slideInHorizontally(slideSpec) { width -> sign * width } togetherWith
        slideOutHorizontally(slideSpec) { width -> -sign * width }
}

/**
 * Going back with the back gesture, which the finger drives: the screen being left slides a part of the width
 * towards where it came from, shrinking and fading, while the one underneath grows into place and fades in. The
 * shrinking shows, while the finger is still down, that letting go leaves the screen.
 */
fun predictiveBackTransition(): ContentTransform {
    val enter = slideInHorizontally(slideSpec) { width -> -width / BACK_SLIDE_DIVISOR } +
        scaleIn(floatSpec, initialScale = BACK_SCALE) +
        fadeIn(floatSpec, initialAlpha = BACK_FADE_IN_FROM)
    val exit = slideOutHorizontally(slideSpec) { width -> width / BACK_SLIDE_DIVISOR } +
        scaleOut(floatSpec, targetScale = BACK_SCALE) +
        fadeOut(floatSpec)
    return enter togetherWith exit
}

/** With the back gesture both screens move a fifth of the width. */
private const val BACK_SLIDE_DIVISOR = 5

/** The size a screen shrinks to, and grows from, under the back gesture. */
private const val BACK_SCALE = 0.9f

/** Not from 0: there would be a stretch where neither screen is drawn and the background flashes through. */
private const val BACK_FADE_IN_FROM = 0.6f

/** Every part of both transitions rides this spring: stiff and barely underdamped, so a screen lands with a snap. */
private const val DAMPING = 0.85f
private const val STIFFNESS = 420f

private val slideSpec = spring(
    dampingRatio = DAMPING,
    stiffness = STIFFNESS,
    visibilityThreshold = IntOffset.VisibilityThreshold,
)

private val floatSpec = spring<Float>(dampingRatio = DAMPING, stiffness = STIFFNESS)
