package io.github.barqallayl.burkan.feature.crash

import org.junit.Test
import java.time.ZoneOffset
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.time.Instant

class CrashReportTest {

    private val source = CrashSource(
        appVersion = "1.0.0",
        versionCode = 1,
        manufacturer = "samsung",
        model = "SM-S911B",
        androidRelease = "16",
        sdk = 36,
    )
    private val at = Instant.parse("2026-10-01T09:30:00Z")

    private fun report(error: Throwable) = crashReportText(error, "main", source, at, ZoneOffset.UTC)

    @Test
    fun `the report says where it came from, then gives the error`() {
        val lines = report(IllegalStateException("No such state")).lines()

        assertEquals(
            listOf(
                "Burkan 1.0.0 (1) crash report",
                "Device: samsung SM-S911B, Android 16 (SDK 36)",
                "Time: 2026-10-01 09:30:00 +00:00",
                "Thread: main",
                "",
                "java.lang.IllegalStateException: No such state",
            ),
            lines.take(6),
        )
    }

    @Test
    fun `the report names the cause of the error`() {
        val text = report(IllegalStateException("Outer", IllegalArgumentException("Inner")))

        assertTrue("Caused by: java.lang.IllegalArgumentException: Inner" in text)
    }

    @Test
    fun `a trace too long to send is cut short and says so`() {
        val text = report(IllegalStateException("x".repeat(100_000)))

        assertTrue(text.length < 50_000)
        assertTrue(text.trimEnd().endsWith("(cut short)"))
    }
}
