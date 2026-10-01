package io.github.barqallayl.burkan.feature.log.ui

import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesIntoMap
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.binding
import dev.zacsweers.metrox.viewmodel.ViewModelKey
import io.github.barqallayl.burkan.feature.log.data.RunLogStorage
import io.github.barqallayl.burkan.feature.log.model.RunLogEntry
import org.orbitmvi.orbit.OrbitContainerHost
import org.orbitmvi.orbit.viewmodel.orbitContainer
import kotlin.time.Instant

/** [runs] is null until the log has been read. A run is identified by when it started. */
@Immutable
data class LogState(val runs: List<RunLogEntry>? = null, val expanded: Set<Instant> = emptySet())

sealed interface LogSideEffect {
    /** The screen turns the runs into text; a ViewModel holds no user-facing text. */
    data class Share(val runs: List<RunLogEntry>) : LogSideEffect
    data object Back : LogSideEffect
}

@Inject
@ViewModelKey
@ContributesIntoMap(AppScope::class, binding = binding<ViewModel>())
class LogViewModel(
    private val runLog: RunLogStorage,
) : OrbitContainerHost<LogState, LogState, LogSideEffect>, ViewModel() {

    override val container = orbitContainer<LogState, LogSideEffect>(LogState()) {
        runLog.runs.collect { runs -> reduce { state.copy(runs = runs) } }
    }

    fun toggle(run: RunLogEntry) = intent {
        val id = run.startedAt
        reduce { state.copy(expanded = if (id in state.expanded) state.expanded - id else state.expanded + id) }
    }

    fun share() = intent {
        val runs = state.runs
        if (!runs.isNullOrEmpty()) postSideEffect(LogSideEffect.Share(runs))
    }

    fun back() = intent { postSideEffect(LogSideEffect.Back) }
}
