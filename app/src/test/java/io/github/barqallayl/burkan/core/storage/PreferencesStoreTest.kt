package io.github.barqallayl.burkan.core.storage

import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File
import kotlin.test.assertEquals

class PreferencesStoreTest {

    @get:Rule
    val folder = TemporaryFolder()

    private val key = stringPreferencesKey("theme_mode")

    @Test
    fun `a file that is no longer preferences reads as a fresh install, and is written to again`() = runTest {
        val file = File(folder.root, "settings.preferences_pb").apply { writeText("not a preferences file") }
        val store = preferencesStore(backgroundScope) { file }

        assertEquals(emptyPreferences(), store.data.first())

        store.edit { it[key] = "Dark" }
        assertEquals("Dark", store.data.first()[key])
    }

    @Test
    fun `a file that is in order is read as it is`() = runTest {
        val file = File(folder.root, "settings.preferences_pb")
        preferencesStore(backgroundScope) { file }.edit { it[key] = "Dark" }

        // A second store on the same file, as the next start of the app would open it.
        val copy = File(folder.root, "copy.preferences_pb").apply { writeBytes(file.readBytes()) }
        assertEquals("Dark", preferencesStore(backgroundScope) { copy }.data.first()[key])
    }
}
