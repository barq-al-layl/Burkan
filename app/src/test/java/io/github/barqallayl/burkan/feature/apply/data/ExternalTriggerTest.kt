package io.github.barqallayl.burkan.feature.apply.data

import io.github.barqallayl.burkan.core.storage.FakeDeviceStateStorage
import io.github.barqallayl.burkan.core.storage.FakeSettingsStorage
import io.github.barqallayl.burkan.feature.apply.FakeApplyLauncher
import io.github.barqallayl.burkan.feature.apply.FakeRunAlerts
import io.github.barqallayl.burkan.feature.apply.model.ApplyKind
import io.github.barqallayl.burkan.feature.apply.model.RunTrigger
import io.github.barqallayl.burkan.feature.log.data.FakeRunLogStorage
import io.github.barqallayl.burkan.feature.log.model.RunLogEntry
import io.github.barqallayl.burkan.feature.log.model.RunResult
import kotlinx.coroutines.test.runTest
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.seconds

class ExternalTriggerTest {

    private val deviceState = FakeDeviceStateStorage(paired = true, setupStepsDone = 7, setupComplete = true)
    private val launcher = FakeApplyLauncher()
    private val alerts = FakeRunAlerts()
    private val settings = FakeSettingsStorage()
    private val runLog = FakeRunLogStorage()
    private val clock = TestClock()
    private val trigger = ExternalTrigger(deviceState, launcher, alerts, settings, runLog, clock)

    /** A run of [kind] started from outside that took ten seconds and has just ended. */
    private suspend fun justRan(kind: ApplyKind, trigger: RunTrigger = RunTrigger.External) {
        runLog.add(RunLogEntry(clock.now(), trigger, kind, RunResult.Succeeded, 10.seconds, emptyList()))
        clock.advance(10.seconds)
    }

    @Test
    fun `with the broadcasts switched off, none starts a run, and the shortcuts and the tile still do`() = runTest {
        settings.allowExternalTriggers.value = false

        assertFalse(trigger.onBroadcast(ExternalTrigger.ACTION_APPLY))
        assertFalse(trigger.onBroadcast(ExternalTrigger.ACTION_RESTART_ALL))
        assertTrue(alerts.notStarted.isEmpty(), "nothing is offered for a run the user switched off")

        assertTrue(trigger.onShortcut(ExternalTrigger.SHORTCUT_APPLY))
        assertTrue(trigger.onTile())
    }

    @Test
    fun `a broadcast for a run that has just ended starts nothing, until a minute has passed`() = runTest {
        justRan(ApplyKind.Full)

        assertFalse(trigger.onBroadcast(ExternalTrigger.ACTION_RESTART_ALL))
        clock.advance(ExternalTrigger.MIN_INTERVAL - 1.seconds)
        assertFalse(trigger.onBroadcast(ExternalTrigger.ACTION_RESTART_ALL))

        clock.advance(1.seconds)
        assertTrue(trigger.onBroadcast(ExternalTrigger.ACTION_RESTART_ALL))
    }

    @Test
    fun `the wait is for the same run only, and only after one started from outside`() = runTest {
        justRan(ApplyKind.Light)
        justRan(ApplyKind.Full, RunTrigger.Manual)

        // An automation that applies and then restarts the open apps is not held up.
        assertTrue(trigger.onBroadcast(ExternalTrigger.ACTION_RESTART_OPEN_APPS))
        assertTrue(trigger.onBroadcast(ExternalTrigger.ACTION_RESTART_ALL))
        assertFalse(trigger.onBroadcast(ExternalTrigger.ACTION_APPLY))
        // The user's own shortcut is never made to wait.
        assertTrue(trigger.onShortcut(ExternalTrigger.SHORTCUT_APPLY))
    }

    @Test
    fun `each action starts its run`() = runTest {
        assertTrue(trigger.onBroadcast(ExternalTrigger.ACTION_APPLY))
        assertTrue(trigger.onBroadcast(ExternalTrigger.ACTION_RESTART_OPEN_APPS))
        assertTrue(trigger.onBroadcast(ExternalTrigger.ACTION_RESTART_ALL))

        assertEquals(
            listOf(ApplyKind.Light, ApplyKind.OpenApps, ApplyKind.Full).map { it to RunTrigger.External },
            launcher.unattended,
        )
    }

    @Test
    fun `an action that is not one of the three starts nothing`() = runTest {
        // The app's own internal actions among them: cancelling a run is not for another app to do.
        assertFalse(trigger.onBroadcast("io.github.barqallayl.burkan.action.CANCEL_RUN"))
        assertFalse(trigger.onBroadcast("android.intent.action.BOOT_COMPLETED"))
        assertFalse(trigger.onBroadcast(null))

        assertTrue(launcher.unattended.isEmpty())
    }

    @Test
    fun `nothing starts before setup is done`() = runTest {
        deviceState.isSetupComplete.value = false

        assertFalse(trigger.onBroadcast(ExternalTrigger.ACTION_APPLY))
        assertFalse(trigger.onShortcut(ExternalTrigger.SHORTCUT_APPLY))
        assertTrue(alerts.notStarted.isEmpty(), "nothing to offer either")
    }

    @Test
    fun `a run Android will not start from the background is offered in a notification`() = runTest {
        launcher.backgroundStartAllowed = false

        assertFalse(trigger.onBroadcast(ExternalTrigger.ACTION_RESTART_ALL))
        assertFalse(trigger.onTile())

        assertEquals(listOf(ApplyKind.Full, ApplyKind.Light), alerts.notStarted)
    }

    @Test
    fun `each shortcut starts its run, and anything else starts nothing`() = runTest {
        assertTrue(trigger.onShortcut(ExternalTrigger.SHORTCUT_APPLY))
        assertTrue(trigger.onShortcut(ExternalTrigger.SHORTCUT_RESTART_ALL))
        assertFalse(trigger.onShortcut("android.intent.action.VIEW"))
        assertFalse(trigger.onShortcut(null))

        assertEquals(
            listOf(ApplyKind.Light, ApplyKind.Full).map { it to RunTrigger.External },
            launcher.unattended,
        )
    }

    @Test
    fun `Android refusing to start the run in the background is reported as nothing started`() = runTest {
        launcher.backgroundStartAllowed = false

        assertFalse(trigger.onBroadcast(ExternalTrigger.ACTION_APPLY))
    }
}
