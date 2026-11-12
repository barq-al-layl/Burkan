package io.github.barqallayl.burkan.feature.status.ui

import io.github.barqallayl.burkan.core.model.Renderer.OpenGL
import io.github.barqallayl.burkan.core.model.Renderer.Vulkan
import io.github.barqallayl.burkan.core.model.RendererStatus
import io.github.barqallayl.burkan.core.shell.PackageName
import io.github.barqallayl.burkan.feature.apply.data.ApplyRunState
import io.github.barqallayl.burkan.feature.apply.data.RunPhase
import io.github.barqallayl.burkan.feature.apply.model.ApplyKind
import io.github.barqallayl.burkan.feature.apply.model.RunTrigger
import io.github.barqallayl.burkan.feature.connection.model.ConnectionError
import io.github.barqallayl.burkan.feature.status.model.BehindApp
import io.github.barqallayl.burkan.feature.status.model.Headline
import org.junit.Test
import kotlin.test.assertEquals

/** Home's main action follows what there is to do. */
class HomeMainActionTest {

    private val active = StatusState.Loaded(RendererStatus(Vulkan, Vulkan, Vulkan, Vulkan))
    private val notApplied = StatusState.Loaded(RendererStatus(OpenGL, OpenGL, OpenGL, OpenGL))
    private val systemUiLeft = StatusState.Loaded(RendererStatus(Vulkan, OpenGL, Vulkan, Vulkan))
    private val chat = BehindApp(PackageName.known("com.example.chat"), "Chat")

    @Test
    fun `nothing applied asks for Apply now`() {
        assertEquals(MainAction.Apply, HomeState(status = notApplied).mainAction)
    }

    @Test
    fun `Vulkan active with apps left behind offers to restart them`() {
        assertEquals(MainAction.RestartBehind(1), HomeState(status = active, behind = listOf(chat)).mainAction)
    }

    @Test
    fun `Vulkan active with nothing left behind keeps Apply now quiet`() {
        assertEquals(MainAction.ApplyQuietly, HomeState(status = active, behind = emptyList()).mainAction)
        // The open apps not read yet: nothing to offer about them.
        assertEquals(MainAction.ApplyQuietly, HomeState(status = active).mainAction)
    }

    @Test
    fun `waiting for the lock has nothing to do`() {
        val state = HomeState(status = systemUiLeft, systemUiAtNextLock = true)
        assertEquals(Headline.WaitingForLock, state.headline)
        assertEquals(MainAction.ApplyQuietly, state.mainAction)
    }

    @Test
    fun `System UI on OpenGL without a wait for the lock is partly applied, and asks for Apply now`() {
        val state = HomeState(status = systemUiLeft)
        assertEquals(Headline.PartlyApplied, state.headline)
        assertEquals(MainAction.Apply, state.mainAction)
    }

    @Test
    fun `a phone that cannot be asked cannot be applied either`() {
        assertEquals(MainAction.Unavailable, HomeState(status = StatusState.Failed(ConnectionError.NoWifi)).mainAction)
    }

    @Test
    fun `a run under way is cancelled from the main action`() {
        val run = ApplyRunState.Running(ApplyKind.Light, RunTrigger.Manual, RunPhase.Reading)
        assertEquals(MainAction.Cancel, HomeState(status = active, run = run).mainAction)
    }
}
