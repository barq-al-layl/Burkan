package io.github.barqallayl.burkan.feature.settings.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.PreviewWrapper
import com.composables.icons.tabler.Tabler
import com.composables.icons.tabler.outline.AlertCircle
import com.composables.icons.tabler.outline.License
import com.mikepenz.aboutlibraries.Libs
import com.mikepenz.aboutlibraries.entity.Library
import com.mikepenz.aboutlibraries.entity.License
import com.mikepenz.aboutlibraries.ui.compose.LibraryDefaults
import com.mikepenz.aboutlibraries.ui.compose.m3.LibrariesContainer
import com.mikepenz.aboutlibraries.ui.compose.m3.style.m3VariantColors
import com.mikepenz.aboutlibraries.ui.compose.style.LicenseHueResolver
import dev.zacsweers.metrox.viewmodel.metroViewModel
import io.github.barqallayl.burkan.R
import io.github.barqallayl.burkan.core.model.messageRes
import io.github.barqallayl.burkan.core.navigation.LocalNavigator
import io.github.barqallayl.burkan.designsystem.component.BurkanMessage
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
            // Each library opens its licence in a dialog of the library's own. Its licence chips take the theme's
            // colours rather than a colour per licence: the app's colours come from the scheme alone.
            LibrariesContainer(
                libraries,
                Modifier.fillMaxSize(),
                contentPadding = innerPadding,
                variantColors = LibraryDefaults.m3VariantColors(
                    licenseHueResolver = LicenseHueResolver.None,
                    licenseBadgeContainer = MaterialTheme.colorScheme.secondaryContainer,
                    licenseBadgeContent = MaterialTheme.colorScheme.onSecondaryContainer,
                ),
            )
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
