package io.github.barqallayl.burkan.feature.log.ui

import io.github.barqallayl.burkan.RoborazziTestApplication
import io.github.barqallayl.burkan.core.model.AppErrorType
import io.github.barqallayl.burkan.feature.apply.model.ApplyKind
import io.github.barqallayl.burkan.feature.apply.model.RunTrigger
import io.github.barqallayl.burkan.feature.apply.model.StepKind
import io.github.barqallayl.burkan.feature.log.model.LoggedStep
import io.github.barqallayl.burkan.feature.log.model.RunLogEntry
import io.github.barqallayl.burkan.feature.log.model.RunResult
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import java.time.ZoneOffset
import kotlin.test.assertEquals
import kotlin.time.Duration.Companion.seconds
import kotlin.time.Instant

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = RoborazziTestApplication::class)
class LogExportTest {

    private val resources = RuntimeEnvironment.getApplication().resources
    private val source = LogSource(appVersion = "1.0", model = "SM-S911B", androidRelease = "16")
    private val exportedAt = Instant.parse("2026-10-04T10:00:00Z")

    @Test
    fun `the file has a header, then each run with its steps marked`() {
        val runs = listOf(
            RunLogEntry(
                startedAt = Instant.parse("2026-10-01T09:30:00Z"),
                trigger = RunTrigger.Manual,
                kind = ApplyKind.Full,
                result = RunResult.Failed,
                duration = 73.seconds,
                steps = listOf(
                    LoggedStep(StepKind.SetRenderer),
                    LoggedStep(StepKind.StopApps(612)),
                    LoggedStep(StepKind.RelaunchApps(110), error = AppErrorType.CommandTimedOut),
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
        )

        assertEquals(
            """
            Burkan 1.0 run log
            Device: SM-S911B, Android 16
            Exported: 2026-10-04 10:00:00 +00:00
            Runs: 2, newest first

            [2026-10-01 09:30:00 +00:00] | Restart all apps | Manual | Failed | 1 min 13 s
                [ ok ] Set new apps to Vulkan
                [ ok ] Stop 612 apps
                [FAIL] Reopen 110 apps
                       The phone took too long to answer. Try again.

            [2026-10-01 08:02:00 +00:00] | Apply | After restart | Failed | 4 s
                [FAIL] Wireless debugging needs Wi-Fi. Connect to a Wi-Fi network and try again.

            """.trimIndent(),
            // Durations keep their number and unit together with a no-break space.
            resources.logFileText(runs, source, exportedAt, ZoneOffset.UTC).replace('\u00A0', ' '),
        )
    }

    @Test
    fun `an empty log is still a file that says so`() {
        assertEquals(
            """
            Burkan 1.0 run log
            Device: SM-S911B, Android 16
            Exported: 2026-10-04 10:00:00 +00:00
            Runs: 0, newest first

            """.trimIndent(),
            resources.logFileText(emptyList(), source, exportedAt, ZoneOffset.UTC),
        )
    }
}
