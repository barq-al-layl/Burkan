package io.github.barqallayl.burkan.designsystem

import com.materialkolor.PaletteStyle
import org.junit.Test
import kotlin.test.assertEquals

class ThemeOptionsTest {

    @Test
    fun `every MaterialKolor palette style can be chosen, once`() {
        assertEquals(PaletteStyle.KnownStyles.map { it.name }, PaletteStyles.entries.map { it.style.name })
    }
}
