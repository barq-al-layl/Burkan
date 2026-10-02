package io.github.barqallayl.burkan.feature.status.model

import io.github.barqallayl.burkan.core.model.Renderer
import io.github.barqallayl.burkan.core.model.Renderer.OpenGL
import io.github.barqallayl.burkan.core.model.Renderer.Unknown
import io.github.barqallayl.burkan.core.model.Renderer.Vulkan
import io.github.barqallayl.burkan.core.model.RendererStatus
import org.junit.Test
import kotlin.test.assertEquals

class HeadlineTest {

    private fun status(newApps: Renderer, systemUi: Renderer, launcher: Renderer, keyboard: Renderer) =
        RendererStatus(newApps, systemUi, launcher, keyboard)

    @Test
    fun `everything on Vulkan is active`() {
        assertEquals(Headline.VulkanActive, status(Vulkan, Vulkan, Vulkan, Vulkan).headline())
    }

    @Test
    fun `a surface that cannot be read does not spoil an active result`() {
        assertEquals(Headline.VulkanActive, status(Vulkan, Vulkan, Vulkan, Unknown).headline())
    }

    @Test
    fun `after a restart nothing is applied`() {
        assertEquals(Headline.NotApplied, status(OpenGL, OpenGL, OpenGL, OpenGL).headline())
    }

    @Test
    fun `the property set but a surface still on OpenGL is partly applied`() {
        assertEquals(Headline.PartlyApplied, status(Vulkan, OpenGL, Vulkan, Vulkan).headline())
    }

    @Test
    fun `a surface on Vulkan without the property is partly applied`() {
        assertEquals(Headline.PartlyApplied, status(OpenGL, Vulkan, OpenGL, OpenGL).headline())
    }
}
