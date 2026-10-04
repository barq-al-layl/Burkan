package io.github.barqallayl.burkan.feature.apply

import io.github.barqallayl.burkan.core.model.AppError
import io.github.barqallayl.burkan.core.shell.PackageName
import io.github.barqallayl.burkan.feature.apply.data.AutoApplyStorage
import io.github.barqallayl.burkan.feature.apply.data.BootCount
import io.github.barqallayl.burkan.feature.apply.data.LockEvents
import io.github.barqallayl.burkan.feature.apply.data.RecentApps
import io.github.barqallayl.burkan.feature.apply.data.RunAlerts
import io.github.barqallayl.burkan.feature.apply.data.SystemUiRestarts
import io.github.barqallayl.burkan.feature.apply.data.WifiWatch
import io.github.barqallayl.burkan.feature.apply.model.ApplyKind
import io.github.barqallayl.burkan.feature.apply.model.AutoApplyState
import io.github.barqallayl.burkan.feature.apply.model.RestartScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlin.time.Instant

class FakeApplyLauncher : ApplyLauncher {
    val started = mutableListOf<ApplyKind>()
    val scopes = mutableListOf<RestartScope>()
    var automaticStarts = 0
    var lockWaits = 0
    var cancels = 0

    /** False plays Android refusing a foreground service started from the background. */
    var backgroundStartAllowed = true

    override fun start(kind: ApplyKind, scope: RestartScope) {
        started += kind
        scopes += scope
    }

    override fun awaitLock() {
        lockWaits++
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
    var lastBoot: Int? = null

    override suspend fun set(state: AutoApplyState) {
        this.state.value = state
    }

    override suspend fun lastBoot(): Int? = lastBoot

    override suspend fun setLastBoot(count: Int) {
        lastBoot = count
    }
}

/** The phone's boot count, which a test raises to play a restart. */
class FakeBootCount(var count: Int? = 1) : BootCount {
    override fun current(): Int? = count
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

/** The phone locks when a test says so, and stays locked until [locked] is set back to false: the user unlocked. */
/** Usage access as the phone would have it: refused until [allowed], then [recent], most recent first. */
class FakeRecentApps(var allowed: Boolean = false, var recent: List<PackageName> = emptyList()) : RecentApps {
    override fun isAllowed(): Boolean = allowed

    override suspend fun byRecency(): List<PackageName> = if (allowed) recent else emptyList()
}

class FakeLockEvents : LockEvents {
    private val mutableLocks = MutableSharedFlow<Unit>()
    override val locks: Flow<Unit> = mutableLocks
    var locked = false

    suspend fun lock() {
        locked = true
        mutableLocks.emit(Unit)
    }

    override fun isLocked(): Boolean = locked
}

class FakeSystemUiRestarts : SystemUiRestarts {
    private var last: Instant? = null

    override suspend fun last(): Instant? = last

    override suspend fun record(at: Instant) {
        last = at
    }
}
