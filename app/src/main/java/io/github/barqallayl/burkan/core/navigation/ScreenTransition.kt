package io.github.barqallayl.burkan.core.navigation

import androidx.compose.animation.ContentTransform
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.ui.unit.IntOffset
import io.github.barqallayl.burkan.designsystem.BurkanMotion

/**
 * A change of screen in the Material style, as Material has it on Android for going forward and back: the two
 * screens slide a short way in the direction of travel while one fades into the other, so neither has to cross the
 * whole width. Going back reverses the direction, whether by the back button or by the back gesture, which carries
 * the change along with the finger. The slide rides the theme's spring for what fills the screen; the fades are
 * timed, the new screen's waiting for the old one's.
 *
 * Forward is towards the end of a line of text: [rtl] turns the whole movement round, as it does in
 * [oneUiTransition].
 */
fun slideTransition(forward: Boolean, motion: BurkanMotion, rtl: Boolean = false): ContentTransform {
    val sign = (if (forward) 1 else -1) * (if (rtl) -1 else 1)
    return (slideInHorizontally(motion.slide()) { width -> sign * width / SLIDE_SHARE } + fadeIn(motion.appear())) togetherWith
        (slideOutHorizontally(motion.slide()) { width -> -sign * width / SLIDE_SHARE } + fadeOut(motion.vanish()))
}

/** How far a screen slides, as one part in this many of its width: about 40 dp on a phone. */
private const val SLIDE_SHARE = 10

/**
 * A change of screen in the One UI style, as the phone's own apps do it: the new screen slides in over the old one,
 * which gives way a short distance and dims out behind it. Going back runs it the other way, the old screen
 * returning from where it waited as the top one slides off.
 */
fun oneUiTransition(forward: Boolean, rtl: Boolean = false): ContentTransform {
    val side = if (rtl) -1 else 1
    return if (forward) {
        slideInHorizontally(oneUiSlide) { width -> side * width } togetherWith
            slideOutHorizontally(oneUiSlide) { width -> -side * (width * ONE_UI_GIVE).toInt() } + fadeOut(
            oneUiFade,
        )
    } else {
        (
            slideInHorizontally(oneUiSlide) { width -> -side * (width * ONE_UI_GIVE).toInt() } + fadeIn(
                oneUiFade,
            ) togetherWith
                slideOutHorizontally(oneUiSlide) { width -> side * width }
            ).apply {
                // The screen being left is the one on top, so it stays above the one coming back.
                targetContentZIndex = -1f
            }
    }
}

/** How far the screen underneath moves, as a share of the screen's width. */
private const val ONE_UI_GIVE = 0.25f
private const val ONE_UI_MILLIS = 400

/** Quick off the mark and long in settling, like One UI's own. */
private val OneUiEasing = CubicBezierEasing(0.22f, 0.25f, 0f, 1f)
private val oneUiSlide = tween<IntOffset>(ONE_UI_MILLIS, easing = OneUiEasing)
private val oneUiFade = tween<Float>(ONE_UI_MILLIS, easing = OneUiEasing)
