package io.github.barqallayl.burkan.core.navigation

import androidx.compose.animation.ContentTransform
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.VisibilityThreshold
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
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

/**
 * A change of screen in the One UI style, as the phone's own apps do it: the new screen slides in over the old one,
 * which gives way a short distance and dims out behind it. Going back runs it the other way, the old screen
 * returning from where it waited as the top one slides off.
 */
fun oneUiTransition(forward: Boolean): ContentTransform =
    if (forward) {
        slideInHorizontally(oneUiSlide) { width -> width } togetherWith
            slideOutHorizontally(oneUiSlide) { width -> -(width * ONE_UI_GIVE).toInt() } + fadeOut(
            oneUiFade,
        )
    } else {
        (
            slideInHorizontally(oneUiSlide) { width -> -(width * ONE_UI_GIVE).toInt() } + fadeIn(
                oneUiFade,
            ) togetherWith
                slideOutHorizontally(oneUiSlide) { width -> width }
            ).apply {
                // The screen being left is the one on top, so it stays above the one coming back.
                targetContentZIndex = -1f
            }
    }

/** How far the screen underneath moves, as a share of the screen's width. */
private const val ONE_UI_GIVE = 0.25f
private const val ONE_UI_MILLIS = 400

/** Quick off the mark and long in settling, like One UI's own. */
private val OneUiEasing = CubicBezierEasing(0.22f, 0.25f, 0f, 1f)
private val oneUiSlide = tween<IntOffset>(ONE_UI_MILLIS, easing = OneUiEasing)
private val oneUiFade = tween<Float>(ONE_UI_MILLIS, easing = OneUiEasing)
