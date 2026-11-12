package io.github.barqallayl.burkan

import io.github.barqallayl.burkan.core.navigation.ConnectionRoute
import io.github.barqallayl.burkan.core.navigation.HomeRoute
import io.github.barqallayl.burkan.core.navigation.Route
import io.github.barqallayl.burkan.core.navigation.SettingsRoute
import io.github.barqallayl.burkan.core.navigation.SetupRoute
import io.github.barqallayl.burkan.core.navigation.WelcomeRoute
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

/** Getting started as a one-way door: what the back stack is replaced with as the opening changes. */
class OpeningNavigationTest {

    private val welcome = Opening(isGettingStarted = true, showsWelcome = true)
    private val setup = Opening(isGettingStarted = true, showsWelcome = false)
    private val firstApply = Opening(isGettingStarted = true, showsWelcome = false, isSetupComplete = true)
    private val home = Opening(isGettingStarted = false, showsWelcome = false, isSetupComplete = true)

    private val gettingStarted = listOf<Route>(WelcomeRoute, ConnectionRoute, SetupRoute)

    @Test
    fun `each opening starts on its own screen`() {
        assertEquals(WelcomeRoute, welcome.startRoute())
        assertEquals(SetupRoute, setup.startRoute())
        assertEquals(SetupRoute, firstApply.startRoute())
        assertEquals(HomeRoute, home.startRoute())
    }

    @Test
    fun `while getting started, the screens gone through stay for Back`() {
        assertNull(setup.routeToReplace(gettingStarted))
        assertNull(welcome.routeToReplace(listOf(WelcomeRoute, ConnectionRoute)))
    }

    @Test
    fun `once setup is complete, nothing before Setup stays behind it`() {
        assertEquals(SetupRoute, firstApply.routeToReplace(gettingStarted))
        assertNull(firstApply.routeToReplace(listOf(SetupRoute)))
    }

    @Test
    fun `getting started ends in Home alone`() {
        assertEquals(HomeRoute, home.routeToReplace(listOf(SetupRoute)))
        assertEquals(HomeRoute, home.routeToReplace(gettingStarted))
        assertNull(home.routeToReplace(listOf(HomeRoute, SettingsRoute)))
    }

    @Test
    fun `getting started again, after Redo setup or a dropped pairing, leaves Home for Setup alone`() {
        assertEquals(SetupRoute, setup.routeToReplace(listOf(HomeRoute, SettingsRoute)))
        assertEquals(SetupRoute, setup.routeToReplace(listOf(HomeRoute)))
    }
}
