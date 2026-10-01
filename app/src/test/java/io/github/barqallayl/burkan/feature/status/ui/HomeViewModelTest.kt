package io.github.barqallayl.burkan.feature.status.ui

import io.github.barqallayl.burkan.core.model.Renderer
import io.github.barqallayl.burkan.core.shell.FakeShellExecutor
import io.github.barqallayl.burkan.core.storage.FakeSettingsStorage
import io.github.barqallayl.burkan.core.shell.ShellCommands
import io.github.barqallayl.burkan.core.shell.fixture
import io.github.barqallayl.burkan.feature.apply.FakeApplyLauncher
import io.github.barqallayl.burkan.feature.apply.FakeAutoApplyStorage
import io.github.barqallayl.burkan.feature.apply.data.ApplyController
import io.github.barqallayl.burkan.feature.apply.data.FixtureDevice
import io.github.barqallayl.burkan.feature.apply.data.FixtureDevice.replyLikeFixtureDevice
import io.github.barqallayl.burkan.feature.apply.model.ApplyKind
import io.github.barqallayl.burkan.feature.apply.model.AutoApplyState
import io.github.barqallayl.burkan.feature.apply.model.WaitReason
import io.github.barqallayl.burkan.feature.apply.model.RunTrigger
import io.github.barqallayl.burkan.feature.connection.data.FakeShellAccess
import io.github.barqallayl.burkan.feature.connection.model.ConnectionError
import io.github.barqallayl.burkan.feature.log.data.FakeRunLogStorage
import io.github.barqallayl.burkan.feature.status.data.StatusRepository
import io.github.barqallayl.burkan.feature.status.model.RendererStatus
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Test
import org.orbitmvi.orbit.test.Item
import org.orbitmvi.orbit.test.OrbitScopedTestContextInternal
import org.orbitmvi.orbit.test.testWithInternalState
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlin.time.Clock

private typealias HomeTest = OrbitScopedTestContextInternal<HomeState, HomeState, HomeSideEffect, HomeViewModel>

class HomeViewModelTest {

    private val shell = FakeShellExecutor().apply {
        replyLikeFixtureDevice()
        reply(ShellCommands.getRenderer(), stdout = "\n")
        listOf(ShellCommands.SystemUi, ShellCommands.Launcher, FixtureDevice.Keyboard).forEach {
            reply(ShellCommands.gfxInfo(it), stdout = fixture("gfxinfo-opengl.txt"))
        }
    }
    private val access = FakeShellAccess(shell)
    private val log = FakeRunLogStorage()
    private val settings = FakeSettingsStorage()
    private val controller = ApplyController(access, log, settings, Clock.System, FixtureDevice.Self)
    private val launcher = FakeApplyLauncher()
    private val autoApply = FakeAutoApplyStorage()

    private fun viewModel() = HomeViewModel(StatusRepository(access), controller, launcher, log, autoApply)

    private val notApplied = StatusState.Loaded(
        RendererStatus(Renderer.OpenGL, Renderer.OpenGL, Renderer.OpenGL, Renderer.OpenGL),
    )

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
    fun `apply now starts the light apply`() = runTest {
        viewModel().testWithInternalState(this) {
            val creating = runOnCreate()

            containerHost.applyNow().join()

            assertEquals(listOf(ApplyKind.Light), launcher.started)
            creating.cancel()
            cancelAndIgnoreRemainingItems()
        }
    }

    @Test
    fun `a finished run shows as the last run and the status is read again`() = runTest {
        viewModel().testWithInternalState(this) {
            val creating = runOnCreate()
            // Let the first read of the log land, so the run's entry arrives as a new one.
            this@runTest.runCurrent()

            controller.run(ApplyKind.Light, RunTrigger.Manual)

            val state = awaitState { it.lastRun != null && it.status is StatusState.Loaded }
            assertEquals(ApplyKind.Light, state.lastRun?.kind)
            assertFalse(state.isRefreshing)
            // The light apply never reads the property; only the status read after it does.
            val runEnd = shell.lines.indexOf("am crash com.samsung.android.honeyboard")
            assertTrue(shell.lines.indexOf("getprop debug.hwui.renderer") > runEnd)
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
}
