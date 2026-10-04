package io.github.barqallayl.burkan.designsystem.component

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch

/**
 * The app's bottom sheet. It sits on the screen's own surface colour, so segments inside it look as they do on a
 * screen, and it is either open in full or closed: there is no half-open stop.
 *
 * [content] is handed `hide`, which slides the sheet away and then runs what it is given. Something that changes
 * the screen underneath, or removes the sheet, goes there so it happens once the sheet has gone.
 */
@Composable
fun BurkanBottomSheet(
    onDismiss: () -> Unit,
    content: @Composable ColumnScope.(hide: (then: () -> Unit) -> Unit) -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scope = rememberCoroutineScope()
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface,
    ) {
        content { then -> scope.launch { sheetState.hide() }.invokeOnCompletion { then() } }
    }
}

/**
 * One choice out of [options], as a title over a segmented group of radio rows; made for a [BurkanBottomSheet].
 * [text] says what is being chosen, [leading] draws ahead of an option's label, and [fontFamily] sets a label in a typeface of its own.
 */
@Composable
fun <T> BurkanChoiceList(
    title: String,
    options: List<T>,
    selected: T,
    label: @Composable (T) -> String,
    onSelect: (T) -> Unit,
    text: String? = null,
    fontFamily: ((T) -> FontFamily?)? = null,
    leading: (@Composable (T) -> Unit)? = null,
) {
    Column {
        Text(
            title,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(horizontal = 24.dp),
        )
        if (text != null) {
            Text(
                text,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(horizontal = 24.dp).padding(top = 8.dp),
            )
        }
        SegmentedColumn(
            modifier = Modifier
                .verticalScroll(rememberScrollState())
                .padding(ScreenMargin)
                .selectableGroup(),
        ) {
            options.forEachIndexed { index, option ->
                BurkanSegmentChoice(
                    index = index,
                    count = options.size,
                    text = label(option),
                    selected = option == selected,
                    onClick = { onSelect(option) },
                    fontFamily = fontFamily?.invoke(option),
                    leading = leading?.let { { it(option) } },
                )
            }
        }
    }
}

/** A segment that is one option of a single choice: its label, then a radio button. The chosen one is filled. */
@Composable
fun BurkanSegmentChoice(
    index: Int,
    count: Int,
    text: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    fontFamily: FontFamily? = null,
    leading: (@Composable () -> Unit)? = null,
) {
    BurkanSegmentItem(
        index = index,
        count = count,
        headline = text,
        modifier = modifier,
        headlineStyle = MaterialTheme.typography.titleMedium.copy(
            fontFamily = fontFamily ?: MaterialTheme.typography.titleMedium.fontFamily,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
        ),
        selected = selected,
        onClick = onClick,
        leading = leading,
        // The row is the control; the button only shows its state, in the row's own colour.
        trailing = {
            val color = LocalContentColor.current
            RadioButton(
                selected = selected,
                onClick = null,
                colors = RadioButtonDefaults.colors(selectedColor = color, unselectedColor = color),
            )
        },
    )
}
