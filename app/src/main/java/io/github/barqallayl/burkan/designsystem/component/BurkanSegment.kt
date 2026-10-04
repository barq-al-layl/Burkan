package io.github.barqallayl.burkan.designsystem.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedListItem
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.contentColorFor
import androidx.compose.material3.surfaceColorAtElevation
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * The shape of item [index] of [count] in a segmented group: the group's outer corners are large, every corner
 * between two segments is extra small, and a lone item is simply large.
 */
@Composable
@ReadOnlyComposable
fun segmentedShape(index: Int, count: Int): Shape {
    val outer = MaterialTheme.shapes.large
    val inner = MaterialTheme.shapes.extraSmall
    val isTop = index == 0
    val isBottom = index == count - 1
    return outer.copy(
        topStart = if (isTop) outer.topStart else inner.topStart,
        topEnd = if (isTop) outer.topEnd else inner.topEnd,
        bottomStart = if (isBottom) outer.bottomStart else inner.bottomStart,
        bottomEnd = if (isBottom) outer.bottomEnd else inner.bottomEnd,
    )
}

/** The space between segments of one group. */
val SegmentGap: Dp = 2.dp

/** The space between one group and the next. */
val GroupGap: Dp = 16.dp

/** The space between the screen's edge and its groups. */
val ScreenMargin: Dp = 16.dp

/** The segments' fill: a surface raised just off the screen's background. */
val segmentContainerColor: Color
    @Composable
    @ReadOnlyComposable
    get() = MaterialTheme.colorScheme.surfaceColorAtElevation(3.dp)

/**
 * One segment of a group. Place segments in a [SegmentedColumn] (or any column spaced by [SegmentGap]) and pass
 * each its [index] and the group's [count]. A segment always spans its slot: a group only reads as one card when
 * its edges line up.
 */
@Composable
fun BurkanSegment(
    index: Int,
    count: Int,
    modifier: Modifier = Modifier,
    containerColor: Color = segmentContainerColor,
    onClick: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    val shape = segmentedShape(index, count)
    val fill = modifier.fillMaxWidth()
    val contentColor = contentColorFor(containerColor)
    if (onClick != null) {
        Surface(onClick = onClick, modifier = fill, shape = shape, color = containerColor, contentColor = contentColor) {
            Column(content = content)
        }
    } else {
        Surface(modifier = fill, shape = shape, color = containerColor, contentColor = contentColor) {
            Column(content = content)
        }
    }
}

/** A group of segments: a column with the segmented gap between its items. */
@Composable
fun SegmentedColumn(modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(SegmentGap),
        content = content,
    )
}

/**
 * A segment laid out as a row of a list: [leading], then [headline] over [supporting], then [trailing]. [content]
 * goes under the text, for a row that carries more than two lines. It is Material's own segmented list item, so a
 * row that is pressed, and one that is selected, changes shape as well as colour.
 *
 * A row is one of four things. With nothing more it only shows. With [onClick] it is tapped. With [selected] it is
 * one option out of several, and [onClick] chooses it. With [checked] it is ticked or not, on its own, and
 * [onCheckedChange] flips it. The selected and the ticked row are filled and rounded.
 */
@Composable
fun BurkanSegmentItem(
    index: Int,
    count: Int,
    headline: String,
    modifier: Modifier = Modifier,
    supporting: String? = null,
    containerColor: Color = segmentContainerColor,
    headlineStyle: TextStyle = MaterialTheme.typography.titleMedium,
    verticalAlignment: Alignment.Vertical = Alignment.CenterVertically,
    selected: Boolean? = null,
    checked: Boolean? = null,
    onCheckedChange: ((Boolean) -> Unit)? = null,
    onClick: (() -> Unit)? = null,
    leading: (@Composable () -> Unit)? = null,
    trailing: (@Composable () -> Unit)? = null,
    content: (@Composable ColumnScope.() -> Unit)? = null,
) {
    val contentColor = contentColorFor(containerColor)
    val selectedContentColor = MaterialTheme.colorScheme.onSecondaryContainer
    val colors = ListItemDefaults.segmentedColors(
        containerColor = containerColor,
        contentColor = contentColor,
        leadingContentColor = contentColor,
        trailingContentColor = contentColor,
        supportingContentColor = contentColor.copy(alpha = SUPPORTING_ALPHA),
        selectedContainerColor = MaterialTheme.colorScheme.secondaryContainer,
        selectedContentColor = selectedContentColor,
        selectedLeadingContentColor = selectedContentColor,
        selectedTrailingContentColor = selectedContentColor,
        selectedSupportingContentColor = selectedContentColor.copy(alpha = SUPPORTING_ALPHA),
    )
    val shapes = ListItemDefaults.segmentedShapes(index, count)
    val fill = modifier.fillMaxWidth()
    val supportingContent: (@Composable () -> Unit)? = if (supporting == null && content == null) {
        null
    } else {
        {
            Column {
                if (supporting != null) Text(supporting, style = MaterialTheme.typography.bodyMedium)
                // What goes under the text is the row's own content, not a quieter line of it.
                if (content != null) CompositionLocalProvider(LocalContentColor provides contentColor) { content() }
            }
        }
    }
    val headlineContent = @Composable { Text(headline, style = headlineStyle) }
    when {
        checked != null && onCheckedChange != null -> SegmentedListItem(
            checked = checked,
            onCheckedChange = onCheckedChange,
            shapes = shapes,
            modifier = fill,
            leadingContent = leading,
            trailingContent = trailing,
            supportingContent = supportingContent,
            verticalAlignment = verticalAlignment,
            colors = colors,
            content = headlineContent,
        )
        selected != null && onClick != null -> SegmentedListItem(
            selected = selected,
            onClick = onClick,
            shapes = shapes,
            modifier = fill,
            leadingContent = leading,
            trailingContent = trailing,
            supportingContent = supportingContent,
            verticalAlignment = verticalAlignment,
            colors = colors,
            content = headlineContent,
        )
        onClick != null -> SegmentedListItem(
            onClick = onClick,
            shapes = shapes,
            modifier = fill,
            leadingContent = leading,
            trailingContent = trailing,
            supportingContent = supportingContent,
            verticalAlignment = verticalAlignment,
            colors = colors,
            content = headlineContent,
        )
        else -> SegmentedListItem(
            shapes = shapes,
            modifier = fill,
            leadingContent = leading,
            trailingContent = trailing,
            supportingContent = supportingContent,
            verticalAlignment = verticalAlignment,
            colors = colors,
            content = headlineContent,
        )
    }
}

/** The title above a group. */
@Composable
fun BurkanSectionTitle(text: String, modifier: Modifier = Modifier) {
    Text(
        text,
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.primary,
        modifier = modifier.padding(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 8.dp),
    )
}

/** Supporting text is the content colour, quieter: it follows whatever container the segment has. */
private const val SUPPORTING_ALPHA = 0.74f
