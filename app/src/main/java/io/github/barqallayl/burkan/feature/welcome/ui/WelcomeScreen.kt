package io.github.barqallayl.burkan.feature.welcome.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.LifecycleResumeEffect
import com.composables.icons.tabler.Tabler
import com.composables.icons.tabler.outline.Clock
import com.composables.icons.tabler.outline.Settings
import com.composables.icons.tabler.outline.WifiOff
import io.github.barqallayl.burkan.R
import io.github.barqallayl.burkan.core.navigation.ConnectionRoute
import io.github.barqallayl.burkan.core.navigation.LocalNavigator
import io.github.barqallayl.burkan.core.navigation.SetupRoute
import dev.zacsweers.metrox.viewmodel.metroViewModel
import io.github.barqallayl.burkan.designsystem.AppStyle
import io.github.barqallayl.burkan.designsystem.ThemeMode
import io.github.barqallayl.burkan.designsystem.burkanMotion
import io.github.barqallayl.burkan.designsystem.component.ActionHeight
import io.github.barqallayl.burkan.designsystem.component.GroupGap
import io.github.barqallayl.burkan.designsystem.component.ScreenMargin
import io.github.barqallayl.burkan.designsystem.component.burkanButtonShapes
import io.github.barqallayl.burkan.designsystem.component.readableWidth
import io.github.barqallayl.burkan.designsystem.emphasis
import io.github.barqallayl.burkan.designsystem.preview.BurkanPreview
import io.github.barqallayl.burkan.designsystem.preview.BurkanPreviewTheme
import io.github.barqallayl.burkan.designsystem.preview.BurkanPreviewWrapper
import io.github.barqallayl.burkan.designsystem.preview.LargeFontScale
import androidx.compose.ui.tooling.preview.PreviewWrapper
import org.orbitmvi.orbit.compose.collectAsState
import org.orbitmvi.orbit.compose.collectSideEffect

/**
 * The screen a fresh install opens on, once: what Burkan does, what setup asks of the owner, and the way in. The phone
 * redrawing above it is what Burkan does, shown rather than told.
 */
@Composable
fun WelcomeScreen() {
    val viewModel = metroViewModel<WelcomeViewModel>()
    val state by viewModel.collectAsState()
    val navigator = LocalNavigator.current
    LifecycleResumeEffect(viewModel) {
        viewModel.refresh()
        onPauseOrDispose { }
    }
    viewModel.collectSideEffect { effect ->
        when (effect) {
            WelcomeSideEffect.ChooseConnection -> navigator.push(ConnectionRoute)
            WelcomeSideEffect.StartSetup -> navigator.push(SetupRoute)
        }
    }
    WelcomeContent(state = state, onGetStarted = viewModel::getStarted)
}

@Composable
private fun WelcomeContent(state: WelcomeState, onGetStarted: () -> Unit) {
    val motion = burkanMotion
    Scaffold(
        // The one way on, at the bottom, in reach of the thumb, where it stays whatever the text above does.
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
                    onClick = onGetStarted,
                    shapes = burkanButtonShapes(ActionHeight),
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = ActionHeight),
                ) {
                    Text(stringResource(R.string.welcome_start), style = ButtonDefaults.textStyleFor(ActionHeight))
                }
            }
        },
    ) { innerPadding ->
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = innerPadding.calculateBottomPadding()),
        ) {
            // The phone takes what the text leaves it, and a little less on a short screen or at a large text size,
            // where the text is what has to fit.
            val phoneHeight = (maxHeight * 0.46f).coerceIn(160.dp, 380.dp)
            val screenHeight = maxHeight
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState()),
            ) {
                // As tall as the screen at least, with what it holds in the middle of it: the space left over goes
                // above and below, not all under the text.
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = screenHeight)
                        .statusBarsPadding()
                        .padding(horizontal = ScreenMargin)
                        .padding(top = 24.dp, bottom = GroupGap)
                        .readableWidth(),
                    verticalArrangement = Arrangement.Center,
                ) {
                    PhoneRedraw(
                        description = stringResource(R.string.welcome_illustration),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(phoneHeight),
                    )
                    Column(
                        modifier = Modifier.padding(top = 40.dp, start = 8.dp, end = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        Text(
                            stringResource(R.string.welcome_title),
                            style = emphasis(
                                MaterialTheme.typography.displaySmall,
                                MaterialTheme.typography.displaySmallEmphasized,
                            ),
                            modifier = Modifier.semantics { heading() },
                        )
                        Text(
                            stringResource(R.string.welcome_text),
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    Column(
                        modifier = Modifier.padding(top = 28.dp, start = 8.dp, end = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp),
                    ) {
                        Fact(Tabler.Outline.Clock, stringResource(R.string.welcome_time))
                        Fact(Tabler.Outline.Settings, stringResource(R.string.welcome_developer_options))
                        // Said here, not as a wall: a Shizuku user may need no Wi-Fi at all.
                        AnimatedVisibility(
                            visible = !state.isOnWifi,
                            enter = expandVertically(
                                motion.move(),
                                expandFrom = Alignment.Top,
                            ) + fadeIn(motion.appear()),
                            exit = shrinkVertically(
                                motion.move(),
                                shrinkTowards = Alignment.Top,
                            ) + fadeOut(motion.vanish()),
                        ) {
                            Fact(
                                Tabler.Outline.WifiOff,
                                stringResource(R.string.welcome_no_wifi),
                                emphasized = true,
                            )
                        }
                    }
                }
            }
        }
    }
}

/** One thing setup asks of the owner, with an icon beside it in the accent. */
@Composable
private fun Fact(icon: ImageVector, text: String, emphasized: Boolean = false) {
    Row(horizontalArrangement = Arrangement.spacedBy(14.dp), verticalAlignment = Alignment.CenterVertically) {
        Icon(
            icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(24.dp),
        )
        Text(
            text,
            style = if (emphasized) {
                emphasis(MaterialTheme.typography.bodyLarge, MaterialTheme.typography.bodyLargeEmphasized)
            } else {
                MaterialTheme.typography.bodyLarge
            },
        )
    }
}

@PreviewWrapper(BurkanPreviewWrapper::class)
@BurkanPreview
@Composable
private fun WelcomePreview() = WelcomeContent(WelcomeState(), onGetStarted = {})

@PreviewWrapper(BurkanPreviewWrapper::class)
@BurkanPreview
@Composable
private fun WelcomeNoWifiPreview() = WelcomeContent(WelcomeState(isOnWifi = false), onGetStarted = {})

@BurkanPreview
@Composable
private fun WelcomeDarkPreview() {
    BurkanPreviewTheme(themeMode = ThemeMode.Dark) { WelcomeContent(WelcomeState(), onGetStarted = {}) }
}

@BurkanPreview
@Composable
private fun WelcomeOneUiPreview() {
    BurkanPreviewTheme(appStyle = AppStyle.OneUi) { WelcomeContent(WelcomeState(), onGetStarted = {}) }
}

@BurkanPreview
@Composable
private fun WelcomeOneUiDarkPreview() {
    BurkanPreviewTheme(themeMode = ThemeMode.Dark, appStyle = AppStyle.OneUi) {
        WelcomeContent(WelcomeState(), onGetStarted = {})
    }
}

@BurkanPreview
@Composable
private fun WelcomeLargeTextPreview() {
    BurkanPreviewTheme(fontScale = LargeFontScale) { WelcomeContent(WelcomeState(isOnWifi = false), onGetStarted = {}) }
}
