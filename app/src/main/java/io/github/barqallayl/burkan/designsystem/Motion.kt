package io.github.barqallayl.burkan.designsystem

import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.FastOutLinearInEasing
import androidx.compose.animation.core.FiniteAnimationSpec
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.SpringSpec
import androidx.compose.animation.core.VisibilityThreshold
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MotionScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.remember
import androidx.compose.ui.unit.IntOffset

/**
 * How things move, in the style the app is drawn in.
 *
 * In the Material style what moves, changes size or changes colour rides one of the theme's motion scheme's
 * springs, which is what Material's own components move on, so the app's own animations keep time with them. A
 * spatial spring is for what moves or changes size and may overshoot; an effects spring is for colour and never
 * does. A fade that makes way for another is timed instead: the second has to wait for the first, and a spring
 * cannot be made to wait. One UI has no such scheme: its motion is quick off the mark and long in settling, on a
 * fixed curve.
 *
 * Take it once at the top of a composable (`val motion = burkanMotion`); its specs can then be asked for anywhere,
 * a transition's own lambda included.
 */
@Stable
class BurkanMotion internal constructor(private val scheme: MotionScheme?) {

    /** For what moves or changes size as part of a deliberate change: a step opening, one screen giving way. */
    fun <T> move(): FiniteAnimationSpec<T> = scheme?.defaultSpatialSpec() ?: tween(ONE_UI_MILLIS, easing = OneUiEasing)

    /**
     * For a whole screen sliding. It is the scheme's slow spring, Material's for what fills the screen, told to end
     * within a pixel of its place. A scheme hands its springs out for any kind of value, so as it comes this one
     * does not know how close is close enough for a position, and goes on running long after the screen has
     * visibly stopped: time in which a tap on the new screen is lost.
     */
    fun slide(): FiniteAnimationSpec<IntOffset> {
        val spec = scheme?.slowSpatialSpec<IntOffset>() ?: return tween(ONE_UI_MILLIS, easing = OneUiEasing)
        return if (spec is SpringSpec<IntOffset>) {
            spring(spec.dampingRatio, spec.stiffness, IntOffset.VisibilityThreshold)
        } else {
            spec
        }
    }

    /** For a part of a list making or giving up its room: a notice coming in, a run's steps opening. */
    fun <T> settle(): FiniteAnimationSpec<T> =
        scheme?.defaultSpatialSpec() ?: spring(stiffness = Spring.StiffnessMediumLow)

    /** For a colour changing, and for a fade that keeps time with [move]. */
    fun <T> tint(): FiniteAnimationSpec<T> = scheme?.defaultEffectsSpec() ?: tween(ONE_UI_MILLIS, easing = OneUiEasing)

    /** For what fades in where something else has just faded out: it waits for [vanish] to have finished. */
    fun <T> appear(): FiniteAnimationSpec<T> = if (scheme != null) {
        tween(MATERIAL_FADE_IN_MILLIS, delayMillis = MATERIAL_FADE_OUT_MILLIS, easing = LinearOutSlowInEasing)
    } else {
        tween(FADE_IN_MILLIS, delayMillis = FADE_OUT_MILLIS)
    }

    /** For what fades out to make way: quick, so that it has gone before its room has. */
    fun <T> vanish(): FiniteAnimationSpec<T> = if (scheme != null) {
        tween(MATERIAL_FADE_OUT_MILLIS, easing = FastOutLinearInEasing)
    } else {
        tween(FADE_OUT_MILLIS)
    }

    private companion object {
        const val ONE_UI_MILLIS = 420
        const val FADE_OUT_MILLIS = 110
        const val FADE_IN_MILLIS = 260

        /** Material's own split of a change of content: the first three tenths to go, the rest to arrive. */
        const val MATERIAL_FADE_OUT_MILLIS = 90
        const val MATERIAL_FADE_IN_MILLIS = 210

        /** Quick off the mark and long in settling, like One UI's own. */
        val OneUiEasing = CubicBezierEasing(0.22f, 0.25f, 0f, 1f)
    }
}

val burkanMotion: BurkanMotion
    @Composable
    get() {
        val scheme = if (LocalAppStyle.current == AppStyle.OneUi) null else MaterialTheme.motionScheme
        return remember(scheme) { BurkanMotion(scheme) }
    }
