package io.github.barqallayl.burkan.designsystem.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedListItem
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.contentColorFor
import androidx.compose.material3.surfaceColorAtElevation
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.layout.layout
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import io.github.barqallayl.burkan.designsystem.AppStyle
import io.github.barqallayl.burkan.designsystem.LocalAppStyle

/**
 * The shape of item [index] of [count] in a segmented group: the group's outer corners are large, every corner
 * between two segments is extra small, and a lone item is simply large.
 */
@Composable
@ReadOnlyComposable
fun segmentedShape(index: Int, count: Int): Shape {
    val oneUi = LocalAppStyle.current == AppStyle.OneUi
    // In One UI a group is one card: its rows meet square, with a hairline between them.
    val outer = if (oneUi) OneUiCardShape else MaterialTheme.shapes.large
    val inner = if (oneUi) SquareCorners else MaterialTheme.shapes.extraSmall
    val isTop = index == 0
    val isBottom = index == count - 1
    return outer.copy(
        topStart = if (isTop) outer.topStart else inner.topStart,
        topEnd = if (isTop) outer.topEnd else inner.topEnd,
        bottomStart = if (isBottom) outer.bottomStart else inner.bottomStart,
        bottomEnd = if (isBottom) outer.bottomEnd else inner.bottomEnd,
    )
}

/**
 * The fill and the text colour of a notice: something being waited for, or a caution. Amber, in a pair for each
 * theme, because the scheme has no colour that means it: under the expressive palette the secondary and tertiary
 * colours are greens, which read as good news.
 */
@Immutable
data class NoticeColors(val container: Color, val content: Color)

val noticeColors: NoticeColors
    @Composable
    @ReadOnlyComposable
    get() = if (MaterialTheme.colorScheme.surface.luminance() < DARK_SURFACE_LUMINANCE) {
        NoticeColors(container = Color(0xFF4A3B10), content = Color(0xFFFFE9B0))
    } else {
        NoticeColors(container = Color(0xFFFFEBB8), content = Color(0xFF3A2C00))
    }

private const val DARK_SURFACE_LUMINANCE = 0.5f

private val OneUiCardShape = RoundedCornerShape(27.dp)
private val SquareCorners = RoundedCornerShape(0.dp)

/** Whether what is drawn sits on a floating sheet. The sheet says so. */
val LocalOnSheet = staticCompositionLocalOf { false }

/** The space between segments of one group. One UI's rows touch, parted by a line instead. */
val SegmentGap: Dp
    @Composable
    @ReadOnlyComposable
    get() = if (LocalAppStyle.current == AppStyle.OneUi) 0.dp else 2.dp

/**
 * In One UI, the hairline that parts a row from the one above it, drawn inside the row's own top edge and inset
 * from the card's sides. Nothing in the Material style, and nothing on a group's first row.
 */
@Composable
fun Modifier.segmentDivider(index: Int): Modifier {
    if (LocalAppStyle.current != AppStyle.OneUi || index == 0) return this
    val color = MaterialTheme.colorScheme.outlineVariant
    return drawWithContent {
        drawContent()
        val inset = DividerInset.toPx()
        drawLine(
            color,
            Offset(inset, 0f),
            Offset(size.width - inset, 0f),
            strokeWidth = 1.dp.toPx(),
        )
    }
}

private val DividerInset = 16.dp

/** The space between one group and the next. */
val GroupGap: Dp
    @Composable
    @ReadOnlyComposable
    get() = if (LocalAppStyle.current == AppStyle.OneUi) 20.dp else 16.dp

/** The space between the screen's edge and its groups. One UI's cards come close to the edge. */
val ScreenMargin: Dp
    @Composable
    @ReadOnlyComposable
    get() = if (LocalAppStyle.current == AppStyle.OneUi) 10.dp else 16.dp

/**
 * Lets a row that scrolls sideways run to the screen's edges, from inside a list that keeps [ScreenMargin] at its
 * sides: it is laid out a margin wider each way. Give the row's content the margin back as padding, so it starts
 * in line with everything else and is only cut off at the edge of the screen.
 */
@Composable
fun Modifier.bleedsToScreenEdges(): Modifier {
    val screenMargin = ScreenMargin
    return bleeds(screenMargin)
}

private fun Modifier.bleeds(screenMargin: Dp): Modifier = layout { measurable, constraints ->
    val margin = screenMargin.roundToPx()
    val width = constraints.maxWidth + margin * 2
    val placeable = measurable.measure(constraints.copy(minWidth = width, maxWidth = width))
    layout(constraints.maxWidth, placeable.height) { placeable.place(-margin, 0) }
}

/**
 * The space between the screen's edge and anything that is read or touched outside a card. One UI's cards come
 * near the edge, but what stands alone keeps the distance their content does.
 */
val ContentMargin: Dp
    @Composable
    @ReadOnlyComposable
    get() = if (LocalAppStyle.current == AppStyle.OneUi) 24.dp else 16.dp

/** The segments' fill: a surface raised just off the screen's background. */
val segmentContainerColor: Color
    @Composable
    @ReadOnlyComposable
    get() = if (LocalAppStyle.current == AppStyle.OneUi) {
        // On a floating sheet, which is itself raised, a group is a shade lighter still.
        if (LocalOnSheet.current) {
            MaterialTheme.colorScheme.onSurface.copy(alpha = 0.07f)
                .compositeOver(MaterialTheme.colorScheme.surfaceContainerHigh)
        } else {
            MaterialTheme.colorScheme.surfaceContainer
        }
    } else {
        MaterialTheme.colorScheme.surfaceColorAtElevation(3.dp)
    }

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
    contentColor: Color = contentColorFor(containerColor),
    onClick: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    val shape = segmentedShape(index, count)
    val fill = modifier
        .fillMaxWidth()
        .segmentDivider(index)
    if (onClick != null) {
        Surface(
            onClick = onClick,
            modifier = fill,
            shape = shape,
            color = containerColor,
            contentColor = contentColor,
        ) {
            Column(content = content)
        }
    } else {
        Surface(
            modifier = fill,
            shape = shape,
            color = containerColor,
            contentColor = contentColor,
        ) {
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
    contentColor: Color = contentColorFor(containerColor),
    headlineStyle: TextStyle = MaterialTheme.typography.titleMedium,
    selected: Boolean? = null,
    checked: Boolean? = null,
    onCheckedChange: ((Boolean) -> Unit)? = null,
    onClick: (() -> Unit)? = null,
    leading: (@Composable () -> Unit)? = null,
    trailing: (@Composable () -> Unit)? = null,
    content: (@Composable ColumnScope.() -> Unit)? = null,
) {
    // Selection is the primary colour's tint, like the app's other marks of "this one": under the expressive palette
    // the secondary colour is another hue altogether, and reads as a different meaning.
    val oneUi = LocalAppStyle.current == AppStyle.OneUi
    // One UI marks the chosen row with its control alone: the row keeps its colour and its shape.
    val selectedContentColor =
        if (oneUi) contentColor else MaterialTheme.colorScheme.onPrimaryContainer
    val colors = ListItemDefaults.segmentedColors(
        containerColor = containerColor,
        contentColor = contentColor,
        leadingContentColor = contentColor,
        trailingContentColor = contentColor,
        supportingContentColor = contentColor.copy(alpha = SUPPORTING_ALPHA),
        selectedContainerColor = if (oneUi) containerColor else MaterialTheme.colorScheme.primaryContainer,
        selectedContentColor = selectedContentColor,
        selectedLeadingContentColor = selectedContentColor,
        selectedTrailingContentColor = selectedContentColor,
        selectedSupportingContentColor = selectedContentColor.copy(alpha = SUPPORTING_ALPHA),
    )
    // A row's icon and control sit level with the middle of its text. A row that carries more under its text keeps
    // them at the top, beside the text they belong to.
    val verticalAlignment = if (content == null) Alignment.CenterVertically else Alignment.Top
    val shapes = if (oneUi) {
        val shape = segmentedShape(index, count)
        ListItemDefaults.shapes(shape, shape, shape, shape, shape, shape)
    } else {
        ListItemDefaults.segmentedShapes(index, count)
    }
    val fill = modifier
        .fillMaxWidth()
        .segmentDivider(index)
    val supportingContent: (@Composable () -> Unit)? = if (supporting == null && content == null) {
        null
    } else {
        {
            Column {
                if (supporting != null) Text(
                    supporting,
                    style = MaterialTheme.typography.bodyMedium,
                )
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
    // One UI's is quiet and sits further in, over the card's rounded corner.
    val oneUi = LocalAppStyle.current == AppStyle.OneUi
    Text(
        text,
        style = MaterialTheme.typography.titleSmall,
        color = if (oneUi) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.primary,
        modifier = modifier.padding(
            start = if (oneUi) 18.dp else 16.dp,
            end = 16.dp,
            top = 8.dp,
            bottom = 8.dp,
        ),
    )
}

/** Supporting text is the content colour, quieter: it follows whatever container the segment has. */
private const val SUPPORTING_ALPHA = 0.74f
