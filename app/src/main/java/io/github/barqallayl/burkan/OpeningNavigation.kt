package io.github.barqallayl.burkan

import io.github.barqallayl.burkan.core.navigation.ConnectionRoute
import io.github.barqallayl.burkan.core.navigation.HomeRoute
import io.github.barqallayl.burkan.core.navigation.Route
import io.github.barqallayl.burkan.core.navigation.SetupRoute
import io.github.barqallayl.burkan.core.navigation.WelcomeRoute

/** The screens of getting started, which belong to it and to nothing after it. */
val Route.isGettingStarted: Boolean
    get() = this == WelcomeRoute || this == ConnectionRoute || this == SetupRoute

/** Where a phone in this state opens: Welcome on a fresh install, Setup while getting started otherwise, else Home. */
fun Opening.startRoute(): Route = when {
    !isGettingStarted -> HomeRoute
    showsWelcome -> WelcomeRoute
    else -> SetupRoute
}

/**
 * The route the whole of [stack] is to be replaced with so that it matches this opening, or null when it already
 * does. Getting started is a one-way door, closed from both sides:
 *
 * - Once setup is complete, nothing before Setup stays behind it: Back from the First apply must not reach the choice
 *   of connection, where choosing again would undo the setup just finished.
 * - Once getting started ends, its screens leave the stack for Home; once it begins again (Redo setup, another
 *   connection, a pairing the phone dropped), Home and what is above it leave the stack for Setup.
 *
 * The stack is replaced rather than swapped for a new one, because only a screen taken off the stack has its
 * ViewModel cleared: a stack dropped whole would hand the old screens' ViewModels, and what they held, to the next
 * time those screens open.
 */
fun Opening.routeToReplace(stack: List<Route>): Route? = when {
    !isGettingStarted -> HomeRoute.takeIf { stack.any { it.isGettingStarted } }
    stack.any { !it.isGettingStarted } -> startRoute()
    isSetupComplete -> SetupRoute.takeIf { stack != listOf(SetupRoute) }
    else -> null
}
