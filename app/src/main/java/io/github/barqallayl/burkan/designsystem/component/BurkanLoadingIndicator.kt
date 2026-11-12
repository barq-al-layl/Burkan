package io.github.barqallayl.burkan.designsystem.component

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.LocalContentColor
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.progressBarRangeInfo
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.lerp
import io.github.barqallayl.burkan.designsystem.AppStyle
import io.github.barqallayl.burkan.designsystem.LocalAppStyle
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.sin

/**
 * Something is loading and how long it takes is not known. In the Material style, Material's own expressive
 * indicator. In the One UI style, the one Samsung's apps show: [OneUiLoadingIndicator].
 *
 * [tinted] is for an indicator drawn on a filled surface, such as a button, where Samsung's blue and green would not
 * show: One UI's dots then take the content colour. Material's indicator is the same either way.
 */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun BurkanLoadingIndicator(modifier: Modifier = Modifier, tinted: Boolean = false) {
    if (LocalAppStyle.current == AppStyle.OneUi) {
        OneUiLoadingIndicator(modifier, if (tinted) tintedDots(LocalContentColor.current) else SamsungDots)
    } else {
        LoadingIndicator(modifier)
    }
}

/**
 * One UI's loading indicator: four dots circling clockwise, a turn a second, which every two seconds draw in, merge
 * into one twice their size, and spring back out to circle again. Measured frame by frame from a recording of Quick
 * Share's "Preparing…" on a Galaxy phone; [dotsPoseAt] holds the timings.
 *
 * Spread out, the dots reach the edges of the indicator, so one 24 dp across is as wide as the icons beside it. They
 * are drawn in [colours], clockwise from the first. With animations removed, and in previews, they hold still,
 * spread out.
 */
@Composable
fun OneUiLoadingIndicator(modifier: Modifier = Modifier, colours: List<Color> = SamsungDots) {
    val still = animationsRemoved()
    val time = if (still) {
        null
    } else {
        rememberInfiniteTransition(label = "loading").animateFloat(
            initialValue = 0f,
            targetValue = CYCLE_MILLIS,
            animationSpec = infiniteRepeatable(tween(CYCLE_MILLIS.toInt(), easing = LinearEasing)),
            label = "loading time",
        )
    }
    Canvas(
        modifier
            .semantics { progressBarRangeInfo = ProgressBarRangeInfo.Indeterminate }
            .size(DefaultSize),
    ) {
        // Read here, while drawing, so that only the drawing is redone on every frame.
        val millis = time?.value ?: 0f
        val pose = dotsPoseAt(millis)
        val extent = size.minDimension / 2
        val turn = START_DEGREES + millis / TURN_MILLIS * 360f
        for (index in DRAW_ORDER) {
            val angle = Math.toRadians((turn + index * 90f).toDouble())
            val offset = Offset(cos(angle).toFloat(), sin(angle).toFloat()) * (pose.orbit * extent)
            drawCircle(colours[index], radius = pose.dot * extent, center = center + offset)
        }
    }
}

/**
 * Where the dots are [millis] into a cycle: [DotsPose.orbit], the radius of the circle their centres lie on, and
 * [DotsPose.dot], each dot's radius, both as shares of the indicator's outer radius. Measured from One UI's own:
 * - for 500 ms the circle draws in to a third of its size, slowly at first;
 * - for 400 ms it closes almost to nothing as the dots grow to twice their size;
 * - for 450 ms they spring back out, quickly at first, shrinking as they go;
 * - and for the last 650 ms they circle as they were.
 */
internal fun dotsPoseAt(millis: Float): DotsPose {
    val t = millis.mod(CYCLE_MILLIS)
    return when {
        t < DRAW_IN_END -> {
            val x = t / DRAW_IN_END
            DotsPose(orbit = lerp(REST_ORBIT, NEAR_ORBIT, x.pow(1.5f)), dot = REST_DOT)
        }

        t < MERGE_END -> {
            val x = (t - DRAW_IN_END) / (MERGE_END - DRAW_IN_END)
            val smooth = x * x * (3 - 2 * x)
            DotsPose(
                orbit = lerp(NEAR_ORBIT, MERGED_ORBIT, 1 - (1 - x).pow(2)),
                dot = lerp(REST_DOT, MERGED_DOT, smooth),
            )
        }

        t < SPRING_OUT_END -> {
            val x = (t - MERGE_END) / (SPRING_OUT_END - MERGE_END)
            val out = 1 - (1 - x).pow(2.5f)
            DotsPose(orbit = lerp(MERGED_ORBIT, REST_ORBIT, out), dot = lerp(MERGED_DOT, REST_DOT, out))
        }

        else -> DotsPose(orbit = REST_ORBIT, dot = REST_DOT)
    }
}

/** [orbit] and [dot] are shares of the indicator's outer radius. */
@Immutable
internal data class DotsPose(val orbit: Float, val dot: Float)

/**
 * Samsung's four, clockwise: bright green, bright blue, teal and a softer blue. All but the bright blue are a little
 * see-through, so that where the dots overlap as they merge, the green shows over the blue.
 */
val SamsungDots = listOf(
    Color(0xCC3AD993),
    Color(0xFF3B79E6),
    Color(0xCC40929A),
    Color(0xCC3E89D8),
)

/** The dots in one [colour], the second pair fainter, for an indicator drawn on a filled surface. */
fun tintedDots(colour: Color): List<Color> {
    val faint = colour.copy(alpha = colour.alpha * 0.6f)
    return listOf(colour, colour, faint, faint)
}

/** The bright blue first, as it is the one that is not see-through; the green last, on top. */
private val DRAW_ORDER = intArrayOf(1, 2, 3, 0)

private val DefaultSize = 48.dp

/** Where the first dot is as the dots start drawing in: on the left, a little above, as in the recording. */
private const val START_DEGREES = 162f

internal const val CYCLE_MILLIS = 2000f
private const val TURN_MILLIS = 1000f
private const val DRAW_IN_END = 500f
private const val MERGE_END = 900f
private const val SPRING_OUT_END = 1350f

internal const val REST_ORBIT = 0.75f
internal const val REST_DOT = 0.25f
private const val NEAR_ORBIT = 0.25f
internal const val MERGED_ORBIT = 0.05f
internal const val MERGED_DOT = 0.5f
