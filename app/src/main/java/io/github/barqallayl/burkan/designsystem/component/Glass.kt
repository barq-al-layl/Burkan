package io.github.barqallayl.burkan.designsystem.component

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import dev.chrisbanes.haze.HazeInput
import dev.chrisbanes.haze.HazeProgressive
import dev.chrisbanes.haze.blur.HazeBlurStyle
import dev.chrisbanes.haze.blur.HazeColorEffect
import dev.chrisbanes.haze.blur.hazeBlur
import dev.chrisbanes.haze.hazeSource
import io.github.barqallayl.burkan.designsystem.AppStyle
import io.github.barqallayl.burkan.designsystem.LocalAppStyle
import io.github.barqallayl.burkan.designsystem.burkanMotion

/**
 * Frosted glass, for the Material style's bars: what scrolls under a bar shows through it, blurred and washed with
 * the screen's own colour. With nothing under it a bar is simply the colour of the screen, so it needs no line and
 * no fill of its own to appear when the content moves.
 *
 * A screen marks its scrolling content with [glassSource] and each bar over it with [glass], both with the screen's
 * [BurkanAppBar]. One UI's bars are not glass: there the content fades out under them and [glass] does nothing. What
 * is glass in One UI is what floats over the content, a [glassPane].
 */
fun Modifier.glassSource(bar: BurkanAppBar): Modifier = hazeSource(bar.haze)

/**
 * Frosts a bar from the content marked with [glassSource]. [risingOver] is for a bar at the bottom of the screen:
 * the frost comes in gradually over that much of its upper edge, so the content dims away into it and is not cut
 * off at a line. Keep the bar's own content below that edge: under it the frost is at full strength.
 */
@Composable
fun Modifier.glass(bar: BurkanAppBar, risingOver: Dp? = null): Modifier {
    if (LocalAppStyle.current == AppStyle.OneUi) return this
    val surface = MaterialTheme.colorScheme.surface
    val blurs = LocalGlassBlurs.current
    val rise = risingOver?.let { with(LocalDensity.current) { it.toPx() } }
    return hazeBlur(
        input = HazeInput.Sources(bar.haze),
        style = HazeBlurStyle {
            blurEnabled(blurs)
            blurRadius(GlassBlur)
            backgroundColor(surface)
            colorEffects(listOf(HazeColorEffect.tint(surface.copy(alpha = GLASS_TINT))))
            // Where the phone cannot blur, the bar is all but solid, so what is under it does not show through sharp.
            fallbackColorEffect(HazeColorEffect.tint(surface.copy(alpha = GLASS_FALLBACK_TINT)))
            if (rise != null) {
                progressive(
                    HazeProgressive.verticalGradient(startIntensity = 0f, startY = 0f, endIntensity = 1f, endY = rise),
                )
            }
        },
    )
}

/** How far above a bottom bar's buttons its frost comes in. */
val GlassRise: Dp = 24.dp

private val GlassBlur = 24.dp
private val PaneBlur = 12.dp

/**
 * A floating piece of glass, in either style: a round button of One UI's bar, its floating search. What passes
 * under it shows through blurred and washed with [tint], the colour the piece would otherwise be filled with.
 */
@Composable
fun Modifier.glassPane(bar: BurkanAppBar, tint: Color, shape: Shape): Modifier {
    val blurs = LocalGlassBlurs.current
    // What is behind the content, where the content itself is clear: the screen.
    val screen = MaterialTheme.colorScheme.surface
    return clip(shape).hazeBlur(
        input = HazeInput.Sources(bar.haze),
        style = HazeBlurStyle {
            blurEnabled(blurs)
            blurRadius(PaneBlur)
            backgroundColor(screen)
            colorEffects(listOf(HazeColorEffect.tint(tint.copy(alpha = PANE_TINT))))
            fallbackColorEffect(HazeColorEffect.tint(tint.copy(alpha = GLASS_FALLBACK_TINT)))
        },
    )
}

/**
 * What the app's screens are frosted by while a sheet or a dialog is open over them: [covers] counts those open.
 * `App` provides one and blurs the screens with [glassBackdrop]; a sheet or a dialog counts itself in with
 * [GlassBehindWindow].
 *
 * The blur is the app's own, of its own screens. Android can blur whatever is behind a window by itself, but only
 * on phones built to: the Galaxy S23 is not one of them, and asking for it there does nothing.
 */
@Stable
class GlassBackdrop {
    internal var covers by mutableIntStateOf(0)
}

val LocalGlassBackdrop = staticCompositionLocalOf<GlassBackdrop?> { null }

/** Called from inside a sheet or a dialog: the screens behind it are frosted for as long as it is shown. */
@Composable
fun GlassBehindWindow() {
    val backdrop = LocalGlassBackdrop.current ?: return
    DisposableEffect(backdrop) {
        backdrop.covers++
        onDispose { backdrop.covers-- }
    }
}

/** Blurs the screens while [backdrop] says something is open over them. The dimming is the sheet's own. */
@Composable
fun Modifier.glassBackdrop(backdrop: GlassBackdrop): Modifier {
    val covered = backdrop.covers > 0 && LocalGlassBlurs.current
    val radius by animateDpAsState(if (covered) BackdropBlur else 0.dp, burkanMotion.tint(), label = "backdrop blur")
    return if (radius > 0.dp) blur(radius) else this
}

private val BackdropBlur = 16.dp

/**
 * Whether glass really blurs. A preview says no: the screenshot tests draw with a renderer that cannot compile the
 * blur's shader, so there a bar is the all but solid wash it also is on a phone that cannot blur.
 */
val LocalGlassBlurs = staticCompositionLocalOf { true }

/** How much of the screen's colour is laid over the blur: enough for a title to be read over anything. */
private const val GLASS_TINT = 0.55f
private const val GLASS_FALLBACK_TINT = 0.97f

/**
 * A floating piece is small, and what passes under it is mostly a line of text or an icon: blurred as far as a bar
 * blurs, nothing of it would be left to see. So it blurs less, and keeps half of its own colour.
 */
private const val PANE_TINT = 0.5f
