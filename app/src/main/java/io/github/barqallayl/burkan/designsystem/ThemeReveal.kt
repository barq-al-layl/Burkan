package io.github.barqallayl.burkan.designsystem

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.ClipOp
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.layer.GraphicsLayer
import androidx.compose.ui.graphics.layer.drawLayer
import androidx.compose.ui.graphics.rememberGraphicsLayer
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.onGloballyPositioned
import kotlin.math.hypot
import kotlin.math.max

/** Where on the screen the user last put a finger down: the point a change of theme spreads from. */
class RevealOrigin {
    var onScreen: Offset? = null
}

val LocalRevealOrigin = staticCompositionLocalOf { RevealOrigin() }

/**
 * Notes where each touch inside this element starts, without taking the touch. The screens and the sheets both
 * carry it: a sheet is a window of its own, so the app's own window never sees a tap on it.
 */
fun Modifier.recordsRevealOrigin(): Modifier = composed {
    val origin = LocalRevealOrigin.current
    var coordinates by remember { mutableStateOf<LayoutCoordinates?>(null) }
    onGloballyPositioned { coordinates = it }.pointerInput(origin) {
        awaitEachGesture {
            val down = awaitFirstDown(requireUnconsumed = false, pass = PointerEventPass.Initial)
            coordinates?.takeIf { it.isAttached }?.let { origin.onScreen = it.localToScreen(down.position) }
        }
    }
}

/**
 * What a change of theme shares with whatever floats over the screen in a window of its own, a sheet above all, so
 * that it changes in the same circle as the screen behind it. [ThemeReveal] provides it; a [RevealParticipant] joins.
 */
class RevealScene internal constructor() {
    internal val participants = mutableListOf<RevealParticipantState>()

    /** The middle of the circle, on the screen, which every window can place in its own coordinates. */
    internal var centreOnScreen by mutableStateOf(Offset.Unspecified)

    /** How far the circle has spread, in pixels. */
    internal var radius: () -> Float = { 0f }
}

internal class RevealParticipantState(val layer: GraphicsLayer) {
    /** A picture of the old look, while the circle is spreading. */
    var old by mutableStateOf<ImageBitmap?>(null)
}

val LocalRevealScene = staticCompositionLocalOf<RevealScene?> { null }

/**
 * Shows [content] for [target], and when [target] changes, reveals the new look in a circle that grows from where
 * the user tapped until it covers the screen.
 *
 * The content is drawn through a layer so that a picture of it can be taken. On a change the picture of the old
 * look is laid over the content, the content switches at once underneath, and the picture is cut away from the
 * tap outwards. Nothing is cross-faded: inside the circle is the new look, outside it the old.
 */
@Composable
fun <T> ThemeReveal(target: T, content: @Composable (applied: T) -> Unit) {
    var applied by remember { mutableStateOf(target) }
    var old by remember { mutableStateOf<ImageBitmap?>(null) }
    var centre by remember { mutableStateOf(Offset.Unspecified) }
    var coordinates by remember { mutableStateOf<LayoutCoordinates?>(null) }
    val progress = remember { Animatable(0f) }
    val layer = rememberGraphicsLayer()
    val origin = LocalRevealOrigin.current
    var reach by remember { mutableFloatStateOf(0f) }
    val scene = remember { RevealScene().apply { radius = { reach * progress.value } } }

    LaunchedEffect(target) {
        if (target == applied) {
            // A change taken back before its reveal finished: nothing is left to reveal.
            old = null
            scene.participants.forEach { it.old = null }
            return@LaunchedEffect
        }
        old = layer.toImageBitmap()
        // Whatever floats over the screen is pictured as it is now too, before anything changes under it.
        scene.participants.forEach { participant ->
            val size = participant.layer.size
            participant.old = if (size.width > 0 && size.height > 0) participant.layer.toImageBitmap() else null
        }
        val box = coordinates?.takeIf { it.isAttached }
        centre = origin.onScreen?.let { tap -> box?.screenToLocal(tap) } ?: Offset.Unspecified
        if (box != null) {
            val width = box.size.width.toFloat()
            val height = box.size.height.toFloat()
            val from = if (centre == Offset.Unspecified) Offset(width / 2, height / 2) else centre
            reach = hypot(max(from.x, width - from.x), max(from.y, height - from.y))
            scene.centreOnScreen = box.localToScreen(from)
        }
        progress.snapTo(0f)
        applied = target
        progress.animateTo(1f, tween(REVEAL_MILLIS, easing = FastOutSlowInEasing))
        old = null
        scene.participants.forEach { it.old = null }
    }

    Box(modifier = Modifier.fillMaxSize().onGloballyPositioned { coordinates = it }) {
        Box(
            modifier = Modifier.fillMaxSize().drawWithContent {
                layer.record { this@drawWithContent.drawContent() }
                drawLayer(layer)
            },
        ) {
            CompositionLocalProvider(LocalRevealScene provides scene) { content(applied) }
        }
        old?.let { picture ->
            Canvas(modifier = Modifier.fillMaxSize()) {
                val from = if (centre == Offset.Unspecified) center else centre
                // Far enough to reach the corner furthest from the tap.
                val reach = hypot(max(from.x, size.width - from.x), max(from.y, size.height - from.y))
                val hole = Path().apply { addOval(Rect(from, reach * progress.value)) }
                clipPath(hole, clipOp = ClipOp.Difference) { drawImage(picture) }
            }
        }
    }
}

/**
 * Something in a window of its own that changes with the theme in the same circle as the screen: inside the circle
 * its new look, outside it a picture of the old one. The picture is held to the bottom edge, where a sheet stands,
 * in case the new look is a different height.
 */
@Composable
fun RevealParticipant(modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    val scene = LocalRevealScene.current
    if (scene == null) {
        Box(modifier = modifier) { content() }
        return
    }
    val layer = rememberGraphicsLayer()
    val state = remember(layer) { RevealParticipantState(layer) }
    var coordinates by remember { mutableStateOf<LayoutCoordinates?>(null) }
    DisposableEffect(scene, state) {
        scene.participants += state
        onDispose { scene.participants -= state }
    }
    Box(
        modifier = modifier
            .onGloballyPositioned { coordinates = it }
            .drawWithContent {
                layer.record { this@drawWithContent.drawContent() }
                val picture = state.old
                if (picture == null) {
                    drawLayer(layer)
                    return@drawWithContent
                }
                val from = coordinates?.takeIf { it.isAttached && scene.centreOnScreen != Offset.Unspecified }
                    ?.screenToLocal(scene.centreOnScreen)
                    ?: center
                val hole = Path().apply { addOval(Rect(from, scene.radius())) }
                clipPath(hole) { drawLayer(layer) }
                clipPath(hole, clipOp = ClipOp.Difference) {
                    drawImage(picture, topLeft = Offset(0f, size.height - picture.height))
                }
            },
    ) {
        content()
    }
}

private const val REVEAL_MILLIS = 550
