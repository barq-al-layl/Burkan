package io.github.barqallayl.burkan.feature.welcome.ui

import android.provider.Settings
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp

/**
 * A phone, drawn in the style's own colours, whose surfaces switch one after another: the status bar, which is System
 * UI, then the home screen's icons, which are the launcher, then the keyboard. It is what Burkan does to a phone, and
 * claims nothing about what that does for it.
 *
 * Each part is drawn in the scheme's neutral tone until a line passing down the screen reaches it, and in the primary
 * colour after. It plays once, about a second and a half from the screen opening, then rests on the redrawn phone;
 * with Android's animations removed, and in previews, it is drawn redrawn from the start. TalkBack hears
 * [description], not the drawing.
 */
@Composable
fun PhoneRedraw(description: String, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val still = LocalInspectionMode.current ||
        Settings.Global.getFloat(context.contentResolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f) == 0f
    // Played once: coming back to the screen, or turning the phone, shows it as it ended.
    var played by rememberSaveable { mutableStateOf(still) }
    val progress = remember { Animatable(if (played) 1f else 0f) }
    LaunchedEffect(Unit) {
        if (!played) {
            progress.animateTo(1f, tween(REDRAW_MILLIS, delayMillis = START_DELAY_MILLIS, easing = ExpoOut))
            played = true
        }
    }
    val scheme = MaterialTheme.colorScheme
    val colours = PhoneColours(
        body = scheme.surfaceContainerHigh,
        frame = scheme.outline,
        screen = scheme.surfaceContainerLowest,
        before = scheme.surfaceContainerHighest,
        after = scheme.primary,
        panelAfter = scheme.primaryContainer,
        sweep = scheme.primary,
    )
    Canvas(modifier = modifier.semantics { contentDescription = description }) {
        drawPhone(progress.value, colours)
    }
}

/** The colours the phone is drawn in; [before] and [after] are a part's colour before and after it is redrawn. */
private class PhoneColours(
    val body: Color,
    val frame: Color,
    val screen: Color,
    val before: Color,
    val after: Color,
    val panelAfter: Color,
    val sweep: Color,
)

/** The phone at [progress], 0 before anything is redrawn and 1 once all of it is, fitted and centred in the canvas. */
private fun DrawScope.drawPhone(progress: Float, colours: PhoneColours) {
    // A phone's proportions, as large as the room allows.
    val height = minOf(size.height, size.width / PHONE_ASPECT)
    val width = height * PHONE_ASPECT
    val left = (size.width - width) / 2
    val top = (size.height - height) / 2
    val frame = 2.dp.toPx()

    drawRoundRect(
        colours.body,
        topLeft = Offset(left, top),
        size = Size(width, height),
        cornerRadius = CornerRadius(width * 0.16f),
    )
    drawRoundRect(
        colours.frame,
        topLeft = Offset(left + frame / 2, top + frame / 2),
        size = Size(width - frame, height - frame),
        cornerRadius = CornerRadius(width * 0.16f),
        style = Stroke(frame),
    )
    val inset = width * 0.055f
    val screen = Area(left + inset, top + inset, width - inset * 2, height - inset * 2)
    drawRoundRect(
        colours.screen,
        topLeft = Offset(screen.left, screen.top),
        size = Size(screen.width, screen.height),
        cornerRadius = CornerRadius(width * 0.11f),
    )

    // The line that redraws the screen, from just above it to just below it, and how far behind it a part takes to
    // change colour.
    val fade = screen.height * 0.12f
    val sweepY = screen.top - fade + (screen.height + fade * 2) * progress
    val redrawn = { y: Float -> ((sweepY - y) / fade).coerceIn(0f, 1f) }
    val colourAt = { y: Float -> lerp(colours.before, colours.after, redrawn(y)) }

    drawStatusBar(screen, colourAt)
    drawHomeScreen(screen, colourAt)
    drawKeyboard(screen, colours, redrawn, colourAt)

    // The line itself, soft above and sharp at its edge, gone once the whole screen is redrawn.
    if (progress in 0.001f..0.999f) {
        val band = fade * 0.9f
        val bandTop = (sweepY - band).coerceAtLeast(screen.top)
        val bandBottom = sweepY.coerceAtMost(screen.top + screen.height)
        if (bandBottom > bandTop) {
            drawRect(
                Brush.verticalGradient(
                    0f to colours.sweep.copy(alpha = 0f),
                    1f to colours.sweep.copy(alpha = 0.28f),
                    startY = sweepY - band,
                    endY = sweepY,
                ),
                topLeft = Offset(screen.left, bandTop),
                size = Size(screen.width, bandBottom - bandTop),
            )
        }
        if (sweepY in screen.top..(screen.top + screen.height)) {
            drawLine(
                colours.sweep.copy(alpha = 0.8f),
                start = Offset(screen.left + screen.width * 0.04f, sweepY),
                end = Offset(screen.left + screen.width * 0.96f, sweepY),
                strokeWidth = 1.5.dp.toPx(),
            )
        }
    }
}

/** The status bar: the time on the left, and the signal, Wi-Fi and battery on the right. */
private fun DrawScope.drawStatusBar(screen: Area, colourAt: (Float) -> Color) {
    val y = screen.top + screen.height * 0.035f
    val h = screen.height * 0.026f
    val margin = screen.width * 0.09f
    val colour = colourAt(y + h / 2)
    drawRoundRect(
        colour,
        topLeft = Offset(screen.left + margin, y),
        size = Size(screen.width * 0.17f, h),
        cornerRadius = CornerRadius(h / 2),
    )
    val dot = h
    repeat(3) { index ->
        val x = screen.left + screen.width - margin - dot * (index + 1) - dot * 0.6f * index
        drawRoundRect(colour, topLeft = Offset(x, y), size = Size(dot, dot), cornerRadius = CornerRadius(dot * 0.3f))
    }
}

/** The home screen: a grid of app icons, four by four, then the row of the dock. */
private fun DrawScope.drawHomeScreen(screen: Area, colourAt: (Float) -> Color) {
    val columns = 4
    val margin = screen.width * 0.11f
    val gap = (screen.width - margin * 2) / columns
    val tile = gap * 0.62f
    val first = screen.top + screen.height * 0.13f
    val rows = listOf(0, 1, 2, 3).map { first + it * gap * 1.12f } +
        // The dock, a little apart from the grid.
        (first + 4 * gap * 1.12f + gap * 0.25f)
    rows.forEach { y ->
        val colour = colourAt(y + tile / 2)
        repeat(columns) { column ->
            val x = screen.left + margin + column * gap + (gap - tile) / 2
            drawRoundRect(
                colour,
                topLeft = Offset(x, y),
                size = Size(tile, tile),
                cornerRadius = CornerRadius(tile * 0.32f),
            )
        }
    }
}

/** The keyboard, open at the bottom: its panel, three rows of keys and the space bar. */
private fun DrawScope.drawKeyboard(
    screen: Area,
    colours: PhoneColours,
    redrawn: (Float) -> Float,
    colourAt: (Float) -> Color,
) {
    val panelTop = screen.top + screen.height * 0.70f
    val panel = Area(screen.left, panelTop, screen.width, screen.top + screen.height - panelTop)
    drawRoundRect(
        lerp(colours.before, colours.panelAfter, redrawn(panel.top + panel.height * 0.1f)).copy(alpha = 0.55f),
        topLeft = Offset(panel.left, panel.top),
        size = Size(panel.width, panel.height),
        cornerRadius = CornerRadius(screen.width * 0.08f),
    )
    val rowCounts = listOf(10, 9, 7)
    val margin = panel.width * 0.05f
    val keyGap = panel.width * 0.012f
    val keyHeight = panel.height * 0.16f
    val rowGap = panel.height * 0.06f
    var y = panel.top + panel.height * 0.12f
    rowCounts.forEach { count ->
        val keyWidth = (panel.width - margin * 2 - keyGap * 9) / 10
        val rowWidth = keyWidth * count + keyGap * (count - 1)
        val start = panel.left + (panel.width - rowWidth) / 2
        val colour = colourAt(y + keyHeight / 2)
        repeat(count) { index ->
            drawRoundRect(
                colour,
                topLeft = Offset(start + index * (keyWidth + keyGap), y),
                size = Size(keyWidth, keyHeight),
                cornerRadius = CornerRadius(keyHeight * 0.25f),
            )
        }
        y += keyHeight + rowGap
    }
    val spaceWidth = panel.width * 0.46f
    drawRoundRect(
        colourAt(y + keyHeight / 2),
        topLeft = Offset(panel.left + (panel.width - spaceWidth) / 2, y),
        size = Size(spaceWidth, keyHeight),
        cornerRadius = CornerRadius(keyHeight * 0.5f),
    )
}

private class Area(val left: Float, val top: Float, val width: Float, val height: Float)

/** A phone's width over its height. */
private const val PHONE_ASPECT = 0.5f

private const val REDRAW_MILLIS = 1500

/** Long enough for the screen's own transition to have settled first. */
private const val START_DELAY_MILLIS = 350

/** Fast at first and slow to settle, as a redraw that starts at once and finishes gently. */
private val ExpoOut = CubicBezierEasing(0.16f, 1f, 0.3f, 1f)
