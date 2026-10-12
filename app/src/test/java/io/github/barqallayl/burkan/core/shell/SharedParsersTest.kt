package io.github.barqallayl.burkan.core.shell

import kotlin.test.assertTrue
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

    @Test
    fun `the property reads skiavk as Vulkan and nothing as the OpenGL default`() {
        assertEquals(Renderer.Vulkan, rendererFromProperty("skiavk\n"))
        assertEquals(Renderer.OpenGL, rendererFromProperty("\n"))
        assertEquals(Renderer.OpenGL, rendererFromProperty("skiagl\n"))
        assertEquals(Renderer.Unknown, rendererFromProperty("something-else\n"))
    }

    @Test
    fun `the home intent resolves to the home app, but the chooser is no home app`() {
        assertEquals(
            PackageName.known("com.sec.android.app.launcher"),
            parseHomeActivity(fixture("resolve-activity-home.txt")),
        )
        assertNull(parseHomeActivity(fixture("resolve-activity-home-chooser.txt")))
        assertNull(parseHomeActivity("No activity found\n"))
    }

    @Test
    fun `the home role's holder is read from a line, or from a list separated by semicolons`() {
        assertEquals(
            PackageName.known("com.sec.android.app.launcher"),
            parseRoleHolders(fixture("role-holders-home.txt")),
        )
        assertEquals(PackageName.known("org.example.home"), parseRoleHolders("org.example.home;org.example.other\n"))
        assertNull(parseRoleHolders("\n"))
    }

    @Test
    fun `the dump of every process gives each one that has drawn its renderer, by process name`() {
        val renderers = parseProcessRenderers(fixture("gfxinfo-all.txt"))

        assertEquals(Renderer.Vulkan, renderers["com.android.systemui"])
        assertEquals(Renderer.OpenGL, renderers["com.example.notes"])
        assertEquals(Renderer.OpenGL, renderers["org.example.weather:widget"])
        assertEquals(Renderer.Vulkan, renderers["net.example.reader"])
        // A process that has drawn nothing has no pipeline, and so no renderer to report.
        assertTrue("com.example.notes:sync" !in renderers)
        assertTrue("android.process.acore" !in renderers)
        assertEquals(9, renderers.size)
        assertTrue(parseProcessRenderers("").isEmpty())
    }
}
