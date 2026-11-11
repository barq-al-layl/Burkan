package io.github.barqallayl.burkan.feature.status.ui

import io.github.barqallayl.burkan.core.model.Renderer
import io.github.barqallayl.burkan.core.model.Renderer.NotDrawn
import io.github.barqallayl.burkan.core.model.Renderer.OpenGL
import io.github.barqallayl.burkan.core.model.Renderer.Vulkan
import io.github.barqallayl.burkan.core.model.RendererStatus
import io.github.barqallayl.burkan.designsystem.component.PartLook
import io.github.barqallayl.burkan.designsystem.component.PhoneLooks
import io.github.barqallayl.burkan.feature.apply.data.ApplyRunState
import io.github.barqallayl.burkan.feature.apply.data.RunPhase
import io.github.barqallayl.burkan.feature.apply.model.ApplyKind
import io.github.barqallayl.burkan.feature.apply.model.RunTrigger
import io.github.barqallayl.burkan.feature.connection.model.ConnectionError
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/** What Home's drawn phone shows for each state of the status. */
class HomePhoneTest {

    private fun loaded(systemUi: Renderer, launcher: Renderer, keyboard: Renderer) =
        StatusState.Loaded(RendererStatus(Vulkan, systemUi, launcher, keyboard))

    @Test
    fun `each part shows what it draws with, and a part not drawn yet is an outline`() {
        assertEquals(
            PhoneLooks(PartLook.OpenGL, PartLook.Vulkan, PartLook.Outline),
            HomeState(status = loaded(OpenGL, Vulkan, NotDrawn)).phoneLooks,
        )
    }

    @Test
    fun `System UI waiting for the lock has a padlock in the status bar`() {
        val state = HomeState(status = loaded(OpenGL, Vulkan, Vulkan), systemUiAtNextLock = true)
        assertEquals(
            PhoneLooks(PartLook.OpenGL, PartLook.Vulkan, PartLook.Vulkan, statusBarLocked = true),
            state.phoneLooks,
        )
    }

    @Test
    fun `while the status is read or a run is under way, the phone is busy and drawn in outline`() {
        val reading = HomeState(status = StatusState.Loading)
        assertTrue(reading.isPhoneBusy)
        assertEquals(PhoneLooks.all(PartLook.Outline), reading.phoneLooks)

        val run = ApplyRunState.Running(ApplyKind.Light, RunTrigger.Manual, RunPhase.Reading)
        val running = HomeState(status = loaded(Vulkan, Vulkan, Vulkan), run = run)
        assertTrue(running.isPhoneBusy)
        assertEquals(PhoneLooks.all(PartLook.Outline), running.phoneLooks)
    }

    @Test
    fun `a phone that cannot be asked is drawn in outline, and still`() {
        val failed = HomeState(status = StatusState.Failed(ConnectionError.NoWifi))
        assertFalse(failed.isPhoneBusy)
        assertEquals(PhoneLooks.all(PartLook.Outline), failed.phoneLooks)
    }
}
