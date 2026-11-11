package io.github.barqallayl.burkan.core.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSerializable
import androidx.compose.runtime.snapshots.Snapshot
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.serialization.NavBackStackSerializer

/** The back stack. Created in `App.kt` and reached through [LocalNavigator]; nothing outside the composition navigates. */
class Navigator(private val backStack: NavBackStack<Route>) {

    val entries: List<Route> get() = backStack

    /** Whether there is a destination behind this one for [pop] to go back to. Read in composition, it is observed. */
    val canPop: Boolean get() = backStack.size > 1

    fun push(route: Route) {
        backStack.add(route)
    }

    /** Leaves the last destination in place, since an empty back stack has nothing to show. */
    fun pop() {
        if (backStack.size > 1) {
            backStack.removeAt(backStack.lastIndex)
        }
    }

    fun replaceAll(route: Route) {
        Snapshot.withMutableSnapshot {
            backStack.clear()
            backStack.add(route)
        }
    }
}

val LocalNavigator = staticCompositionLocalOf<Navigator> { error("No Navigator provided") }

/**
 * A [Navigator] whose back stack survives configuration changes and process death. A different [key] replaces the
 * whole back stack with one holding [start]; a different [start] under the same key changes nothing, so that leaving
 * Welcome, which makes Setup the place to start, does not take Welcome out from under the Back gesture.
 */
@Composable
fun rememberNavigator(start: Route, key: Any = start): Navigator {
    val backStack = rememberSerializable(key, serializer = NavBackStackSerializer(Route.serializer())) {
        NavBackStack(start)
    }
    return remember(backStack) { Navigator(backStack) }
}
