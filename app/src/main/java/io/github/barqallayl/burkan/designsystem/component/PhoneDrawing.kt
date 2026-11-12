package io.github.barqallayl.burkan.designsystem.component

import android.provider.Settings
import androidx.compose.foundation.Canvas
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
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
import androidx.compose.ui.unit.dp

/**
 * How one part of the drawn phone looks: drawn with Vulkan, in the accent; drawn with OpenGL, in the scheme's neutral
 * tone; or not known, or not drawn yet, as an outline with nothing in it.
 */
enum class PartLook { Vulkan, OpenGL, Outline }

/**
 * The phone's three parts as they look, from the top: the status bar, which is System UI, the home screen's icons,
 * which are the launcher, and the keyboard. [statusBarLocked] draws a padlock in the status bar, for System UI that
 * switches at the next lock.
 */
@Immutable
data class PhoneLooks(
    val statusBar: PartLook,
    val homeScreen: PartLook,
    val keyboard: PartLook,
    val statusBarLocked: Boolean = false,
) {
    companion object {
        fun all(look: PartLook) = PhoneLooks(look, look, look)
    }
}

/**
 * True where drawings must hold still: in previews and the screenshots taken of them, which are recorded one frame at
 * a time and would never see a drawing that keeps moving settle.
 */
val LocalStillDrawings = staticCompositionLocalOf { false }

/**
 * Whether the phone has Android's animations removed, or is drawn for a preview: a drawing then shows how it ends,
 * and nothing in it moves.
 */
@Composable
fun animationsRemoved(): Boolean {
    val context = LocalContext.current
    return LocalStillDrawings.current || LocalInspectionMode.current ||
        Settings.Global.getFloat(context.contentResolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f) == 0f
}

/**
 * A phone, drawn in the style's own colours, its parts looking as [from] says until a line passing down the screen
 * reaches them, and as [to] says after. [sweep] is how far down the line is: 0 above the screen, 1 below it, where
 * nothing is left to change and the line is gone. With [from] and [to] the same, it draws the phone as they say,
 * with the line passing over it for as long as [sweep] is between the two.
 *
 * It is a picture and nothing else: whatever it shows, the screen says in words as well.
 */
@Composable
fun PhoneDrawing(from: PhoneLooks, to: PhoneLooks, sweep: Float, modifier: Modifier = Modifier) {
    val scheme = MaterialTheme.colorScheme
    val colours = PhoneColours(
        body = scheme.surfaceContainerHigh,
        frame = scheme.outline,
        screen = scheme.surfaceContainerLowest,
        openGl = scheme.surfaceContainerHighest,
        vulkan = scheme.primary,
        panelVulkan = scheme.primaryContainer,
        outline = scheme.outlineVariant,
        sweep = scheme.primary,
        lock = scheme.onSurfaceVariant,
    )
    Canvas(modifier = modifier) { drawPhone(from, to, sweep, colours) }
}

/** The colours the phone is drawn in. */
private class PhoneColours(
    val body: Color,
    val frame: Color,
    val screen: Color,
    val openGl: Color,
    val vulkan: Color,
    val panelVulkan: Color,
    val outline: Color,
    val sweep: Color,
    val lock: Color,
)

/** A part's fill and the line round it, which an outline has and a filled part does not. */
private class Paint(val fill: Color, val stroke: Color)

private fun PhoneColours.paint(look: PartLook): Paint = when (look) {
    PartLook.Vulkan -> Paint(vulkan, Color.Transparent)
    PartLook.OpenGL -> Paint(openGl, Color.Transparent)
    PartLook.Outline -> Paint(Color.Transparent, outline)
}

/** The keyboard's panel, set behind its keys in a lighter tone of the part's colour. */
private fun PhoneColours.panelPaint(look: PartLook): Paint = when (look) {
    PartLook.Vulkan -> Paint(panelVulkan, Color.Transparent)
    PartLook.OpenGL -> Paint(openGl, Color.Transparent)
    PartLook.Outline -> Paint(Color.Transparent, outline)
}

private fun blend(from: Paint, to: Paint, amount: Float) =
    Paint(lerp(from.fill, to.fill, amount), lerp(from.stroke, to.stroke, amount))

/** The phone, fitted and centred in the canvas. */
private fun DrawScope.drawPhone(from: PhoneLooks, to: PhoneLooks, sweep: Float, colours: PhoneColours) {
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
    // change.
    val fade = screen.height * 0.12f
    val sweepY = screen.top - fade + (screen.height + fade * 2) * sweep
    val redrawn = { y: Float -> ((sweepY - y) / fade).coerceIn(0f, 1f) }
    val stroke = 1.dp.toPx()

    drawStatusBar(screen, stroke) { y -> blend(colours.paint(from.statusBar), colours.paint(to.statusBar), redrawn(y)) }
    if (to.statusBarLocked || from.statusBarLocked) {
        val shown = if (to.statusBarLocked == from.statusBarLocked) 1f else {
            val amount = redrawn(screen.top)
            if (to.statusBarLocked) amount else 1f - amount
        }
        drawPadlock(screen, colours.lock.copy(alpha = colours.lock.alpha * shown))
    }
    drawHomeScreen(screen, stroke) { y ->
        blend(colours.paint(from.homeScreen), colours.paint(to.homeScreen), redrawn(y))
    }
    drawKeyboard(
        screen,
        stroke,
        panelAt = { y -> blend(colours.panelPaint(from.keyboard), colours.panelPaint(to.keyboard), redrawn(y)) },
        keyAt = { y -> blend(colours.paint(from.keyboard), colours.paint(to.keyboard), redrawn(y)) },
    )

    // The line itself, soft above and sharp at its edge, gone once the whole screen is redrawn.
    if (sweep in 0.001f..0.999f) {
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

/** A rounded shape in [paint]: its fill, then the line round it where it is an outline. */
private fun DrawScope.part(paint: Paint, topLeft: Offset, size: Size, radius: Float, stroke: Float) {
    drawRoundRect(paint.fill, topLeft = topLeft, size = size, cornerRadius = CornerRadius(radius))
    if (paint.stroke.alpha > 0f) {
        drawRoundRect(
            paint.stroke,
            topLeft = Offset(topLeft.x + stroke / 2, topLeft.y + stroke / 2),
            size = Size(size.width - stroke, size.height - stroke),
            cornerRadius = CornerRadius(radius),
            style = Stroke(stroke),
        )
    }
}

/** The status bar: the time on the left, and the signal, Wi-Fi and battery on the right. */
private fun DrawScope.drawStatusBar(screen: Area, stroke: Float, paintAt: (Float) -> Paint) {
    val y = screen.top + screen.height * 0.035f
    val h = screen.height * 0.026f
    val margin = screen.width * 0.09f
    val paint = paintAt(y + h / 2)
    part(paint, Offset(screen.left + margin, y), Size(screen.width * 0.17f, h), h / 2, stroke)
    val dot = h
    repeat(3) { index ->
        val x = screen.left + screen.width - margin - dot * (index + 1) - dot * 0.6f * index
        part(paint, Offset(x, y), Size(dot, dot), dot * 0.3f, stroke)
    }
}

/**
 * A padlock in the middle of the status bar: System UI waits for the next lock. It stands taller than the bar, so
 * that it can still be made out on a phone drawn small.
 */
private fun DrawScope.drawPadlock(screen: Area, colour: Color) {
    if (colour.alpha == 0f) return
    val barMiddle = screen.top + screen.height * 0.05f
    val bodyHeight = screen.height * 0.05f
    val bodyWidth = bodyHeight * 1.2f
    val centreX = screen.left + screen.width / 2
    val bodyTop = barMiddle - bodyHeight * 0.2f
    drawRoundRect(
        colour,
        topLeft = Offset(centreX - bodyWidth / 2, bodyTop),
        size = Size(bodyWidth, bodyHeight),
        cornerRadius = CornerRadius(bodyHeight * 0.2f),
    )
    val shackleWidth = bodyWidth * 0.62f
    drawArc(
        colour,
        startAngle = 180f,
        sweepAngle = 180f,
        useCenter = false,
        topLeft = Offset(centreX - shackleWidth / 2, bodyTop - shackleWidth * 0.55f),
        size = Size(shackleWidth, shackleWidth * 1.1f),
        style = Stroke(bodyHeight * 0.22f),
    )
}

/** The home screen: a grid of app icons, four by four, then the row of the dock. */
private fun DrawScope.drawHomeScreen(screen: Area, stroke: Float, paintAt: (Float) -> Paint) {
    val columns = 4
    val margin = screen.width * 0.11f
    val gap = (screen.width - margin * 2) / columns
    val tile = gap * 0.62f
    val first = screen.top + screen.height * 0.13f
    val rows = listOf(0, 1, 2, 3).map { first + it * gap * 1.12f } +
        // The dock, a little apart from the grid.
        (first + 4 * gap * 1.12f + gap * 0.25f)
    rows.forEach { y ->
        val paint = paintAt(y + tile / 2)
        repeat(columns) { column ->
            val x = screen.left + margin + column * gap + (gap - tile) / 2
            part(paint, Offset(x, y), Size(tile, tile), tile * 0.32f, stroke)
        }
    }
}

/** The keyboard, open at the bottom: its panel, three rows of keys and the space bar. */
private fun DrawScope.drawKeyboard(
    screen: Area,
    stroke: Float,
    panelAt: (Float) -> Paint,
    keyAt: (Float) -> Paint,
) {
    val panelTop = screen.top + screen.height * 0.70f
    val panel = Area(screen.left, panelTop, screen.width, screen.top + screen.height - panelTop)
    val panelPaint = panelAt(panel.top + panel.height * 0.1f)
    // The panel's fill is lighter than the keys on it.
    drawRoundRect(
        panelPaint.fill.copy(alpha = panelPaint.fill.alpha * PANEL_ALPHA),
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
        val paint = keyAt(y + keyHeight / 2)
        repeat(count) { index ->
            part(
                paint,
                Offset(start + index * (keyWidth + keyGap), y),
                Size(keyWidth, keyHeight),
                keyHeight * 0.25f,
                stroke,
            )
        }
        y += keyHeight + rowGap
    }
    val spaceWidth = panel.width * 0.46f
    part(
        keyAt(y + keyHeight / 2),
        Offset(panel.left + (panel.width - spaceWidth) / 2, y),
        Size(spaceWidth, keyHeight),
        keyHeight * 0.5f,
        stroke,
    )
}

private class Area(val left: Float, val top: Float, val width: Float, val height: Float)

/** A phone's width over its height. */
private const val PHONE_ASPECT = 0.5f

/** How much of its colour the keyboard's panel takes, behind the keys. */
private const val PANEL_ALPHA = 0.55f
