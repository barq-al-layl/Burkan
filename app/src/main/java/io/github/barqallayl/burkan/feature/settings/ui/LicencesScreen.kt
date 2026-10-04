package io.github.barqallayl.burkan.feature.settings.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.PreviewWrapper
import androidx.compose.ui.unit.dp
import com.composables.icons.tabler.Tabler
import com.composables.icons.tabler.outline.AlertCircle
import com.composables.icons.tabler.outline.License
import com.mikepenz.aboutlibraries.Libs
import com.mikepenz.aboutlibraries.entity.Library
import com.mikepenz.aboutlibraries.entity.License
import dev.zacsweers.metrox.viewmodel.metroViewModel
import io.github.barqallayl.burkan.R
import io.github.barqallayl.burkan.core.model.messageRes
import io.github.barqallayl.burkan.core.navigation.LocalNavigator
import io.github.barqallayl.burkan.designsystem.component.BurkanBottomSheet
import io.github.barqallayl.burkan.designsystem.component.BurkanMessage
import io.github.barqallayl.burkan.designsystem.component.BurkanSegmentItem
import io.github.barqallayl.burkan.designsystem.component.GroupGap
import io.github.barqallayl.burkan.designsystem.component.ScreenMargin
import io.github.barqallayl.burkan.designsystem.component.SegmentGap
import io.github.barqallayl.burkan.designsystem.preview.BurkanPreview
import io.github.barqallayl.burkan.designsystem.preview.BurkanPreviewWrapper
import io.github.barqallayl.burkan.feature.settings.model.SettingsError
import org.orbitmvi.orbit.compose.collectAsState
import org.orbitmvi.orbit.compose.collectSideEffect

@Composable
fun LicencesScreen() {
    val viewModel = metroViewModel<LicencesViewModel>()
    val state by viewModel.collectAsState()
    val navigator = LocalNavigator.current
    viewModel.collectSideEffect { effect ->
        when (effect) {
            LicencesSideEffect.Back -> navigator.pop()
        }
    }
    LicencesContent(state = state, onBack = viewModel::back)
}

@Composable
private fun LicencesContent(state: LicencesState, onBack: () -> Unit) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.settings_licences)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Outlined.ArrowBack, stringResource(R.string.navigate_back))
                    }
                },
            )
        },
    ) { innerPadding ->
        val libraries = state.libraries
        val error = state.error
        if (error != null) {
            BurkanMessage(
                icon = Tabler.Outline.AlertCircle,
                title = stringResource(R.string.licences_failed_title),
                text = stringResource(error.messageRes()),
                modifier = Modifier.padding(innerPadding),
            )
        } else if (libraries == null) {
            Box(Modifier.fillMaxSize().padding(innerPadding), contentAlignment = Alignment.Center) {
                LoadingIndicator()
            }
        } else if (libraries.libraries.isEmpty()) {
            BurkanMessage(
                icon = Tabler.Outline.License,
                title = stringResource(R.string.licences_empty_title),
                text = stringResource(R.string.licences_empty_text),
                modifier = Modifier.padding(innerPadding),
            )
        } else {
            var opened by remember { mutableStateOf<Library?>(null) }
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(horizontal = ScreenMargin),
                contentPadding = PaddingValues(
                    top = innerPadding.calculateTopPadding(),
                    bottom = innerPadding.calculateBottomPadding() + GroupGap,
                ),
                verticalArrangement = Arrangement.spacedBy(SegmentGap),
            ) {
                itemsIndexed(libraries.libraries, key = { _, library -> library.uniqueId }) { index, library ->
                    BurkanSegmentItem(
                        index = index,
                        count = libraries.libraries.size,
                        headline = library.name,
                        supporting = library.detail(),
                        onClick = { opened = library },
                    )
                }
            }
            opened?.let { library ->
                BurkanBottomSheet(onDismiss = { opened = null }) { LibrarySheet(library) }
            }
        }
    }
}

/** A library's version and the licences it is under, as one line. */
@Composable
private fun Library.detail(): String {
    val names = licenses.joinToString(separator = stringResource(R.string.list_separator)) { it.name }
        .ifEmpty { stringResource(R.string.licences_no_licence) }
    val version = artifactVersion ?: return names
    return stringResource(R.string.licences_item_detail, version, names)
}

/** What a library is, and the ways out to its website and to the text of its licence. */
@Composable
private fun LibrarySheet(library: Library) {
    val uriHandler = LocalUriHandler.current
    Column(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp).padding(bottom = 16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(library.name, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
        Text(
            library.detail(),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        library.description?.takeIf { it.isNotBlank() }?.let { Text(it, style = MaterialTheme.typography.bodyMedium) }
        val website = library.website?.takeIf { it.isNotBlank() }
        val licence = library.licenses.firstNotNullOfOrNull { it.url?.takeIf(String::isNotBlank) }
        if (website != null || licence != null) {
            Row(modifier = Modifier.padding(top = 8.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                if (website != null) {
                    OutlinedButton(onClick = { uriHandler.openUri(website) }, modifier = Modifier.weight(1f)) {
                        Text(stringResource(R.string.licences_open_website))
                    }
                }
                if (licence != null) {
                    Button(onClick = { uriHandler.openUri(licence) }, modifier = Modifier.weight(1f)) {
                        Text(stringResource(R.string.licences_open_licence))
                    }
                }
            }
        }
    }
}

private val apache = License(
    name = "The Apache Software License, Version 2.0",
    url = "https://www.apache.org/licenses/LICENSE-2.0.txt",
    hash = "Apache-2.0",
)

private val sampleLibraries = Libs(
    libraries = listOf(
        Library(
            uniqueId = "org.example:compose-widgets",
            artifactVersion = "2.1.0",
            name = "Compose Widgets",
            description = "Widgets for Jetpack Compose.",
            website = "https://example.org/compose-widgets",
            developers = emptyList(),
            organization = null,
            scm = null,
            licenses = setOf(apache),
        ),
        Library(
            uniqueId = "com.example:shell-client",
            artifactVersion = "3.0.4",
            name = "Shell Client",
            description = "A client for the shell protocol.",
            website = "https://example.com/shell-client",
            developers = emptyList(),
            organization = null,
            scm = null,
            licenses = setOf(apache),
        ),
    ),
    licenses = setOf(apache),
)

@PreviewWrapper(BurkanPreviewWrapper::class)
@BurkanPreview
@Composable
private fun LicencesPreview() = LicencesContent(LicencesState(sampleLibraries), onBack = {})

@PreviewWrapper(BurkanPreviewWrapper::class)
@BurkanPreview
@Composable
private fun LicencesLibrarySheetPreview() = LibrarySheet(sampleLibraries.libraries.first())

@PreviewWrapper(BurkanPreviewWrapper::class)
@BurkanPreview
@Composable
private fun LicencesLoadingPreview() = LicencesContent(LicencesState(), onBack = {})

@PreviewWrapper(BurkanPreviewWrapper::class)
@BurkanPreview
@Composable
private fun LicencesEmptyPreview() = LicencesContent(LicencesState(Libs(emptyList(), emptySet())), onBack = {})

@PreviewWrapper(BurkanPreviewWrapper::class)
@BurkanPreview
@Composable
private fun LicencesFailedPreview() =
    LicencesContent(LicencesState(error = SettingsError.LicencesUnreadable), onBack = {})
