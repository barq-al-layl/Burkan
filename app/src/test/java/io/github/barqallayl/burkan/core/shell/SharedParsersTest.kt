package io.github.barqallayl.burkan.core.shell

import io.github.barqallayl.burkan.core.model.Renderer
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class SharedParsersTest {

    @Test
    fun `the renderer is read from the pipeline line`() {
        assertEquals(Renderer.Vulkan, parseRenderer(fixture("gfxinfo-vulkan.txt")))
        assertEquals(Renderer.OpenGL, parseRenderer(fixture("gfxinfo-opengl.txt")))
        assertEquals(Renderer.Unknown, parseRenderer("No process found for: com.example.a\n"))
    }

    @Test
    fun `a setting that prints null or nothing is unset`() {
        assertNull(parseSettingValue("null\n"))
        assertNull(parseSettingValue("\n"))
        assertNull(parseSettingValue(""))
        assertEquals("1", parseSettingValue("1\n"))
        assertEquals("a:b\$c", parseSettingValue("a:b\$c\n"))
    }

    @Test
    fun `a component's package is the part before the slash`() {
        assertEquals(
            PackageName.known("com.samsung.android.honeyboard"),
            parseComponentPackage("com.samsung.android.honeyboard/.service.HoneyBoardService"),
        )
        assertNull(parseComponentPackage("com.samsung.android.honeyboard"))
    }
}
