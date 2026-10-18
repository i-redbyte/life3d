package ru.redbyte.life3d.domain

enum class CellPalette(
    val red: Float,
    val green: Float,
    val blue: Float,
    val endRed: Float = red,
    val endGreen: Float = green,
    val endBlue: Float = blue,
) {
    Turquoise(0.25f, 0.95f, 0.78f),
    Emerald(0.08f, 0.8f, 0.42f),
    Amber(1f, 0.68f, 0.2f),
    Fuchsia(1f, 0.3f, 0.7f),
    Azure(0.3f, 0.62f, 1f),
    Lime(0.62f, 1f, 0.3f),
    Coral(1f, 0.45f, 0.38f),
    Aurora(0.15f, 1f, 0.6f, endRed = 0.78f, endGreen = 0.22f, endBlue = 1f),
    Custom(1f, 0.6f, 0.1f, endRed = 1f, endGreen = 0.25f, endBlue = 0.75f),
    ;

    val gradient: Boolean get() = this == Custom || red != endRed || green != endGreen || blue != endBlue

    val colors: CellColors get() = CellColors(Rgb(red, green, blue), Rgb(endRed, endGreen, endBlue))
}

data class LifeSettings(
    val palette: CellPalette = CellPalette.Turquoise,
    val custom: CellColors = CellColors.DefaultCustom,
    val rule: LifeRule = LifeRule.Lava,
    val gridSize: Int = 48,
    val start: StartCells = StartCells(count = 400, random = false),
    val speed: Int = 6,
    val mode: FieldMode = FieldMode.Torus,
) {
    init {
        require(gridSize in GRID_SIZES) { "Grid size must be one of $GRID_SIZES, was $gridSize" }
        require(speed in SPEED_RANGE) { "Speed must be in $SPEED_RANGE, was $speed" }
    }

    val intervalMillis: Long get() = 1_000L / speed

    val colors: CellColors get() = if (palette == CellPalette.Custom) custom else palette.colors

    companion object {
        val GRID_SIZES: List<Int> = listOf(24, 32, 48, 64)

        val SPEED_RANGE: IntRange = 1..20
    }
}
