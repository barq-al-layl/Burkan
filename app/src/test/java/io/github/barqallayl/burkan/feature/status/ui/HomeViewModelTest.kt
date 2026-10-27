package io.github.barqallayl.burkan.feature.status.ui

import io.github.barqallayl.burkan.feature.status.model.BehindApp
import io.github.barqallayl.burkan.core.shell.PackageName
import io.github.barqallayl.burkan.feature.settings.data.FakeInstalledApps
import io.github.barqallayl.burkan.core.model.Renderer
import io.github.barqallayl.burkan.core.model.RendererStatus
import io.github.barqallayl.burkan.core.shell.FakeShellExecutor
import io.github.barqallayl.burkan.core.shell.SettingKey
import io.github.barqallayl.burkan.core.shell.ShellCommands
import io.github.barqallayl.burkan.core.shell.ShellError
import io.github.barqallayl.burkan.core.shell.fixture
import io.github.barqallayl.burkan.core.storage.FakeDeviceStateStorage
import io.github.barqallayl.burkan.core.storage.FakeSettingsStorage
import io.github.barqallayl.burkan.core.store.SettingsStore
import io.github.barqallayl.burkan.feature.apply.FakeAppIcons
import io.github.barqallayl.burkan.feature.apply.FakeApplyLauncher
import io.github.barqallayl.burkan.feature.apply.FakeAutoApplyStorage
import io.github.barqallayl.burkan.feature.apply.FakeCapturedSettingsStorage
import io.github.barqallayl.burkan.feature.apply.FakeDeviceSettings
import io.github.barqallayl.burkan.feature.apply.FakeLockEvents
import io.github.barqallayl.burkan.feature.apply.FakeRecentApps
import io.github.barqallayl.burkan.feature.apply.FakeRunAnnouncer
import io.github.barqallayl.burkan.feature.apply.FakeSystemUiRestarts
import io.github.barqallayl.burkan.feature.apply.data.ApplyController
import io.github.barqallayl.burkan.feature.apply.data.CapturedSettings
import io.github.barqallayl.burkan.feature.apply.data.FixtureDevice
import io.github.barqallayl.burkan.feature.apply.data.FixtureDevice.replyLikeFixtureDevice
import io.github.barqallayl.burkan.feature.apply.data.SystemUiCooldown
import io.github.barqallayl.burkan.feature.apply.data.TestClock
import io.github.barqallayl.burkan.feature.apply.model.ApplyKind
import io.github.barqallayl.burkan.feature.apply.model.AutoApplyState
import io.github.barqallayl.burkan.feature.apply.model.RestartScope
import io.github.barqallayl.burkan.feature.apply.model.RestoredSetting
import io.github.barqallayl.burkan.feature.apply.model.RunTrigger
import io.github.barqallayl.burkan.feature.apply.model.WaitReason
import io.github.barqallayl.burkan.feature.connection.data.FakeShellAccess
import io.github.barqallayl.burkan.feature.connection.data.FakeWirelessDebugging
import io.github.barqallayl.burkan.feature.connection.model.ConnectionError
import io.github.barqallayl.burkan.feature.log.data.FakeRunLogStorage
import io.github.barqallayl.burkan.feature.log.data.RunLogStore
import io.github.barqallayl.burkan.feature.status.data.StatusRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Test
import org.orbitmvi.orbit.test.Item
import org.orbitmvi.orbit.test.OrbitScopedTestContextInternal
import org.orbitmvi.orbit.test.testWithInternalState
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

private typealias HomeTest = OrbitScopedTestContextInternal<HomeState, HomeState, HomeSideEffect, HomeViewModel>

class HomeViewModelTest {

    private val shell = FakeShellExecutor().apply {
        replyLikeFixtureDevice()
        reply(ShellCommands.getRenderer(), stdout = "\n")
        listOf(ShellCommands.SystemUi, FixtureDevice.Launcher, FixtureDevice.Keyboard).forEach {
            reply(ShellCommands.gfxInfo(it), stdout = fixture("gfxinfo-opengl.txt"))
        }
    }
    private val access = FakeShellAccess(shell)
    private val wirelessDebugging = FakeWirelessDebugging()
    private val log = FakeRunLogStorage()
    private val settings = FakeSettingsStorage()
    private val clock = TestClock()
    private val cooldown = SystemUiCooldown(FakeSystemUiRestarts(), clock)
    private val deviceSettings = FakeDeviceSettings()
    private val kept = FakeCapturedSettingsStorage()
    private val announcer = FakeRunAnnouncer()
    private val controller = ApplyController(
        access,
        log,
        settings,
        cooldown,
        CapturedSettings(kept, deviceSettings, clock),
        FakeLockEvents(),
        FakeRecentApps(),
        clock,
        FixtureDevice.Self,
        announcer,
    )
    private val launcher = FakeApplyLauncher()
    private val installedApps = FakeInstalledApps(mapOf("net.example.reader" to "Reader"))

    /** How many times the status was read. Each read of it starts by asking for the property. */
    private fun statusReads() = shell.lines.count { it == "getprop debug.hwui.renderer" }
    private val autoApply = FakeAutoApplyStorage()

    private fun TestScope.viewModel() = HomeViewModel(
        StatusRepository(access, installedApps, settings, wirelessDebugging, FixtureDevice.Self),
        controller,
        launcher,
        RunLogStore(log, backgroundScope),
        autoApply,
        SettingsStore(settings, FakeDeviceStateStorage(), backgroundScope),
        clock,
        FakeAppIcons(),
    )

    private val notApplied = StatusState.Loaded(
        RendererStatus(Renderer.OpenGL, Renderer.OpenGL, Renderer.OpenGL, Renderer.OpenGL),
    )

    @Test
    fun `opening Home puts back the settings a run left changed when the app died under it`() = runTest {
        kept.kept = mapOf(RestoredSetting.EdgeEnabled to "1")

        viewModel().testWithInternalState(this) {
            val creating = runOnCreate()
            runCurrent()

            assertEquals(listOf(SettingKey.EdgeEnabled to "1"), deviceSettings.written)
            assertEquals(emptyMap(), kept.kept)
            creating.cancel()
            cancelAndIgnoreRemainingItems()
        }
    }

    @Test
    fun `refreshing reads the status`() = runTest {
        viewModel().testWithInternalState(this) {
            val creating = runOnCreate()

            containerHost.refresh()

            assertEquals(notApplied, awaitState { !it.isRefreshing && it.status != StatusState.Loading }.status)
            creating.cancel()
            cancelAndIgnoreRemainingItems()
        }
    }

    @Test
    fun `reading the status says whether Android may forget the pairing`() = runTest {
        wirelessDebugging.forgetsPairings = true

        viewModel().testWithInternalState(this) {
            val creating = runOnCreate()

            containerHost.refresh()

            assertTrue(awaitState { !it.isRefreshing && it.status != StatusState.Loading }.pairingCanLapse)
            creating.cancel()
            cancelAndIgnoreRemainingItems()
        }
    }

    @Test
    fun `no connection shows why`() = runTest {
        access.failure = ConnectionError.WirelessDebuggingRefused

        viewModel().testWithInternalState(this) {
            val creating = runOnCreate()

            containerHost.refresh()

            assertEquals(
                StatusState.Failed(ConnectionError.WirelessDebuggingRefused),
                awaitState { it.status is StatusState.Failed }.status,
            )
            creating.cancel()
            cancelAndIgnoreRemainingItems()
        }
    }

    @Test
    fun `restarting all apps asks first`() = runTest {
        viewModel().testWithInternalState(this) {
            val creating = runOnCreate()

            containerHost.requestRestartAll()
            awaitState { it.isConfirmingFullApply }
            assertEquals(emptyList(), launcher.started)

            containerHost.confirmRestartAll()
            awaitState { !it.isConfirmingFullApply }
            assertEquals(listOf(ApplyKind.Full), launcher.started)

            creating.cancel()
            cancelAndIgnoreRemainingItems()
        }
    }

    @Test
    fun `restart all goes for every app each time, unless fewer are picked for that run`() = runTest {
        viewModel().testWithInternalState(this) {
            val creating = runOnCreate()

            containerHost.requestRestartAll()
            containerHost.chooseRestartScope(RestartScope.Recent70)
            awaitState { it.restartScope == RestartScope.Recent70 }
            containerHost.confirmRestartAll()
            awaitState { !it.isConfirmingFullApply }

            // The next time starts from every app again.
            containerHost.requestRestartAll()
            assertEquals(RestartScope.All, awaitState { it.isConfirmingFullApply }.restartScope)
            containerHost.confirmRestartAll()
            awaitState { !it.isConfirmingFullApply }

            assertEquals(listOf(RestartScope.Recent70, RestartScope.All), launcher.scopes)
            creating.cancel()
            cancelAndIgnoreRemainingItems()
        }
    }

    @Test
    fun `dismissing the question starts nothing`() = runTest {
        viewModel().testWithInternalState(this) {
            val creating = runOnCreate()

            containerHost.requestRestartAll()
            awaitState { it.isConfirmingFullApply }
            containerHost.dismissRestartAll()
            awaitState { !it.isConfirmingFullApply }

            assertEquals(emptyList(), launcher.started)
            creating.cancel()
            cancelAndIgnoreRemainingItems()
        }
    }

    @Test
    fun `the waiting card follows the automatic apply`() = runTest {
        viewModel().testWithInternalState(this) {
            val creating = runOnCreate()

            autoApply.state.value = AutoApplyState(WaitReason.Wifi)
            assertEquals(WaitReason.Wifi, awaitState { it.waitingFor != null }.waitingFor)

            autoApply.state.value = AutoApplyState()
            assertEquals(null, awaitState { it.waitingFor == null }.waitingFor)
            creating.cancel()
            cancelAndIgnoreRemainingItems()
        }
    }

    @Test
    fun `apply now says first that the screen will lock, when System UI is to restart`() = runTest {
        viewModel().testWithInternalState(this) {
            val creating = runOnCreate()

            containerHost.applyNow()
            awaitState { it.isConfirmingApply }
            assertEquals(emptyList(), launcher.started)

            containerHost.confirmApply()
            awaitState { !it.isConfirmingApply }
            assertEquals(listOf(ApplyKind.Light), launcher.started)
            creating.cancel()
            cancelAndIgnoreRemainingItems()
        }
    }

    @Test
    fun `apply now with System UI already on Vulkan starts at once`() = runTest {
        shell.reply(ShellCommands.gfxInfo(ShellCommands.SystemUi), stdout = fixture("gfxinfo-vulkan.txt"))

        viewModel().testWithInternalState(this) {
            val creating = runOnCreate()
            containerHost.refresh().join()

            containerHost.applyNow().join()

            assertEquals(listOf(ApplyKind.Light), launcher.started)
            creating.cancel()
            cancelAndIgnoreRemainingItems()
        }
    }

    @Test
    fun `a finished run shows the status it read itself, without connecting again`() = runTest {
        shell.thenReply(ShellCommands.gfxInfo(ShellCommands.SystemUi), stdout = fixture("gfxinfo-vulkan.txt"))

        viewModel().testWithInternalState(this) {
            val creating = runOnCreate()
            // Let the first read of the log land, so the run's entry arrives as a new one.
            this@runTest.runCurrent()

            controller.run(ApplyKind.Light, RunTrigger.Manual)

            val state = awaitState { it.lastRun != null && it.status is StatusState.Loaded }
            assertEquals(ApplyKind.Light, state.lastRun?.kind)
            assertEquals(Renderer.Vulkan, (state.status as StatusState.Loaded).status.systemUi)
            assertEquals(1, access.runs, "only the run connected")
            creating.cancel()
            cancelAndIgnoreRemainingItems()
        }
    }

    @Test
    fun `a run's steps stay, with how it ended, until the user has seen them`() = runTest {
        shell.thenReply(ShellCommands.gfxInfo(ShellCommands.SystemUi), stdout = fixture("gfxinfo-vulkan.txt"))

        viewModel().testWithInternalState(this) {
            val creating = runOnCreate()
            this@runTest.runCurrent()

            controller.run(ApplyKind.Light, RunTrigger.Manual)

            val finished = awaitState { it.finishedRun != null }
            assertEquals(ApplyKind.Light, finished.finishedRun?.kind)
            assertTrue(finished.runSteps.isNotEmpty(), "the steps the run went through")

            containerHost.dismissRun()

            val dismissed = awaitState { it.runSteps.isEmpty() }
            assertEquals(null, dismissed.finishedRun)
            creating.cancel()
            cancelAndIgnoreRemainingItems()
        }
    }

    @Test
    fun `resuming soon after a read shows it as it is, and later reads again`() = runTest {
        viewModel().testWithInternalState(this) {
            val creating = runOnCreate()

            containerHost.onResume().join()
            containerHost.onResume().join()
            assertEquals(1, statusReads())

            clock.advance(31.seconds)
            containerHost.onResume().join()
            assertEquals(2, statusReads())
            creating.cancel()
            cancelAndIgnoreRemainingItems()
        }
    }

    @Test
    fun `back from another of the app's screens, the status is shown as it is however long ago it was read`() =
        runTest {
            viewModel().testWithInternalState(this) {
                val creating = runOnCreate()

                containerHost.onResume().join()
                containerHost.onPause(leavingApp = false).join()
                clock.advance(10.minutes)
                containerHost.onResume().join()
                assertEquals(1, statusReads())

                containerHost.onPause(leavingApp = true).join()
                containerHost.onResume().join()
                assertEquals(2, statusReads())
                creating.cancel()
                cancelAndIgnoreRemainingItems()
            }
        }

    @Test
    fun `leaving the screen lets the connection go`() = runTest {
        viewModel().testWithInternalState(this) {
            containerHost.onPause().join()

            assertEquals(1, access.releases)
            cancelAndIgnoreRemainingItems()
        }
    }

    @Test
    fun `System UI left for the next lock shows, and the wait for the lock is set up again`() = runTest {
        viewModel().testWithInternalState(this) {
            val creating = runOnCreate()

            autoApply.state.value = AutoApplyState(systemUiAtNextLock = true)

            assertTrue(awaitState { it.systemUiAtNextLock }.systemUiAtNextLock)
            assertEquals(1, launcher.lockWaits)
            creating.cancel()
            cancelAndIgnoreRemainingItems()
        }
    }

    private suspend fun HomeTest.awaitState(matches: (HomeState) -> Boolean): HomeState {
        while (true) {
            val item = awaitItem()
            if (item is Item.StateItem && matches(item.value)) return item.value
        }
    }

    @Test
    fun `the open apps that are behind are read once the status is shown`() = runTest {
        shell.reply(ShellCommands.gfxInfoAll(), stdout = fixture("gfxinfo-all.txt"))

        viewModel().testWithInternalState(this) {
            val creating = runOnCreate()

            containerHost.refresh()

            // The status arrives first, with the open apps still to come.
            val shown = awaitState { it.status is StatusState.Loaded }
            assertEquals(null, shown.behind)
            // New apps get OpenGL on this phone, so it is the app on Vulkan that is behind.
            val read = awaitState { it.behind != null }
            assertEquals(listOf(BehindApp(PackageName.known("net.example.reader"), "Reader")), read.behind)
            creating.cancel()
            cancelAndIgnoreRemainingItems()
        }
    }

    @Test
    fun `open apps that cannot be read stop being waited for`() = runTest {
        shell.fail(ShellCommands.gfxInfoAll(), ShellError.ConnectionLost)

        viewModel().testWithInternalState(this) {
            val creating = runOnCreate()

            containerHost.refresh()

            val asking = awaitState { it.status is StatusState.Loaded }
            assertTrue(asking.isCheckingBehind)
            val given = awaitState { !it.isCheckingBehind }
            assertEquals(null, given.behind)
            creating.cancel()
            cancelAndIgnoreRemainingItems()
        }
    }

    @Test
    fun `the apps that are behind are shown, and restarted only when the user says so`() = runTest {
        shell.reply(ShellCommands.gfxInfoAll(), stdout = fixture("gfxinfo-all.txt"))

        viewModel().testWithInternalState(this) {
            val creating = runOnCreate()
            containerHost.refresh().join()

            containerHost.showBehind()
            awaitState { it.isShowingBehind }
            assertTrue(launcher.started.isEmpty())

            containerHost.confirmRestartBehind()
            awaitState { !it.isShowingBehind }
            assertEquals(listOf(ApplyKind.OpenApps), launcher.started)
            creating.cancel()
            cancelAndIgnoreRemainingItems()
        }
    }

    @Test
    fun `one app is restarted alone from its row`() = runTest {
        val chat = PackageName.known("org.example.chat")

        viewModel().testWithInternalState(this) {
            val creating = runOnCreate()

            containerHost.restartOne(chat).join()

            assertEquals(listOf(ApplyKind.OpenApps), launcher.started)
            assertEquals(listOf<Set<PackageName>?>(setOf(chat)), launcher.only)
            creating.cancel()
            cancelAndIgnoreRemainingItems()
        }
    }

    @Test
    fun `an app kept from its row joins the exclusions and leaves the list`() = runTest {
        shell.reply(ShellCommands.gfxInfoAll(), stdout = fixture("gfxinfo-all.txt"))
        val reader = PackageName.known("net.example.reader")

        viewModel().testWithInternalState(this) {
            val creating = runOnCreate()
            containerHost.refresh()
            awaitState { it.behind != null }

            containerHost.keep(reader).join()

            assertEquals(emptyList<BehindApp>(), awaitState { it.behind?.isEmpty() == true }.behind)
            assertTrue(reader in settings.userExclusions.first())
            creating.cancel()
            cancelAndIgnoreRemainingItems()
        }
    }

    @Test
    fun `with nothing behind there is nothing to show`() = runTest {
        viewModel().testWithInternalState(this) {
            val creating = runOnCreate()
            containerHost.refresh().join()

            containerHost.showBehind().join()

            assertTrue(launcher.started.isEmpty())
            creating.cancel()
            cancelAndIgnoreRemainingItems()
        }
    }

    @Test
    fun `going back to OpenGL asks first`() = runTest {
        viewModel().testWithInternalState(this) {
            val creating = runOnCreate()

            containerHost.requestRevert()
            awaitState { it.isConfirmingRevert }
            assertTrue(launcher.started.isEmpty())

            containerHost.confirmRevert()
            awaitState { !it.isConfirmingRevert }
            assertEquals(listOf(ApplyKind.Revert), launcher.started)
            creating.cancel()
            cancelAndIgnoreRemainingItems()
        }
    }
}
