package io.github.barqallayl.burkan.core.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.navigation3.rememberViewModelStoreNavEntryDecorator
import androidx.navigation3.runtime.NavEntry
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
import androidx.navigation3.ui.NavDisplay
import io.github.barqallayl.burkan.RoborazziTestApplication
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import kotlin.test.assertFalse
import kotlin.test.assertSame
import kotlin.test.assertTrue

/**
 * Why getting started is ended with [Navigator.replaceAll]: a screen it takes off the stack has its ViewModel
 * cleared, so nothing the screen held comes back the next time it opens, while a screen it keeps on top keeps its own.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = RoborazziTestApplication::class)
class ReplaceAllViewModelTest {

    @get:Rule
    val rule = createComposeRule()

    /** What a screen was given, so it can be told whether it was cleared and compared with a later visit. */
    class ScreenViewModel : ViewModel() {
        var cleared = false
            private set

        override fun onCleared() {
            cleared = true
        }
    }

    private lateinit var navigator: Navigator
    private val given = mutableMapOf<Route, MutableList<ScreenViewModel>>()

    private fun show(start: Route) {
        rule.setContent {
            navigator = rememberNavigator(start)
            NavDisplay(
                backStack = navigator.entries,
                onBack = navigator::pop,
                entryDecorators = listOf(
                    rememberSaveableStateHolderNavEntryDecorator(),
                    rememberViewModelStoreNavEntryDecorator(),
                ),
                entryProvider = { route ->
                    NavEntry(route) {
                        val viewModel = viewModel { ScreenViewModel() }
                        LaunchedEffect(viewModel) {
                            val seen = given.getOrPut(route) { mutableListOf() }
                            if (seen.none { it === viewModel }) seen += viewModel
                        }
                        Box(Modifier.fillMaxSize())
                    }
                },
            )
        }
        rule.waitForIdle()
    }

    @Test
    fun `replacing the stack clears the screens taken off it, and a later visit starts afresh`() {
        show(WelcomeRoute)
        rule.runOnIdle { navigator.push(SetupRoute) }
        rule.waitForIdle()
        val firstSetup = given.getValue(SetupRoute).single()

        rule.runOnIdle { navigator.replaceAll(HomeRoute) }
        rule.waitForIdle()
        assertTrue(firstSetup.cleared, "Setup's ViewModel is cleared once Home replaces it")
        assertTrue(given.getValue(WelcomeRoute).single().cleared)

        rule.runOnIdle { navigator.replaceAll(SetupRoute) }
        rule.waitForIdle()
        val secondSetup = given.getValue(SetupRoute).last()
        assertFalse(secondSetup === firstSetup, "Setup opened again gets a ViewModel of its own")
        assertFalse(secondSetup.cleared)
    }

    @Test
    fun `the screen on top keeps its ViewModel when the ones under it are taken away`() {
        show(WelcomeRoute)
        // One at a time, as the user goes, so that each screen is shown and given its ViewModel.
        rule.runOnIdle { navigator.push(ConnectionRoute) }
        rule.waitForIdle()
        rule.runOnIdle { navigator.push(SetupRoute) }
        rule.waitForIdle()
        val setup = given.getValue(SetupRoute).single()

        rule.runOnIdle { navigator.replaceAll(SetupRoute) }
        rule.waitForIdle()

        assertSame(setup, given.getValue(SetupRoute).last())
        assertFalse(setup.cleared, "Setup, still on top for the First apply, keeps what it holds")
        assertTrue(given.getValue(WelcomeRoute).single().cleared)
        assertTrue(given.getValue(ConnectionRoute).single().cleared)
    }
}
