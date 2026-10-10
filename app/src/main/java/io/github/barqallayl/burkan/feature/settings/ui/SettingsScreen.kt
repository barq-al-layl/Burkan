package io.github.barqallayl.burkan.feature.settings.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowRight
import androidx.compose.material3.Icon
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.toggleableState
import androidx.compose.ui.state.ToggleableState
import androidx.compose.ui.tooling.preview.PreviewWrapper
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.composables.icons.tabler.Tabler
import com.composables.icons.tabler.outline.Apps
import com.composables.icons.tabler.outline.BrandGithub
import com.composables.icons.tabler.outline.Brush
import com.composables.icons.tabler.outline.Bug
import com.composables.icons.tabler.outline.Check
import com.composables.icons.tabler.outline.ColorSwatch
import com.composables.icons.tabler.outline.InfoCircle
import com.composables.icons.tabler.outline.License
import com.composables.icons.tabler.outline.Palette
import com.composables.icons.tabler.outline.Power
import com.composables.icons.tabler.outline.Refresh
import com.composables.icons.tabler.outline.Scale
import com.composables.icons.tabler.outline.SunMoon
import com.composables.icons.tabler.outline.WifiOff
import dev.zacsweers.metrox.viewmodel.metroViewModel
import io.github.barqallayl.burkan.Appearance
import io.github.barqallayl.burkan.R
import io.github.barqallayl.burkan.core.navigation.ExclusionsRoute
import io.github.barqallayl.burkan.core.navigation.LicencesRoute
import io.github.barqallayl.burkan.core.navigation.LocalNavigator
import io.github.barqallayl.burkan.core.storage.SettingsStorage
import io.github.barqallayl.burkan.designsystem.AppStyle
import io.github.barqallayl.burkan.designsystem.LocalAppStyle
import io.github.barqallayl.burkan.designsystem.PaletteStyles
import io.github.barqallayl.burkan.designsystem.SeedColors
import io.github.barqallayl.burkan.designsystem.ThemeMode
import io.github.barqallayl.burkan.designsystem.component.listInset
import io.github.barqallayl.burkan.designsystem.component.rememberBurkanAppBar
import io.github.barqallayl.burkan.designsystem.component.listTop
import io.github.barqallayl.burkan.designsystem.component.topBarScroll
import io.github.barqallayl.burkan.designsystem.component.oneUiScrollFade
import io.github.barqallayl.burkan.designsystem.component.BurkanBottomSheet
import io.github.barqallayl.burkan.designsystem.component.BurkanConfirm
import io.github.barqallayl.burkan.designsystem.resolved
import io.github.barqallayl.burkan.designsystem.component.BurkanChoiceList
import io.github.barqallayl.burkan.designsystem.component.BurkanPill
import io.github.barqallayl.burkan.designsystem.component.BurkanSectionTitle
import io.github.barqallayl.burkan.designsystem.component.BurkanSheetPreview
import io.github.barqallayl.burkan.designsystem.component.glassSource
import io.github.barqallayl.burkan.designsystem.component.readableWidth
import io.github.barqallayl.burkan.designsystem.component.BurkanTopBar
import io.github.barqallayl.burkan.designsystem.component.OneUiSwitch
import io.github.barqallayl.burkan.designsystem.component.BurkanSegmentItem
import io.github.barqallayl.burkan.designsystem.component.GroupGap
import io.github.barqallayl.burkan.designsystem.component.ScreenMargin
import io.github.barqallayl.burkan.designsystem.component.SegmentedColumn
import io.github.barqallayl.burkan.designsystem.preview.BurkanPreview
import io.github.barqallayl.burkan.designsystem.preview.BurkanPreviewTheme
import io.github.barqallayl.burkan.designsystem.preview.BurkanPreviewWrapper
import io.github.barqallayl.burkan.designsystem.preview.LargeFontScale
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
            is SettingsSideEffect.OpenUrl -> uriHandler.openIfPossible(effect.url)
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
            onAppStyle = viewModel::setAppStyle,
            onThemeMode = viewModel::setThemeMode,
            onSeedColor = viewModel::setSeedColor,
            onPaletteStyle = viewModel::setPaletteStyle,
            onConfirmRedoSetup = viewModel::confirmRedoSetup,
            onOpenSource = viewModel::openSource,
            onReportProblem = viewModel::reportProblem,
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
    val onAppStyle: (AppStyle) -> Unit = {},
    val onThemeMode: (ThemeMode) -> Unit = {},
    val onSeedColor: (SeedColors) -> Unit = {},
    val onPaletteStyle: (PaletteStyles) -> Unit = {},
    val onConfirmRedoSetup: () -> Unit = {},
    val onOpenSource: () -> Unit = {},
    val onReportProblem: () -> Unit = {},
    val onOpenLicence: () -> Unit = {},
    val onOpenLicences: () -> Unit = {},
)

@Composable
private fun SettingsContent(state: SettingsState, actions: SettingsActions) {
    // The title starts large and shrinks into the bar as the list is scrolled up.
    val appBar = rememberBurkanAppBar(large = true)
    val scrollState = rememberScrollState()
    Scaffold(
        modifier = Modifier.topBarScroll(appBar),
        topBar = {
            BurkanTopBar(
                title = { Text(stringResource(R.string.settings_title)) },
                onBack = actions.onBack,
                appBar = appBar,
                contentScroll = { scrollState.value },
            )
        },
    ) { innerPadding ->
        val values = state.values
        if (values == null) {
            Box(
                Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentAlignment = Alignment.Center,
            ) {
                LoadingIndicator()
            }
            return@Scaffold
        }
        Column(
            modifier = Modifier
                .fillMaxSize()
                .glassSource(appBar)
                .padding(
                    top = innerPadding.listTop(),
                    bottom = innerPadding.calculateBottomPadding(),
                )
                .oneUiScrollFade(scrollState)
                .verticalScroll(scrollState)
                .padding(top = innerPadding.listInset())

                .padding(horizontal = ScreenMargin)
                .padding(bottom = GroupGap)
                .readableWidth(),
            verticalArrangement = Arrangement.spacedBy(GroupGap),
        ) {
            Section(R.string.settings_section_restart) { RestartItems(values, actions) }
            Section(R.string.settings_section_appearance) {
                AppearanceItems(
                    values.appearance,
                    actions,
                )
            }
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
        // One UI leaves a row that opens a screen to that screen to explain itself, and writes the row's value,
        // here the count, under its name.
        supporting = if (values.exclusionCount == 0) {
            if (LocalAppStyle.current == AppStyle.OneUi) null else stringResource(R.string.settings_exclusions_none)
        } else {
            pluralStringResource(
                R.plurals.settings_exclusions_count,
                values.exclusionCount,
                values.exclusionCount,
            )
        },
        supportingColor = valueColor,
        onClick = actions.onOpenExclusions,
        leading = { Icon(Tabler.Outline.Apps, contentDescription = null) },
        // The count is in the line under the name; a second copy of it at the end would say nothing new.
        trailing = { Chevron() },
    )
}

/** One row of the Appearance group: a preference, what it is set to, and the sheet that changes it. */
private class AppearanceRow(
    val icon: ImageVector,
    val title: Int,
    val value: String,
    val dialog: SettingsDialog,
    val swatch: Color? = null,
)

@Composable
private fun AppearanceItems(appearance: Appearance, actions: SettingsActions) {
    val rows = buildList {
        add(
            AppearanceRow(
                Tabler.Outline.Brush,
                R.string.settings_style,
                stringResource(appearance.appStyle.label),
                SettingsDialog.Style,
            ),
        )
        add(
            AppearanceRow(
                Tabler.Outline.SunMoon,
                R.string.settings_theme,
                stringResource(appearance.themeMode.label),
                SettingsDialog.ThemeMode,
            ),
        )
        add(
            AppearanceRow(
                icon = Tabler.Outline.Palette,
                title = R.string.settings_seed_color,
                value = stringResource(appearance.seedColor.label),
                dialog = SettingsDialog.SeedColor,
                swatch = appearance.seedColor.resolved(),
            ),
        )
        // Palette styles and typefaces are Material's: One UI has one palette and the phone's own font.
        if (appearance.appStyle == AppStyle.Material) {
            add(
                AppearanceRow(
                    Tabler.Outline.ColorSwatch,
                    R.string.settings_palette_style,
                    stringResource(appearance.paletteStyle.label),
                    SettingsDialog.PaletteStyle,
                ),
            )
        }
    }
    rows.forEachIndexed { index, row ->
        ChoiceItem(index, rows.size, row.icon, row.title, row.value, row.swatch) { actions.onShow(row.dialog) }
    }
}

/**
 * What can be done first, then what can be read, and the version last, where it is looked for when a problem is
 * reported.
 */
@Composable
private fun AboutItems(version: String, actions: SettingsActions) {
    BurkanSegmentItem(
        index = 0,
        count = ABOUT_ITEMS,
        headline = stringResource(R.string.settings_report_problem),
        onClick = actions.onReportProblem,
        leading = { Icon(Tabler.Outline.Bug, contentDescription = null) },
        trailing = { Chevron() },
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
    BurkanSegmentItem(
        index = 4,
        count = ABOUT_ITEMS,
        headline = stringResource(R.string.settings_version),
        leading = { Icon(Tabler.Outline.InfoCircle, contentDescription = null) },
        // A value that only informs is a row's trailing text in Material. One UI keeps its pill.
        trailing = {
            if (LocalAppStyle.current == AppStyle.OneUi) BurkanPill(version) else TrailingValue(version)
        },
    )
}

@Composable
private fun SettingsDialogs(
    dialog: SettingsDialog,
    values: SettingsValues,
    actions: SettingsActions,
) {
    val appearance = values.appearance
    if (dialog == SettingsDialog.RedoSetup) {
        BurkanConfirm(
            title = stringResource(R.string.settings_redo_setup_confirm_title),
            text = stringResource(R.string.settings_redo_setup_confirm_text),
            confirm = stringResource(R.string.settings_redo_setup_confirm_action),
            dismiss = stringResource(R.string.dialog_cancel),
            onConfirm = actions.onConfirmRedoSetup,
            onDismiss = actions.onDismissDialog,
        )
    } else {
        BurkanBottomSheet(onDismiss = actions.onDismissDialog) { AppearanceChoices(dialog, appearance, actions) }
    }
}

/** The options of one appearance preference, as they are listed in its sheet. */
@Composable
private fun AppearanceChoices(
    dialog: SettingsDialog,
    appearance: Appearance,
    actions: SettingsActions,
) {
    when (dialog) {
        SettingsDialog.Style -> BurkanChoiceList(
            title = stringResource(R.string.settings_style),
            text = stringResource(R.string.settings_style_text),
            options = AppStyle.entries,
            selected = appearance.appStyle,
            label = { stringResource(it.label) },
            onSelect = actions.onAppStyle,
            changesLook = true,
        )

        SettingsDialog.ThemeMode -> BurkanChoiceList(
            title = stringResource(R.string.settings_theme),
            options = ThemeMode.entries,
            selected = appearance.themeMode,
            label = { stringResource(it.label) },
            onSelect = actions.onThemeMode,
            leading = { Icon(it.icon, contentDescription = null) },
            changesLook = true,
        )

        SettingsDialog.SeedColor -> BurkanChoiceList(
            title = stringResource(R.string.settings_seed_color),
            options = SeedColors.entries,
            selected = appearance.seedColor,
            label = { stringResource(it.label) },
            onSelect = actions.onSeedColor,
            changesLook = true,
            leading = {
                Box(
                    Modifier
                        .size(24.dp)
                        .background(it.resolved(), CircleShape),
                )
            },
        )

        SettingsDialog.PaletteStyle -> BurkanChoiceList(
            title = stringResource(R.string.settings_palette_style),
            options = PaletteStyles.entries,
            selected = appearance.paletteStyle,
            label = { stringResource(it.label) },
            onSelect = actions.onPaletteStyle,
            changesLook = true,
        )

        SettingsDialog.RedoSetup -> Unit
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
            if (LocalAppStyle.current == AppStyle.OneUi) {
                OneUiSwitch(checked)
            } else {
                Switch(
                    checked = checked,
                    onCheckedChange = null,
                    thumbContent = {
                        if (checked) {
                            Icon(
                                Tabler.Outline.Check,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp),
                            )
                        }
                    },
                )
            }
        },
    )
}

/**
 * A preference that opens a sheet of choices. In Material what is chosen now sits at the end of the row, before the
 * chevron, so the group reads as two columns: the names down one side, their values down the other. One UI writes
 * it under the name, in the accent colour, and has no chevron.
 */
@Composable
private fun ChoiceItem(
    index: Int,
    count: Int,
    icon: ImageVector,
    title: Int,
    value: String,
    swatch: Color? = null,
    onClick: () -> Unit,
) {
    val oneUi = LocalAppStyle.current == AppStyle.OneUi
    BurkanSegmentItem(
        index = index,
        count = count,
        headline = stringResource(title),
        supporting = if (oneUi) value else null,
        supportingColor = valueColor,
        onClick = onClick,
        leading = { Icon(icon, contentDescription = null) },
        trailing = {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                if (swatch != null) Box(
                    Modifier
                        .size(14.dp)
                        .background(swatch, CircleShape),
                )
                if (!oneUi) {
                    TrailingValue(value)
                }
                Chevron()
            }
        },
    )
}

/** What a row is set to, or simply says, at its end. */
@Composable
private fun TrailingValue(value: String) {
    Text(
        value,
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        modifier = Modifier.widthIn(max = VALUE_MAX_WIDTH),
    )
}

/** The colour of a row's second line when that line is its value: One UI's accent. Material has no such line. */
private val valueColor: Color?
    @Composable
    get() = if (LocalAppStyle.current == AppStyle.OneUi) MaterialTheme.colorScheme.primary else null

/** A value longer than this is cut short, so the row's name always has the room it needs. */
private val VALUE_MAX_WIDTH = 150.dp

/** Marks a row that opens something: another screen, or a page in the browser. One UI's rows carry no such mark. */
@Composable
private fun Chevron() {
    if (LocalAppStyle.current == AppStyle.OneUi) return
    Icon(Icons.AutoMirrored.Outlined.KeyboardArrowRight, contentDescription = null)
}

private const val RESTART_ITEMS = 3
private const val ABOUT_ITEMS = 5

private fun sample(
    dialog: SettingsDialog? = null,
    exclusions: Int = 0,
    appStyle: AppStyle = AppStyle.Material,
) = SettingsState(
    values = SettingsValues(
        applyOnBoot = SettingsStorage.Defaults.APPLY_ON_BOOT,
        turnOffWirelessDebugging = SettingsStorage.Defaults.TURN_OFF_WIRELESS_DEBUGGING,
        exclusionCount = exclusions,
        appearance = sampleAppearance(appStyle),
    ),
    dialog = dialog,
    version = "1.0.0",
)

private fun sampleAppearance(
    appStyle: AppStyle = AppStyle.Material,
) = Appearance(
    appStyle = appStyle,
    themeMode = SettingsStorage.Defaults.themeMode,
    seedColor = SeedColors.Blue,
    paletteStyle = SettingsStorage.Defaults.paletteStyle,
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
private fun SettingsThemeSheetPreview() =
    BurkanSheetPreview { AppearanceChoices(SettingsDialog.ThemeMode, sampleAppearance(), SettingsActions()) }

@PreviewWrapper(BurkanPreviewWrapper::class)
@BurkanPreview
@Composable
private fun SettingsSeedColorSheetPreview() =
    BurkanSheetPreview { AppearanceChoices(SettingsDialog.SeedColor, sampleAppearance(), SettingsActions()) }

@PreviewWrapper(BurkanPreviewWrapper::class)
@BurkanPreview
@Composable
private fun SettingsRedoSetupPreview() =
    SettingsContent(sample(SettingsDialog.RedoSetup), SettingsActions())

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
    BurkanPreviewTheme(fontScale = LargeFontScale) {
        SettingsContent(sample(), SettingsActions())
    }
}

@BurkanPreview
@Composable
private fun SettingsPaletteStyleSheetTealPreview() {
    BurkanPreviewTheme(seedColor = SeedColors.Teal) {
        BurkanSheetPreview {
            AppearanceChoices(SettingsDialog.PaletteStyle, sampleAppearance(), SettingsActions())
        }
    }
}

@BurkanPreview
@Composable
private fun SettingsOneUiPreview() {
    BurkanPreviewTheme(appStyle = AppStyle.OneUi) {
        SettingsContent(sample(exclusions = 3, appStyle = AppStyle.OneUi), SettingsActions())
    }
}

@BurkanPreview
@Composable
private fun SettingsOneUiDarkPreview() {
    BurkanPreviewTheme(themeMode = ThemeMode.Dark, appStyle = AppStyle.OneUi) {
        SettingsContent(sample(exclusions = 3, appStyle = AppStyle.OneUi), SettingsActions())
    }
}

@BurkanPreview
@Composable
private fun SettingsStyleSheetOneUiPreview() {
    BurkanPreviewTheme(appStyle = AppStyle.OneUi) {
        BurkanSheetPreview {
            AppearanceChoices(
                SettingsDialog.Style,
                sampleAppearance(appStyle = AppStyle.OneUi),
                SettingsActions(),
            )
        }
    }
}
