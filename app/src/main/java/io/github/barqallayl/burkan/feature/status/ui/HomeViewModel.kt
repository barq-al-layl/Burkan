package io.github.barqallayl.burkan.feature.status.ui

import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesIntoMap
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.binding
import dev.zacsweers.metrox.viewmodel.ViewModelKey
import io.github.barqallayl.burkan.core.model.AppError
import io.github.barqallayl.burkan.feature.apply.ApplyLauncher
import io.github.barqallayl.burkan.feature.apply.data.ApplyController
import io.github.barqallayl.burkan.feature.apply.data.ApplyRunState
import io.github.barqallayl.burkan.feature.apply.data.AutoApplyStorage
import io.github.barqallayl.burkan.feature.apply.model.ApplyKind
import io.github.barqallayl.burkan.feature.apply.model.WaitReason
import io.github.barqallayl.burkan.feature.log.data.RunLogStorage
import io.github.barqallayl.burkan.feature.log.model.RunLogEntry
import io.github.barqallayl.burkan.feature.status.data.StatusRepository
import io.github.barqallayl.burkan.feature.status.model.RendererStatus
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.combine
import org.orbitmvi.orbit.OrbitContainerHost
import org.orbitmvi.orbit.viewmodel.orbitContainer

/** The status as last read. A refresh keeps showing the previous one until the new one arrives. */
sealed interface StatusState {
    data object Loading : StatusState
    data class Loaded(val status: RendererStatus) : StatusState
    data class Failed(val error: AppError) : StatusState
}

@Immutable
data class HomeState(
    val status: StatusState = StatusState.Loading,
    val isRefreshing: Boolean = false,
    val lastRun: RunLogEntry? = null,
    val run: ApplyRunState = ApplyRunState.Idle,
    val isConfirmingFullApply: Boolean = false,
    /** What the automatic apply after a restart is waiting for, if anything. */
    val waitingFor: WaitReason? = null,
)

sealed interface HomeSideEffect {
    data object OpenLog : HomeSideEffect
    data object OpenSettings : HomeSideEffect
    data object OpenDeveloperOptions : HomeSideEffect
}

@Inject
@ViewModelKey
@ContributesIntoMap(AppScope::class, binding = binding<ViewModel>())
class HomeViewModel(
    private val statusRepository: StatusRepository,
    private val controller: ApplyController,
    private val launcher: ApplyLauncher,
    private val runLog: RunLogStorage,
    private val autoApply: AutoApplyStorage,
) : OrbitContainerHost<HomeState, HomeState, HomeSideEffect>, ViewModel() {

    override val container = orbitContainer<HomeState, HomeSideEffect>(HomeState()) {
        var logRead = false
        combine(runLog.runs, controller.state, autoApply.state) { runs, run, auto ->
            Triple(runs.firstOrNull(), run, auto.waitingFor)
        }.collect { (lastRun, run, waitingFor) ->
            // Every run that ends adds to the log, so a new entry means a run has finished. What it changed is only
            // known by asking again.
            val runFinished = logRead && lastRun != state.lastRun
            logRead = true
            reduce { state.copy(lastRun = lastRun, run = run, waitingFor = waitingFor) }
            if (runFinished) refresh()
        }
    }

    /** Reads the status again. Skipped while a run holds the connection; the run's end refreshes it. */
    fun refresh(): Job = intent {
        if (state.run is ApplyRunState.Running || state.isRefreshing) return@intent
        reduce { state.copy(isRefreshing = true) }
        val status = statusRepository.read().fold({ StatusState.Failed(it) }, { StatusState.Loaded(it) })
        reduce { state.copy(status = status, isRefreshing = false) }
    }

    fun applyNow() = intent { launcher.start(ApplyKind.Light) }

    fun requestRestartAll() = intent { reduce { state.copy(isConfirmingFullApply = true) } }

    fun confirmRestartAll() = intent {
        reduce { state.copy(isConfirmingFullApply = false) }
        launcher.start(ApplyKind.Full)
    }

    fun dismissRestartAll() = intent { reduce { state.copy(isConfirmingFullApply = false) } }

    fun cancelRun() = intent { launcher.cancel() }

    fun openLog() = intent { postSideEffect(HomeSideEffect.OpenLog) }

    fun openSettings() = intent { postSideEffect(HomeSideEffect.OpenSettings) }

    fun openDeveloperOptions() = intent { postSideEffect(HomeSideEffect.OpenDeveloperOptions) }
}
