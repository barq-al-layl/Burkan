package io.github.barqallayl.burkan.feature.settings.ui

import androidx.annotation.StringRes
import androidx.compose.ui.graphics.vector.ImageVector
import com.composables.icons.tabler.Tabler
import com.composables.icons.tabler.outline.Moon
import com.composables.icons.tabler.outline.Palette
import com.composables.icons.tabler.outline.Sun
import io.github.barqallayl.burkan.R
import io.github.barqallayl.burkan.designsystem.AppStyle
import io.github.barqallayl.burkan.designsystem.PaletteStyles
import io.github.barqallayl.burkan.designsystem.SeedColors
import io.github.barqallayl.burkan.designsystem.ThemeMode

@get:StringRes
val AppStyle.label: Int
    get() = when (this) {
        AppStyle.Material -> R.string.style_material
        AppStyle.OneUi -> R.string.style_one_ui
    }

@get:StringRes
val ThemeMode.label: Int
    get() = when (this) {
        ThemeMode.FollowSystem -> R.string.theme_follow_system
        ThemeMode.Light -> R.string.theme_light
        ThemeMode.Dark -> R.string.theme_dark
    }

val ThemeMode.icon: ImageVector
    get() = when (this) {
        ThemeMode.FollowSystem -> Tabler.Outline.Palette
        ThemeMode.Light -> Tabler.Outline.Sun
        ThemeMode.Dark -> Tabler.Outline.Moon
    }

@get:StringRes
val SeedColors.label: Int
    get() = when (this) {
        SeedColors.Wallpaper -> R.string.seed_wallpaper
        SeedColors.Blue -> R.string.seed_blue
        SeedColors.Indigo -> R.string.seed_indigo
        SeedColors.Purple -> R.string.seed_purple
        SeedColors.Pink -> R.string.seed_pink
        SeedColors.Red -> R.string.seed_red
        SeedColors.Orange -> R.string.seed_orange
        SeedColors.Green -> R.string.seed_green
        SeedColors.Teal -> R.string.seed_teal
    }

@get:StringRes
val PaletteStyles.label: Int
    get() = when (this) {
        PaletteStyles.TonalSpot -> R.string.palette_tonal_spot
        PaletteStyles.Neutral -> R.string.palette_neutral
        PaletteStyles.Vibrant -> R.string.palette_vibrant
        PaletteStyles.Expressive -> R.string.palette_expressive
        PaletteStyles.Rainbow -> R.string.palette_rainbow
        PaletteStyles.FruitSalad -> R.string.palette_fruit_salad
        PaletteStyles.Monochrome -> R.string.palette_monochrome
        PaletteStyles.Fidelity -> R.string.palette_fidelity
        PaletteStyles.Content -> R.string.palette_content
        PaletteStyles.Cmf -> R.string.palette_cmf
    }
