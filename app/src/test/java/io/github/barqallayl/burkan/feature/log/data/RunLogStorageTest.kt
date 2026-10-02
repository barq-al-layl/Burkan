package io.github.barqallayl.burkan.feature.log.data

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import io.github.barqallayl.burkan.core.model.AppErrorType
import io.github.barqallayl.burkan.feature.apply.model.ApplyKind
import io.github.barqallayl.burkan.feature.apply.model.RestoredSetting
import io.github.barqallayl.burkan.feature.apply.model.RunTrigger
import io.github.barqallayl.burkan.feature.apply.model.StepKind
import io.github.barqallayl.burkan.feature.log.model.LoggedStep
import io.github.barqallayl.burkan.feature.log.model.RunLogEntry
import io.github.barqallayl.burkan.feature.log.model.RunResult
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.seconds
import kotlin.time.Instant

class RunLogStorageTest {

    @get:Rule
    val folder = TemporaryFolder()

    private val fullRun = RunLogEntry(
        startedAt = Instant.parse("2026-10-01T09:30:00Z"),
        trigger = RunTrigger.Manual,
        kind = ApplyKind.Full,
        result = RunResult.Failed,
        duration = 73.seconds,
        steps = listOf(
            LoggedStep(StepKind.SetRenderer),
            LoggedStep(StepKind.StopApps(600)),
            LoggedStep(StepKind.RestartSystemUi, AppErrorType.ConnectionLost),
            LoggedStep(StepKind.RestoreSetting(RestoredSetting.AccessibilityServices)),
        ),
    )

    private val unreachedRun = fullRun.copy(
        kind = ApplyKind.Light,
        steps = emptyList(),
        error = AppErrorType.NoWifi,
    )

    @Test
    fun `a run reads back as it was written`() = runTest {
        val storage = DataStoreRunLogStorage(dataStore())

        storage.add(fullRun)
        storage.add(unreachedRun)

        assertEquals(listOf(unreachedRun, fullRun), storage.runs.first())
    }

    @Test
    fun `the newest fifty are kept, newest first`() = runTest {
        val storage = DataStoreRunLogStorage(dataStore())
        val runs = (1..51).map { minute ->
            fullRun.copy(startedAt = Instant.parse("2026-10-01T10:00:00Z").plus(minute.seconds * 60))
        }

        runs.forEach { storage.add(it) }

        val kept = storage.runs.first()
        assertEquals(50, kept.size)
        assertEquals(runs.last(), kept.first())
        assertEquals(runs[1], kept.last())
    }

    @Test
    fun `a log this version cannot read says so, and starts afresh with the next run`() = runTest {
        val dataStore = dataStore()
        dataStore.edit { it[stringPreferencesKey("run_log")] = "not json" }
        val storage = DataStoreRunLogStorage(dataStore)

        assertEquals(emptyList(), storage.runs.first())
        assertTrue(storage.unreadable.first())
        storage.add(fullRun)
        assertEquals(listOf(fullRun), storage.runs.first())
        assertFalse(storage.unreadable.first())
    }

    private fun TestScope.dataStore(): DataStore<Preferences> = PreferenceDataStoreFactory.create(
        scope = backgroundScope,
        produceFile = { File(folder.root, "settings.preferences_pb") },
    )
}
