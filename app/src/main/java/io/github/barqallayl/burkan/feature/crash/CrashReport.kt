package io.github.barqallayl.burkan.feature.crash

import android.content.Context
import android.os.Build
import java.io.File
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import kotlin.time.Instant
import kotlin.time.toJavaInstant

/** What a crash report says about where it came from. */
data class CrashSource(
    val appVersion: String,
    val versionCode: Long,
    val manufacturer: String,
    val model: String,
    val androidRelease: String,
    val sdk: Int,
)

/**
 * A crash as the text of a report: where it came from, when, on which thread, then the error with its causes, as
 * Java writes them. It holds nothing else: no settings, no key, and nothing read from the phone beyond its model
 * and Android version. A trace too long to be worth sending whole is cut at [MAX_TRACE_CHARS].
 */
fun crashReportText(
    error: Throwable,
    thread: String,
    source: CrashSource,
    at: Instant,
    zone: ZoneId,
): String = buildString {
    appendLine("Burkan ${source.appVersion} (${source.versionCode}) crash report")
    appendLine("Device: ${source.manufacturer} ${source.model}, Android ${source.androidRelease} (SDK ${source.sdk})")
    appendLine("Time: ${STAMP.withZone(zone).format(at.toJavaInstant())}")
    appendLine("Thread: $thread")
    appendLine()
    val trace = error.stackTraceToString().trimEnd()
    if (trace.length > MAX_TRACE_CHARS) {
        appendLine(trace.take(MAX_TRACE_CHARS))
        appendLine(CUT_MARK)
    } else {
        appendLine(trace)
    }
}

/**
 * The one crash report the app keeps: the newest. The process that crashed writes it and the crash screen's reads
 * it. It is in the cache, so Android clears it away by itself.
 */
class CrashReportFile(context: Context) {

    private val file: File = File(File(context.cacheDir, FOLDER), NAME)

    fun write(text: String) {
        file.parentFile?.mkdirs()
        file.writeText(text)
    }

    /** Null when there is no report, or it cannot be read. */
    fun read(): String? = runCatching { file.takeIf { it.isFile }?.readText() }.getOrNull()

    private companion object {
        const val FOLDER = "crash"
        const val NAME = "burkan-crash.txt"
    }
}

fun Context.crashSource(): CrashSource {
    val info = packageManager.getPackageInfo(packageName, 0)
    return CrashSource(
        appVersion = info.versionName.orEmpty(),
        versionCode = info.longVersionCode,
        manufacturer = Build.MANUFACTURER,
        model = Build.MODEL,
        androidRelease = Build.VERSION.RELEASE,
        sdk = Build.VERSION.SDK_INT,
    )
}

/** Shared as text, which has to fit in what one app may hand another. */
private const val MAX_TRACE_CHARS = 40_000
private const val CUT_MARK = "… (cut short)"

/** Fixed, not the phone's own format: a report is read by whoever it is sent to. */
private val STAMP = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss xxx")
