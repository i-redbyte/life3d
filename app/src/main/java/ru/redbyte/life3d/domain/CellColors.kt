package ru.redbyte.life3d.domain

import kotlin.math.abs

data class Rgb(val red: Float, val green: Float, val blue: Float)

data class CellColors(val start: Rgb, val end: Rgb) {
    val gradient: Boolean get() = start != end

    companion object {
        val DefaultCustom = CellColors(Hsv(25f, 0.9f, 1f).toRgb(), Hsv(330f, 0.75f, 1f).toRgb())
    }
}

data class Hsv(val hue: Float, val saturation: Float, val value: Float) {
    fun toRgb(): Rgb {
        val h = ((hue % 360f) + 360f) % 360f / 60f
        val chroma = value * saturation
        val x = chroma * (1f - abs(h % 2f - 1f))
        val m = value - chroma
        val (r, g, b) = when (h.toInt()) {
            0 -> Triple(chroma, x, 0f)
            1 -> Triple(x, chroma, 0f)
            2 -> Triple(0f, chroma, x)
            3 -> Triple(0f, x, chroma)
            4 -> Triple(x, 0f, chroma)
            else -> Triple(chroma, 0f, x)
        }
        return Rgb(r + m, g + m, b + m)
    }

    companion object {
        fun of(rgb: Rgb): Hsv {
            val max = maxOf(rgb.red, rgb.green, rgb.blue)
            val min = minOf(rgb.red, rgb.green, rgb.blue)
            val delta = max - min
            val hue = when {
                delta == 0f -> 0f
                max == rgb.red -> 60f * (((rgb.green - rgb.blue) / delta) % 6f)
                max == rgb.green -> 60f * ((rgb.blue - rgb.red) / delta + 2f)
                else -> 60f * ((rgb.red - rgb.green) / delta + 4f)
            }
            return Hsv((hue + 360f) % 360f, if (max == 0f) 0f else delta / max, max)
        }
    }
}
