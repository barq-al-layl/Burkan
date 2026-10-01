package io.github.barqallayl.burkan.feature.log.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.PreviewWrapper
import io.github.barqallayl.burkan.R
import io.github.barqallayl.burkan.core.ui.PlaceholderContent
import io.github.barqallayl.burkan.designsystem.preview.BurkanPreview
import io.github.barqallayl.burkan.designsystem.preview.BurkanPreviewWrapper

@Composable
fun LogScreen() {
    LogContent()
}

@Composable
private fun LogContent() {
    PlaceholderContent(title = stringResource(R.string.log_title))
}

@PreviewWrapper(BurkanPreviewWrapper::class)
@BurkanPreview
@Composable
private fun LogContentPreview() {
    LogContent()
}
