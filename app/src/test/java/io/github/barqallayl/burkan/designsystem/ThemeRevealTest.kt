package io.github.barqallayl.burkan.designsystem

import android.os.Looper
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.junit4.createComposeRule
import io.github.barqallayl.burkan.RoborazziTestApplication
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/** When the look that [ThemeReveal] shows changes, as one change follows another. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = RoborazziTestApplication::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class ThemeRevealTest {

    @get:Rule
    val rule = createComposeRule()

    private var target by mutableStateOf("first")
    private val shown = mutableListOf<String>()
    private var scene: RevealScene? = null

    private fun start() {
        rule.mainClock.autoAdvance = false
        rule.setContent {
            ThemeReveal(target) { applied ->
                if (shown.lastOrNull() != applied) shown += applied
                scene = LocalRevealScene.current
                Box(Modifier.fillMaxSize())
            }
        }
        pass(0)
    }

    /**
     * Lets [millis] go by a frame at a time, as they do on a phone. Between frames the main thread does what was
     * waiting on it: it hears that the target changed, and it is handed the picture of the old look.
     */
    private fun pass(millis: Long) {
        var left = millis
        do {
            shadowOf(Looper.getMainLooper()).idle()
            val step = minOf(left, FRAME_MILLIS)
            rule.mainClock.advanceTimeBy(step, ignoreFrameDuration = true)
            left -= step
        } while (left > 0)
        shadowOf(Looper.getMainLooper()).idle()
        rule.mainClock.advanceTimeByFrame()
    }

    private fun radius(): Float = checkNotNull(scene).radius()

    private companion object {
        const val FRAME_MILLIS = 16L
    }

    @Test
    fun `a change shows without a wait`() {
        start()

        target = "second"
        pass(48)

        assertEquals(listOf("first", "second"), shown)
    }

    @Test
    fun `of two changes made together only the last shows`() {
        start()

        target = "second"
        target = "third"
        pass(300)

        assertEquals(listOf("first", "third"), shown)
    }

    @Test
    fun `a change made during a reveal takes over from it and spreads in its turn`() {
        start()
        target = "second"
        pass(200)
        assertEquals("second", shown.last())
        val firstReveal = radius()
        assertTrue(firstReveal > 0f, "the first reveal has not begun")

        target = "third"
        pass(48)
        val afterTheTap = radius()
        pass(48)

        // The new look is shown already, in a circle of its own that starts small and is growing.
        assertEquals(listOf("first", "second", "third"), shown)
        assertTrue(afterTheTap < firstReveal, "the second reveal began at $afterTheTap, where the first had got to")
        assertTrue(radius() > afterTheTap, "the second reveal stood still at ${radius()}")
    }

    @Test
    fun `a change taken back before it was heard changes nothing`() {
        start()

        target = "second"
        target = "first"
        pass(400)

        assertEquals(listOf("first"), shown)
    }

    @Test
    fun `a control holds its own change back until the reveal begins`() {
        val held = mutableListOf<Pair<String, String>>()
        rule.mainClock.autoAdvance = false
        rule.setContent {
            ThemeReveal(target) { applied ->
                val control = heldUntilRevealed(target)
                if (held.lastOrNull() != applied to control) held += applied to control
                Box(Modifier.fillMaxSize())
            }
        }
        pass(0)

        target = "second"
        pass(200)

        // The control never showed the new choice while the look was still the old one.
        assertTrue(("first" to "second") !in held, "the control changed first: $held")
        assertEquals("second" to "second", held.last())
    }

    @Test
    fun `a control shows a change that no reveal follows`() {
        var choice by mutableStateOf("a")
        var control = ""
        rule.mainClock.autoAdvance = false
        rule.setContent {
            ThemeReveal(target) {
                control = heldUntilRevealed(choice)
                Box(Modifier.fillMaxSize())
            }
        }
        pass(0)

        choice = "b"
        pass(120)

        assertEquals("b", control)
    }
}
