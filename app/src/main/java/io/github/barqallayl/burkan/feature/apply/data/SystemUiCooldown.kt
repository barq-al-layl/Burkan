package io.github.barqallayl.burkan.feature.apply.data

import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesBinding
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlin.time.Clock
import kotlin.time.Duration.Companion.seconds
import kotlin.time.Instant

/** When System UI was last restarted by this app. */
interface SystemUiRestarts {
    suspend fun last(): Instant?

    suspend fun record(at: Instant)
}

/** Kept for the life of the process. */
@Inject
@SingleIn(AppScope::class)
@ContributesBinding(AppScope::class)
class InMemorySystemUiRestarts : SystemUiRestarts {
    private var last: Instant? = null

    override suspend fun last(): Instant? = last

    override suspend fun record(at: Instant) {
        last = at
    }
}

/**
 * Never restarts System UI twice within [COOLDOWN]: One UI switches off its customisation add-ons when System UI
 * crashes repeatedly.
 */
@Inject
@SingleIn(AppScope::class)
class SystemUiCooldown(private val restarts: SystemUiRestarts, private val clock: Clock) {

    private val mutex = Mutex()

    /**
     * Records a restart now and returns true, unless one was recorded within [COOLDOWN]. A restart is recorded before
     * it is sent: if the app dies with it, the next run must still wait.
     */
    suspend fun claim(): Boolean = mutex.withLock {
        val now = clock.now()
        val last = restarts.last()
        // A recorded time in the future means the clock was set back; it says nothing about the last minute.
        if (last != null && last <= now && now - last < COOLDOWN) return false
        restarts.record(now)
        true
    }

    companion object {
        val COOLDOWN = 60.seconds
    }
}
