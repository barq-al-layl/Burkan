package io.github.barqallayl.burkan.feature.settings.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material3.Checkbox
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.tooling.preview.PreviewWrapper
import androidx.compose.ui.unit.dp
import com.composables.icons.tabler.Tabler
import com.composables.icons.tabler.outline.AlertCircle
import com.composables.icons.tabler.outline.Apps
import dev.zacsweers.metrox.viewmodel.metroViewModel
import io.github.barqallayl.burkan.R
import io.github.barqallayl.burkan.core.model.messageRes
import io.github.barqallayl.burkan.core.navigation.LocalNavigator
import io.github.barqallayl.burkan.core.shell.PackageName
import io.github.barqallayl.burkan.designsystem.SeedColors
import io.github.barqallayl.burkan.designsystem.TextScale
import io.github.barqallayl.burkan.designsystem.ThemeMode
import io.github.barqallayl.burkan.designsystem.component.BurkanMessage
import io.github.barqallayl.burkan.designsystem.preview.BurkanPreview
import io.github.barqallayl.burkan.designsystem.preview.BurkanPreviewTheme
import io.github.barqallayl.burkan.designsystem.preview.BurkanPreviewWrapper
import io.github.barqallayl.burkan.feature.apply.data.FullApplyPlan
import io.github.barqallayl.burkan.feature.settings.model.ExcludableApp
import io.github.barqallayl.burkan.feature.settings.model.SettingsError
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
    ExclusionsContent(
        state = state,
        onToggle = viewModel::toggle,
        onRetry = viewModel::list,
        onBack = viewModel::back,
        icon = viewModel::icon,
    )
}

/** [icon] loads an app's icon when its row is shown; null leaves a placeholder. */
@Composable
private fun ExclusionsContent(
    state: ExclusionsState,
    onToggle: (PackageName) -> Unit,
    onRetry: () -> Unit,
    onBack: () -> Unit,
    icon: suspend (PackageName) -> ImageBitmap?,
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
        LazyColumn(modifier = Modifier.fillMaxSize(), contentPadding = innerPadding) {
            item {
                Text(
                    stringResource(R.string.exclusions_text),
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                )
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
                is AppList.Loaded -> if (apps.apps.isEmpty()) {
                    item {
                        BurkanMessage(
                            icon = Tabler.Outline.Apps,
                            title = stringResource(R.string.exclusions_empty_title),
                            text = stringResource(R.string.exclusions_empty_text),
                        )
                    }
                } else {
                    items(apps.apps, key = { it.packageName.value }) { app ->
                        AppItem(app, app.packageName in state.excluded, icon, onToggle = { onToggle(app.packageName) })
                    }
                }
            }
            item {
                Text(
                    stringResource(R.string.exclusions_fixed_title),
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 24.dp, bottom = 8.dp),
                )
                Text(
                    stringResource(R.string.exclusions_fixed_text),
                    modifier = Modifier.padding(horizontal = 16.dp),
                )
            }
            val fixed = state.fixed
            if (fixed == null) {
                // Until it is known which are installed, the names alone.
                items(FullApplyPlan.FixedExclusions, key = { "fixed:${it.value}" }) { packageName ->
                    ListItem(leadingContent = { AppIcon(packageName, icon) }) { Text(packageName.value) }
                }
            } else {
                items(fixed, key = { "fixed:${it.packageName.value}" }) { app ->
                    ListItem(
                        leadingContent = { AppIcon(app.packageName, icon) },
                        supportingContent = { Text(if (app.label != null) app.packageName.value else notInstalled()) },
                    ) { Text(app.label ?: app.packageName.value) }
                }
            }
        }
    }
}

@Composable
private fun AppItem(
    app: ExcludableApp,
    excluded: Boolean,
    icon: suspend (PackageName) -> ImageBitmap?,
    onToggle: () -> Unit,
) {
    ListItem(
        modifier = Modifier.toggleable(value = excluded, role = Role.Checkbox, onValueChange = { onToggle() }),
        leadingContent = { AppIcon(app.packageName, icon) },
        supportingContent = { Text(app.packageName.value) },
        trailingContent = { Checkbox(checked = excluded, onCheckedChange = null) },
    ) {
        Text(app.label ?: notInstalled())
    }
}

/** The app's own icon once loaded; a generic one until then, and for a package that is not installed. */
@Composable
private fun AppIcon(packageName: PackageName, icon: suspend (PackageName) -> ImageBitmap?) {
    val bitmap by produceState<ImageBitmap?>(initialValue = null, packageName) { value = icon(packageName) }
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
private fun ExclusionsPreviewContent(state: ExclusionsState) =
    ExclusionsContent(state = state, onToggle = {}, onRetry = {}, onBack = {}, icon = { null })

@PreviewWrapper(BurkanPreviewWrapper::class)
@BurkanPreview
@Composable
private fun ExclusionsPreview() = ExclusionsPreviewContent(sampleExclusions)

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
