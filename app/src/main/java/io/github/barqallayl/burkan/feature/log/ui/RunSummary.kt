package io.github.barqallayl.burkan.feature.log.ui

import io.github.barqallayl.burkan.core.model.AppErrorType
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import io.github.barqallayl.burkan.core.ui.formatTime
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.composables.icons.tabler.Tabler
import com.composables.icons.tabler.outline.Ban
import com.composables.icons.tabler.outline.Check
import com.composables.icons.tabler.outline.Clock
import com.composables.icons.tabler.outline.X
import io.github.barqallayl.burkan.R
import io.github.barqallayl.burkan.core.ui.LocalCurrentYear
import io.github.barqallayl.burkan.core.ui.durationText
import io.github.barqallayl.burkan.core.ui.formatDateTime
import io.github.barqallayl.burkan.designsystem.component.BurkanIconBadge
import io.github.barqallayl.burkan.designsystem.component.BurkanPill
import io.github.barqallayl.burkan.designsystem.component.Tone
import io.github.barqallayl.burkan.feature.log.model.RunLogEntry
import io.github.barqallayl.burkan.feature.log.model.RunResult
import java.time.ZoneId

/** How a result is toned wherever it shows: fine, held up, failed, or neither. */
val RunResult.tone: Tone
    get() = when (this) {
        RunResult.Succeeded, RunResult.AlreadyApplied -> Tone.Good
        RunResult.Postponed -> Tone.HeldUp
        RunResult.Failed -> Tone.Bad
        RunResult.Cancelled -> Tone.Neutral
    }

val RunResult.icon: ImageVector
    get() = when (this) {
        RunResult.Succeeded, RunResult.AlreadyApplied -> Tabler.Outline.Check
        RunResult.Postponed -> Tabler.Outline.Clock
        RunResult.Failed -> Tabler.Outline.X
        RunResult.Cancelled -> Tabler.Outline.Ban
    }

/**
 * A run's result as the round badge that leads its row. A result that wants attention is said in words too, in a pill
 * beside the run's name; one that went as it should is said by the badge alone.
 */
@Composable
fun RunResultBadge(result: RunResult, modifier: Modifier = Modifier) {
    BurkanIconBadge(result.icon, modifier = modifier, tone = result.tone)
}

/** Whether a result is one to look at: it failed, was stopped, or was left for later. */
val RunResult.wantsAttention: Boolean
    get() = when (this) {
        RunResult.Failed, RunResult.Cancelled, RunResult.Postponed -> true
        RunResult.Succeeded, RunResult.AlreadyApplied -> false
    }

/**
 * Why a run failed: its own error, or the error of the step that failed. Null for a run that did not fail. The log's
 * form of it says what happened, without an instruction to try again: the run is over.
 */
val RunLogEntry.failureReason: AppErrorType?
    get() = if (result != RunResult.Failed) null else error ?: steps.firstNotNullOfOrNull { it.error }

/**
 * A run in two lines, as Home and the Log both show it: what ran, with its result in a pill when it wants attention,
 * then when, why and for how long. The pill moves under the name when the two do not fit side by side. With
 * [timeOnly] the second line gives the time without the date, for a list that is headed by its days. The result is
 * said once to TalkBack, in the pill or, without one, with the run's name.
 */
@Composable
fun RunSummary(run: RunLogEntry, zone: ZoneId, modifier: Modifier = Modifier, timeOnly: Boolean = false) {
    val result = stringResource(run.resultLabel)
    val pill = run.result.wantsAttention
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(4.dp)) {
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
            itemVerticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                stringResource(run.kind.label),
                style = MaterialTheme.typography.titleMedium,
                modifier = if (pill) Modifier else Modifier.semantics { stateDescription = result },
            )
            if (pill) BurkanPill(result, tone = run.result.tone)
        }
        Text(
            stringResource(
                R.string.log_run_detail,
                if (timeOnly) formatTime(run.startedAt, zone) else formatDateTime(
                    run.startedAt,
                    zone,
                    LocalCurrentYear.current,
                ),
                stringResource(run.trigger.label),
                durationText(run.duration),
            ),
            style = MaterialTheme.typography.bodyMedium,
            color = LocalContentColor.current.copy(alpha = DETAIL_ALPHA),
        )
    }
}

private const val DETAIL_ALPHA = 0.74f
