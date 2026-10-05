package io.github.barqallayl.burkan.designsystem.component

import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.materialkolor.material3.ktx.animateColorScheme
import io.github.barqallayl.burkan.designsystem.AppStyle
import io.github.barqallayl.burkan.designsystem.LocalAppStyle
import io.github.barqallayl.burkan.designsystem.recordsRevealOrigin
import kotlinx.coroutines.launch

/**
 * The app's bottom sheet: a card that floats above the bottom of the screen, with the screen showing around it. It
 * sits on the screen's own surface colour, so segments inside it look as they do on a screen, and it is either open
 * in full or closed: there is no half-open stop.
 *
 * [content] is handed `hide`, which slides the sheet away and then runs what it is given. Something that changes
 * the screen underneath, or removes the sheet, goes there so it happens once the sheet has gone.
 *
 * A sheet is a window of its own, which the circle a change of theme spreads in does not reach: its colours fade
 * to the new scheme instead.
 */
@Composable
fun BurkanBottomSheet(
    onDismiss: () -> Unit,
    content: @Composable ColumnScope.(hide: (then: () -> Unit) -> Unit) -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scope = rememberCoroutineScope()
    // The sheet Material draws is invisible and edge to edge; the card inside it is what is seen.
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        shape = RectangleShape,
        containerColor = Color.Transparent,
        tonalElevation = 0.dp,
        dragHandle = null,
        contentWindowInsets = { WindowInsets(0, 0, 0, 0) },
    ) {
        val colors = animateColorScheme(
            MaterialTheme.colorScheme,
            animationSpec = { tween(SCHEME_FADE_MILLIS) },
        )
        MaterialTheme(colorScheme = colors) {
            val oneUi = LocalAppStyle.current == AppStyle.OneUi
            Surface(
                modifier = Modifier
                    .recordsRevealOrigin()
                    .windowInsetsPadding(WindowInsets.systemBars)
                    .padding(horizontal = if (oneUi) 10.dp else SheetMargin)
                    .padding(bottom = SheetMargin),
                // One UI's is rounder and a lighter grey than the cards it floats over, which is all that sets it
                // apart. Material's is the colour of the screen behind it, so in a dark theme only its edge does.
                shape = if (oneUi) OneUiSheetShape else MaterialTheme.shapes.extraLarge,
                color = if (oneUi) MaterialTheme.colorScheme.surfaceContainerHigh else MaterialTheme.colorScheme.surface,
                contentColor = MaterialTheme.colorScheme.onSurface,
                border = if (oneUi) null else BorderStroke(
                    1.dp,
                    MaterialTheme.colorScheme.outlineVariant,
                ),
                shadowElevation = if (oneUi) 6.dp else 0.dp,
            ) {
                CompositionLocalProvider(LocalOnSheet provides true) {
                    Column(modifier = Modifier.padding(bottom = 8.dp)) {
                        BottomSheetDefaults.DragHandle(modifier = Modifier.align(Alignment.CenterHorizontally))
                        content { then ->
                            scope.launch { sheetState.hide() }.invokeOnCompletion { then() }
                        }
                    }
                }
            }
        }
    }
}

private val OneUiSheetShape = RoundedCornerShape(34.dp)

/** About as long as the circle takes to cross the screen behind the sheet. */
private const val SCHEME_FADE_MILLIS = 350

/** The space between a sheet and the edges of the screen. */
private val SheetMargin = 12.dp

/**
 * One choice out of [options], as a title over a segmented group of radio rows; made for a [BurkanBottomSheet].
 * [text] says what is being chosen, and [leading] draws ahead of an option's label.
 */
@Composable
fun <T> BurkanChoiceList(
    title: String,
    options: List<T>,
    selected: T,
    label: @Composable (T) -> String,
    onSelect: (T) -> Unit,
    text: String? = null,
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
                modifier = Modifier
                    .padding(horizontal = 24.dp)
                    .padding(top = 8.dp),
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
    supporting: String? = null,
    leading: (@Composable () -> Unit)? = null,
) {
    BurkanSegmentItem(
        index = index,
        count = count,
        headline = text,
        modifier = modifier,
        supporting = supporting,
        headlineStyle = MaterialTheme.typography.titleMedium.copy(
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
        ),
        selected = selected,
        onClick = onClick,
        leading = leading,
        // The row is the control; the button only shows its state, in the row's own colour.
        trailing = {
            val color = LocalContentColor.current
            // One UI's row does not fill when chosen, so its button carries the accent instead.
            val oneUi = LocalAppStyle.current == AppStyle.OneUi
            RadioButton(
                selected = selected,
                onClick = null,
                colors = RadioButtonDefaults.colors(
                    selectedColor = if (oneUi) MaterialTheme.colorScheme.primary else color,
                    unselectedColor = if (oneUi) MaterialTheme.colorScheme.onSurfaceVariant else color,
                ),
            )
        },
    )
}
