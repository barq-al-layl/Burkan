package io.github.barqallayl.burkan.designsystem.component

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.LocalRippleConfiguration
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
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import io.github.barqallayl.burkan.designsystem.AppStyle
import io.github.barqallayl.burkan.designsystem.LocalAppStyle
import io.github.barqallayl.burkan.designsystem.RevealParticipant
import io.github.barqallayl.burkan.designsystem.emphasis
import io.github.barqallayl.burkan.designsystem.heldUntilRevealed
import io.github.barqallayl.burkan.designsystem.recordsRevealOrigin
import kotlinx.coroutines.launch

/**
 * The app's bottom sheet. In the Material style it is Material's: as wide as the screen, docked to its bottom edge,
 * rounded at the top and a tone off the screen's colour. In One UI it is a card that floats above the bottom of the
 * screen, with the screen showing around it. Either way it is open in full or closed: there is no half-open stop.
 *
 * [content] is handed `hide`, which slides the sheet away and then runs what it is given. Something that changes
 * the screen underneath, or removes the sheet, goes there so it happens once the sheet has gone.
 *
 * A sheet is a window of its own, so it joins the circle a change of theme or style spreads in: the new look
 * inside the circle, the old outside it, as on the screen behind.
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
        GlassBehindWindow()
        RevealParticipant {
            SheetSurface(modifier = Modifier.recordsRevealOrigin()) {
                content { then ->
                    scope.launch { sheetState.hide() }.invokeOnCompletion { then() }
                }
            }
        }
    }
}

/** What is seen of a sheet: its container, its handle, and [content] under it. */
@Composable
private fun SheetSurface(modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    val oneUi = LocalAppStyle.current == AppStyle.OneUi
    Surface(
        // One UI's stands clear of every edge. Material's runs to the bottom of the screen, under the navigation
        // bar, and keeps only its content above that bar.
        modifier = if (oneUi) {
            modifier
                .windowInsetsPadding(WindowInsets.systemBars)
                .padding(horizontal = 10.dp)
                .padding(bottom = SheetMargin)
        } else {
            modifier.windowInsetsPadding(WindowInsets.statusBars)
        },
        // One UI's is rounder and a lighter grey than the cards it floats over, which is all that sets it apart.
        shape = if (oneUi) OneUiSheetShape else BottomSheetDefaults.ExpandedShape,
        color = if (oneUi) MaterialTheme.colorScheme.surfaceContainerHigh else BottomSheetDefaults.ContainerColor,
        contentColor = MaterialTheme.colorScheme.onSurface,
        shadowElevation = if (oneUi) 6.dp else 0.dp,
    ) {
        CompositionLocalProvider(LocalOnSheet provides true) {
            Column(
                modifier = (if (oneUi) Modifier else Modifier.navigationBarsPadding()).padding(bottom = 8.dp),
            ) {
                BottomSheetDefaults.DragHandle(modifier = Modifier.align(Alignment.CenterHorizontally))
                content()
            }
        }
    }
}

/** A sheet's content on the sheet's own container, without the window: how a preview draws a sheet. */
@Composable
fun BurkanSheetPreview(content: @Composable ColumnScope.() -> Unit) {
    SheetSurface(content = content)
}

private val OneUiSheetShape = RoundedCornerShape(34.dp)

/** The space between One UI's sheet and the bottom of the screen. */
private val SheetMargin = 12.dp

/**
 * One choice out of [options], as a title over a segmented group of radio rows; made for a [BurkanBottomSheet].
 * [text] says what is being chosen, and [leading] draws ahead of an option's label.
 *
 * [changesLook] is for a choice that changes how the whole app looks, which spreads from the tap in a circle. That
 * circle is the tap's answer, so a row that would start one draws no ripple of its own: two things spreading from
 * one finger at different speeds read as a stutter. Nor does it change shape under the finger, and the mark of
 * which row is chosen moves only once the circle has begun: the old look is pictured the moment the row is tapped,
 * and has to be at rest. The row already chosen changes nothing and keeps its ripple.
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
    changesLook: Boolean = false,
) {
    val shown = if (changesLook) heldUntilRevealed(selected) else selected
    Column {
        Text(
            title,
            style = emphasis(MaterialTheme.typography.titleLarge, MaterialTheme.typography.titleLargeEmphasized),
            modifier = Modifier
                .padding(horizontal = 24.dp)
                .semantics { heading() },
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
                val quiet = changesLook && option != selected
                // No configuration is how Material is told to draw no ripple.
                CompositionLocalProvider(
                    LocalRippleConfiguration provides if (quiet) null else LocalRippleConfiguration.current,
                ) {
                    BurkanSegmentChoice(
                        index = index,
                        count = options.size,
                        text = label(option),
                        selected = option == shown,
                        onClick = { onSelect(option) },
                        leading = leading?.let { { it(option) } },
                        pressMorph = !quiet,
                    )
                }
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
    pressMorph: Boolean = true,
) {
    BurkanSegmentItem(
        index = index,
        count = count,
        headline = text,
        modifier = modifier,
        supporting = supporting,
        // The chosen option is set heavier than the others.
        headlineStyle = when {
            selected -> emphasis(
                MaterialTheme.typography.titleMedium,
                MaterialTheme.typography.titleMediumEmphasized,
                oneUiWeight = FontWeight.Bold,
            )

            LocalAppStyle.current == AppStyle.OneUi ->
                MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Normal)

            else -> MaterialTheme.typography.titleMedium
        },
        selected = selected,
        onClick = onClick,
        leading = leading,
        pressMorph = pressMorph,
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
