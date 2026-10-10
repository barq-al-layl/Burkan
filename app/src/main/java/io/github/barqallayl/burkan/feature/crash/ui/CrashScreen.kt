package io.github.barqallayl.burkan.feature.crash.ui

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.PreviewWrapper
import androidx.compose.ui.unit.dp
import com.composables.icons.tabler.Tabler
import com.composables.icons.tabler.outline.Share
import com.composables.icons.tabler.outline.X
import io.github.barqallayl.burkan.R
import io.github.barqallayl.burkan.designsystem.AppStyle
import io.github.barqallayl.burkan.designsystem.MonoFontFamily
import io.github.barqallayl.burkan.designsystem.ThemeMode
import io.github.barqallayl.burkan.designsystem.component.ActionHeight
import io.github.barqallayl.burkan.designsystem.component.BurkanIconButton
import io.github.barqallayl.burkan.designsystem.component.BurkanSegment
import io.github.barqallayl.burkan.designsystem.component.BurkanTopBar
import io.github.barqallayl.burkan.designsystem.component.ContentMargin
import io.github.barqallayl.burkan.designsystem.component.GlassRise
import io.github.barqallayl.burkan.designsystem.component.GroupGap
import io.github.barqallayl.burkan.designsystem.component.ScreenMargin
import io.github.barqallayl.burkan.designsystem.component.burkanButtonShapes
import io.github.barqallayl.burkan.designsystem.component.burkanTonalButtonColors
import io.github.barqallayl.burkan.designsystem.component.glass
import io.github.barqallayl.burkan.designsystem.component.glassSource
import io.github.barqallayl.burkan.designsystem.component.readableWidth
import io.github.barqallayl.burkan.designsystem.component.listInset
import io.github.barqallayl.burkan.designsystem.component.listTop
import io.github.barqallayl.burkan.designsystem.component.oneUiScrollFade
import io.github.barqallayl.burkan.designsystem.component.rememberBurkanAppBar
import io.github.barqallayl.burkan.designsystem.component.topBarScroll
import io.github.barqallayl.burkan.designsystem.preview.BurkanPreview
import io.github.barqallayl.burkan.designsystem.preview.BurkanPreviewTheme
import io.github.barqallayl.burkan.designsystem.preview.BurkanPreviewWrapper

/**
 * What the app shows in place of itself after a crash: that it stopped, the report, and two things to do with it.
 * [report] is null when it could not be read, and then there is nothing to share.
 */
@Composable
fun CrashScreen(report: String?, onShare: () -> Unit, onReport: () -> Unit, onClose: () -> Unit) {
    val appBar = rememberBurkanAppBar()
    val scrollState = rememberScrollState()
    Scaffold(
        modifier = Modifier.topBarScroll(appBar),
        topBar = {
            BurkanTopBar(
                title = { Text(stringResource(R.string.crash_title)) },
                appBar = appBar,
                contentScroll = { scrollState.value },
                actions = {
                    BurkanIconButton(Tabler.Outline.X, stringResource(R.string.crash_close), onClose)
                },
            )
        },
        bottomBar = {
            // Frosted in the Material style: the report scrolls on under the buttons.
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .glass(appBar, risingOver = GlassRise)
                    .navigationBarsPadding()
                    .padding(horizontal = ContentMargin)
                    .padding(top = GlassRise, bottom = GroupGap)
                    .readableWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                val label = ButtonDefaults.textStyleFor(ActionHeight)
                Button(
                    onClick = onShare,
                    shapes = burkanButtonShapes(ActionHeight),
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = ActionHeight),
                    enabled = report != null,
                ) {
                    Icon(
                        Tabler.Outline.Share,
                        contentDescription = null,
                        modifier = Modifier.size(ButtonDefaults.iconSizeFor(ActionHeight)),
                    )
                    Spacer(Modifier.width(ButtonDefaults.iconSpacingFor(ActionHeight)))
                    Text(stringResource(R.string.crash_share), style = label)
                }
                FilledTonalButton(
                    onClick = onReport,
                    shapes = burkanButtonShapes(ActionHeight),
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = ActionHeight),
                    colors = burkanTonalButtonColors(),
                ) {
                    Text(stringResource(R.string.settings_report_problem), style = label)
                }
            }
        },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .glassSource(appBar)
                .padding(top = innerPadding.listTop())
                .oneUiScrollFade(scrollState)
                .verticalScroll(scrollState)
                .padding(top = innerPadding.listInset())
                .padding(horizontal = ScreenMargin)
                .padding(bottom = innerPadding.calculateBottomPadding() + GroupGap)
                .readableWidth(),
            verticalArrangement = Arrangement.spacedBy(GroupGap),
        ) {
            Text(
                stringResource(R.string.crash_text),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = ContentMargin - ScreenMargin + 8.dp),
            )
            BurkanSegment(index = 0, count = 1) {
                // A trace is read line by line: a long line runs on sideways and is not folded into the next.
                SelectionContainer {
                    Text(
                        report ?: stringResource(R.string.crash_no_report),
                        style = MaterialTheme.typography.bodySmall,
                        fontFamily = MonoFontFamily,
                        softWrap = false,
                        modifier = Modifier
                            .horizontalScroll(rememberScrollState())
                            .padding(16.dp),
                    )
                }
            }
        }
    }
}

private val sampleReport = """
    Burkan 1.0.0 (1) crash report
    Device: samsung SM-S911B, Android 16 (SDK 36)
    Time: 2026-10-01 09:30:00 +00:00
    Thread: main

    java.lang.IllegalStateException: The run ended in a state it cannot be in
        at io.github.barqallayl.burkan.feature.apply.data.ApplyRunner.run(ApplyRunner.kt:88)
        at io.github.barqallayl.burkan.feature.apply.data.ApplyController.start(ApplyController.kt:131)
        at kotlinx.coroutines.DispatchedTask.run(DispatchedTask.kt:100)
        at android.os.Handler.handleCallback(Handler.java:995)
        at android.os.Looper.loop(Looper.java:288)
    Caused by: java.io.IOException: Stream closed
        at io.github.barqallayl.burkan.core.shell.AdbShellExecutor.run(AdbShellExecutor.kt:61)
        ... 4 more
""".trimIndent()

@PreviewWrapper(BurkanPreviewWrapper::class)
@BurkanPreview
@Composable
private fun CrashPreview() = CrashScreen(sampleReport, {}, {}, {})

@PreviewWrapper(BurkanPreviewWrapper::class)
@BurkanPreview
@Composable
private fun CrashNoReportPreview() = CrashScreen(null, {}, {}, {})

@BurkanPreview
@Composable
private fun CrashDarkPreview() {
    BurkanPreviewTheme(themeMode = ThemeMode.Dark) { CrashScreen(sampleReport, {}, {}, {}) }
}

@BurkanPreview
@Composable
private fun CrashOneUiPreview() {
    BurkanPreviewTheme(appStyle = AppStyle.OneUi) { CrashScreen(sampleReport, {}, {}, {}) }
}
