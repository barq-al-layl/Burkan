package io.github.barqallayl.burkan.feature.apply.data

import io.github.barqallayl.burkan.feature.apply.model.ApplyKind
import io.github.barqallayl.burkan.feature.log.model.RunLogEntry
import io.github.barqallayl.burkan.feature.log.model.RunResult
import kotlin.time.Instant

/** What the Quick Settings tile shows. */
enum class TileStatus {
    /** Vulkan was applied since the phone started, and not undone since. */
    Active,

    /** Nothing was applied since the phone started, or the last word was going back to OpenGL. */
    Inactive,

    /** A run is under way. */
    Busy,

    /** Setup is not done: there is nothing the tile could start. */
    Unavailable,
}

/**
 * The tile's state, from the log alone. Asking the phone itself needs a connection, which is too much for a tile
 * that is redrawn whenever the shade opens; and the renderer goes back to OpenGL at every restart, so a run only
 * counts if it began after [bootedAt]. [runs] is newest first.
 *
 * A run that restarted only the open apps changed nothing about which renderer is set, and is passed over.
 */
fun tileStatus(runs: List<RunLogEntry>, running: Boolean, setupComplete: Boolean, bootedAt: Instant): TileStatus {
    if (!setupComplete) return TileStatus.Unavailable
    if (running) return TileStatus.Busy
    val last = runs.firstOrNull { run ->
        run.startedAt >= bootedAt && run.kind != ApplyKind.OpenApps &&
            (run.result == RunResult.Succeeded || run.result == RunResult.AlreadyApplied || run.result == RunResult.Postponed)
    }
    return if (last != null && last.kind != ApplyKind.Revert) TileStatus.Active else TileStatus.Inactive
}
