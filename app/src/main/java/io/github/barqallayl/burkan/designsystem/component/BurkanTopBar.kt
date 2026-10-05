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
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.composables.icons.tabler.Tabler
import com.composables.icons.tabler.outline.ChevronLeft
import io.github.barqallayl.burkan.R
import io.github.barqallayl.burkan.designsystem.AppStyle
import io.github.barqallayl.burkan.designsystem.LocalAppStyle

/**
 * What ties a screen's scrolling to its top bar. Only Material's bar needs it, and only on a screen with a large
 * title: One UI's title is not the bar's to collapse, it leaves with the content.
 */
@Stable
class BurkanAppBar internal constructor(internal val material: TopAppBarScrollBehavior?)

/** [large] is for Material: whether the title starts large and shrinks into the bar. */
@Composable
fun rememberBurkanAppBar(large: Boolean = false): BurkanAppBar {
    val behavior =
        if (large && LocalAppStyle.current == AppStyle.Material) TopAppBarDefaults.exitUntilCollapsedScrollBehavior() else null
    return remember(behavior) { BurkanAppBar(behavior) }
}

/** Ties the screen's scrolling to its top bar. Put it on the screen's scaffold. */
fun Modifier.topBarScroll(bar: BurkanAppBar): Modifier =
    if (bar.material != null) nestedScroll(bar.material.nestedScrollConnection) else this

/**
 * Where a screen's list begins, given the scaffold's padding. Under a Material bar, which is solid. At the very top
 * of the screen in One UI, whose bar is only a title and buttons over the content: the list runs under them, and
 * under the status bar, fading as it goes.
 */
@Composable
fun PaddingValues.listTop(): Dp =
    if (LocalAppStyle.current == AppStyle.OneUi) 0.dp else calculateTopPadding()

/**
 * The room a One UI list keeps clear at its own top, so that it starts below the bar it otherwise runs under.
 * Nothing in the Material style.
 */
@Composable
fun PaddingValues.listInset(): Dp =
    if (LocalAppStyle.current == AppStyle.OneUi) calculateTopPadding() else 0.dp

/**
 * How far a lazy list has been scrolled, for [BurkanTopBar]. Exact while its first item is in view, which is as
 * long as the bar needs it: give the list a first item as tall as [listInset].
 */
fun LazyListState.scrolledPx(): Int =
    if (firstVisibleItemIndex == 0) firstVisibleItemScrollOffset else Int.MAX_VALUE

/**
 * A screen's top bar.
 *
 * In the Material style it is Material's: pinned, and with a large title on a screen whose [appBar] asks for one.
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
    when {
        LocalAppStyle.current == AppStyle.OneUi -> OneUiTopBar(
            title,
            modifier,
            onBack,
            contentScroll ?: { 0 },
            actions,
        )

        material != null -> LargeFlexibleTopAppBar(
            title = title,
            modifier = modifier,
            navigationIcon = { if (onBack != null) BurkanBackButton(onBack) },
            actions = actions,
            scrollBehavior = material,
        )

        else -> TopAppBar(
            title = title,
            modifier = modifier,
            navigationIcon = { if (onBack != null) BurkanBackButton(onBack) },
            actions = actions,
        )
    }
}

@Composable
private fun OneUiTopBar(
    title: @Composable () -> Unit,
    modifier: Modifier,
    onBack: (() -> Unit)?,
    contentScroll: () -> Int,
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
                FloatingCard(scrolled, onClick = onBack, modifier = Modifier.size(OneUiButtonSize)) {
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
            // A first screen names the app, larger; a screen reached from one names itself beside the way back.
            val style =
                if (onBack != null) MaterialTheme.typography.titleLarge else MaterialTheme.typography.headlineMedium
            ProvideTextStyle(style, title)
        }
        FloatingCard(scrolled) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                content = actions,
            )
        }
    }
}

/**
 * A round card that a button of the bar floats on. With the content at its top there is nothing under the button
 * and the card is not seen; it comes in as content moves up beneath it, with a soft shadow that lifts it off that
 * content.
 */
@Composable
private fun FloatingCard(
    scrolled: () -> Float,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    content: @Composable () -> Unit,
) {
    val color = MaterialTheme.colorScheme.surfaceContainerHighest
    val card = modifier
        .graphicsLayer {
            shadowElevation = FloatingShadow.toPx() * scrolled()
            shape = CircleShape
            clip = true
        }
        .drawBehind { drawRect(color, alpha = scrolled()) }
    CompositionLocalProvider(LocalContentColor provides MaterialTheme.colorScheme.onSurface) {
        Box(
            modifier = if (onClick != null) card.clickable(onClick = onClick) else card,
            propagateMinConstraints = true,
        ) {
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
