package io.github.barqallayl.burkan.core.ui

import android.content.res.Resources
import android.text.format.DateFormat
import androidx.compose.runtime.Composable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.platform.LocalResources
import io.github.barqallayl.burkan.R
import java.time.Year
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.Locale
import kotlin.time.Duration
import kotlin.time.Instant
import kotlin.time.toJavaInstant

/** The year it is now. Previews pin it, so a screenshot does not change on New Year's Day. */
val LocalCurrentYear = staticCompositionLocalOf { Year.now().value }

/**
 * A moment as this phone writes dates and times. A moment in [currentYear] is written without its year, which
 * would only repeat what the reader knows. [zone] is a parameter so previews can pin it.
 */
fun formatDateTime(instant: Instant, zone: ZoneId, currentYear: Int): String {
    val moment = instant.toJavaInstant().atZone(zone)
    if (moment.year != currentYear) {
        return DateTimeFormatter.ofLocalizedDateTime(FormatStyle.MEDIUM, FormatStyle.SHORT).format(moment)
    }
    val locale = Locale.getDefault()
    val date = DateTimeFormatter.ofPattern(DateFormat.getBestDateTimePattern(locale, "MMMd"), locale).format(moment)
    val time = DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT).format(moment)
    return "$date, $time"
}

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
