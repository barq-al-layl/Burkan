package io.github.barqallayl.burkan.feature.status.ui

import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesIntoMap
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.binding
import dev.zacsweers.metrox.viewmodel.ViewModelKey
import io.github.barqallayl.burkan.core.model.AppError
import io.github.barqallayl.burkan.core.model.Renderer
import io.github.barqallayl.burkan.core.model.RendererStatus
import io.github.barqallayl.burkan.feature.apply.ApplyLauncher
import io.github.barqallayl.burkan.feature.apply.data.ApplyController
import io.github.barqallayl.burkan.feature.apply.data.ApplyRunState
import io.github.barqallayl.burkan.feature.apply.data.AutoApplyStorage
import io.github.barqallayl.burkan.feature.apply.model.ApplyKind
import io.github.barqallayl.burkan.feature.apply.model.WaitReason
import io.github.barqallayl.burkan.feature.log.data.RunLogStorage
import io.github.barqallayl.burkan.feature.log.model.RunLogEntry
import io.github.barqallayl.burkan.feature.status.data.StatusRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.combine
import org.orbitmvi.orbit.OrbitContainerHost
import org.orbitmvi.orbit.viewmodel.orbitContainer
import kotlin.time.Clock
import kotlin.time.Duration.Companion.seconds
import kotlin.time.Instant

/** The status as last read. A refresh keeps showing the previous one until the new one arrives. */
sealed interface StatusState {
    data object Loading : StatusState
    data class Loaded(val status: RendererStatus) : StatusState
    data class Failed(val error: AppError) : StatusState
}

/** [statusReadAt] is when [status] was read, so a resume shortly after does not connect again. */
@Immutable
data class HomeState(
    val status: StatusState = StatusState.Loading,
    val statusReadAt: Instant? = null,
    val isRefreshing: Boolean = false,
    val lastRun: RunLogEntry? = null,
    val run: ApplyRunState = ApplyRunState.Idle,
    val isConfirmingApply: Boolean = false,
    val isConfirmingFullApply: Boolean = false,
    /** What the automatic apply after a restart is waiting for, if anything. */
    val waitingFor: WaitReason? = null,
    /** The run after a restart left System UI for the next time the phone locks. */
    val systemUiAtNextLock: Boolean = false,
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
    private val clock: Clock,
) : OrbitContainerHost<HomeState, HomeState, HomeSideEffect>, ViewModel() {

    override val container = orbitContainer<HomeState, HomeSideEffect>(HomeState()) {
        var logRead = false
        var lockWaitArmed = false
        combine(runLog.runs, controller.state, autoApply.state) { runs, run, auto ->
            Triple(runs.firstOrNull(), run, auto)
        }.collect { (lastRun, run, auto) ->
            // Every run that ends adds to the log, so a new entry means a run has finished.
            val runFinished = logRead && lastRun != state.lastRun
            logRead = true
            reduce {
                state.copy(
                    lastRun = lastRun,
                    run = run,
                    waitingFor = auto.waitingFor,
                    systemUiAtNextLock = auto.systemUiAtNextLock,
                )
            }
            // The service waits for the lock while it lives. If the process has gone since, wait again from here.
            if (auto.systemUiAtNextLock && !lockWaitArmed) launcher.awaitLock()
            lockWaitArmed = auto.systemUiAtNextLock
            if (runFinished && lastRun != null) showRunResult(lastRun)
        }
    }

    /** Called when the screen resumes. Reads the status unless it was read moments ago. */
    fun onResume(): Job = intent {
        val readAt = state.statusReadAt
        val fresh = state.status is StatusState.Loaded && readAt != null && clock.now() - readAt < FRESH_FOR
        if (!fresh) refresh().join()
    }

    /** The screen is going away: nothing will need the connection soon, so close it now. */
    fun onPause(): Job = intent { statusRepository.release() }

    /** Reads the status again. Skipped while a run holds the connection; the run's end refreshes it. */
    fun refresh(): Job = intent {
        if (state.run is ApplyRunState.Running || state.isRefreshing) return@intent
        reduce { state.copy(isRefreshing = true) }
        val status = statusRepository.read().fold({ StatusState.Failed(it) }, { StatusState.Loaded(it) })
        reduce { state.copy(status = status, statusReadAt = clock.now(), isRefreshing = false) }
    }

    /** Apply now. Restarting System UI locks the screen, so the user is told first when that will happen. */
    fun applyNow() = intent {
        val systemUi = (state.status as? StatusState.Loaded)?.status?.systemUi
        if (systemUi == Renderer.Vulkan) {
            launcher.start(ApplyKind.Light)
        } else {
            reduce { state.copy(isConfirmingApply = true) }
        }
    }

    fun confirmApply() = intent {
        reduce { state.copy(isConfirmingApply = false) }
        launcher.start(ApplyKind.Light)
    }

    fun dismissApply() = intent { reduce { state.copy(isConfirmingApply = false) } }

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

    /**
     * Shows what the finished run read at its end. Only when it read nothing (it failed before it could) does Home
     * connect to ask.
     */
    private fun showRunResult(run: RunLogEntry): Job = intent {
        val verified = controller.verified.value
        if (verified != null && verified.at >= run.startedAt) {
            reduce { state.copy(status = StatusState.Loaded(verified.status), statusReadAt = verified.at) }
        } else {
            refresh().join()
        }
    }

    private companion object {
        /** A status read this recently is shown as it is when the screen resumes. */
        val FRESH_FOR = 30.seconds
    }
}
