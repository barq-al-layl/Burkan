package io.github.barqallayl.burkan.feature.apply.data

import io.github.barqallayl.burkan.core.storage.FakeDeviceStateStorage
import io.github.barqallayl.burkan.feature.apply.FakeApplyLauncher
import io.github.barqallayl.burkan.feature.apply.model.ApplyKind
import io.github.barqallayl.burkan.feature.apply.model.RunTrigger
import kotlinx.coroutines.test.runTest
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ExternalTriggerTest {

    private val deviceState = FakeDeviceStateStorage(paired = true, setupStepsDone = 7, setupComplete = true)
    private val launcher = FakeApplyLauncher()
    private val trigger = ExternalTrigger(deviceState, launcher)

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
