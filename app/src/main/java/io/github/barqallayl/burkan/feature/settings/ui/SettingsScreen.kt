package io.github.barqallayl.burkan.feature.settings.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowRight
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LargeFlexibleTopAppBar
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.toggleableState
import androidx.compose.ui.state.ToggleableState
import androidx.compose.ui.tooling.preview.PreviewWrapper
import androidx.compose.ui.unit.dp
import com.composables.icons.tabler.Tabler
import com.composables.icons.tabler.outline.Apps
import com.composables.icons.tabler.outline.BrandGithub
import com.composables.icons.tabler.outline.Check
import com.composables.icons.tabler.outline.ColorSwatch
import com.composables.icons.tabler.outline.InfoCircle
import com.composables.icons.tabler.outline.License
import com.composables.icons.tabler.outline.Palette
import com.composables.icons.tabler.outline.Power
import com.composables.icons.tabler.outline.Refresh
import com.composables.icons.tabler.outline.Scale
import com.composables.icons.tabler.outline.SunMoon
import com.composables.icons.tabler.outline.TextSize
import com.composables.icons.tabler.outline.Typography
import com.composables.icons.tabler.outline.WifiOff
import dev.zacsweers.metrox.viewmodel.metroViewModel
import io.github.barqallayl.burkan.Appearance
import io.github.barqallayl.burkan.R
import io.github.barqallayl.burkan.core.navigation.ExclusionsRoute
import io.github.barqallayl.burkan.core.navigation.LicencesRoute
import io.github.barqallayl.burkan.core.navigation.LocalNavigator
import io.github.barqallayl.burkan.core.storage.SettingsStorage
import io.github.barqallayl.burkan.designsystem.AppFont
import io.github.barqallayl.burkan.designsystem.PaletteStyles
import io.github.barqallayl.burkan.designsystem.SeedColors
import io.github.barqallayl.burkan.designsystem.TextScale
import io.github.barqallayl.burkan.designsystem.ThemeMode
import io.github.barqallayl.burkan.designsystem.component.BurkanBottomSheet
import io.github.barqallayl.burkan.designsystem.component.BurkanChoiceList
import io.github.barqallayl.burkan.designsystem.component.BurkanSectionTitle
import io.github.barqallayl.burkan.designsystem.component.BurkanSegmentItem
import io.github.barqallayl.burkan.designsystem.component.GroupGap
import io.github.barqallayl.burkan.designsystem.component.ScreenMargin
import io.github.barqallayl.burkan.designsystem.component.SegmentedColumn
import io.github.barqallayl.burkan.designsystem.preview.BurkanPreview
import io.github.barqallayl.burkan.designsystem.preview.BurkanPreviewTheme
import io.github.barqallayl.burkan.designsystem.preview.BurkanPreviewWrapper
import io.github.barqallayl.burkan.feature.settings.model.About
import org.orbitmvi.orbit.compose.collectAsState
import org.orbitmvi.orbit.compose.collectSideEffect

@Composable
fun SettingsScreen() {
    val viewModel = metroViewModel<SettingsViewModel>()
    val state by viewModel.collectAsState()
    val navigator = LocalNavigator.current
    val uriHandler = LocalUriHandler.current
    viewModel.collectSideEffect { effect ->
        when (effect) {
            SettingsSideEffect.Back -> navigator.pop()
            SettingsSideEffect.OpenExclusions -> navigator.push(ExclusionsRoute)
            SettingsSideEffect.OpenLicences -> navigator.push(LicencesRoute)
            is SettingsSideEffect.OpenUrl -> uriHandler.openUri(effect.url)
        }
    }
    SettingsContent(
        state = state,
        actions = SettingsActions(
            onBack = viewModel::back,
            onApplyOnBoot = viewModel::setApplyOnBoot,
            onTurnOffWirelessDebugging = viewModel::setTurnOffWirelessDebugging,
            onOpenExclusions = viewModel::openExclusions,
            onShow = viewModel::show,
            onDismissDialog = viewModel::dismissDialog,
            onThemeMode = viewModel::setThemeMode,
            onSeedColor = viewModel::setSeedColor,
            onPaletteStyle = viewModel::setPaletteStyle,
            onAppFont = viewModel::setAppFont,
            onTextScale = viewModel::setTextScalePercent,
            onConfirmRedoSetup = viewModel::confirmRedoSetup,
            onOpenSource = viewModel::openSource,
            onOpenLicence = viewModel::openLicence,
            onOpenLicences = viewModel::openLicences,
        ),
    )
}

private class SettingsActions(
    val onBack: () -> Unit = {},
    val onApplyOnBoot: (Boolean) -> Unit = {},
    val onTurnOffWirelessDebugging: (Boolean) -> Unit = {},
    val onOpenExclusions: () -> Unit = {},
    val onShow: (SettingsDialog) -> Unit = {},
    val onDismissDialog: () -> Unit = {},
    val onThemeMode: (ThemeMode) -> Unit = {},
    val onSeedColor: (SeedColors) -> Unit = {},
    val onPaletteStyle: (PaletteStyles) -> Unit = {},
    val onAppFont: (AppFont) -> Unit = {},
    val onTextScale: (Int) -> Unit = {},
    val onConfirmRedoSetup: () -> Unit = {},
    val onOpenSource: () -> Unit = {},
    val onOpenLicence: () -> Unit = {},
    val onOpenLicences: () -> Unit = {},
)

@Composable
private fun SettingsContent(state: SettingsState, actions: SettingsActions) {
    // The title starts large and shrinks into the bar as the list is scrolled up.
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            LargeFlexibleTopAppBar(
                title = { Text(stringResource(R.string.settings_title)) },
                navigationIcon = {
                    IconButton(onClick = actions.onBack) {
                        Icon(Icons.AutoMirrored.Outlined.ArrowBack, stringResource(R.string.navigate_back))
                    }
                },
                scrollBehavior = scrollBehavior,
            )
        },
    ) { innerPadding ->
        val values = state.values
        if (values == null) {
            Box(Modifier.fillMaxSize().padding(innerPadding), contentAlignment = Alignment.Center) {
                LoadingIndicator()
            }
            return@Scaffold
        }
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = ScreenMargin)
                .padding(bottom = GroupGap),
            verticalArrangement = Arrangement.spacedBy(GroupGap),
        ) {
            Section(R.string.settings_section_restart) { RestartItems(values, actions) }
            Section(R.string.settings_section_appearance) { AppearanceItems(values.appearance, actions) }
            Section(R.string.settings_section_setup) {
                BurkanSegmentItem(
                    index = 0,
                    count = 1,
                    headline = stringResource(R.string.settings_redo_setup),
                    supporting = stringResource(R.string.settings_redo_setup_text),
                    onClick = { actions.onShow(SettingsDialog.RedoSetup) },
                    leading = { Icon(Tabler.Outline.Refresh, contentDescription = null) },
                )
            }
            Section(R.string.settings_section_about) { AboutItems(state.version, actions) }
        }
        state.dialog?.let { SettingsDialogs(it, values, actions) }
    }
}

/** A titled group of segments. */
@Composable
private fun Section(title: Int, content: @Composable () -> Unit) {
    Column {
        BurkanSectionTitle(stringResource(title))
        SegmentedColumn { content() }
    }
}

@Composable
private fun RestartItems(values: SettingsValues, actions: SettingsActions) {
    SwitchItem(
        index = 0,
        count = RESTART_ITEMS,
        icon = Tabler.Outline.Power,
        title = R.string.settings_apply_on_boot,
        text = R.string.settings_apply_on_boot_text,
        checked = values.applyOnBoot,
        onCheckedChange = actions.onApplyOnBoot,
    )
    SwitchItem(
        index = 1,
        count = RESTART_ITEMS,
        icon = Tabler.Outline.WifiOff,
        title = R.string.settings_turn_off_wireless_debugging,
        text = R.string.settings_turn_off_wireless_debugging_text,
        checked = values.turnOffWirelessDebugging,
        onCheckedChange = actions.onTurnOffWirelessDebugging,
    )
    BurkanSegmentItem(
        index = 2,
        count = RESTART_ITEMS,
        headline = stringResource(R.string.settings_exclusions),
        supporting = if (values.exclusionCount == 0) {
            stringResource(R.string.settings_exclusions_none)
        } else {
            pluralStringResource(R.plurals.settings_exclusions_count, values.exclusionCount, values.exclusionCount)
        },
        onClick = actions.onOpenExclusions,
        leading = { Icon(Tabler.Outline.Apps, contentDescription = null) },
        trailing = { Chevron() },
    )
}

@Composable
private fun AppearanceItems(appearance: Appearance, actions: SettingsActions) {
    ChoiceItem(0, Tabler.Outline.SunMoon, R.string.settings_theme, stringResource(appearance.themeMode.label)) {
        actions.onShow(SettingsDialog.ThemeMode)
    }
    ChoiceItem(1, Tabler.Outline.Palette, R.string.settings_seed_color, stringResource(appearance.seedColor.label)) {
        actions.onShow(SettingsDialog.SeedColor)
    }
    ChoiceItem(2, Tabler.Outline.ColorSwatch, R.string.settings_palette_style, stringResource(appearance.paletteStyle.label)) {
        actions.onShow(SettingsDialog.PaletteStyle)
    }
    ChoiceItem(3, Tabler.Outline.Typography, R.string.settings_font, appearance.appFont.label()) {
        actions.onShow(SettingsDialog.Font)
    }
    ChoiceItem(4, Tabler.Outline.TextSize, R.string.settings_text_size, textSizeLabel(appearance.textScalePercent)) {
        actions.onShow(SettingsDialog.TextSize)
    }
}

@Composable
private fun AboutItems(version: String, actions: SettingsActions) {
    BurkanSegmentItem(
        index = 0,
        count = ABOUT_ITEMS,
        headline = stringResource(R.string.settings_version),
        supporting = version,
        leading = { Icon(Tabler.Outline.InfoCircle, contentDescription = null) },
    )
    BurkanSegmentItem(
        index = 1,
        count = ABOUT_ITEMS,
        headline = stringResource(R.string.settings_star),
        onClick = actions.onOpenSource,
        leading = { Icon(Tabler.Outline.BrandGithub, contentDescription = null) },
        trailing = { Chevron() },
    )
    BurkanSegmentItem(
        index = 2,
        count = ABOUT_ITEMS,
        headline = stringResource(R.string.settings_licence),
        onClick = actions.onOpenLicence,
        leading = { Icon(Tabler.Outline.Scale, contentDescription = null) },
        trailing = { Chevron() },
    )
    BurkanSegmentItem(
        index = 3,
        count = ABOUT_ITEMS,
        headline = stringResource(R.string.settings_licences),
        onClick = actions.onOpenLicences,
        leading = { Icon(Tabler.Outline.License, contentDescription = null) },
        trailing = { Chevron() },
    )
}

@Composable
private fun SettingsDialogs(dialog: SettingsDialog, values: SettingsValues, actions: SettingsActions) {
    val appearance = values.appearance
    if (dialog == SettingsDialog.RedoSetup) {
        AlertDialog(
            onDismissRequest = actions.onDismissDialog,
            title = { Text(stringResource(R.string.settings_redo_setup_confirm_title)) },
            text = { Text(stringResource(R.string.settings_redo_setup_confirm_text)) },
            confirmButton = {
                TextButton(onClick = actions.onConfirmRedoSetup) {
                    Text(stringResource(R.string.settings_redo_setup_confirm_action))
                }
            },
            dismissButton = {
                TextButton(onClick = actions.onDismissDialog) { Text(stringResource(R.string.dialog_cancel)) }
            },
        )
    } else {
        BurkanBottomSheet(onDismiss = actions.onDismissDialog) { hide ->
            if (dialog == SettingsDialog.TextSize) {
                TextSizeSheet(
                    savedPercent = appearance.textScalePercent,
                    // After the sheet has gone, so the app does not lay itself out again under a closing sheet.
                    onSave = { percent -> hide { actions.onTextScale(percent) } },
                    onCancel = { hide(actions.onDismissDialog) },
                )
            } else {
                AppearanceChoices(dialog, appearance, actions)
            }
        }
    }
}

/** The options of one appearance preference, as they are listed in its sheet. */
@Composable
private fun AppearanceChoices(dialog: SettingsDialog, appearance: Appearance, actions: SettingsActions) {
    when (dialog) {
        SettingsDialog.ThemeMode -> BurkanChoiceList(
            title = stringResource(R.string.settings_theme),
            options = ThemeMode.entries,
            selected = appearance.themeMode,
            label = { stringResource(it.label) },
            onSelect = actions.onThemeMode,
            leading = { Icon(it.icon, contentDescription = null) },
        )
        SettingsDialog.SeedColor -> BurkanChoiceList(
            title = stringResource(R.string.settings_seed_color),
            options = SeedColors.entries,
            selected = appearance.seedColor,
            label = { stringResource(it.label) },
            onSelect = actions.onSeedColor,
            leading = { Box(Modifier.size(24.dp).background(it.color, CircleShape)) },
        )
        SettingsDialog.PaletteStyle -> BurkanChoiceList(
            title = stringResource(R.string.settings_palette_style),
            options = PaletteStyles.entries,
            selected = appearance.paletteStyle,
            label = { stringResource(it.label) },
            onSelect = actions.onPaletteStyle,
        )
        // Each name is set in its own typeface, so the list is its own specimen.
        SettingsDialog.Font -> BurkanChoiceList(
            title = stringResource(R.string.settings_font),
            options = AppFont.entries,
            selected = appearance.appFont,
            label = { it.label() },
            onSelect = actions.onAppFont,
            fontFamily = { it.family },
        )
        SettingsDialog.TextSize, SettingsDialog.RedoSetup -> Unit
    }
}

@Composable
private fun SwitchItem(
    index: Int,
    count: Int,
    icon: ImageVector,
    title: Int,
    text: Int,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    BurkanSegmentItem(
        index = index,
        count = count,
        headline = stringResource(title),
        supporting = stringResource(text),
        // A setting that is on is not a selected row: only the switch shows it. The row still reads as a switch.
        modifier = Modifier.semantics(mergeDescendants = true) {
            role = Role.Switch
            toggleableState = ToggleableState(checked)
        },
        onClick = { onCheckedChange(!checked) },
        leading = { Icon(icon, contentDescription = null) },
        trailing = {
            Switch(
                checked = checked,
                onCheckedChange = null,
                thumbContent = {
                    if (checked) {
                        Icon(Tabler.Outline.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                    }
                },
            )
        },
    )
}

@Composable
private fun ChoiceItem(index: Int, icon: ImageVector, title: Int, value: String, onClick: () -> Unit) {
    BurkanSegmentItem(
        index = index,
        count = APPEARANCE_ITEMS,
        headline = stringResource(title),
        supporting = value,
        onClick = onClick,
        leading = { Icon(icon, contentDescription = null) },
    )
}

/** Marks a row that opens something: another screen, or a page in the browser. */
@Composable
private fun Chevron() {
    Icon(Icons.AutoMirrored.Outlined.KeyboardArrowRight, contentDescription = null)
}

private const val RESTART_ITEMS = 3

/** Theme, colour, palette style, font and text size. */
private const val APPEARANCE_ITEMS = 5
private const val ABOUT_ITEMS = 4

private fun sample(
    dialog: SettingsDialog? = null,
    exclusions: Int = 0,
    textScalePercent: Int = SettingsStorage.Defaults.TEXT_SCALE_PERCENT,
    appFont: AppFont = SettingsStorage.Defaults.appFont,
) = SettingsState(
    values = SettingsValues(
        applyOnBoot = SettingsStorage.Defaults.APPLY_ON_BOOT,
        turnOffWirelessDebugging = SettingsStorage.Defaults.TURN_OFF_WIRELESS_DEBUGGING,
        exclusionCount = exclusions,
        appearance = sampleAppearance(textScalePercent, appFont),
    ),
    dialog = dialog,
    version = "1.0.0",
)

private fun sampleAppearance(
    textScalePercent: Int = SettingsStorage.Defaults.TEXT_SCALE_PERCENT,
    appFont: AppFont = SettingsStorage.Defaults.appFont,
) = Appearance(
    themeMode = SettingsStorage.Defaults.themeMode,
    seedColor = SettingsStorage.Defaults.seedColor,
    paletteStyle = SettingsStorage.Defaults.paletteStyle,
    appFont = appFont,
    textScalePercent = textScalePercent,
)

@PreviewWrapper(BurkanPreviewWrapper::class)
@BurkanPreview
@Composable
private fun SettingsPreview() = SettingsContent(sample(exclusions = 3), SettingsActions())

@PreviewWrapper(BurkanPreviewWrapper::class)
@BurkanPreview
@Composable
private fun SettingsLoadingPreview() = SettingsContent(SettingsState(), SettingsActions())

@PreviewWrapper(BurkanPreviewWrapper::class)
@BurkanPreview
@Composable
private fun SettingsThemeSheetPreview() = AppearanceChoices(SettingsDialog.ThemeMode, sampleAppearance(), SettingsActions())

@PreviewWrapper(BurkanPreviewWrapper::class)
@BurkanPreview
@Composable
private fun SettingsSeedColorSheetPreview() =
    AppearanceChoices(SettingsDialog.SeedColor, sampleAppearance(), SettingsActions())

@PreviewWrapper(BurkanPreviewWrapper::class)
@BurkanPreview
@Composable
private fun SettingsRedoSetupPreview() = SettingsContent(sample(SettingsDialog.RedoSetup), SettingsActions())

@BurkanPreview
@Composable
private fun SettingsDarkPreview() {
    BurkanPreviewTheme(themeMode = ThemeMode.Dark) {
        SettingsContent(sample(exclusions = 1), SettingsActions())
    }
}

@BurkanPreview
@Composable
private fun SettingsLargeTextPreview() {
    BurkanPreviewTheme(textScalePercent = TextScale.percentages.last) {
        SettingsContent(sample(textScalePercent = TextScale.percentages.last), SettingsActions())
    }
}

@BurkanPreview
@Composable
private fun SettingsPaletteStyleSheetTealPreview() {
    BurkanPreviewTheme(seedColor = SeedColors.Teal) {
        AppearanceChoices(SettingsDialog.PaletteStyle, sampleAppearance(), SettingsActions())
    }
}

@BurkanPreview
@Composable
private fun SettingsFontSheetDarkPreview() {
    BurkanPreviewTheme(themeMode = ThemeMode.Dark, appFont = AppFont.Poppins) {
        AppearanceChoices(SettingsDialog.Font, sampleAppearance(appFont = AppFont.Poppins), SettingsActions())
    }
}

@BurkanPreview
@Composable
private fun SettingsFontPreview() {
    BurkanPreviewTheme(appFont = AppFont.Poppins) {
        SettingsContent(sample(appFont = AppFont.Poppins), SettingsActions())
    }
}
