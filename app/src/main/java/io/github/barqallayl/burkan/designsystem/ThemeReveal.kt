package io.github.barqallayl.burkan.designsystem

import android.app.Activity
import android.content.ContextWrapper
import android.graphics.Bitmap
import android.os.Handler
import android.os.Looper
import android.view.PixelCopy
import android.view.View
import android.view.ViewParent
import android.view.Window
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
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.ClipOp
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.layer.GraphicsLayer
import androidx.compose.ui.graphics.layer.drawLayer
import androidx.compose.ui.graphics.rememberGraphicsLayer
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.window.DialogWindowProvider
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
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

    /** Takes that picture; the participant, which knows its own window, says how. */
    var picture: suspend () -> ImageBitmap? = { null }
}

val LocalRevealScene = staticCompositionLocalOf<RevealScene?> { null }

/**
 * Whether a new look has been asked for and is not yet shown: the short while in which the old one is pictured.
 * A control that asked for it keeps its own state as it was until this is over, so that the picture is of the old
 * look entire, and the control changes inside the circle with everything else. See [heldUntilRevealed].
 */
val LocalRevealPending = compositionLocalOf { false }

/**
 * [value] as a control that changes the app's look should show it: the old one until the reveal that shows the new
 * look has begun, then the new. Shown at once, the control would be part way through turning itself on when the
 * old look is pictured, and would be seen to stop there outside the circle.
 *
 * The control and [ThemeReveal] hear of a change separately, a frame or two apart and in either order. So a change
 * is first held for [NOTICE_MILLIS], which is long enough for a reveal to say it is coming, and shown then if none
 * does: a choice that changes nothing to look at, as the system's theme does when it is already the one in use.
 */
@Composable
fun <T> heldUntilRevealed(value: T): T {
    val pending = LocalRevealPending.current
    var shown by remember { mutableStateOf(value) }
    val waited = remember { booleanArrayOf(false) }
    LaunchedEffect(value, pending) {
        when {
            shown == value -> waited[0] = false
            pending -> waited[0] = true
            waited[0] -> {
                waited[0] = false
                shown = value
            }

            else -> {
                delay(NOTICE_MILLIS)
                shown = value
            }
        }
    }
    return shown
}

private const val NOTICE_MILLIS = 48L

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
    val view = LocalView.current
    // A reveal runs in this scope and not in the effect below, so that the next change does not stop it where it
    // stands: it goes on spreading until the moment the next one takes over from it.
    val scope = rememberCoroutineScope()
    val spreading = remember { arrayOfNulls<Job>(1) }

    LaunchedEffect(target) {
        // A change taken back before its own reveal began: the look is already this one, or on its way to it.
        if (target == applied) return@LaunchedEffect
        // The old look is pictured straight away, so the change answers the tap. That only works because nothing
        // is moving yet: the control that was tapped draws no ripple and holds its own change back until the
        // reveal begins ([heldUntilRevealed]). Anything caught half way would be seen to stop there in the picture.
        // The pictures are of the screen as it is, so one taken while an earlier reveal is still spreading shows
        // that reveal as far as it has got, and this one starts from exactly what the user is looking at.
        val picture = pictureOf(view, coordinates, layer)
        // Whatever floats over the screen is pictured as it is now too, before anything changes under it.
        val pictures = scene.participants.toList().map { participant -> participant to participant.picture() }
        spreading[0]?.cancel()
        old = picture
        pictures.forEach { (participant, itsPicture) -> participant.old = itsPicture }
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
        spreading[0] = scope.launch {
            progress.animateTo(1f, tween(REVEAL_MILLIS, easing = FastOutSlowInEasing))
            old = null
            scene.participants.forEach { it.old = null }
        }
    }

    Box(modifier = Modifier
        .fillMaxSize()
        .onGloballyPositioned { coordinates = it }) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .drawWithContent {
                    layer.record { this@drawWithContent.drawContent() }
                    drawLayer(layer)
                },
        ) {
            CompositionLocalProvider(
                LocalRevealScene provides scene,
                LocalRevealPending provides (target != applied),
            ) { content(applied) }
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
    val view = LocalView.current
    state.picture = { pictureOf(view, coordinates, layer) }
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
                    // The picture is of the window, so it also holds whatever of the window shows around this
                    // element: the dimming behind a sheet, at its corners and margins. Only the part of it where
                    // the element itself is drawn is kept, or that dimming would be laid on twice.
                    drawIntoCanvas { canvas ->
                        val bounds = Rect(Offset.Zero, size)
                        canvas.saveLayer(bounds, Paint())
                        drawImage(picture, topLeft = Offset(0f, size.height - picture.height))
                        canvas.saveLayer(bounds, Paint().apply { blendMode = BlendMode.DstIn })
                        drawLayer(layer)
                        canvas.restore()
                        canvas.restore()
                    }
                }
            },
    ) {
        content()
    }
}

private const val REVEAL_MILLIS = 550

/**
 * A picture of what is on the screen now within [coordinates], for the old look to be shown from.
 *
 * It is copied from the window, so it is exactly the frame the user is looking at. Drawing the layer again into a
 * bitmap is not the same thing: a ripple is animated by the system's render thread, and drawn again anywhere else
 * it starts over, so the tap that changed the theme was pictured with its ripple back at the beginning. The layer
 * is still the fallback where there is no window to copy from, as in a preview.
 */
private suspend fun pictureOf(view: View, coordinates: LayoutCoordinates?, layer: GraphicsLayer): ImageBitmap? {
    val bounds = coordinates?.takeIf { it.isAttached }?.boundsInWindow()
    val window = view.hostWindow()
    if (bounds != null && window != null && bounds.width >= 1f && bounds.height >= 1f) {
        copyFromWindow(window, bounds)?.let { return it }
    }
    val size = layer.size
    return if (size.width > 0 && size.height > 0) layer.toImageBitmap() else null
}

private suspend fun copyFromWindow(window: Window, bounds: Rect): ImageBitmap? =
    suspendCancellableCoroutine { continuation ->
        val bitmap = Bitmap.createBitmap(bounds.width.toInt(), bounds.height.toInt(), Bitmap.Config.ARGB_8888)
        val source = android.graphics.Rect(
            bounds.left.toInt(),
            bounds.top.toInt(),
            bounds.left.toInt() + bitmap.width,
            bounds.top.toInt() + bitmap.height,
        )
        try {
            PixelCopy.request(
                window,
                source,
                bitmap,
                { result -> continuation.resume(bitmap.asImageBitmap().takeIf { result == PixelCopy.SUCCESS }) },
                Handler(Looper.getMainLooper()),
            )
        } catch (_: IllegalArgumentException) {
            // The window has no surface yet, or has lost it.
            continuation.resume(null)
        }
    }

/** The window this view is drawn in: a sheet's or a dialog's own when it is in one, the activity's otherwise. */
private fun View.hostWindow(): Window? {
    var ancestor: ViewParent? = parent
    if (this is DialogWindowProvider) return window
    while (ancestor != null) {
        if (ancestor is DialogWindowProvider) return ancestor.window
        ancestor = ancestor.parent
    }
    var holder = context
    while (holder is ContextWrapper) {
        if (holder is Activity) return holder.window
        holder = holder.baseContext
    }
    return null
}
