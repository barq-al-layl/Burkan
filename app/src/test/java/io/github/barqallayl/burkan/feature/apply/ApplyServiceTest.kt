package io.github.barqallayl.burkan.feature.apply

import android.app.NotificationManager
import android.content.Intent
import android.os.Looper
import io.github.barqallayl.burkan.RoborazziTestApplication
import io.github.barqallayl.burkan.core.shell.FakeShellExecutor
import io.github.barqallayl.burkan.core.shell.ShellCommands
import io.github.barqallayl.burkan.core.shell.fixture
import io.github.barqallayl.burkan.core.storage.FakeDeviceStateStorage
import io.github.barqallayl.burkan.core.storage.FakeSettingsStorage
import io.github.barqallayl.burkan.feature.apply.data.ApplyController
import io.github.barqallayl.burkan.feature.apply.data.AutoApply
import io.github.barqallayl.burkan.feature.apply.data.CapturedSettings
import io.github.barqallayl.burkan.feature.apply.data.FixedClock
import io.github.barqallayl.burkan.feature.apply.data.FixtureDevice
import io.github.barqallayl.burkan.feature.apply.data.SystemUiCooldown
import io.github.barqallayl.burkan.feature.apply.model.ApplyKind
import io.github.barqallayl.burkan.feature.apply.model.AutoApplyState
import io.github.barqallayl.burkan.feature.apply.model.RunTrigger
import io.github.barqallayl.burkan.feature.connection.data.FakeShellAccess
import io.github.barqallayl.burkan.feature.connection.data.FakeShizukuClient
import io.github.barqallayl.burkan.feature.connection.data.FakeWirelessDebugging
import io.github.barqallayl.burkan.feature.log.data.FakeRunLogStorage
import io.github.barqallayl.burkan.feature.log.model.RunResult
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.runBlocking
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.Shadows.shadowOf
import org.robolectric.android.controller.ServiceController
import org.robolectric.annotation.Config
import java.util.concurrent.TimeUnit
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = RoborazziTestApplication::class)
class ApplyServiceTest {

    private val application = RuntimeEnvironment.getApplication()
    private val notifications = ApplyNotifications(application)

    /** New apps are on Vulkan already and System UI is not: what a run at the lock finds. */
    private val shell = FakeShellExecutor().apply {
        reply(ShellCommands.getRenderer(), stdout = "skiavk\n")
        reply(ShellCommands.gfxInfo(ShellCommands.SystemUi), stdout = fixture("gfxinfo-opengl.txt"))
    }
    private val log = FakeRunLogStorage()
    private val settings = FakeSettingsStorage()
    private val storage = FakeAutoApplyStorage(AutoApplyState(systemUiAtNextLock = true))
    private val lockEvents = FakeLockEvents()
    private val controller = ApplyController(
        FakeShellAccess(shell),
        log,
        settings,
        SystemUiCooldown(FakeSystemUiRestarts(), FixedClock),
        CapturedSettings(FakeCapturedSettingsStorage(), FakeDeviceSettings(), FixedClock),
        lockEvents,
        FakeRecentApps(),
        FixedClock,
        FixtureDevice.Self,
        FakeRunAnnouncer(),
        FakeShizukuClient(),
    )
    private val autoApply = AutoApply(
        settings,
        FakeDeviceStateStorage(paired = true, setupComplete = true),
        storage,
        FakeWirelessDebugging(),
        FakeWifiWatch(),
        FakeApplyLauncher(),
        FakeRunAlerts(),
        lockEvents,
        FakeBootCount(),
    )

    private fun intent() = Intent(application, ApplyService::class.java)

    /** Lets everything waiting on the main thread run, timers included, up to [seconds] from now. */
    private fun pass(seconds: Long) = shadowOf(Looper.getMainLooper()).idleFor(seconds, TimeUnit.SECONDS)

    @Test
    fun `the two buttons that start a run each keep the run they are for`() {
        notifications.showApplied(behind = 2)
        notifications.showNotStarted(ApplyKind.Full)

        val shown = shadowOf(application.getSystemService(NotificationManager::class.java))
        val restart = shown.getNotification(ApplyNotifications.APPLIED_ID).actions.single().actionIntent
        val start = shown.getNotification(ApplyNotifications.FAILURE_ID).actions.single().actionIntent

        assertNotEquals(restart, start)
        assertEquals(ApplyKind.OpenApps.name, shadowOf(restart).savedIntent.getStringExtra(ApplyService.EXTRA_KIND))
        assertEquals(ApplyKind.Full.name, shadowOf(start).savedIntent.getStringExtra(ApplyService.EXTRA_KIND))
    }

    @Test
    fun `a run asked for during the run at the lock waits its turn, and neither is lost`() {
        // The run at the lock is held where it restarts System UI, until the test lets it go on.
        val restarting = CompletableDeferred<Unit>()
        val crash = ShellCommands.crash(ShellCommands.SystemUi)
        shell.beforeEach = { command -> if (command.line == crash.line && log.runs.value.isEmpty()) restarting.await() }
        val service = ApplyService(controller, autoApply, notifications)
        ServiceController.of(service, intent().setAction(ApplyService.ACTION_AWAIT_LOCK)).create().startCommand(0, 1)
        pass(1)
        runBlocking { lockEvents.lock() }
        pass(10)
        assertTrue(crash.line in shell.lines, "the run at the lock is under way: ${shell.lines}")

        service.onStartCommand(intent().putExtra(ApplyService.EXTRA_KIND, ApplyKind.Light.name), 0, 2)
        pass(1)
        assertTrue(log.runs.value.isEmpty(), "nothing has ended, and nothing was cancelled")

        shell.reply(ShellCommands.gfxInfo(ShellCommands.SystemUi), stdout = fixture("gfxinfo-vulkan.txt"))
        restarting.complete(Unit)
        pass(60)

        // Newest first: the run asked for, after the run at the lock.
        assertEquals(
            listOf(RunTrigger.Manual to ApplyKind.Light, RunTrigger.AtLock to ApplyKind.Light),
            log.runs.value.map { it.trigger to it.kind },
        )
        assertTrue(log.runs.value.none { it.result == RunResult.Cancelled }, "${log.runs.value.map { it.result }}")
    }
}
