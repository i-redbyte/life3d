package ru.redbyte.life3d.domain

import org.junit.Assert.assertEquals
import org.junit.Test

class CellColorsTest {
    private fun assertRgb(expected: Rgb, actual: Rgb) {
        assertEquals(expected.red, actual.red, 1e-4f)
        assertEquals(expected.green, actual.green, 1e-4f)
        assertEquals(expected.blue, actual.blue, 1e-4f)
    }

    @Test
    fun primaryHuesTurnIntoPrimaryColours() {
        assertRgb(Rgb(1f, 0f, 0f), Hsv(0f, 1f, 1f).toRgb())
        assertRgb(Rgb(0f, 1f, 0f), Hsv(120f, 1f, 1f).toRgb())
        assertRgb(Rgb(0f, 0f, 1f), Hsv(240f, 1f, 1f).toRgb())
        assertRgb(Rgb(1f, 0f, 1f), Hsv(300f, 1f, 1f).toRgb())
    }

    @Test
    fun noSaturationIsGreyAndNoValueIsBlack() {
        assertRgb(Rgb(0.4f, 0.4f, 0.4f), Hsv(200f, 0f, 0.4f).toRgb())
        assertRgb(Rgb(0f, 0f, 0f), Hsv(80f, 1f, 0f).toRgb())
    }

    @Test
    fun aColourSurvivesTheRoundTripThroughHsv() {
        val colour = Rgb(0.15f, 0.9f, 0.6f)

        assertRgb(colour, Hsv.of(colour).toRgb())
    }

    @Test
    fun theCustomPaletteUsesTheChosenGradientAndPresetsUseTheirOwnColours() {
        val custom = CellColors(Rgb(1f, 0f, 0f), Rgb(0f, 0f, 1f))

        assertEquals(custom, LifeSettings(palette = CellPalette.Custom, custom = custom).colors)
        val emerald = LifeSettings(palette = CellPalette.Emerald, custom = custom).colors
        assertEquals(Rgb(CellPalette.Emerald.red, CellPalette.Emerald.green, CellPalette.Emerald.blue), emerald.start)
        assertEquals(emerald.start, emerald.end)
    }
}
