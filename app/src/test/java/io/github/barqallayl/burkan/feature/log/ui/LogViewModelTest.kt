package io.github.barqallayl.burkan.feature.log.ui

import io.github.barqallayl.burkan.feature.apply.model.ApplyKind
import io.github.barqallayl.burkan.feature.apply.model.RunTrigger
import io.github.barqallayl.burkan.feature.log.data.FakeRunLogStorage
import io.github.barqallayl.burkan.feature.log.model.RunLogEntry
import io.github.barqallayl.burkan.feature.log.model.RunResult
import kotlinx.coroutines.test.runTest
import org.junit.Test
import org.orbitmvi.orbit.test.testWithInternalState
import kotlin.time.Duration.Companion.seconds
import kotlin.time.Instant

class LogViewModelTest {

    private val run = RunLogEntry(
        startedAt = Instant.parse("2026-10-01T09:30:00Z"),
        trigger = RunTrigger.Manual,
        kind = ApplyKind.Light,
        result = RunResult.Succeeded,
        duration = 6.seconds,
        steps = emptyList(),
    )

    @Test
    fun `a run opens and closes`() = runTest {
        LogViewModel(FakeRunLogStorage(listOf(run))).testWithInternalState(this) {
            val reading = runOnCreate()
            expectInternalState(LogState(runs = listOf(run)))

            containerHost.toggle(run)
            expectInternalState(LogState(runs = listOf(run), expanded = setOf(run.startedAt)))
            containerHost.toggle(run)
            expectInternalState(LogState(runs = listOf(run)))

            reading.cancel()
        }
    }

    @Test
    fun `share hands the runs to the screen`() = runTest {
        LogViewModel(FakeRunLogStorage(listOf(run))).testWithInternalState(this) {
            val reading = runOnCreate()
            expectInternalState(LogState(runs = listOf(run)))

            containerHost.share()
            expectSideEffect(LogSideEffect.Share(listOf(run)))

            reading.cancel()
        }
    }

    @Test
    fun `an empty log has nothing to share`() = runTest {
        LogViewModel(FakeRunLogStorage()).testWithInternalState(this) {
            val reading = runOnCreate()
            expectInternalState(LogState(runs = emptyList()))

            containerHost.share().join()
            expectNoItems()

            reading.cancel()
        }
    }

    @Test
    fun `an unreadable log is shown as such`() = runTest {
        val log = FakeRunLogStorage().apply { unreadable.value = true }

        LogViewModel(log).testWithInternalState(this) {
            val reading = runOnCreate()
            expectInternalState(LogState(runs = emptyList(), isUnreadable = true))

            reading.cancel()
        }
    }
}
