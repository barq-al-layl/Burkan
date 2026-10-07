package io.github.barqallayl.burkan.core.storage

import android.os.Build
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesBinding
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn
import io.github.barqallayl.burkan.core.shell.PackageName
import io.github.barqallayl.burkan.designsystem.AppStyle
import io.github.barqallayl.burkan.designsystem.PaletteStyles
import io.github.barqallayl.burkan.designsystem.SeedColors
import io.github.barqallayl.burkan.designsystem.ThemeMode
import java.io.IOException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlin.enums.enumEntries

/** The user's settings. A missing or unreadable value reads as its entry in [Defaults]. */
interface SettingsStorage {
    val appStyle: Flow<AppStyle>
    val themeMode: Flow<ThemeMode>
    val seedColor: Flow<SeedColors>
    val paletteStyle: Flow<PaletteStyles>

    /** Apply Vulkan by itself after every restart. */
    val applyOnBoot: Flow<Boolean>

    /** Switch wireless debugging off again after a run, when the app was the one that switched it on. */
    val turnOffWirelessDebugging: Flow<Boolean>

    /** Apps the user never wants a full apply to restart. */
    val userExclusions: Flow<Set<PackageName>>

    suspend fun setAppStyle(style: AppStyle)
    suspend fun setThemeMode(mode: ThemeMode)
    suspend fun setSeedColor(color: SeedColors)
    suspend fun setPaletteStyle(style: PaletteStyles)
    suspend fun setApplyOnBoot(enabled: Boolean)
    suspend fun setTurnOffWirelessDebugging(enabled: Boolean)
    suspend fun setUserExclusions(packages: Set<PackageName>)

    /**
     * What a fresh install gets, and what previews render with. The style is the exception: a Samsung phone starts
     * in One UI, which the storage decides from the phone it is on, and [appStyle] is what any other gets. The
     * colour goes with the style: [seedColor] is the one for [appStyle].
     */
    object Defaults {
        val appStyle: AppStyle = AppStyle.Material
        val themeMode: ThemeMode = ThemeMode.FollowSystem
        val seedColor: SeedColors = SeedColors.defaultFor(appStyle)
        val paletteStyle: PaletteStyles = PaletteStyles.Expressive
        const val APPLY_ON_BOOT: Boolean = true
        const val TURN_OFF_WIRELESS_DEBUGGING: Boolean = true
    }
}

@Inject
@SingleIn(AppScope::class)
@ContributesBinding(AppScope::class)
class DataStoreSettingsStorage(private val dataStore: DataStore<Preferences>) : SettingsStorage {

    private val preferences: Flow<Preferences> = dataStore.data.catch { error ->
        // A corrupt file reads as a fresh install rather than taking the app down with it.
        if (error is IOException) emit(emptyPreferences()) else throw error
    }

    override val appStyle: Flow<AppStyle> =
        read { it.enumValue(Keys.appStyle) ?: AppStyle.defaultFor(Build.MANUFACTURER) }

    override val themeMode: Flow<ThemeMode> =
        read { it.enumValue(Keys.themeMode) ?: SettingsStorage.Defaults.themeMode }

    /** Until the user chooses a colour, it is the one that goes with the style in use. */
    override val seedColor: Flow<SeedColors> = read {
        it.enumValue(Keys.seedColor)
            ?: SeedColors.defaultFor(it.enumValue(Keys.appStyle) ?: AppStyle.defaultFor(Build.MANUFACTURER))
    }

    override val paletteStyle: Flow<PaletteStyles> =
        read { it.enumValue(Keys.paletteStyle) ?: SettingsStorage.Defaults.paletteStyle }

    override val applyOnBoot: Flow<Boolean> =
        read { it[Keys.applyOnBoot] ?: SettingsStorage.Defaults.APPLY_ON_BOOT }

    override val turnOffWirelessDebugging: Flow<Boolean> =
        read {
            it[Keys.turnOffWirelessDebugging]
                ?: SettingsStorage.Defaults.TURN_OFF_WIRELESS_DEBUGGING
        }

    /** A stored name that is not a valid package name is dropped: it never reaches a command. */
    override val userExclusions: Flow<Set<PackageName>> =
        read { preferences ->
            preferences[Keys.userExclusions].orEmpty().mapNotNull(PackageName::parse).toSet()
        }

    override suspend fun setAppStyle(style: AppStyle) = write(Keys.appStyle, style.name)

    override suspend fun setThemeMode(mode: ThemeMode) = write(Keys.themeMode, mode.name)

    override suspend fun setSeedColor(color: SeedColors) = write(Keys.seedColor, color.name)

    override suspend fun setPaletteStyle(style: PaletteStyles) =
        write(Keys.paletteStyle, style.name)

    override suspend fun setApplyOnBoot(enabled: Boolean) = write(Keys.applyOnBoot, enabled)

    override suspend fun setTurnOffWirelessDebugging(enabled: Boolean) =
        write(Keys.turnOffWirelessDebugging, enabled)

    override suspend fun setUserExclusions(packages: Set<PackageName>) =
        write(Keys.userExclusions, packages.map { it.value }.toSet())

    private fun <T> read(transform: (Preferences) -> T): Flow<T> =
        preferences.map(transform).distinctUntilChanged()

    private suspend fun <T> write(key: Preferences.Key<T>, value: T) {
        dataStore.edit { it[key] = value }
    }

    private inline fun <reified E : Enum<E>> Preferences.enumValue(key: Preferences.Key<String>): E? =
        this[key]?.let { name -> enumEntries<E>().firstOrNull { it.name == name } }

    private object Keys {
        val appStyle = stringPreferencesKey("app_style")
        val themeMode = stringPreferencesKey("theme_mode")
        val seedColor = stringPreferencesKey("seed_color")
        val paletteStyle = stringPreferencesKey("palette_style")
        val applyOnBoot = booleanPreferencesKey("apply_on_boot")
        val turnOffWirelessDebugging = booleanPreferencesKey("turn_off_wireless_debugging")
        val userExclusions = stringSetPreferencesKey("user_exclusions")
    }
}
