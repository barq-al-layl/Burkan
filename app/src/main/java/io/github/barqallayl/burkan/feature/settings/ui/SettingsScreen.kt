package io.github.barqallayl.burkan.feature.settings.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.PreviewWrapper
import io.github.barqallayl.burkan.R
import io.github.barqallayl.burkan.core.ui.PlaceholderContent
import io.github.barqallayl.burkan.designsystem.preview.BurkanPreview
import io.github.barqallayl.burkan.designsystem.preview.BurkanPreviewWrapper

@Composable
fun SettingsScreen() {
    SettingsContent()
}

@Composable
private fun SettingsContent() {
    PlaceholderContent(title = stringResource(R.string.settings_title))
}

@PreviewWrapper(BurkanPreviewWrapper::class)
@BurkanPreview
@Composable
private fun SettingsContentPreview() {
    SettingsContent()
}
