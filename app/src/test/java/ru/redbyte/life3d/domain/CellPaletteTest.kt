package ru.redbyte.life3d.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CellPaletteTest {
    @Test
    fun aSolidPaletteEndsWithTheColourItStartsWith() {
        for (palette in CellPalette.entries.filterNot { it.gradient }) {
            assertFalse(palette.colors.gradient)
            assertEquals(palette.red, palette.endRed)
            assertEquals(palette.green, palette.endGreen)
            assertEquals(palette.blue, palette.endBlue)
        }
    }

    @Test
    fun emeraldIsASolidGreenAndAuroraBlendsTwoColours() {
        val emerald = CellPalette.Emerald
        assertFalse(emerald.gradient)
        assertTrue(emerald.green > emerald.red && emerald.green > emerald.blue)

        val aurora = CellPalette.Aurora
        assertTrue(aurora.gradient)
        assertTrue(aurora.blue < aurora.endBlue)
    }
}
