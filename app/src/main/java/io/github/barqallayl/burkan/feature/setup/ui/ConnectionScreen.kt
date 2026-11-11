package io.github.barqallayl.burkan.feature.setup.ui

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.PreviewWrapper
import androidx.compose.ui.unit.dp
import dev.zacsweers.metrox.viewmodel.metroViewModel
import io.github.barqallayl.burkan.R
import io.github.barqallayl.burkan.core.model.ShellBackend
import io.github.barqallayl.burkan.core.navigation.LocalNavigator
import io.github.barqallayl.burkan.core.navigation.SetupRoute
import io.github.barqallayl.burkan.designsystem.AppStyle
import io.github.barqallayl.burkan.designsystem.ThemeMode
import io.github.barqallayl.burkan.designsystem.component.ActionHeight
import io.github.barqallayl.burkan.designsystem.component.BurkanSegmentChoice
import io.github.barqallayl.burkan.designsystem.component.BurkanTopBar
import io.github.barqallayl.burkan.designsystem.component.GroupGap
import io.github.barqallayl.burkan.designsystem.component.ScreenMargin
import io.github.barqallayl.burkan.designsystem.component.SegmentedColumn
import io.github.barqallayl.burkan.designsystem.component.burkanButtonShapes
import io.github.barqallayl.burkan.designsystem.component.glassSource
import io.github.barqallayl.burkan.designsystem.component.listInset
import io.github.barqallayl.burkan.designsystem.component.listTop
import io.github.barqallayl.burkan.designsystem.component.readableWidth
import io.github.barqallayl.burkan.designsystem.component.rememberBurkanAppBar
import io.github.barqallayl.burkan.designsystem.component.topBarScroll
import io.github.barqallayl.burkan.designsystem.preview.BurkanPreview
import io.github.barqallayl.burkan.designsystem.preview.BurkanPreviewTheme
import io.github.barqallayl.burkan.designsystem.preview.BurkanPreviewWrapper
import org.orbitmvi.orbit.compose.collectAsState
import org.orbitmvi.orbit.compose.collectSideEffect

/**
 * The choice of connection, offered only to a phone that has Shizuku's app. Wireless debugging is chosen to begin
 * with and marked as recommended; Shizuku's row says what it asks of the owner after every restart.
 */
@Composable
fun ConnectionScreen() {
    val viewModel = metroViewModel<ConnectionViewModel>()
    val state by viewModel.collectAsState()
    val navigator = LocalNavigator.current
    viewModel.collectSideEffect { effect ->
        when (effect) {
            ConnectionSideEffect.StartSetup -> navigator.push(SetupRoute)
        }
    }
    ConnectionContent(
        state = state,
        onBack = navigator::pop,
        onSelect = viewModel::select,
        onContinue = viewModel::confirm,
    )
}

@Composable
private fun ConnectionContent(
    state: ConnectionState,
    onBack: () -> Unit,
    onSelect: (ShellBackend) -> Unit,
    onContinue: () -> Unit,
) {
    val appBar = rememberBurkanAppBar()
    val scrollState = rememberScrollState()
    Scaffold(
        modifier = Modifier.topBarScroll(appBar),
        topBar = {
            BurkanTopBar(
                title = { Text(stringResource(R.string.connection_title)) },
                onBack = onBack,
                appBar = appBar,
                contentScroll = { scrollState.value },
            )
        },
        bottomBar = {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(horizontal = ScreenMargin)
                    .padding(top = 12.dp, bottom = GroupGap)
                    .readableWidth(),
            ) {
                Button(
                    onClick = onContinue,
                    shapes = burkanButtonShapes(ActionHeight),
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = ActionHeight),
                ) {
                    Text(
                        stringResource(R.string.connection_continue),
                        style = ButtonDefaults.textStyleFor(ActionHeight),
                    )
                }
            }
        },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .glassSource(appBar)
                .padding(top = innerPadding.listTop(), bottom = innerPadding.calculateBottomPadding())
                .verticalScroll(scrollState)
                .padding(top = innerPadding.listInset())
                .padding(horizontal = ScreenMargin)
                .padding(bottom = GroupGap)
                .readableWidth(),
            verticalArrangement = Arrangement.spacedBy(GroupGap),
        ) {
            Text(
                stringResource(R.string.connection_text),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 8.dp),
            )
            SegmentedColumn {
                BurkanSegmentChoice(
                    index = 0,
                    count = 2,
                    text = stringResource(R.string.settings_connection_wireless_debugging),
                    supporting = stringResource(R.string.connection_wireless_debugging_text),
                    selected = state.selected == ShellBackend.WirelessDebugging,
                    onClick = { onSelect(ShellBackend.WirelessDebugging) },
                ) {
                    RecommendedLabel(modifier = Modifier.padding(top = 8.dp))
                }
                BurkanSegmentChoice(
                    index = 1,
                    count = 2,
                    text = stringResource(R.string.settings_connection_shizuku),
                    supporting = stringResource(R.string.connection_shizuku_text),
                    selected = state.selected == ShellBackend.Shizuku,
                    onClick = { onSelect(ShellBackend.Shizuku) },
                )
            }
        }
    }
}

/**
 * "Recommended", outlined in the row's own text colour. A filled pill would vanish into Material's tinted row when the
 * option is chosen, which is exactly when it is read.
 */
@Composable
private fun RecommendedLabel(modifier: Modifier = Modifier) {
    val colour = LocalContentColor.current
    Text(
        stringResource(R.string.connection_recommended),
        style = MaterialTheme.typography.labelMedium,
        color = colour,
        modifier = modifier
            .border(1.dp, colour.copy(alpha = 0.6f), CircleShape)
            .padding(horizontal = 10.dp, vertical = 3.dp),
    )
}

@PreviewWrapper(BurkanPreviewWrapper::class)
@BurkanPreview
@Composable
private fun ConnectionPreview() = ConnectionContent(ConnectionState(), onBack = {}, onSelect = {}, onContinue = {})

@BurkanPreview
@Composable
private fun ConnectionShizukuOneUiDarkPreview() {
    BurkanPreviewTheme(themeMode = ThemeMode.Dark, appStyle = AppStyle.OneUi) {
        ConnectionContent(ConnectionState(ShellBackend.Shizuku), onBack = {}, onSelect = {}, onContinue = {})
    }
}
