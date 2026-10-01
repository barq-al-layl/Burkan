package io.github.barqallayl.burkan.designsystem

import com.materialkolor.PaletteStyle
import com.materialkolor.dynamiccolor.ColorSpec
import org.junit.Test
import kotlin.test.assertEquals

class ThemeOptionsTest {

    @Test
    fun `every MaterialKolor palette style can be chosen, once`() {
        assertEquals(PaletteStyle.entries, PaletteStyles.entries.map { it.style }.sortedBy { it.ordinal })
    }

    @Test
    fun `every MaterialKolor colour spec can be chosen, once`() {
        assertEquals(ColorSpec.SpecVersion.entries.toSet(), ColorSpecs.entries.map { it.version }.toSet())
        assertEquals(ColorSpec.SpecVersion.entries.size, ColorSpecs.entries.size)
    }
}
