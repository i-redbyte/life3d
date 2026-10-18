package ru.redbyte.life3d.domain

import kotlin.math.PI
import kotlin.math.cbrt
import kotlin.random.Random

/** How many cells the first generation starts with; [random] picks a count from [range]. */
data class StartCells(val count: Int, val random: Boolean) {
    companion object {
        private const val MIN_CELLS = 20

        /** Allowed start counts for an [n]³ field: from a small seed up to a quarter of the field. */
        fun range(n: Int): IntRange = MIN_CELLS..(n * n * n / 4).coerceAtLeast(MIN_CELLS)

        fun resolve(start: StartCells, n: Int, random: Random): Int {
            val range = range(n)
            return if (start.random) random.nextInt(range.first, range.last + 1) else start.count.coerceIn(range)
        }
    }
}

/** Places the first generation as a random cloud in the middle of the field. */
object Seeder {

    /**
     * An [n]³ world with exactly [cells] live cells (or every cell, if the field is smaller),
     * scattered inside a ball sized so that it is about [density] full.
     */
    fun seed(n: Int, cells: Int, density: Float, random: Random): FloatArray {
        val world = FloatArray(n * n * n)
        val wanted = cells.coerceIn(0, world.size)
        val centre = (n - 1) / 2f
        var radius = ballRadius(wanted, density, n)
        var candidates = cellsWithin(n, centre, radius)
        while (candidates.size < wanted) {
            radius += RADIUS_STEP
            candidates = cellsWithin(n, centre, radius)
        }
        candidates.shuffle(random)
        for (i in 0 until wanted) world[candidates[i]] = 1f
        return world
    }

    /** Radius of a ball whose volume holds [cells] cells at [density]; never wider than the field. */
    fun ballRadius(cells: Int, density: Float, n: Int): Float {
        val volume = cells / density.coerceIn(MIN_DENSITY, 1f)
        return cbrt(3f * volume / (4f * PI.toFloat())).coerceAtMost(n.toFloat())
    }

    private fun cellsWithin(n: Int, centre: Float, radius: Float): MutableList<Int> {
        val limit = radius * radius
        val inside = mutableListOf<Int>()
        for (z in 0 until n) for (y in 0 until n) for (x in 0 until n) {
            val dx = x - centre
            val dy = y - centre
            val dz = z - centre
            if (dx * dx + dy * dy + dz * dz <= limit) inside += cellIndex(x, y, z, n)
        }
        return inside
    }

    private const val RADIUS_STEP = 0.5f
    private const val MIN_DENSITY = 0.01f
}
