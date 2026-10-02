package io.github.barqallayl.burkan.feature.apply.data

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.time.Duration.Companion.seconds
import kotlin.time.Instant

class DataStoreSystemUiRestartsTest {

    @get:Rule
    val folder = TemporaryFolder()

    private val restartedAt = Instant.parse("2026-10-02T09:30:00Z")

    @Test
    fun `nothing is recorded on a fresh install`() = runTest {
        assertNull(DataStoreSystemUiRestarts(dataStore()).last())
    }

    @Test
    fun `a restart recorded by one process is seen by the next`() = runTest {
        val firstProcess = CoroutineScope(SupervisorJob() + StandardTestDispatcher(testScheduler))
        val first = PreferenceDataStoreFactory.create(scope = firstProcess, produceFile = { file })
        DataStoreSystemUiRestarts(first).record(restartedAt)
        firstProcess.cancel()
        runCurrent()

        // The next process opens the file afresh, and still has to wait out the minute.
        val restarts = DataStoreSystemUiRestarts(dataStore())
        assertEquals(restartedAt, restarts.last())
        assertFalse(SystemUiCooldown(restarts, TestClock(restartedAt + 30.seconds)).claim())
    }

    private val file: File get() = File(folder.root, "device_state.preferences_pb")

    private fun TestScope.dataStore(): DataStore<Preferences> =
        PreferenceDataStoreFactory.create(scope = backgroundScope, produceFile = { file })
}
