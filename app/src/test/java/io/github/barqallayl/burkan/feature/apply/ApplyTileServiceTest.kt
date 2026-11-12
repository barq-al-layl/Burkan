package io.github.barqallayl.burkan.feature.apply

import android.os.Looper
import android.service.quicksettings.TileService
import io.github.barqallayl.burkan.RoborazziTestApplication
import io.github.barqallayl.burkan.core.shell.FakeShellExecutor
import io.github.barqallayl.burkan.core.storage.FakeDeviceStateStorage
import io.github.barqallayl.burkan.core.storage.FakeSettingsStorage
import io.github.barqallayl.burkan.feature.apply.data.ApplyController
import io.github.barqallayl.burkan.feature.apply.data.CapturedSettings
import io.github.barqallayl.burkan.feature.apply.data.ExternalTrigger
import io.github.barqallayl.burkan.feature.apply.data.FixedClock
import io.github.barqallayl.burkan.feature.apply.data.FixtureDevice
import io.github.barqallayl.burkan.feature.apply.data.SystemUiCooldown
import io.github.barqallayl.burkan.feature.apply.model.ApplyKind
import io.github.barqallayl.burkan.feature.apply.model.RunTrigger
import io.github.barqallayl.burkan.feature.connection.data.FakeShellAccess
import io.github.barqallayl.burkan.feature.connection.data.FakeShizukuClient
import io.github.barqallayl.burkan.feature.log.data.FakeRunLogStorage
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.annotation.Implementation
import org.robolectric.annotation.Implements
import org.robolectric.shadow.api.Shadow
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = RoborazziTestApplication::class, shadows = [ShadowLockedTileService::class])
class ApplyTileServiceTest {

    private val log = FakeRunLogStorage()
    private val settings = FakeSettingsStorage()
    private val launcher = FakeApplyLauncher()
    private val deviceState = FakeDeviceStateStorage(paired = true, setupComplete = true)
    private val controller = ApplyController(
        FakeShellAccess(FakeShellExecutor()),
        log,
        settings,
        SystemUiCooldown(FakeSystemUiRestarts(), FixedClock),
        CapturedSettings(FakeCapturedSettingsStorage(), FakeDeviceSettings(), FixedClock),
        FakeLockEvents(),
        FakeRecentApps(),
        FixedClock,
        FixtureDevice.Self,
        FakeRunAnnouncer(),
        FakeShizukuClient(),
    )
    private val trigger = ExternalTrigger(deviceState, launcher, FakeRunAlerts(), settings, log, FixedClock)
    private val service = ApplyTileService(controller, log, deviceState, trigger, FixedClock)
    private val phone = Shadow.extract<ShadowLockedTileService>(service)

    private fun tap() {
        service.onClick()
        shadowOf(Looper.getMainLooper()).idle()
    }

    @Test
    fun `a tap on an unlocked phone applies at once`() {
        phone.secure = true

        tap()

        assertEquals(listOf(ApplyKind.Light to RunTrigger.External), launcher.unattended)
    }

    @Test
    fun `a tap on a locked phone with a secure lock applies only once it is unlocked`() {
        phone.secure = true
        phone.locked = true

        tap()

        assertTrue(launcher.unattended.isEmpty(), "nothing runs from the lock screen")
        val afterUnlock = assertNotNull(phone.afterUnlock, "the tile asks for the unlock")

        afterUnlock.run()
        shadowOf(Looper.getMainLooper()).idle()

        assertEquals(listOf(ApplyKind.Light to RunTrigger.External), launcher.unattended)
    }

    @Test
    fun `a phone locked with no secure lock is not asked to unlock`() {
        // A swipe lock: the keyguard shows, but there is nothing to unlock with and nobody to keep out.
        phone.locked = true

        tap()

        assertEquals(null, phone.afterUnlock)
        assertEquals(listOf(ApplyKind.Light to RunTrigger.External), launcher.unattended)
    }
}

/**
 * Plays the phone's lock for a tile. Robolectric's own shadow has no [TileService.isSecure] (the real one asks the
 * system and fails in a test), and its [TileService.unlockAndRun] runs at once, as if the user unlocked straight away.
 * This one keeps what is to run after the unlock, for the test to run when it chooses.
 */
@Implements(TileService::class)
class ShadowLockedTileService {
    var secure = false
    var locked = false

    /** What the tile asked to run once the phone is unlocked; null while it has not asked. */
    var afterUnlock: Runnable? = null
        private set

    @Implementation
    protected fun isSecure(): Boolean = secure

    @Implementation
    protected fun isLocked(): Boolean = locked

    @Implementation
    protected fun unlockAndRun(runnable: Runnable) {
        afterUnlock = runnable
    }
}
