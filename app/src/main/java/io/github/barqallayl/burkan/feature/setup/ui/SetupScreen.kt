package io.github.barqallayl.burkan.feature.setup.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.PreviewWrapper
import io.github.barqallayl.burkan.R
import io.github.barqallayl.burkan.core.ui.PlaceholderContent
import io.github.barqallayl.burkan.designsystem.SeedColors
import io.github.barqallayl.burkan.designsystem.TextScale
import io.github.barqallayl.burkan.designsystem.ThemeMode
import io.github.barqallayl.burkan.designsystem.preview.BurkanPreview
import io.github.barqallayl.burkan.designsystem.preview.BurkanPreviewTheme
import io.github.barqallayl.burkan.designsystem.preview.BurkanPreviewWrapper

@Composable
fun SetupScreen() {
    SetupContent()
}

@Composable
private fun SetupContent() {
    PlaceholderContent(title = stringResource(R.string.setup_title))
}

@PreviewWrapper(BurkanPreviewWrapper::class)
@BurkanPreview
@Composable
private fun SetupContentPreview() {
    SetupContent()
}

@BurkanPreview
@Composable
private fun SetupContentDarkPreview() {
    BurkanPreviewTheme(themeMode = ThemeMode.Dark) {
        SetupContent()
    }
}

@BurkanPreview
@Composable
private fun SetupContentLargeTextGreenSeedPreview() {
    BurkanPreviewTheme(textScalePercent = TextScale.percentages.last, seedColor = SeedColors.Green) {
        SetupContent()
    }
}
