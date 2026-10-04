package io.github.barqallayl.burkan.feature.log.data

import androidx.compose.runtime.Immutable
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.Named
import dev.zacsweers.metro.SingleIn
import io.github.barqallayl.burkan.core.di.AppBindings
import io.github.barqallayl.burkan.feature.log.model.RunLogEntry
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

/** The stored runs, newest first. [isUnreadable] says they could not be read: [runs] is then empty. */
@Immutable
data class RunLog(val runs: List<RunLogEntry>, val isUnreadable: Boolean)

/** The run log, read when the process starts and kept current. Null only until the first read. */
@Inject
@SingleIn(AppScope::class)
class RunLogStore(storage: RunLogStorage, @Named(AppBindings.APP_SCOPE) scope: CoroutineScope) {
    val log: StateFlow<RunLog?> =
        combine(storage.runs, storage.unreadable, ::RunLog).stateIn(scope, SharingStarted.Eagerly, null)
}
