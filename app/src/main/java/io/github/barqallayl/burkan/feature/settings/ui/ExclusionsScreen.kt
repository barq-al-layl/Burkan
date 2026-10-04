package io.github.barqallayl.burkan.feature.settings.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material3.Checkbox
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.tooling.preview.PreviewWrapper
import androidx.compose.ui.unit.dp
import com.composables.icons.tabler.Tabler
import com.composables.icons.tabler.outline.AlertCircle
import com.composables.icons.tabler.outline.Apps
import com.composables.icons.tabler.outline.Check
import com.composables.icons.tabler.outline.Search
import com.composables.icons.tabler.outline.X
import dev.zacsweers.metrox.viewmodel.metroViewModel
import io.github.barqallayl.burkan.R
import io.github.barqallayl.burkan.core.model.messageRes
import io.github.barqallayl.burkan.core.navigation.LocalNavigator
import io.github.barqallayl.burkan.core.shell.PackageName
import io.github.barqallayl.burkan.designsystem.SeedColors
import io.github.barqallayl.burkan.designsystem.TextScale
import io.github.barqallayl.burkan.designsystem.ThemeMode
import io.github.barqallayl.burkan.designsystem.component.BurkanMessage
import io.github.barqallayl.burkan.designsystem.component.BurkanSectionTitle
import io.github.barqallayl.burkan.designsystem.component.BurkanSegmentItem
import io.github.barqallayl.burkan.designsystem.component.GroupGap
import io.github.barqallayl.burkan.designsystem.component.ScreenMargin
import io.github.barqallayl.burkan.designsystem.component.SegmentGap
import io.github.barqallayl.burkan.designsystem.component.segmentContainerColor
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
private class IconLoader(val cached: (PackageName) -> ImageBitmap?, val load: suspend (PackageName) -> ImageBitmap?)

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
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.exclusions_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Outlined.ArrowBack, stringResource(R.string.navigate_back))
                    }
                },
            )
        },
    ) { innerPadding ->
        val found = remember(state.apps, query, filter, state.excluded) {
            (state.apps as? AppList.Loaded)?.apps?.matching(query, filter, state.excluded).orEmpty()
        }
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(horizontal = ScreenMargin),
            contentPadding = PaddingValues(
                top = innerPadding.calculateTopPadding(),
                bottom = innerPadding.calculateBottomPadding() + GroupGap,
            ),
            verticalArrangement = Arrangement.spacedBy(SegmentGap),
        ) {
            val narrowed = query.isNotBlank() || filter != AppFilter.All
            item {
                Text(
                    stringResource(R.string.exclusions_text),
                    modifier = Modifier.padding(horizontal = 16.dp).padding(bottom = GroupGap),
                )
            }
            // Part of the list, so it scrolls away with it and leaves the screen to the apps.
            if (state.apps is AppList.Loaded) {
                item(key = "search") {
                    Column(modifier = Modifier.padding(bottom = 8.dp)) {
                        SearchField(query, onSearch)
                        Filters(filter, onFilter)
                    }
                }
            }
            when (val apps = state.apps) {
                AppList.Loading -> item {
                    Column(
                        modifier = Modifier.fillParentMaxWidth().fillParentMaxHeight(LIST_STATE_HEIGHT),
                        verticalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterVertically),
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
                        FilledTonalButton(onClick = onRetry) { Text(stringResource(R.string.exclusions_retry)) }
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
                                    stringResource(R.string.exclusions_none_found_text, query.trim())
                                },
                            )
                        }
                    } else {
                        itemsIndexed(found, key = { _, app -> app.packageName.value }) { index, app ->
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
    }
}

private fun LazyListScope.fixedItems(fixed: List<ExcludableApp>?, icon: IconLoader) {
    item {
        Column(modifier = Modifier.padding(top = GroupGap, bottom = 8.dp)) {
            BurkanSectionTitle(stringResource(R.string.exclusions_fixed_title))
            Text(stringResource(R.string.exclusions_fixed_text), modifier = Modifier.padding(horizontal = 16.dp))
        }
    }
    val fixedCount = FullApplyPlan.FixedExclusions.size
    if (fixed == null) {
        // Until it is known which are installed, the names alone.
        itemsIndexed(FullApplyPlan.FixedExclusions, key = { _, name -> "fixed:${name.value}" }) { index, name ->
            BurkanSegmentItem(index, fixedCount, name.value, leading = { AppIcon(name, icon) })
        }
    } else {
        itemsIndexed(fixed, key = { _, app -> "fixed:${app.packageName.value}" }) { index, app ->
            BurkanSegmentItem(
                index = index,
                count = fixed.size,
                headline = app.label ?: app.packageName.value,
                supporting = if (app.label != null) app.packageName.value else notInstalled(),
                leading = { AppIcon(app.packageName, icon) },
            )
        }
    }
}

/** One filter at a time, as a row of chips that scrolls sideways when it does not fit. */
@Composable
private fun Filters(selected: AppFilter, onSelect: (AppFilter) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(top = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        AppFilter.entries.forEach { filter ->
            FilterChip(
                selected = filter == selected,
                onClick = { onSelect(filter) },
                label = { Text(stringResource(filter.label)) },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                    selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer,
                    selectedLeadingIconColor = MaterialTheme.colorScheme.onPrimaryContainer,
                ),
                leadingIcon = if (filter == selected) {
                    { Icon(Tabler.Outline.Check, contentDescription = null, modifier = Modifier.size(18.dp)) }
                } else {
                    null
                },
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
    TextField(
        value = query,
        onValueChange = onSearch,
        modifier = Modifier.fillMaxWidth(),
        placeholder = { Text(stringResource(R.string.exclusions_search)) },
        leadingIcon = { Icon(Tabler.Outline.Search, contentDescription = null) },
        trailingIcon = {
            if (query.isNotEmpty()) {
                IconButton(onClick = { onSearch("") }) {
                    Icon(Tabler.Outline.X, contentDescription = stringResource(R.string.exclusions_search_clear))
                }
            }
        },
        singleLine = true,
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
        shape = CircleShape,
        colors = TextFieldDefaults.colors(
            focusedContainerColor = segmentContainerColor,
            unfocusedContainerColor = segmentContainerColor,
            focusedIndicatorColor = Color.Transparent,
            unfocusedIndicatorColor = Color.Transparent,
        ),
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
        Icon(Tabler.Outline.Apps, contentDescription = null, modifier = Modifier.size(40.dp).padding(8.dp))
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
private fun ExclusionsFilterSelectedPreview() = ExclusionsPreviewContent(sampleExclusions, filter = AppFilter.Selected)

@PreviewWrapper(BurkanPreviewWrapper::class)
@BurkanPreview
@Composable
private fun ExclusionsNoneFoundPreview() = ExclusionsPreviewContent(sampleExclusions, query = "zebra")

@PreviewWrapper(BurkanPreviewWrapper::class)
@BurkanPreview
@Composable
private fun ExclusionsLoadingPreview() = ExclusionsPreviewContent(ExclusionsState())

@PreviewWrapper(BurkanPreviewWrapper::class)
@BurkanPreview
@Composable
private fun ExclusionsEmptyPreview() =
    ExclusionsPreviewContent(ExclusionsState(apps = AppList.Loaded(emptyList()), fixed = sampleFixed))

@PreviewWrapper(BurkanPreviewWrapper::class)
@BurkanPreview
@Composable
private fun ExclusionsFailedPreview() =
    ExclusionsPreviewContent(ExclusionsState(apps = AppList.Failed(SettingsError.AppsNotListed), fixed = sampleFixed))

@BurkanPreview
@Composable
private fun ExclusionsDarkPreview() {
    BurkanPreviewTheme(themeMode = ThemeMode.Dark) { ExclusionsPreviewContent(sampleExclusions) }
}

@BurkanPreview
@Composable
private fun ExclusionsLargeTextPreview() {
    BurkanPreviewTheme(textScalePercent = TextScale.percentages.last) { ExclusionsPreviewContent(sampleExclusions) }
}

@BurkanPreview
@Composable
private fun ExclusionsTealPreview() {
    BurkanPreviewTheme(seedColor = SeedColors.Teal) { ExclusionsPreviewContent(sampleExclusions) }
}
