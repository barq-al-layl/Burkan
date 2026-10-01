package io.github.barqallayl.burkan

import org.orbitmvi.orbit.ContainerHost
import org.orbitmvi.orbit.test.Item
import org.orbitmvi.orbit.test.OrbitScopedTestContextInternal

/** Skips states until one [matches]: a flow collected in `onCreate` may emit states the test does not care about. */
suspend fun <S : Any, H : ContainerHost<S, *>> OrbitScopedTestContextInternal<S, S, *, H>.awaitStateMatching(
    matches: (S) -> Boolean,
): S {
    while (true) {
        val item = awaitItem()
        if (item is Item.StateItem && matches(item.value)) return item.value
    }
}
