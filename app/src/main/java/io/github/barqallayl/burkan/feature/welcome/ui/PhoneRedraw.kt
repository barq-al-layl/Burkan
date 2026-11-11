package io.github.barqallayl.burkan.feature.welcome.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import io.github.barqallayl.burkan.designsystem.component.PartLook
import io.github.barqallayl.burkan.designsystem.component.PhoneDrawing
import io.github.barqallayl.burkan.designsystem.component.PhoneLooks
import io.github.barqallayl.burkan.designsystem.component.animationsRemoved

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
    val still = animationsRemoved()
    // Played once: coming back to the screen, or turning the phone, shows it as it ended.
    var played by rememberSaveable { mutableStateOf(still) }
    val progress = remember { Animatable(if (played) 1f else 0f) }
    LaunchedEffect(Unit) {
        if (!played) {
            progress.animateTo(1f, tween(REDRAW_MILLIS, delayMillis = START_DELAY_MILLIS, easing = ExpoOut))
            played = true
        }
    }
    PhoneDrawing(
        from = PhoneLooks.all(PartLook.OpenGL),
        to = PhoneLooks.all(PartLook.Vulkan),
        sweep = progress.value,
        modifier = modifier.semantics { contentDescription = description },
    )
}

private const val REDRAW_MILLIS = 1500

/** Long enough for the screen's own transition to have settled first. */
private const val START_DELAY_MILLIS = 350

/** Fast at first and slow to settle, as a redraw that starts at once and finishes gently. */
private val ExpoOut = CubicBezierEasing(0.16f, 1f, 0.3f, 1f)
