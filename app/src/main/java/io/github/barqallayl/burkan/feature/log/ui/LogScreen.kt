package io.github.barqallayl.burkan.feature.log.ui

import android.content.Intent
import android.content.res.Resources
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.PreviewWrapper
import androidx.compose.ui.unit.dp
import com.composables.icons.tabler.Tabler
import com.composables.icons.tabler.outline.AlertCircle
import com.composables.icons.tabler.outline.CircleCheck
import com.composables.icons.tabler.outline.Share
import dev.zacsweers.metrox.viewmodel.metroViewModel
import io.github.barqallayl.burkan.R
import io.github.barqallayl.burkan.core.model.AppErrorType
import io.github.barqallayl.burkan.core.navigation.LocalNavigator
import io.github.barqallayl.burkan.core.ui.durationText
import io.github.barqallayl.burkan.core.ui.formatDateTime
import io.github.barqallayl.burkan.designsystem.preview.BurkanPreview
import io.github.barqallayl.burkan.designsystem.preview.BurkanPreviewWrapper
import io.github.barqallayl.burkan.feature.apply.model.ApplyKind
import io.github.barqallayl.burkan.feature.apply.model.RestoredSetting
import io.github.barqallayl.burkan.feature.apply.model.RunTrigger
import io.github.barqallayl.burkan.feature.apply.model.StepKind
import io.github.barqallayl.burkan.feature.apply.ui.label
import io.github.barqallayl.burkan.feature.apply.ui.text
import io.github.barqallayl.burkan.feature.log.model.LoggedStep
import io.github.barqallayl.burkan.feature.log.model.RunLogEntry
import io.github.barqallayl.burkan.feature.log.model.RunResult
import org.orbitmvi.orbit.compose.collectAsState
import org.orbitmvi.orbit.compose.collectSideEffect
import java.time.ZoneId
import java.time.ZoneOffset
import kotlin.time.Duration.Companion.seconds
import kotlin.time.Instant

@Composable
fun LogScreen() {
    val viewModel = metroViewModel<LogViewModel>()
    val state by viewModel.collectAsState()
    val navigator = LocalNavigator.current
    val context = LocalContext.current
    val resources = LocalResources.current
    val zone = ZoneId.systemDefault()
    viewModel.collectSideEffect { effect ->
        when (effect) {
            LogSideEffect.Back -> navigator.pop()
            is LogSideEffect.Share -> {
                val send = Intent(Intent.ACTION_SEND)
                    .setType("text/plain")
                    .putExtra(Intent.EXTRA_SUBJECT, resources.getString(R.string.log_share_subject))
                    .putExtra(Intent.EXTRA_TEXT, resources.exportText(effect.runs, zone))
                context.startActivity(Intent.createChooser(send, resources.getString(R.string.log_share_chooser)))
            }
        }
    }
    LogContent(
        state = state,
        zone = zone,
        onToggle = viewModel::toggle,
        onShare = viewModel::share,
        onBack = viewModel::back,
    )
}

/** The log as plain text. It holds what the screen shows: no key, no package names. */
private fun Resources.exportText(runs: List<RunLogEntry>, zone: ZoneId): String = buildString {
    runs.forEach { run ->
        appendLine(
            getString(
                R.string.log_run_detail,
                formatDateTime(run.startedAt, zone),
                getString(run.trigger.label),
                durationText(run.duration),
            ),
        )
        appendLine(getString(R.string.log_run_title, getString(run.kind.label), getString(run.result.label)))
        run.error?.let { appendLine(getString(it.resource)) }
        run.steps.forEach { step ->
            val outcome = step.error?.let { " — ${getString(it.resource)}" } ?: ""
            appendLine("  ${text(step.kind.label())}$outcome")
        }
        appendLine()
    }
}

@Composable
private fun LogContent(
    state: LogState,
    zone: ZoneId,
    onToggle: (RunLogEntry) -> Unit,
    onShare: () -> Unit,
    onBack: () -> Unit,
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.log_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Outlined.ArrowBack, stringResource(R.string.navigate_back))
                    }
                },
                actions = {
                    if (!state.runs.isNullOrEmpty()) {
                        IconButton(onClick = onShare) {
                            Icon(Tabler.Outline.Share, contentDescription = stringResource(R.string.log_share))
                        }
                    }
                },
            )
        },
    ) { innerPadding ->
        val runs = state.runs
        when {
            runs == null -> Unit
            runs.isEmpty() -> Text(
                stringResource(R.string.log_empty),
                modifier = Modifier.padding(innerPadding).padding(16.dp),
            )
            else -> LazyColumn(modifier = Modifier.fillMaxSize(), contentPadding = innerPadding) {
                items(runs, key = { it.startedAt.toEpochMilliseconds() }) { run ->
                    RunItem(run, expanded = run.startedAt in state.expanded, zone = zone, onClick = { onToggle(run) })
                }
            }
        }
    }
}

@Composable
private fun RunItem(run: RunLogEntry, expanded: Boolean, zone: ZoneId, onClick: () -> Unit) {
    ListItem(
        modifier = Modifier.clickable(onClick = onClick),
        leadingContent = { ResultIcon(failed = run.result == RunResult.Failed) },
        supportingContent = {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    stringResource(
                        R.string.log_run_detail,
                        formatDateTime(run.startedAt, zone),
                        stringResource(run.trigger.label),
                        durationText(run.duration),
                    ),
                )
                if (expanded) RunSteps(run)
            }
        },
    ) {
        Text(stringResource(R.string.log_run_title, stringResource(run.kind.label), stringResource(run.result.label)))
    }
}

@Composable
private fun RunSteps(run: RunLogEntry) {
    Column(modifier = Modifier.padding(top = 8.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        run.error?.let { Text(stringResource(it.resource), color = MaterialTheme.colorScheme.error) }
        run.steps.forEach { step ->
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.Top) {
                ResultIcon(failed = step.error != null)
                Column {
                    Text(step.kind.label().text())
                    step.error?.let { Text(stringResource(it.resource), color = MaterialTheme.colorScheme.error) }
                }
            }
        }
    }
}

@Composable
private fun ResultIcon(failed: Boolean) {
    // The words carry the result too: never colour alone.
    if (failed) {
        Icon(
            Tabler.Outline.AlertCircle,
            contentDescription = stringResource(R.string.result_failed),
            tint = MaterialTheme.colorScheme.error,
        )
    } else {
        Icon(Tabler.Outline.CircleCheck, contentDescription = null)
    }
}

private val sampleRuns = listOf(
    RunLogEntry(
        startedAt = Instant.parse("2026-10-01T09:30:00Z"),
        trigger = RunTrigger.Manual,
        kind = ApplyKind.Full,
        result = RunResult.Failed,
        duration = 73.seconds,
        steps = listOf(
            LoggedStep(StepKind.SetRenderer),
            LoggedStep(StepKind.StopApps(612)),
            LoggedStep(StepKind.RestartSystemUi),
            LoggedStep(StepKind.RestartLauncher),
            LoggedStep(StepKind.RelaunchApps(110), AppErrorType.CommandTimedOut),
            LoggedStep(StepKind.RestoreSetting(RestoredSetting.AutoRotation)),
            LoggedStep(StepKind.RestoreSetting(RestoredSetting.AccessibilityServices)),
        ),
    ),
    RunLogEntry(
        startedAt = Instant.parse("2026-10-01T08:02:00Z"),
        trigger = RunTrigger.Boot,
        kind = ApplyKind.Light,
        result = RunResult.Failed,
        duration = 4.seconds,
        steps = emptyList(),
        error = AppErrorType.NoWifi,
    ),
    RunLogEntry(
        startedAt = Instant.parse("2026-09-30T22:15:00Z"),
        trigger = RunTrigger.Manual,
        kind = ApplyKind.Light,
        result = RunResult.Succeeded,
        duration = 6.seconds,
        steps = listOf(
            LoggedStep(StepKind.SetRenderer),
            LoggedStep(StepKind.RestartSystemUi),
            LoggedStep(StepKind.RestartLauncher),
            LoggedStep(StepKind.RestartKeyboard),
        ),
    ),
)

@PreviewWrapper(BurkanPreviewWrapper::class)
@BurkanPreview
@Composable
private fun LogPreview() {
    LogContent(LogState(sampleRuns), ZoneOffset.UTC, onToggle = {}, onShare = {}, onBack = {})
}

@PreviewWrapper(BurkanPreviewWrapper::class)
@BurkanPreview
@Composable
private fun LogExpandedPreview() {
    val state = LogState(sampleRuns, expanded = setOf(sampleRuns[0].startedAt, sampleRuns[1].startedAt))
    LogContent(state, ZoneOffset.UTC, onToggle = {}, onShare = {}, onBack = {})
}

@PreviewWrapper(BurkanPreviewWrapper::class)
@BurkanPreview
@Composable
private fun LogEmptyPreview() {
    LogContent(LogState(emptyList()), ZoneOffset.UTC, onToggle = {}, onShare = {}, onBack = {})
}
