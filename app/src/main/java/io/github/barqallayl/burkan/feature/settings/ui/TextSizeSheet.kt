package io.github.barqallayl.burkan.feature.settings.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.PreviewWrapper
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import com.composables.icons.tabler.Tabler
import com.composables.icons.tabler.outline.Check
import io.github.barqallayl.burkan.R
import io.github.barqallayl.burkan.core.storage.SettingsStorage
import io.github.barqallayl.burkan.designsystem.TextScale
import io.github.barqallayl.burkan.designsystem.component.BurkanPill
import io.github.barqallayl.burkan.designsystem.component.BurkanSheetActions
import io.github.barqallayl.burkan.designsystem.component.BurkanSheetHeader
import io.github.barqallayl.burkan.designsystem.component.GroupGap
import io.github.barqallayl.burkan.designsystem.component.Tone
import io.github.barqallayl.burkan.designsystem.component.BurkanSegment
import io.github.barqallayl.burkan.designsystem.component.ScreenMargin
import io.github.barqallayl.burkan.designsystem.preview.BurkanPreview
import io.github.barqallayl.burkan.designsystem.preview.BurkanPreviewTheme
import io.github.barqallayl.burkan.designsystem.preview.BurkanPreviewWrapper
import kotlin.math.roundToInt

/**
 * Picks the app's text size.
 *
 * Only the sample changes while the slider moves: re-scaling the whole app live would grow the sheet under the
 * finger dragging the slider. The app changes on Save. The slider sits above the sample for the same reason, the
 * sample growing downwards, away from it. Reset only moves the slider, so the sample shows the default before Save
 * commits it.
 */
@Composable
fun TextSizeSheet(savedPercent: Int, onSave: (Int) -> Unit, onCancel: () -> Unit) {
    var pending by remember(savedPercent) { mutableIntStateOf(savedPercent) }
    val steps = remember { TextScale.percentages.toList() }
    val label = textSizeLabel(pending)
    Column(modifier = Modifier.fillMaxWidth().padding(horizontal = ScreenMargin), verticalArrangement = Arrangement.spacedBy(GroupGap)) {
        // The size as a number beside the title: it is the one thing the slider changes.
        BurkanSheetHeader(
            stringResource(R.string.settings_text_size),
            modifier = Modifier.padding(horizontal = 8.dp),
            trailing = { BurkanPill(stringResource(R.string.text_size_percent, pending), tone = Tone.Good) },
        )
        BurkanSegment(index = 0, count = 1) {
            Column(modifier = Modifier.padding(horizontal = 16.dp).padding(top = 12.dp, bottom = 4.dp)) {
                // A small letter and a large one at the ends: which way is which, without reading the numbers.
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(SAMPLE_LETTER, style = MaterialTheme.typography.bodySmall, modifier = Modifier.clearAndSetSemantics { })
                    Slider(
                        value = steps.indexOf(pending).toFloat(),
                        onValueChange = { pending = steps[it.roundToInt().coerceIn(steps.indices)] },
                        valueRange = 0f..steps.lastIndex.toFloat(),
                        steps = steps.size - 2,
                        modifier = Modifier.weight(1f).semantics { stateDescription = label },
                    )
                    Text(
                        SAMPLE_LETTER,
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.clearAndSetSemantics { },
                    )
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    EndLabel(steps.first())
                    // Always laid out, so the row keeps its height as the button comes and goes.
                    val canReset = pending != SettingsStorage.Defaults.TEXT_SCALE_PERCENT
                    Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
                        TextButton(
                            onClick = { pending = SettingsStorage.Defaults.TEXT_SCALE_PERCENT },
                            enabled = canReset,
                            modifier = Modifier
                                .alpha(if (canReset) 1f else 0f)
                                .then(if (canReset) Modifier else Modifier.clearAndSetSemantics { }),
                        ) {
                            Text(stringResource(R.string.text_size_reset))
                        }
                    }
                    EndLabel(steps.last())
                }
            }
        }
        TextSizeSample(pending, savedPercent)
        BurkanSheetActions(
            modifier = Modifier.padding(bottom = 8.dp),
            dismiss = stringResource(R.string.dialog_cancel),
            onDismiss = onCancel,
            confirm = stringResource(R.string.dialog_save),
            onConfirm = { onSave(pending) },
            confirmIcon = Tabler.Outline.Check,
            confirmEnabled = pending != savedPercent,
        )
    }
}

/** The size at one end of the slider. */
@Composable
private fun EndLabel(percent: Int) {
    Text(
        stringResource(R.string.text_size_percent, percent),
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}

private const val SAMPLE_LETTER = "A"

/**
 * A few lines in the app's own styles at [percent]. The card is always as tall as the sample at the largest size:
 * the sheet is anchored to the bottom, so a sample that grew would push the slider up under the finger.
 */
@Composable
private fun TextSizeSample(percent: Int, savedPercent: Int) {
    BurkanSegment(index = 0, count = 1) {
        Box(modifier = Modifier.padding(16.dp)) {
            // Laid out for its height only: never drawn, never read by a screen reader.
            ProvideSampleScale(TextScale.percentages.last, savedPercent) {
                TextSizeSampleLines(modifier = Modifier.alpha(0f).clearAndSetSemantics { })
            }
            ProvideSampleScale(percent, savedPercent) { TextSizeSampleLines() }
        }
    }
}

/** The surrounding density is already scaled by [savedPercent]; this swaps that for [percent]. */
@Composable
private fun ProvideSampleScale(percent: Int, savedPercent: Int, content: @Composable () -> Unit) {
    val density = LocalDensity.current
    CompositionLocalProvider(
        LocalDensity provides Density(density.density, density.fontScale * percent / savedPercent),
        content = content,
    )
}

@Composable
private fun TextSizeSampleLines(modifier: Modifier = Modifier) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(
            stringResource(R.string.home_headline_active),
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.primary,
        )
        Text(stringResource(R.string.settings_apply_on_boot), style = MaterialTheme.typography.titleMedium)
        Text(
            stringResource(R.string.settings_apply_on_boot_text),
            style = MaterialTheme.typography.bodyMedium,
            color = LocalContentColor.current.copy(alpha = 0.74f),
        )
    }
}

/** "Default" at the default size, the percentage otherwise. */
@Composable
fun textSizeLabel(percent: Int): String = if (percent == SettingsStorage.Defaults.TEXT_SCALE_PERCENT) {
    stringResource(R.string.text_size_default)
} else {
    stringResource(R.string.text_size_percent, percent)
}

@PreviewWrapper(BurkanPreviewWrapper::class)
@BurkanPreview
@Composable
private fun TextSizeSheetPreview() =
    TextSizeSheet(savedPercent = SettingsStorage.Defaults.TEXT_SCALE_PERCENT, onSave = {}, onCancel = {})

@BurkanPreview
@Composable
private fun TextSizeSheetLargePreview() {
    BurkanPreviewTheme(textScalePercent = TextScale.percentages.last) {
        TextSizeSheet(savedPercent = TextScale.percentages.last, onSave = {}, onCancel = {})
    }
}
