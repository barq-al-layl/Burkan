package io.github.barqallayl.burkan.core.ui

import android.content.res.Resources
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalResources
import io.github.barqallayl.burkan.R
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import kotlin.time.Duration
import kotlin.time.Instant
import kotlin.time.toJavaInstant

/** A moment as this phone writes dates and times. [zone] is a parameter so previews can pin it. */
fun formatDateTime(instant: Instant, zone: ZoneId): String =
    DateTimeFormatter.ofLocalizedDateTime(FormatStyle.MEDIUM, FormatStyle.SHORT)
        .withZone(zone)
        .format(instant.toJavaInstant())

/** "45 s" or "1 min 13 s". */
fun Resources.durationText(duration: Duration): String {
    val seconds = duration.inWholeSeconds
    return if (seconds < 60) {
        getString(R.string.duration_seconds, seconds)
    } else {
        getString(R.string.duration_minutes_seconds, seconds / 60, seconds % 60)
    }
}

@Composable
fun durationText(duration: Duration): String = LocalResources.current.durationText(duration)
