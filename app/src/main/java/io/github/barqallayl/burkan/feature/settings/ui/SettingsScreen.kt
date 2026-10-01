package io.github.barqallayl.burkan.feature.settings.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.tooling.preview.PreviewWrapper
import androidx.compose.ui.unit.dp
import dev.zacsweers.metrox.viewmodel.metroViewModel
import io.github.barqallayl.burkan.Appearance
import io.github.barqallayl.burkan.R
import io.github.barqallayl.burkan.core.navigation.ExclusionsRoute
import io.github.barqallayl.burkan.core.navigation.LocalNavigator
import io.github.barqallayl.burkan.core.storage.SettingsStorage
import io.github.barqallayl.burkan.designsystem.ColorSpecs
import io.github.barqallayl.burkan.designsystem.PaletteStyles
import io.github.barqallayl.burkan.designsystem.SeedColors
import io.github.barqallayl.burkan.designsystem.TextScale
import io.github.barqallayl.burkan.designsystem.ThemeMode
import io.github.barqallayl.burkan.designsystem.preview.BurkanPreview
import io.github.barqallayl.burkan.designsystem.preview.BurkanPreviewWrapper
import io.github.barqallayl.burkan.feature.settings.model.About
import org.orbitmvi.orbit.compose.collectAsState
import org.orbitmvi.orbit.compose.collectSideEffect
import kotlin.math.roundToInt

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
            onColorSpec = viewModel::setColorSpec,
            onTextScale = viewModel::setTextScalePercent,
            onConfirmRedoSetup = viewModel::confirmRedoSetup,
            onOpenSource = viewModel::openSource,
            onOpenUrl = viewModel::openUrl,
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
    val onColorSpec: (ColorSpecs) -> Unit = {},
    val onTextScale: (Int) -> Unit = {},
    val onConfirmRedoSetup: () -> Unit = {},
    val onOpenSource: () -> Unit = {},
    val onOpenUrl: (String) -> Unit = {},
)

@Composable
private fun SettingsContent(state: SettingsState, actions: SettingsActions) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.settings_title)) },
                navigationIcon = {
                    IconButton(onClick = actions.onBack) {
                        Icon(Icons.AutoMirrored.Outlined.ArrowBack, stringResource(R.string.navigate_back))
                    }
                },
            )
        },
    ) { innerPadding ->
        val values = state.values ?: return@Scaffold
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState()),
        ) {
            SectionTitle(R.string.settings_section_restart)
            SwitchItem(
                title = R.string.settings_apply_on_boot,
                text = R.string.settings_apply_on_boot_text,
                checked = values.applyOnBoot,
                onCheckedChange = actions.onApplyOnBoot,
            )
            SwitchItem(
                title = R.string.settings_turn_off_wireless_debugging,
                text = R.string.settings_turn_off_wireless_debugging_text,
                checked = values.turnOffWirelessDebugging,
                onCheckedChange = actions.onTurnOffWirelessDebugging,
            )
            ListItem(
                modifier = Modifier.clickable(onClick = actions.onOpenExclusions),
                supportingContent = {
                    Text(
                        if (values.exclusionCount == 0) {
                            stringResource(R.string.settings_exclusions_none)
                        } else {
                            pluralStringResource(
                                R.plurals.settings_exclusions_count,
                                values.exclusionCount,
                                values.exclusionCount,
                            )
                        },
                    )
                },
            ) { Text(stringResource(R.string.settings_exclusions)) }

            SectionTitle(R.string.settings_section_appearance)
            AppearanceItems(values.appearance, actions)

            SectionTitle(R.string.settings_section_setup)
            ListItem(
                modifier = Modifier.clickable { actions.onShow(SettingsDialog.RedoSetup) },
                supportingContent = { Text(stringResource(R.string.settings_redo_setup_text)) },
            ) { Text(stringResource(R.string.settings_redo_setup)) }

            SectionTitle(R.string.settings_section_about)
            ListItem(supportingContent = { Text(state.version) }) { Text(stringResource(R.string.settings_version)) }
            ListItem(
                modifier = Modifier.clickable(onClick = actions.onOpenSource),
                supportingContent = { Text(About.SOURCE_URL) },
            ) { Text(stringResource(R.string.settings_source)) }
            ListItem(
                modifier = Modifier.clickable { actions.onShow(SettingsDialog.Licences) },
                supportingContent = { Text(stringResource(R.string.settings_licences_text)) },
            ) { Text(stringResource(R.string.settings_licences)) }
        }
        state.dialog?.let { SettingsDialogs(it, values.appearance, actions) }
    }
}

@Composable
private fun AppearanceItems(appearance: Appearance, actions: SettingsActions) {
    ChoiceItem(R.string.settings_theme, appearance.themeMode.label) { actions.onShow(SettingsDialog.ThemeMode) }
    ChoiceItem(R.string.settings_seed_color, appearance.seedColor.label) { actions.onShow(SettingsDialog.SeedColor) }
    ChoiceItem(R.string.settings_palette_style, appearance.paletteStyle.label) {
        actions.onShow(SettingsDialog.PaletteStyle)
    }
    ChoiceItem(R.string.settings_color_spec, appearance.colorSpec.label) { actions.onShow(SettingsDialog.ColorSpec) }
    TextSizeItem(appearance.textScalePercent, actions.onTextScale)
}

@Composable
private fun SettingsDialogs(dialog: SettingsDialog, appearance: Appearance, actions: SettingsActions) {
    when (dialog) {
        SettingsDialog.ThemeMode -> ChoiceDialog(
            title = R.string.settings_theme,
            options = ThemeMode.entries,
            selected = appearance.themeMode,
            label = { it.label },
            onSelect = actions.onThemeMode,
            onDismiss = actions.onDismissDialog,
        )
        SettingsDialog.SeedColor -> ChoiceDialog(
            title = R.string.settings_seed_color,
            options = SeedColors.entries,
            selected = appearance.seedColor,
            label = { it.label },
            swatch = { it.color },
            onSelect = actions.onSeedColor,
            onDismiss = actions.onDismissDialog,
        )
        SettingsDialog.PaletteStyle -> ChoiceDialog(
            title = R.string.settings_palette_style,
            options = PaletteStyles.entries,
            selected = appearance.paletteStyle,
            label = { it.label },
            onSelect = actions.onPaletteStyle,
            onDismiss = actions.onDismissDialog,
        )
        SettingsDialog.ColorSpec -> ChoiceDialog(
            title = R.string.settings_color_spec,
            options = ColorSpecs.entries,
            selected = appearance.colorSpec,
            label = { it.label },
            onSelect = actions.onColorSpec,
            onDismiss = actions.onDismissDialog,
        )
        SettingsDialog.RedoSetup -> AlertDialog(
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
        SettingsDialog.Licences -> LicencesDialog(actions.onOpenUrl, actions.onDismissDialog)
    }
}

@Composable
private fun SectionTitle(title: Int) {
    Text(
        stringResource(title),
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 24.dp, bottom = 8.dp),
    )
}

@Composable
private fun SwitchItem(title: Int, text: Int, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    ListItem(
        modifier = Modifier.toggleable(value = checked, role = Role.Switch, onValueChange = onCheckedChange),
        supportingContent = { Text(stringResource(text)) },
        trailingContent = { Switch(checked = checked, onCheckedChange = null) },
    ) { Text(stringResource(title)) }
}

@Composable
private fun ChoiceItem(title: Int, value: Int, onClick: () -> Unit) {
    ListItem(
        modifier = Modifier.clickable(onClick = onClick),
        supportingContent = { Text(stringResource(value)) },
    ) { Text(stringResource(title)) }
}

@Composable
private fun TextSizeItem(percent: Int, onChange: (Int) -> Unit) {
    val range = TextScale.percentages
    // The slider moves freely while dragged; the size is stored, and applied, when it is let go.
    var position by remember(percent) { mutableFloatStateOf(percent.toFloat()) }
    ListItem(
        supportingContent = {
            Slider(
                value = position,
                onValueChange = { position = it },
                onValueChangeFinished = { onChange(position.roundToInt()) },
                valueRange = range.first.toFloat()..range.last.toFloat(),
                steps = (range.last - range.first) / range.step - 1,
            )
        },
    ) {
        Text(stringResource(R.string.settings_text_size, position.roundToInt()))
    }
}

@Composable
private fun <T> ChoiceDialog(
    title: Int,
    options: List<T>,
    selected: T,
    label: (T) -> Int,
    onSelect: (T) -> Unit,
    onDismiss: () -> Unit,
    swatch: ((T) -> Color)? = null,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(title)) },
        text = {
            Column(modifier = Modifier.selectableGroup().verticalScroll(rememberScrollState())) {
                options.forEach { option ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .selectable(
                                selected = option == selected,
                                role = Role.RadioButton,
                                onClick = { onSelect(option) },
                            )
                            .padding(vertical = 4.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        RadioButton(selected = option == selected, onClick = null)
                        swatch?.let { Box(Modifier.size(24.dp).background(it(option), CircleShape)) }
                        Text(stringResource(label(option)))
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.dialog_cancel)) } },
    )
}

@Composable
private fun LicencesDialog(onOpenUrl: (String) -> Unit, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.settings_licences)) },
        text = {
            Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                About.libraries.forEach { library ->
                    ListItem(
                        modifier = Modifier.clickable { onOpenUrl(library.url) },
                        supportingContent = { Text(library.licence) },
                    ) { Text(library.name) }
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.dialog_close)) } },
    )
}

private fun sample(dialog: SettingsDialog? = null, exclusions: Int = 0) = SettingsState(
    values = SettingsValues(
        applyOnBoot = SettingsStorage.Defaults.APPLY_ON_BOOT,
        turnOffWirelessDebugging = SettingsStorage.Defaults.TURN_OFF_WIRELESS_DEBUGGING,
        exclusionCount = exclusions,
        appearance = Appearance(
            themeMode = SettingsStorage.Defaults.themeMode,
            seedColor = SettingsStorage.Defaults.seedColor,
            paletteStyle = SettingsStorage.Defaults.paletteStyle,
            colorSpec = SettingsStorage.Defaults.colorSpec,
            textScalePercent = SettingsStorage.Defaults.TEXT_SCALE_PERCENT,
        ),
    ),
    dialog = dialog,
    version = "1.0",
)

@PreviewWrapper(BurkanPreviewWrapper::class)
@BurkanPreview
@Composable
private fun SettingsPreview() = SettingsContent(sample(exclusions = 3), SettingsActions())

@PreviewWrapper(BurkanPreviewWrapper::class)
@BurkanPreview
@Composable
private fun SettingsSeedColorPreview() = SettingsContent(sample(SettingsDialog.SeedColor), SettingsActions())

@PreviewWrapper(BurkanPreviewWrapper::class)
@BurkanPreview
@Composable
private fun SettingsRedoSetupPreview() = SettingsContent(sample(SettingsDialog.RedoSetup), SettingsActions())

@PreviewWrapper(BurkanPreviewWrapper::class)
@BurkanPreview
@Composable
private fun SettingsLicencesPreview() = SettingsContent(sample(SettingsDialog.Licences), SettingsActions())
