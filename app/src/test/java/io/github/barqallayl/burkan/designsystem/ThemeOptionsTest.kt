package io.github.barqallayl.burkan.designsystem

import com.materialkolor.PaletteStyle
import org.junit.Test
import kotlin.test.assertEquals

class ThemeOptionsTest {

    @Test
    fun `every MaterialKolor palette style can be chosen, once`() {
        assertEquals(PaletteStyle.KnownStyles.map { it.name }, PaletteStyles.entries.map { it.style.name })
    }

    @Test
    fun `a Samsung phone starts in One UI, however it spells its name`() {
        assertEquals(AppStyle.OneUi, AppStyle.defaultFor("samsung"))
        assertEquals(AppStyle.OneUi, AppStyle.defaultFor("Samsung"))
    }

    @Test
    fun `any other phone starts in Material`() {
        assertEquals(AppStyle.Material, AppStyle.defaultFor("Google"))
        assertEquals(AppStyle.Material, AppStyle.defaultFor(null))
    }
}
