package io.github.barqallayl.burkan.feature.settings.ui

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material3.Checkbox
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.tooling.preview.PreviewWrapper
import androidx.compose.ui.unit.dp
import dev.zacsweers.metrox.viewmodel.metroViewModel
import io.github.barqallayl.burkan.R
import io.github.barqallayl.burkan.core.navigation.LocalNavigator
import io.github.barqallayl.burkan.core.shell.PackageName
import io.github.barqallayl.burkan.designsystem.preview.BurkanPreview
import io.github.barqallayl.burkan.designsystem.preview.BurkanPreviewWrapper
import io.github.barqallayl.burkan.feature.settings.model.ExcludableApp
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
    ExclusionsContent(state = state, onToggle = viewModel::toggle, onBack = viewModel::back)
}

@Composable
private fun ExclusionsContent(state: ExclusionsState, onToggle: (PackageName) -> Unit, onBack: () -> Unit) {
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
            val apps = state.apps
            if (apps == null) {
                item {
                    ListItem(leadingContent = { LoadingIndicator() }) {
                        Text(stringResource(R.string.exclusions_loading))
                    }
                }
            } else {
                items(apps, key = { it.packageName.value }) { app ->
                    AppItem(app, app.packageName in state.excluded, onToggle = { onToggle(app.packageName) })
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
            items(state.fixed, key = { "fixed:${it.value}" }) { name ->
                ListItem { Text(name.value) }
            }
        }
    }
}

@Composable
private fun AppItem(app: ExcludableApp, excluded: Boolean, onToggle: () -> Unit) {
    ListItem(
        modifier = Modifier.toggleable(value = excluded, role = Role.Checkbox, onValueChange = { onToggle() }),
        supportingContent = { Text(app.packageName.value) },
        trailingContent = { Checkbox(checked = excluded, onCheckedChange = null) },
    ) {
        Text(app.label ?: stringResource(R.string.exclusions_not_installed))
    }
}

private val sampleApps = listOf(
    ExcludableApp(PackageName.known("com.example.calendar"), "Calendar"),
    ExcludableApp(PackageName.known("org.example.chat"), "Chat"),
    ExcludableApp(PackageName.known("com.example.maps"), "Maps"),
    ExcludableApp(PackageName.known("com.example.old"), label = null),
)

@PreviewWrapper(BurkanPreviewWrapper::class)
@BurkanPreview
@Composable
private fun ExclusionsPreview() = ExclusionsContent(
    state = ExclusionsState(
        apps = sampleApps,
        excluded = setOf(PackageName.known("org.example.chat"), PackageName.known("com.example.old")),
    ),
    onToggle = {},
    onBack = {},
)

@PreviewWrapper(BurkanPreviewWrapper::class)
@BurkanPreview
@Composable
private fun ExclusionsLoadingPreview() = ExclusionsContent(state = ExclusionsState(), onToggle = {}, onBack = {})
