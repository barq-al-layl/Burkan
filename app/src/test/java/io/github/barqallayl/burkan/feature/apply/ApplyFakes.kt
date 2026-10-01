package io.github.barqallayl.burkan.feature.apply

import io.github.barqallayl.burkan.core.model.AppError
import io.github.barqallayl.burkan.feature.apply.data.AutoApplyStorage
import io.github.barqallayl.burkan.feature.apply.data.RunAlerts
import io.github.barqallayl.burkan.feature.apply.data.WifiWatch
import io.github.barqallayl.burkan.feature.apply.model.ApplyKind
import io.github.barqallayl.burkan.feature.apply.model.AutoApplyState
import kotlinx.coroutines.flow.MutableStateFlow

class FakeApplyLauncher : ApplyLauncher {
    val started = mutableListOf<ApplyKind>()
    var automaticStarts = 0
    var cancels = 0

    /** False plays Android refusing a foreground service started from the background. */
    var backgroundStartAllowed = true

    override fun start(kind: ApplyKind) {
        started += kind
    }

    override fun startAutomatic(): Boolean {
        if (!backgroundStartAllowed) return false
        automaticStarts++
        return true
    }

    override fun cancel() {
        cancels++
    }
}

class FakeAutoApplyStorage(initial: AutoApplyState = AutoApplyState()) : AutoApplyStorage {
    override val state = MutableStateFlow(initial)

    override suspend fun set(state: AutoApplyState) {
        this.state.value = state
    }
}

class FakeWifiWatch(var active: Long? = null) : WifiWatch {
    var watching = false

    override fun start() {
        watching = true
    }

    override fun stop() {
        watching = false
    }

    override fun activeNetwork(): Long? = active
}

class FakeRunAlerts : RunAlerts {
    val failures = mutableListOf<AppError?>()
    var readyToApply = 0

    override fun showFailure(error: AppError?) {
        failures += error
    }

    override fun showReadyToApply() {
        readyToApply++
    }
}
