package io.github.barqallayl.burkan.designsystem.component

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.LargeFlexibleTopAppBar
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ProvideTextStyle
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Stable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.composables.icons.tabler.Tabler
import com.composables.icons.tabler.outline.ChevronLeft
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.rememberHazeState
import io.github.barqallayl.burkan.R
import io.github.barqallayl.burkan.designsystem.AppStyle
import io.github.barqallayl.burkan.designsystem.LocalAppStyle

/**
 * What ties a screen's content to its bars. In the Material style a large title shrinks into the bar as the content
 * scrolls, and the bars are frosted from the content that passes under them ([glassSource], [glass]). One UI's
 * title is not the bar's to collapse, it leaves with the content.
 */
@Stable
class BurkanAppBar internal constructor(
    internal val material: TopAppBarScrollBehavior?,
    internal val large: Boolean,
    internal val haze: HazeState,
)

/** [large] is for Material: whether the title starts large and shrinks into the bar. */
@Composable
fun rememberBurkanAppBar(large: Boolean = false): BurkanAppBar {
    val behavior = when {
        LocalAppStyle.current != AppStyle.Material -> null
        large -> TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
        else -> TopAppBarDefaults.pinnedScrollBehavior()
    }
    val haze = rememberHazeState()
    return remember(behavior, large, haze) { BurkanAppBar(behavior, large, haze) }
}

/** Ties the screen's scrolling to its top bar. Put it on the screen's scaffold. */
fun Modifier.topBarScroll(bar: BurkanAppBar): Modifier =
    if (bar.material != null) nestedScroll(bar.material.nestedScrollConnection) else this

/**
 * Where a screen's list begins, given the scaffold's padding: at the very top of the screen, in both styles. The
 * list runs under the bar and the status bar. Material's bar is glass and shows it blurred; One UI's is only a
 * title and buttons, and the list fades as it goes under them.
 */
@Composable
fun PaddingValues.listTop(): Dp = 0.dp

/**
 * The room a list keeps clear at its own top, so that it starts below the bar it otherwise runs under.
 */
@Composable
fun PaddingValues.listInset(): Dp = calculateTopPadding()

/**
 * How far a lazy list has been scrolled, for [BurkanTopBar]. Exact while its first item is in view, which is as
 * long as the bar needs it: give the list a first item as tall as [listInset].
 */
fun LazyListState.scrolledPx(): Int =
    if (firstVisibleItemIndex == 0) firstVisibleItemScrollOffset else Int.MAX_VALUE

/**
 * A screen's top bar.
 *
 * In the Material style it is Material's: pinned, frosted glass over the content that scrolls under it, and with a
 * large title on a screen whose [appBar] asks for one.
 *
 * In One UI, as of 8.5, nothing is pinned. The title sits beside the way back, bold, and is carried off the top of
 * the screen with the content, by [contentScroll], the distance the content has been scrolled. Only the buttons
 * stay: the way back and the screen's actions, each on a round card that comes in once there is content under it.
 */
@Composable
fun BurkanTopBar(
    title: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    onBack: (() -> Unit)? = null,
    appBar: BurkanAppBar? = null,
    contentScroll: (() -> Int)? = null,
    actions: @Composable RowScope.() -> Unit = {},
) {
    val material = appBar?.material
    // Neither style's bar says so by itself: the title is what a screen reader should find as the screen's heading.
    val heading: @Composable () -> Unit = { Box(Modifier.semantics { heading() }) { title() } }
    when {
        LocalAppStyle.current == AppStyle.OneUi -> OneUiTopBar(
            heading,
            modifier,
            onBack,
            contentScroll ?: { 0 },
            appBar,
            actions,
        )

        material != null && appBar.large -> LargeFlexibleTopAppBar(
            title = heading,
            modifier = modifier.glass(appBar),
            navigationIcon = { if (onBack != null) BurkanBackButton(onBack) },
            actions = actions,
            colors = glassBarColors,
            scrollBehavior = material,
        )

        appBar != null -> TopAppBar(
            title = heading,
            modifier = modifier.glass(appBar),
            navigationIcon = { if (onBack != null) BurkanBackButton(onBack) },
            actions = actions,
            colors = glassBarColors,
            scrollBehavior = material,
        )

        else -> TopAppBar(
            title = heading,
            modifier = modifier,
            navigationIcon = { if (onBack != null) BurkanBackButton(onBack) },
            actions = actions,
        )
    }
}

/** A glass bar has no fill of its own, scrolled or not: the glass behind it is all of its background. */
private val glassBarColors
    @Composable
    get() = TopAppBarDefaults.topAppBarColors(
        containerColor = Color.Transparent,
        scrolledContainerColor = Color.Transparent,
    )

@Composable
private fun OneUiTopBar(
    title: @Composable () -> Unit,
    modifier: Modifier,
    onBack: (() -> Unit)?,
    contentScroll: () -> Int,
    appBar: BurkanAppBar?,
    actions: @Composable RowScope.() -> Unit,
) {
    val barPx = with(LocalDensity.current) { OneUiBarHeight.toPx() }
    // 0 with the content at its top, 1 once it has moved the bar's own height.
    val scrolled = { (contentScroll() / barPx).coerceIn(0f, 1f) }
    Row(
        modifier = modifier
            .fillMaxWidth()
            .windowInsetsPadding(TopAppBarDefaults.windowInsets)
            .height(OneUiBarHeight)
            .padding(horizontal = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (onBack != null) {
            val description = stringResource(R.string.navigate_back)
            BurkanTooltip(description) {
                FloatingCard(scrolled, appBar, onClick = onBack, modifier = Modifier.size(OneUiButtonSize)) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(Tabler.Outline.ChevronLeft, description, modifier = Modifier.size(28.dp))
                    }
                }
            }
        }
        // The title goes up with the content, and thins out so that it does not run through the status bar.
        Box(
            modifier = Modifier
                .weight(1f)
                .padding(start = if (onBack != null) 2.dp else 12.dp, end = 8.dp)
                .graphicsLayer {
                    translationY = -contentScroll().toFloat().coerceAtMost(barPx * 2)
                    alpha = 1f - scrolled()
                },
        ) {
            // A first screen's title is the size of any other's, as in most of Samsung's own apps.
            ProvideTextStyle(MaterialTheme.typography.titleLarge, title)
        }
        FloatingCard(scrolled, appBar) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                content = actions,
            )
        }
    }
}

/**
 * A round card that a button of the bar floats on. With the content at its top there is nothing under the button
 * and the card is not seen; it comes in as content moves up beneath it, as a piece of glass that the content shows
 * through blurred, with a soft shadow that lifts it off that content. Without a [bar] to take the content from it
 * is simply filled.
 */
@Composable
private fun FloatingCard(
    scrolled: () -> Float,
    bar: BurkanAppBar?,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    content: @Composable () -> Unit,
) {
    val color = MaterialTheme.colorScheme.surfaceContainerHighest
    val card = modifier.graphicsLayer {
        shadowElevation = FloatingShadow.toPx() * scrolled()
        shape = CircleShape
        clip = true
    }
    CompositionLocalProvider(LocalContentColor provides MaterialTheme.colorScheme.onSurface) {
        Box(
            modifier = if (onClick != null) card.clickable(onClick = onClick) else card,
            propagateMinConstraints = true,
        ) {
            // The glass is its own layer under the button, so that it can come in without the button's icon
            // fading with it.
            val pane = Modifier
                .matchParentSize()
                .graphicsLayer { alpha = scrolled() }
            Box(
                if (bar != null) pane.glassPane(bar, color, CircleShape) else pane.drawBehind { drawRect(color) },
            )
            content()
        }
    }
}

/** The bar's height under the status bar. */
private val OneUiBarHeight = 76.dp
private val OneUiButtonSize = 46.dp

/** Soft, as One UI's are: enough to part the button from what passes under it, not to make it stand out. */
private val FloatingShadow = 6.dp

/** The way back, on a Material bar. */
@Composable
fun BurkanBackButton(onBack: () -> Unit) {
    val description = stringResource(R.string.navigate_back)
    BurkanIconButton(Icons.AutoMirrored.Outlined.ArrowBack, description, onBack)
}
