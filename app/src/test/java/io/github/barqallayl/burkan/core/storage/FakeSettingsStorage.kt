package io.github.barqallayl.burkan.core.storage

import io.github.barqallayl.burkan.core.shell.PackageName
import io.github.barqallayl.burkan.designsystem.AppFont
import io.github.barqallayl.burkan.designsystem.PaletteStyles
import io.github.barqallayl.burkan.designsystem.SeedColors
import io.github.barqallayl.burkan.designsystem.ThemeMode
import kotlinx.coroutines.flow.MutableStateFlow

/** Holds what is written, starting from the defaults. */
class FakeSettingsStorage : SettingsStorage {
    override val themeMode = MutableStateFlow(SettingsStorage.Defaults.themeMode)
    override val seedColor = MutableStateFlow(SettingsStorage.Defaults.seedColor)
    override val paletteStyle = MutableStateFlow(SettingsStorage.Defaults.paletteStyle)
    override val appFont = MutableStateFlow(SettingsStorage.Defaults.appFont)
    override val textScalePercent = MutableStateFlow(SettingsStorage.Defaults.TEXT_SCALE_PERCENT)
    override val applyOnBoot = MutableStateFlow(SettingsStorage.Defaults.APPLY_ON_BOOT)
    override val turnOffWirelessDebugging = MutableStateFlow(SettingsStorage.Defaults.TURN_OFF_WIRELESS_DEBUGGING)
    override val userExclusions = MutableStateFlow(emptySet<PackageName>())

    override suspend fun setThemeMode(mode: ThemeMode) {
        themeMode.value = mode
    }

    override suspend fun setSeedColor(color: SeedColors) {
        seedColor.value = color
    }

    override suspend fun setPaletteStyle(style: PaletteStyles) {
        paletteStyle.value = style
    }

    override suspend fun setAppFont(font: AppFont) {
        appFont.value = font
    }

    override suspend fun setTextScalePercent(percent: Int) {
        textScalePercent.value = percent
    }

    override suspend fun setApplyOnBoot(enabled: Boolean) {
        applyOnBoot.value = enabled
    }

    override suspend fun setTurnOffWirelessDebugging(enabled: Boolean) {
        turnOffWirelessDebugging.value = enabled
    }

    override suspend fun setUserExclusions(packages: Set<PackageName>) {
        userExclusions.value = packages
    }
}
