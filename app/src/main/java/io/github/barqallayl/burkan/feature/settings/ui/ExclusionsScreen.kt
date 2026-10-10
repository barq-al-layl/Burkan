package io.github.barqallayl.burkan.feature.settings.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Checkbox
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.PreviewWrapper
import androidx.compose.ui.unit.dp
import com.composables.icons.tabler.Tabler
import com.composables.icons.tabler.outline.AlertCircle
import com.composables.icons.tabler.outline.Apps
import com.composables.icons.tabler.outline.Lock
import com.composables.icons.tabler.outline.Search
import com.composables.icons.tabler.outline.ShieldCheck
import dev.zacsweers.metrox.viewmodel.metroViewModel
import io.github.barqallayl.burkan.R
import io.github.barqallayl.burkan.core.model.messageRes
import io.github.barqallayl.burkan.core.navigation.LocalNavigator
import io.github.barqallayl.burkan.core.shell.PackageName
import io.github.barqallayl.burkan.designsystem.SeedColors
import io.github.barqallayl.burkan.designsystem.preview.LargeFontScale
import io.github.barqallayl.burkan.designsystem.ThemeMode
import io.github.barqallayl.burkan.designsystem.component.FloatingSearchRoom
import io.github.barqallayl.burkan.designsystem.component.BurkanFloatingSearch
import io.github.barqallayl.burkan.designsystem.component.scrolledPx
import io.github.barqallayl.burkan.designsystem.component.listInset
import io.github.barqallayl.burkan.designsystem.component.rememberBurkanAppBar
import io.github.barqallayl.burkan.designsystem.component.listTop
import io.github.barqallayl.burkan.designsystem.component.topBarScroll
import io.github.barqallayl.burkan.designsystem.component.oneUiScrollFade
import io.github.barqallayl.burkan.designsystem.component.BurkanTopBar
import io.github.barqallayl.burkan.designsystem.AppStyle
import io.github.barqallayl.burkan.designsystem.LocalAppStyle
import io.github.barqallayl.burkan.designsystem.component.BurkanTonalButton
import io.github.barqallayl.burkan.designsystem.component.glassSource
import io.github.barqallayl.burkan.designsystem.component.readableWidth
import io.github.barqallayl.burkan.designsystem.component.BurkanIconBadge
import io.github.barqallayl.burkan.designsystem.component.BurkanMessage
import io.github.barqallayl.burkan.designsystem.component.BurkanSearchField
import io.github.barqallayl.burkan.designsystem.component.BurkanSegment
import io.github.barqallayl.burkan.designsystem.component.BurkanSegmentItem
import io.github.barqallayl.burkan.designsystem.component.GroupGap
import io.github.barqallayl.burkan.designsystem.component.ScreenMargin
import io.github.barqallayl.burkan.designsystem.component.SegmentGap
import io.github.barqallayl.burkan.designsystem.component.bleedsToScreenEdges
import io.github.barqallayl.burkan.designsystem.component.Tone
import io.github.barqallayl.burkan.designsystem.preview.BurkanPreview
import io.github.barqallayl.burkan.designsystem.preview.BurkanPreviewTheme
import io.github.barqallayl.burkan.designsystem.preview.BurkanPreviewWrapper
import io.github.barqallayl.burkan.feature.apply.data.FullApplyPlan
import io.github.barqallayl.burkan.feature.settings.model.AppFilter
import io.github.barqallayl.burkan.feature.settings.model.AppList
import io.github.barqallayl.burkan.feature.settings.model.ExcludableApp
import io.github.barqallayl.burkan.feature.settings.model.SettingsError
import io.github.barqallayl.burkan.feature.settings.model.matching
import org.orbitmvi.orbit.compose.collectAsState
import org.orbitmvi.orbit.compose.collectSideEffect

@Composable
fun ExclusionsScreen() {
    val viewModel = metroViewModel<ExclusionsViewModel>()
    val state by viewModel.collectAsState()
    val navigator = LocalNavigator.current
    viewModel.collectSideEffect { effect ->
        when (effect) {
            ExclusionsSideEffect.Back -> navigator.pop()
        }
    }
    // What is typed lives with the field: it is not worth keeping once the screen is left.
    var query by rememberSaveable { mutableStateOf("") }
    var filter by rememberSaveable { mutableStateOf(AppFilter.All) }
    ExclusionsContent(
        state = state,
        query = query,
        filter = filter,
        onSearch = { query = it },
        onFilter = { filter = it },
        onToggle = viewModel::toggle,
        onRetry = viewModel::list,
        onBack = viewModel::back,
        icon = remember(viewModel) { IconLoader(viewModel::cachedIcon, viewModel::icon) },
    )
}

/**
 * How a row gets its app's icon: [cached] at once when it was loaded before, [load] otherwise. Null leaves a
 * placeholder.
 */
private class IconLoader(
    val cached: (PackageName) -> ImageBitmap?,
    val load: suspend (PackageName) -> ImageBitmap?,
)

@Composable
private fun ExclusionsContent(
    state: ExclusionsState,
    query: String,
    filter: AppFilter,
    onSearch: (String) -> Unit,
    onFilter: (AppFilter) -> Unit,
    onToggle: (PackageName) -> Unit,
    onRetry: () -> Unit,
    onBack: () -> Unit,
    icon: IconLoader,
) {
    val appBar = rememberBurkanAppBar()
    val listState = rememberLazyListState()
    Scaffold(
        modifier = Modifier.topBarScroll(appBar),
        topBar = {
            BurkanTopBar(
                onBack = onBack,
                appBar = appBar,
                contentScroll = { listState.scrolledPx() },
                title = { Text(stringResource(R.string.exclusions_title)) },
            )
        },
    ) { innerPadding ->
        val found = remember(state.apps, query, filter, state.excluded) {
            (state.apps as? AppList.Loaded)?.apps?.matching(query, filter, state.excluded).orEmpty()
        }
        Box(modifier = Modifier.fillMaxSize()) {
            val barInset = innerPadding.listInset()
            LazyColumn(
                // The margin is content padding, so the row of chips can run to the screen's edges. The bar's height
                // is kept clear here instead, so the fade starts under it.
                modifier = Modifier
                    .fillMaxSize()
                    .glassSource(appBar)
                    .padding(top = innerPadding.listTop())
                    .oneUiScrollFade(listState)
                    .readableWidth(),
                state = listState,
                contentPadding = PaddingValues(
                    start = ScreenMargin,
                    end = ScreenMargin,
                    bottom = innerPadding.calculateBottomPadding() + GroupGap + FloatingSearchRoom,
                ),
                verticalArrangement = Arrangement.spacedBy(SegmentGap),
            ) {
                // In One UI the list runs under the bar: this keeps its start below it, and measures the scroll for it.
                if (barInset > 0.dp) item(key = "bar") { Spacer(Modifier.height(barInset)) }
                val narrowed = query.isNotBlank() || filter != AppFilter.All
                item {
                    // What the screen is for, in a card of its own ahead of the list.
                    BurkanSegment(
                        index = 0,
                        count = 1,
                        modifier = Modifier.padding(bottom = GroupGap),
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            horizontalArrangement = Arrangement.spacedBy(14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            BurkanIconBadge(Tabler.Outline.ShieldCheck, tone = Tone.Good)
                            Text(
                                stringResource(R.string.exclusions_text),
                                style = MaterialTheme.typography.bodyMedium,
                            )
                        }
                    }
                }
                // Part of the list, so it scrolls away with it and leaves the screen to the apps.
                if (state.apps is AppList.Loaded) {
                    item(key = "search") {
                        Column(modifier = Modifier.padding(bottom = 8.dp)) {
                            // One UI's search floats over the foot of the list instead.
                            if (LocalAppStyle.current != AppStyle.OneUi) SearchField(
                                query,
                                onSearch,
                            )
                            Filters(filter, onFilter) { option ->
                                state.apps.apps.matching(
                                    "",
                                    option,
                                    state.excluded,
                                ).size
                            }
                        }
                    }
                }
                when (val apps = state.apps) {
                    AppList.Loading -> item {
                        Column(
                            modifier = Modifier
                                .fillParentMaxWidth()
                                .fillParentMaxHeight(LIST_STATE_HEIGHT),
                            verticalArrangement = Arrangement.spacedBy(
                                12.dp,
                                Alignment.CenterVertically,
                            ),
                            horizontalAlignment = Alignment.CenterHorizontally,
                        ) {
                            LoadingIndicator()
                            Text(stringResource(R.string.exclusions_loading))
                        }
                    }

                    is AppList.Failed -> item {
                        BurkanMessage(
                            icon = Tabler.Outline.AlertCircle,
                            title = stringResource(R.string.exclusions_failed_title),
                            text = stringResource(apps.error.messageRes()),
                        ) {
                            BurkanTonalButton(onClick = onRetry) {
                                Text(stringResource(R.string.exclusions_retry))
                            }
                        }
                    }

                    is AppList.Loaded -> {
                        if (apps.apps.isEmpty()) {
                            item {
                                BurkanMessage(
                                    icon = Tabler.Outline.Apps,
                                    title = stringResource(R.string.exclusions_empty_title),
                                    text = stringResource(R.string.exclusions_empty_text),
                                )
                            }
                        } else if (found.isEmpty()) {
                            item {
                                BurkanMessage(
                                    icon = Tabler.Outline.Search,
                                    title = stringResource(R.string.exclusions_none_found_title),
                                    text = if (query.isBlank()) {
                                        stringResource(R.string.exclusions_none_in_filter_text)
                                    } else {
                                        stringResource(
                                            R.string.exclusions_none_found_text,
                                            query.trim(),
                                        )
                                    },
                                )
                            }
                        } else {
                            itemsIndexed(
                                found,
                                key = { _, app -> app.packageName.value },
                            ) { index, app ->
                                AppItem(
                                    app = app,
                                    index = index,
                                    count = found.size,
                                    excluded = app.packageName in state.excluded,
                                    icon = icon,
                                    onToggle = { onToggle(app.packageName) },
                                )
                            }
                        }
                    }
                }
                // A search or a filter is over the apps that can be chosen; the ones that cannot would only be in its way.
                if (!narrowed) fixedItems(state.fixed, icon)
            }
            if (LocalAppStyle.current == AppStyle.OneUi && state.apps is AppList.Loaded) {
                BurkanFloatingSearch(
                    query = query,
                    onSearch = onSearch,
                    placeholder = stringResource(R.string.exclusions_search),
                    clearLabel = stringResource(R.string.search_clear),
                    glass = appBar,
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .imePadding()
                        .padding(bottom = innerPadding.calculateBottomPadding() + 12.dp),
                )
            }
        }
    }
}

private fun LazyListScope.fixedItems(fixed: List<ExcludableApp>?, icon: IconLoader) {
    item {
        Column(
            modifier = Modifier
                .padding(horizontal = 16.dp)
                .padding(top = GroupGap + 8.dp, bottom = 12.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    Tabler.Outline.Lock,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp),
                    tint = MaterialTheme.colorScheme.primary,
                )
                Text(
                    stringResource(R.string.exclusions_fixed_title),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
            Text(
                stringResource(R.string.exclusions_fixed_text),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
    val fixedCount = FullApplyPlan.FixedExclusions.size
    if (fixed == null) {
        // Until it is known which are installed, the names alone.
        itemsIndexed(
            FullApplyPlan.FixedExclusions,
            key = { _, name -> "fixed:${name.value}" },
        ) { index, name ->
            BurkanSegmentItem(
                index,
                fixedCount,
                name.value,
                leading = { AppIcon(name, icon) },
                trailing = { LockMark() },
            )
        }
    } else {
        itemsIndexed(fixed, key = { _, app -> "fixed:${app.packageName.value}" }) { index, app ->
            BurkanSegmentItem(
                index = index,
                count = fixed.size,
                headline = app.label ?: app.packageName.value,
                supporting = if (app.label != null) app.packageName.value else notInstalled(),
                supportingOneLine = true,
                leading = { AppIcon(app.packageName, icon) },
                trailing = { LockMark() },
            )
        }
    }
}

/** Where a row that can be chosen has its checkbox: this one is kept, and that cannot be changed. */
@Composable
private fun LockMark() {
    Icon(
        Tabler.Outline.Lock,
        contentDescription = stringResource(R.string.exclusions_fixed_mark),
        modifier = Modifier.size(20.dp),
        tint = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}

/** One filter at a time, as a row of chips that scrolls sideways when it does not fit. */
@Composable
private fun Filters(selected: AppFilter, onSelect: (AppFilter) -> Unit, count: (AppFilter) -> Int) {
    Row(
        modifier = Modifier
            .bleedsToScreenEdges()
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = ScreenMargin)
            .padding(top = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        AppFilter.entries.forEach { filter ->
            FilterChip(
                selected = filter == selected,
                onClick = { onSelect(filter) },
                label = {
                    Text(
                        stringResource(
                            R.string.filter_with_count,
                            stringResource(filter.label),
                            count(filter),
                        ),
                    )
                },
                shape = CircleShape,
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                    selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer,
                    selectedLeadingIconColor = MaterialTheme.colorScheme.onPrimaryContainer,
                ),
            )
        }
    }
}

private val AppFilter.label: Int
    get() = when (this) {
        AppFilter.All -> R.string.exclusions_filter_all
        AppFilter.Selected -> R.string.exclusions_filter_selected
        AppFilter.User -> R.string.exclusions_filter_user
        AppFilter.System -> R.string.exclusions_filter_system
    }

/** Searches the apps by name or package name. */
@Composable
private fun SearchField(query: String, onSearch: (String) -> Unit) {
    BurkanSearchField(
        query = query,
        onSearch = onSearch,
        placeholder = stringResource(R.string.exclusions_search),
        clearLabel = stringResource(R.string.search_clear),
    )
}

@Composable
private fun AppItem(
    app: ExcludableApp,
    index: Int,
    count: Int,
    excluded: Boolean,
    icon: IconLoader,
    onToggle: () -> Unit,
) {
    BurkanSegmentItem(
        index = index,
        count = count,
        headline = app.label ?: notInstalled(),
        supporting = app.packageName.value,
        supportingOneLine = true,
        checked = excluded,
        onCheckedChange = { onToggle() },
        leading = { AppIcon(app.packageName, icon) },
        trailing = { Checkbox(checked = excluded, onCheckedChange = null) },
    )
}

/** The app's own icon once loaded; a generic one until then, and for a package that is not installed. */
@Composable
private fun AppIcon(packageName: PackageName, icon: IconLoader) {
    val bitmap by produceState(initialValue = icon.cached(packageName), packageName) {
        value = icon.load(packageName)
    }
    val loaded = bitmap
    if (loaded != null) {
        Image(loaded, contentDescription = null, modifier = Modifier.size(40.dp))
    } else {
        Icon(
            Tabler.Outline.Apps,
            contentDescription = null,
            modifier = Modifier
                .size(40.dp)
                .padding(8.dp),
        )
    }
}

@Composable
private fun notInstalled(): String = stringResource(R.string.exclusions_not_installed)

/** How much of the screen the list's loading state takes, so it does not sit in a sliver at the top. */
private const val LIST_STATE_HEIGHT = 0.5f

private val sampleApps = listOf(
    ExcludableApp(PackageName.known("com.example.calendar"), "Calendar"),
    ExcludableApp(PackageName.known("org.example.chat"), "Chat"),
    ExcludableApp(PackageName.known("com.example.maps"), "Maps"),
    ExcludableApp(PackageName.known("com.example.old"), label = null),
)

/** The fixed exclusions, two of them installed. */
private val sampleFixed = FullApplyPlan.FixedExclusions.mapIndexed { index, packageName ->
    ExcludableApp(packageName, listOf("Blue light filter", "Network stack").getOrNull(index))
}

private val sampleExclusions = ExclusionsState(
    apps = AppList.Loaded(sampleApps),
    excluded = setOf(PackageName.known("org.example.chat"), PackageName.known("com.example.old")),
    fixed = sampleFixed,
)

@Composable
private fun ExclusionsPreviewContent(
    state: ExclusionsState,
    query: String = "",
    filter: AppFilter = AppFilter.All,
) = ExclusionsContent(
    state = state,
    query = query,
    filter = filter,
    onSearch = {},
    onFilter = {},
    onToggle = {},
    onRetry = {},
    onBack = {},
    icon = IconLoader({ null }, { null }),
)

@PreviewWrapper(BurkanPreviewWrapper::class)
@BurkanPreview
@Composable
private fun ExclusionsPreview() = ExclusionsPreviewContent(sampleExclusions)

@PreviewWrapper(BurkanPreviewWrapper::class)
@BurkanPreview
@Composable
private fun ExclusionsSearchPreview() = ExclusionsPreviewContent(sampleExclusions, query = "ma")

@PreviewWrapper(BurkanPreviewWrapper::class)
@BurkanPreview
@Composable
private fun ExclusionsFilterSelectedPreview() =
    ExclusionsPreviewContent(sampleExclusions, filter = AppFilter.Selected)

@PreviewWrapper(BurkanPreviewWrapper::class)
@BurkanPreview
@Composable
private fun ExclusionsNoneFoundPreview() =
    ExclusionsPreviewContent(sampleExclusions, query = "zebra")

@PreviewWrapper(BurkanPreviewWrapper::class)
@BurkanPreview
@Composable
private fun ExclusionsLoadingPreview() = ExclusionsPreviewContent(ExclusionsState())

@PreviewWrapper(BurkanPreviewWrapper::class)
@BurkanPreview
@Composable
private fun ExclusionsEmptyPreview() =
    ExclusionsPreviewContent(
        ExclusionsState(
            apps = AppList.Loaded(emptyList()),
            fixed = sampleFixed,
        ),
    )

@PreviewWrapper(BurkanPreviewWrapper::class)
@BurkanPreview
@Composable
private fun ExclusionsFailedPreview() =
    ExclusionsPreviewContent(
        ExclusionsState(
            apps = AppList.Failed(SettingsError.AppsNotListed),
            fixed = sampleFixed,
        ),
    )

@BurkanPreview
@Composable
private fun ExclusionsDarkPreview() {
    BurkanPreviewTheme(themeMode = ThemeMode.Dark) { ExclusionsPreviewContent(sampleExclusions) }
}

@BurkanPreview
@Composable
private fun ExclusionsLargeTextPreview() {
    BurkanPreviewTheme(fontScale = LargeFontScale) {
        ExclusionsPreviewContent(
            sampleExclusions,
        )
    }
}

@BurkanPreview
@Composable
private fun ExclusionsTealPreview() {
    BurkanPreviewTheme(seedColor = SeedColors.Teal) { ExclusionsPreviewContent(sampleExclusions) }
}

@BurkanPreview
@Composable
private fun ExclusionsOneUiDarkPreview() {
    BurkanPreviewTheme(themeMode = ThemeMode.Dark, appStyle = AppStyle.OneUi) {
        ExclusionsPreviewContent(sampleExclusions)
    }
}
