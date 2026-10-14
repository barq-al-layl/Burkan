package io.github.barqallayl.burkan.feature.apply.data

import io.github.barqallayl.burkan.feature.apply.model.ApplyKind
import io.github.barqallayl.burkan.feature.apply.model.RunTrigger
import io.github.barqallayl.burkan.feature.log.model.RunLogEntry
import io.github.barqallayl.burkan.feature.log.model.RunResult
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds
import kotlin.time.Instant

class TileStatusTest {

    private val bootedAt = Instant.parse("2026-10-12T06:00:00Z")

    private fun run(kind: ApplyKind, result: RunResult, at: Instant = bootedAt + 10.minutes) =
        RunLogEntry(at, RunTrigger.Manual, kind, result, 5.seconds, emptyList())

    private fun status(vararg runs: RunLogEntry, running: Boolean = false, setupComplete: Boolean = true) =
        tileStatus(runs.toList(), running, setupComplete, bootedAt)

    @Test
    fun `with nothing run since the phone started, Vulkan is not applied`() {
        assertEquals(TileStatus.Inactive, status())
        // Yesterday's apply did not survive the restart.
        assertEquals(TileStatus.Inactive, status(run(ApplyKind.Light, RunResult.Succeeded, at = bootedAt - 3.hours)))
    }

    @Test
    fun `an apply since the phone started, however it found things, makes it active`() {
        assertEquals(TileStatus.Active, status(run(ApplyKind.Light, RunResult.Succeeded)))
        assertEquals(TileStatus.Active, status(run(ApplyKind.Light, RunResult.AlreadyApplied)))
        assertEquals(TileStatus.Active, status(run(ApplyKind.Full, RunResult.Succeeded)))
        // System UI is left for the lock, but new apps are on Vulkan already.
        assertEquals(TileStatus.Active, status(run(ApplyKind.Light, RunResult.Postponed)))
    }

    @Test
    fun `a run that failed or was cancelled changes nothing`() {
        assertEquals(TileStatus.Inactive, status(run(ApplyKind.Light, RunResult.Failed)))
        assertEquals(
            TileStatus.Active,
            status(
                run(ApplyKind.Light, RunResult.Cancelled, at = bootedAt + 20.minutes),
                run(ApplyKind.Light, RunResult.Succeeded),
            ),
        )
    }

    @Test
    fun `going back to OpenGL is the last word until Vulkan is applied again`() {
        val applied = run(ApplyKind.Light, RunResult.Succeeded)
        val reverted = run(ApplyKind.Revert, RunResult.Succeeded, at = bootedAt + 20.minutes)
        val again = run(ApplyKind.Light, RunResult.Succeeded, at = bootedAt + 30.minutes)

        assertEquals(TileStatus.Inactive, status(reverted, applied))
        assertEquals(TileStatus.Active, status(again, reverted, applied))
    }

    @Test
    fun `restarting the open apps says nothing about which renderer is set`() {
        val reverted = run(ApplyKind.Revert, RunResult.Succeeded)
        val caughtUp = run(ApplyKind.OpenApps, RunResult.Succeeded, at = bootedAt + 20.minutes)

        assertEquals(TileStatus.Inactive, status(caughtUp, reverted))
    }

    @Test
    fun `a run under way, or setup not done, makes the tile unusable`() {
        assertEquals(TileStatus.Busy, status(run(ApplyKind.Light, RunResult.Succeeded), running = true))
        assertEquals(TileStatus.Unavailable, status(run(ApplyKind.Light, RunResult.Succeeded), setupComplete = false))
    }
}
