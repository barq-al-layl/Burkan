package io.github.barqallayl.burkan.core.navigation

import androidx.navigation3.runtime.NavBackStack
import org.junit.Test
import kotlin.test.assertEquals

class NavigatorTest {

    // NavBackStack does not implement equals, so each assertion compares a copy.
    private val navigator = Navigator(NavBackStack(SetupRoute))

    @Test
    fun `push adds the route on top`() {
        navigator.push(HomeRoute)
        navigator.push(SettingsRoute)

        assertEquals(listOf(SetupRoute, HomeRoute, SettingsRoute), navigator.entries.toList())
    }

    @Test
    fun `pop removes the top route`() {
        navigator.push(LogRoute)

        navigator.pop()

        assertEquals(listOf<Route>(SetupRoute), navigator.entries.toList())
    }

    @Test
    fun `pop keeps the last route`() {
        navigator.pop()

        assertEquals(listOf<Route>(SetupRoute), navigator.entries.toList())
    }

    @Test
    fun `replaceAll leaves only the new route`() {
        navigator.push(SettingsRoute)

        navigator.replaceAll(HomeRoute)

        assertEquals(listOf<Route>(HomeRoute), navigator.entries.toList())
    }
}
