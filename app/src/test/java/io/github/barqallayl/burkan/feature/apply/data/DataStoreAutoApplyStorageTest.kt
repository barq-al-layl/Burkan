package io.github.barqallayl.burkan.feature.apply.data

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import io.github.barqallayl.burkan.feature.apply.model.AutoApplyState
import io.github.barqallayl.burkan.feature.apply.model.WaitReason
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File
import kotlin.test.assertEquals

class DataStoreAutoApplyStorageTest {

    @get:Rule
    val folder = TemporaryFolder()

    @Test
    fun `nothing is waited for on a fresh install`() = runTest {
        assertEquals(AutoApplyState(), DataStoreAutoApplyStorage(dataStore()).state.first())
    }

    @Test
    fun `a wait written reads back, and clearing it removes the network too`() = runTest {
        val storage = DataStoreAutoApplyStorage(dataStore())

        storage.set(AutoApplyState(WaitReason.TrustedNetwork, triedNetwork = 431L))
        assertEquals(AutoApplyState(WaitReason.TrustedNetwork, triedNetwork = 431L), storage.state.first())

        storage.set(AutoApplyState())
        assertEquals(AutoApplyState(), storage.state.first())
    }

    @Test
    fun `the last boot handled is kept apart from the wait`() = runTest {
        val storage = DataStoreAutoApplyStorage(dataStore())
        assertEquals(null, storage.lastBoot())

        storage.setLastBoot(17)
        storage.set(AutoApplyState())

        assertEquals(17, storage.lastBoot())
    }

    @Test
    fun `a reason this version does not know reads as not waiting`() = runTest {
        val dataStore = dataStore()
        dataStore.edit { it[stringPreferencesKey("auto_apply_waiting_for")] = "Bluetooth" }

        assertEquals(AutoApplyState(), DataStoreAutoApplyStorage(dataStore).state.first())
    }

    private fun TestScope.dataStore(): DataStore<Preferences> = PreferenceDataStoreFactory.create(
        scope = backgroundScope,
        produceFile = { File(folder.root, "device_state.preferences_pb") },
    )
}
