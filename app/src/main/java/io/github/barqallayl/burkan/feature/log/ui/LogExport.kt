package io.github.barqallayl.burkan.feature.log.ui

import android.content.ClipData
import android.content.Context
import android.content.Intent
import android.content.res.Resources
import android.os.Build
import androidx.core.content.FileProvider
import io.github.barqallayl.burkan.R
import io.github.barqallayl.burkan.core.ui.durationText
import io.github.barqallayl.burkan.feature.apply.ui.label
import io.github.barqallayl.burkan.feature.apply.ui.text
import io.github.barqallayl.burkan.feature.log.model.RunLogEntry
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import kotlin.time.Instant
import kotlin.time.toJavaInstant

/** What the export says about where it came from. */
data class LogSource(val appVersion: String, val model: String, val androidRelease: String)

/**
 * The log as the text of a `.log` file: a header saying where it came from, then each run as a line followed by its
 * steps, one to a line, every line starting with the moment or with a mark in the same column. It holds what the
 * screen shows and no more: no key, no package names.
 */
fun Resources.logFileText(runs: List<RunLogEntry>, source: LogSource, exportedAt: Instant, zone: ZoneId): String =
    buildString {
        appendLine(getString(R.string.log_file_title, source.appVersion))
        appendLine(getString(R.string.log_file_device, source.model, source.androidRelease))
        appendLine(getString(R.string.log_file_exported, STAMP.withZone(zone).format(exportedAt.toJavaInstant())))
        appendLine(getString(R.string.log_file_runs, runs.size))
        runs.forEach { run ->
            appendLine()
            appendLine(
                listOf(
                    "[${STAMP.withZone(zone).format(run.startedAt.toJavaInstant())}]",
                    getString(run.kind.label),
                    getString(run.trigger.label),
                    getString(run.result.label),
                    durationText(run.duration),
                ).joinToString(separator = " | "),
            )
            run.error?.let { appendLine("$INDENT$MARK_FAILED ${getString(it.resource)}") }
            run.steps.forEach { step ->
                val mark = if (step.error == null) MARK_DONE else MARK_FAILED
                appendLine("$INDENT$mark ${text(step.kind.label())}")
                step.error?.let { appendLine("$INDENT${" ".repeat(mark.length)} ${getString(it.resource)}") }
            }
        }
    }

/**
 * Writes the log to a file in the cache and offers it to other apps. The file is named for the moment it was made,
 * so two exports sent to the same place do not overwrite each other.
 */
suspend fun Context.shareLogFile(runs: List<RunLogEntry>, zone: ZoneId, exportedAt: Instant) {
    val source = LogSource(
        appVersion = packageManager.getPackageInfo(packageName, 0).versionName.orEmpty(),
        model = Build.MODEL,
        androidRelease = Build.VERSION.RELEASE,
    )
    val file = withContext(Dispatchers.IO) {
        val folder = File(cacheDir, LOG_FOLDER).apply {
            mkdirs()
            // One export is kept at a time; the cache is not an archive.
            listFiles()?.forEach { it.delete() }
        }
        val name = "burkan-${FILE_STAMP.withZone(zone).format(exportedAt.toJavaInstant())}.log"
        File(folder, name).apply { writeText(resources.logFileText(runs, source, exportedAt, zone)) }
    }
    val uri = FileProvider.getUriForFile(this, "$packageName.files", file)
    val send = Intent(Intent.ACTION_SEND)
        .setType("text/plain")
        .putExtra(Intent.EXTRA_STREAM, uri)
        .putExtra(Intent.EXTRA_SUBJECT, getString(R.string.log_share_subject))
        .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    // The share sheet reads the file's name from the clip, and the grant travels with it.
    send.clipData = ClipData.newRawUri(file.name, uri)
    startActivity(Intent.createChooser(send, getString(R.string.log_share_chooser)))
}

/** The folder `res/xml/shared_files.xml` opens to the file provider. */
private const val LOG_FOLDER = "logs"

private const val INDENT = "    "
private const val MARK_DONE = "[ ok ]"
private const val MARK_FAILED = "[FAIL]"

/** Fixed, not the phone's own format: a log is read by whoever it is sent to, and sorted by its text. */
private val STAMP = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss xxx")
private val FILE_STAMP = DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss")
