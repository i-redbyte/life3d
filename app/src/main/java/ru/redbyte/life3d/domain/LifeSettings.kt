package ru.redbyte.life3d.domain

enum class CellPalette(val red: Float, val green: Float, val blue: Float) {
    Turquoise(0.25f, 0.95f, 0.78f),
    Amber(1f, 0.68f, 0.2f),
    Fuchsia(1f, 0.3f, 0.7f),
    Azure(0.3f, 0.62f, 1f),
    Lime(0.62f, 1f, 0.3f),
    Coral(1f, 0.45f, 0.38f),
}

data class LifeSettings(
    val palette: CellPalette = CellPalette.Turquoise,
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

    companion object {
        val GRID_SIZES: List<Int> = listOf(24, 32, 48, 64)

        val SPEED_RANGE: IntRange = 1..20
    }
}
