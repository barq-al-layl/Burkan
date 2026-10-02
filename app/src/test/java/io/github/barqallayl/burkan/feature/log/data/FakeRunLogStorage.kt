package io.github.barqallayl.burkan.feature.log.data

import io.github.barqallayl.burkan.feature.log.model.RunLogEntry
import kotlinx.coroutines.flow.MutableStateFlow

class FakeRunLogStorage(runs: List<RunLogEntry> = emptyList()) : RunLogStorage {
    override val runs = MutableStateFlow(runs)
    override val unreadable = MutableStateFlow(false)

    override suspend fun add(entry: RunLogEntry) {
        runs.value = (listOf(entry) + runs.value).take(RunLogStorage.CAPACITY)
    }
}
