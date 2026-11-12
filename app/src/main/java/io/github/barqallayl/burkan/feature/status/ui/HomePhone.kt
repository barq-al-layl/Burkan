package io.github.barqallayl.burkan.feature.status.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import io.github.barqallayl.burkan.core.model.Renderer
import io.github.barqallayl.burkan.designsystem.component.PartLook
import io.github.barqallayl.burkan.designsystem.component.PhoneDrawing
import io.github.barqallayl.burkan.designsystem.component.PhoneLooks
import io.github.barqallayl.burkan.designsystem.component.animationsRemoved
import io.github.barqallayl.burkan.feature.apply.data.ApplyRunState
import io.github.barqallayl.burkan.feature.status.model.Headline

/** Whether the phone is being read or changed, which the drawn phone shows by a line passing over it again and again. */
val HomeState.isPhoneBusy: Boolean
    get() = run is ApplyRunState.Running || status is StatusState.Loading

/**
 * Home's phone, each part as what it draws with: Vulkan in the accent, OpenGL in grey, and an outline where it is not
 * known, not drawn yet, or being read or changed. The status bar carries a padlock while System UI waits for the lock.
 */
val HomeState.phoneLooks: PhoneLooks
    get() {
        val loaded = (status as? StatusState.Loaded)?.status
        if (isPhoneBusy || loaded == null) return PhoneLooks.all(PartLook.Outline)
        return PhoneLooks(
            statusBar = loaded.systemUi.look,
            homeScreen = loaded.launcher.look,
            keyboard = loaded.keyboard.look,
            statusBarLocked = headline == Headline.WaitingForLock,
        )
    }

private val Renderer.look: PartLook
    get() = when (this) {
        Renderer.Vulkan -> PartLook.Vulkan
        Renderer.OpenGL -> PartLook.OpenGL
        Renderer.NotDrawn, Renderer.Unknown -> PartLook.Outline
    }

/**
 * The phone at the head of Home's status card. While the phone is read or changed, a line passes slowly down it, over
 * and over: it is the card's progress indicator. When a run the user watched ends, the line passes once more and the
 * parts take what the run left them with as it reaches them, as Welcome redraws its phone. Opening Home, or coming back
 * to it, shows the phone as it is, at once. With Android's animations removed nothing moves.
 */
@Composable
fun HomePhone(state: HomeState, modifier: Modifier = Modifier) {
    val still = animationsRemoved()
    val busy = state.isPhoneBusy
    val running = state.run is ApplyRunState.Running
    val target = state.phoneLooks
    var from by remember { mutableStateOf(target) }
    var to by remember { mutableStateOf(target) }
    // Whether the busy spell now ending was a run, which ends in the redraw, and not only a read of the status.
    var watchedRun by remember { mutableStateOf(false) }
    val sweep = remember { Animatable(1f) }
    LaunchedEffect(busy, target, still) {
        if (busy) {
            if (running) watchedRun = true
            from = target
            to = target
            if (still) {
                sweep.snapTo(1f)
                return@LaunchedEffect
            }
            while (true) {
                sweep.snapTo(0f)
                sweep.animateTo(1f, tween(PASS_MILLIS, easing = LinearEasing))
            }
        }
        val redraw = watchedRun && !still && to != target
        watchedRun = false
        if (redraw) {
            from = to
            to = target
            sweep.snapTo(0f)
            sweep.animateTo(1f, tween(REDRAW_MILLIS, easing = ExpoOut))
        } else {
            from = target
            to = target
            sweep.snapTo(1f)
        }
    }
    PhoneDrawing(from = from, to = to, sweep = sweep.value, modifier = modifier)
}

/** The phone's height in the status card: large enough for its three parts to be told apart at a glance. */
val PHONE_HEIGHT = 112.dp

/** One pass of the line while the phone is read or changed: slow, a sign of work, not a countdown. */
private const val PASS_MILLIS = 1800

/** The redraw at the end of a run, as long as Welcome's. */
private const val REDRAW_MILLIS = 1500

/** Fast at first and slow to settle, as Welcome's redraw. */
private val ExpoOut = CubicBezierEasing(0.16f, 1f, 0.3f, 1f)
