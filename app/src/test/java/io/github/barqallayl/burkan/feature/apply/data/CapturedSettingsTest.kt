package io.github.barqallayl.burkan.feature.apply.data

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import io.github.barqallayl.burkan.core.shell.SettingKey
import io.github.barqallayl.burkan.feature.apply.FakeCapturedSettingsStorage
import io.github.barqallayl.burkan.feature.apply.FakeDeviceSettings
import io.github.barqallayl.burkan.feature.apply.model.RestoredSetting
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Instant

class CapturedSettingsTest {

    @get:Rule
    val folder = TemporaryFolder()

    private val settings = FakeDeviceSettings()
    private val storage = FakeCapturedSettingsStorage()
    private val clock = TestClock()
    private val captured = CapturedSettings(storage, settings, clock)

    private val values = mapOf(
        RestoredSetting.AutoRotation to "1",
        RestoredSetting.AccessibilityServices to FixtureDevice.ACCESSIBILITY,
    )

    @Test
    fun `what is kept is written back by the app, and then let go of`() = runTest {
        captured.keep(values)

        captured.putBackKept()

        assertEquals(
            listOf(
                SettingKey.AutoRotation to "1",
                SettingKey.AccessibilityServices to FixtureDevice.ACCESSIBILITY,
            ),
            settings.written,
        )
        assertEquals(emptyMap(), storage.kept)
    }

    @Test
    fun `with nothing kept, nothing is written`() = runTest {
        captured.putBackKept()

        assertEquals(emptyList(), settings.written)
    }

    @Test
    fun `what was kept long ago is let go of without being written`() = runTest {
        captured.keep(values)
        // The app was not opened again until the next day: the user may have changed these since.
        clock.advance(CapturedSettings.KEPT_FOR + 1.minutes)

        captured.putBackKept()

        assertEquals(emptyList(), settings.written)
        assertEquals(emptyMap(), storage.kept)
    }

    @Test
    fun `what was kept a little while ago is still written back`() = runTest {
        captured.keep(values)
        clock.advance(CapturedSettings.KEPT_FOR - 1.minutes)

        captured.putBackKept()

        assertEquals(2, settings.written.size)
    }

    @Test
    fun `what an earlier version kept, with no time, is written back as it would have been`() = runTest {
        storage.kept = values

        captured.putBackKept()

        assertEquals(2, settings.written.size)
    }

    @Test
    fun `a value Android will not take is tried once, not kept for ever`() = runTest {
        captured.keep(values)
        settings.refuses = true

        captured.putBackKept()

        assertEquals(emptyMap(), storage.kept)
    }

    @Test
    fun `a plan with nothing to put back keeps nothing, and leaves alone what an earlier run kept`() = runTest {
        captured.keep(values)

        captured.keep(emptyMap())

        assertEquals(values, storage.kept)
    }

    @Test
    fun `what is kept is read by the next process, and letting go removes all of it`() = runTest {
        val file = File(folder.root, "device_state.preferences_pb")
        val at = Instant.parse("2026-10-02T09:30:00Z")
        DataStoreCapturedSettingsStorage(dataStore(file)).set(values, at)

        // A second store on a copy of the file, as the next start of the app would open it.
        val copy = File(folder.root, "copy.preferences_pb").apply { writeBytes(file.readBytes()) }
        val next = DataStoreCapturedSettingsStorage(dataStore(copy))
        assertEquals(values, next.get())
        assertEquals(at, next.keptAt())

        next.set(emptyMap())
        assertEquals(emptyMap(), next.get())
        assertNull(next.keptAt())
    }

    private fun TestScope.dataStore(file: File): DataStore<Preferences> = PreferenceDataStoreFactory.create(
        scope = backgroundScope,
        produceFile = { file },
    )
}
