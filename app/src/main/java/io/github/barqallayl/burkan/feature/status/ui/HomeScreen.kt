package io.github.barqallayl.burkan.feature.status.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.PreviewWrapper
import io.github.barqallayl.burkan.R
import io.github.barqallayl.burkan.core.ui.PlaceholderContent
import io.github.barqallayl.burkan.designsystem.preview.BurkanPreview
import io.github.barqallayl.burkan.designsystem.preview.BurkanPreviewWrapper

@Composable
fun HomeScreen() {
    HomeContent()
}

@Composable
private fun HomeContent() {
    PlaceholderContent(title = stringResource(R.string.app_name))
}

@PreviewWrapper(BurkanPreviewWrapper::class)
@BurkanPreview
@Composable
private fun HomeContentPreview() {
    HomeContent()
}
