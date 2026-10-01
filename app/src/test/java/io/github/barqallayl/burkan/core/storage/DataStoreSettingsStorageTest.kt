package io.github.barqallayl.burkan.core.storage

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import io.github.barqallayl.burkan.core.shell.PackageName
import io.github.barqallayl.burkan.designsystem.ColorSpecs
import io.github.barqallayl.burkan.designsystem.PaletteStyles
import io.github.barqallayl.burkan.designsystem.SeedColors
import io.github.barqallayl.burkan.designsystem.ThemeMode
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class DataStoreSettingsStorageTest {

    @get:Rule
    val folder = TemporaryFolder()

    // The key names are spelled out rather than shared, so renaming one, which loses what users chose, fails here.
    private val themeModeKey = stringPreferencesKey("theme_mode")
    private val seedColorKey = stringPreferencesKey("seed_color")
    private val paletteStyleKey = stringPreferencesKey("palette_style")
    private val colorSpecKey = stringPreferencesKey("color_spec")
    private val textScaleKey = intPreferencesKey("text_scale_percent")

    @Test
    fun `a fresh install reads the defaults`() = runTest {
        val storage = DataStoreSettingsStorage(dataStore())

        assertEquals(SettingsStorage.Defaults.themeMode, storage.themeMode.first())
        assertEquals(SettingsStorage.Defaults.seedColor, storage.seedColor.first())
        assertEquals(SettingsStorage.Defaults.paletteStyle, storage.paletteStyle.first())
        assertEquals(SettingsStorage.Defaults.colorSpec, storage.colorSpec.first())
        assertEquals(SettingsStorage.Defaults.TEXT_SCALE_PERCENT, storage.textScalePercent.first())
    }

    @Test
    fun `stored choices are read back`() = runTest {
        val dataStore = dataStore()
        dataStore.edit {
            it[themeModeKey] = "Dark"
            it[seedColorKey] = "Teal"
            it[paletteStyleKey] = "Monochrome"
            it[colorSpecKey] = "Spec2021"
            it[textScaleKey] = 115
        }
        val storage = DataStoreSettingsStorage(dataStore)

        assertEquals(ThemeMode.Dark, storage.themeMode.first())
        assertEquals(SeedColors.Teal, storage.seedColor.first())
        assertEquals(PaletteStyles.Monochrome, storage.paletteStyle.first())
        assertEquals(ColorSpecs.Spec2021, storage.colorSpec.first())
        assertEquals(115, storage.textScalePercent.first())
    }

    @Test
    fun `values this version does not know read as the defaults`() = runTest {
        val dataStore = dataStore()
        dataStore.edit {
            it[themeModeKey] = "Sepia"
            it[seedColorKey] = "dark"
            it[paletteStyleKey] = ""
            it[colorSpecKey] = "SPEC_2025"
            it[textScaleKey] = 112
        }
        val storage = DataStoreSettingsStorage(dataStore)

        assertEquals(SettingsStorage.Defaults.themeMode, storage.themeMode.first())
        assertEquals(SettingsStorage.Defaults.seedColor, storage.seedColor.first())
        assertEquals(SettingsStorage.Defaults.paletteStyle, storage.paletteStyle.first())
        assertEquals(SettingsStorage.Defaults.colorSpec, storage.colorSpec.first())
        assertEquals(SettingsStorage.Defaults.TEXT_SCALE_PERCENT, storage.textScalePercent.first())
    }

    @Test
    fun `text scale outside the range reads as the default`() = runTest {
        val dataStore = dataStore()
        val storage = DataStoreSettingsStorage(dataStore)

        listOf(80, 135, 0, -100).forEach { percent ->
            dataStore.edit { it[textScaleKey] = percent }

            assertEquals(SettingsStorage.Defaults.TEXT_SCALE_PERCENT, storage.textScalePercent.first(), "$percent")
        }
        listOf(85, 130).forEach { percent ->
            dataStore.edit { it[textScaleKey] = percent }

            assertEquals(percent, storage.textScalePercent.first())
        }
    }

    @Test
    fun `the restart settings are on by default and nothing is excluded`() = runTest {
        val storage = DataStoreSettingsStorage(dataStore())

        assertTrue(storage.applyOnBoot.first())
        assertTrue(storage.turnOffWirelessDebugging.first())
        assertEquals(emptySet(), storage.userExclusions.first())
    }

    @Test
    fun `every setting written reads back`() = runTest {
        val storage = DataStoreSettingsStorage(dataStore())
        val excluded = setOf(PackageName.known("com.example.notes"), PackageName.known("org.example.chat"))

        storage.setThemeMode(ThemeMode.Light)
        storage.setSeedColor(SeedColors.Purple)
        storage.setPaletteStyle(PaletteStyles.Rainbow)
        storage.setColorSpec(ColorSpecs.Spec2021)
        storage.setTextScalePercent(125)
        storage.setApplyOnBoot(false)
        storage.setTurnOffWirelessDebugging(false)
        storage.setUserExclusions(excluded)

        assertEquals(ThemeMode.Light, storage.themeMode.first())
        assertEquals(SeedColors.Purple, storage.seedColor.first())
        assertEquals(PaletteStyles.Rainbow, storage.paletteStyle.first())
        assertEquals(ColorSpecs.Spec2021, storage.colorSpec.first())
        assertEquals(125, storage.textScalePercent.first())
        assertFalse(storage.applyOnBoot.first())
        assertFalse(storage.turnOffWirelessDebugging.first())
        assertEquals(excluded, storage.userExclusions.first())
    }

    @Test
    fun `a stored exclusion that is not a package name never comes back out`() = runTest {
        val dataStore = dataStore()
        dataStore.edit { it[stringSetPreferencesKey("user_exclusions")] = setOf("com.example.notes", "a;reboot") }

        val exclusions = DataStoreSettingsStorage(dataStore).userExclusions.first()

        assertEquals(setOf(PackageName.known("com.example.notes")), exclusions)
    }

    @Test
    fun `a text size off the scale is refused`() = runTest {
        val storage = DataStoreSettingsStorage(dataStore())

        assertFailsWith<IllegalArgumentException> { storage.setTextScalePercent(112) }
    }

    private fun TestScope.dataStore(): DataStore<Preferences> = PreferenceDataStoreFactory.create(
        scope = backgroundScope,
        produceFile = { File(folder.root, "settings.preferences_pb") },
    )
}
