package ru.redbyte.life3d.domain

import org.junit.Assert.assertEquals
import org.junit.Test

class CpuLifeTest {

    private val n = 8

    @Test
    fun aTwoByTwoByTwoBlockIsAStillLifeUnderReef() {
        val world = FloatArray(n * n * n)
        for (z in 3..4) for (y in 3..4) for (x in 3..4) world[cellIndex(x, y, z, n)] = 1f

        val next = CpuLife.step(world, n, LifeRule.Reef)

        for (i in world.indices) {
            val expected = if (world[i] > 0f) 2f else 0f
            assertEquals("cell $i", expected, next[i])
        }
    }

    @Test
    fun aLonelyCellDiesAndIsMarkedAsJustDied() {
        val world = FloatArray(n * n * n)
        world[cellIndex(1, 1, 1, n)] = 7f

        val next = CpuLife.step(world, n, LifeRule.Bays4555)

        assertEquals(DYING, next[cellIndex(1, 1, 1, n)])
        val after = CpuLife.step(next, n, LifeRule.Bays4555)
        assertEquals(0f, after[cellIndex(1, 1, 1, n)])
    }

    @Test
    fun neighboursWrapAroundEveryFaceOfTheTorus() {
        val world = FloatArray(n * n * n)
        // Five live cells around the corner (0, 0, 0), all reached only through the wrap.
        world[cellIndex(n - 1, 0, 0, n)] = 1f
        world[cellIndex(0, n - 1, 0, n)] = 1f
        world[cellIndex(0, 0, n - 1, n)] = 1f
        world[cellIndex(n - 1, n - 1, 0, n)] = 1f
        world[cellIndex(n - 1, n - 1, n - 1, n)] = 1f

        assertEquals(5, CpuLife.neighbours(world, n, 0, 0, 0))
        val next = CpuLife.step(world, n, LifeRule.Bays4555)
        assertEquals("B5 gives birth at the corner", 1f, next[cellIndex(0, 0, 0, n)])
    }

    @Test
    fun anOpenFieldHasNothingBeyondItsWalls() {
        val world = FloatArray(n * n * n)
        world[cellIndex(n - 1, 0, 0, n)] = 1f
        world[cellIndex(0, n - 1, 0, n)] = 1f
        world[cellIndex(0, 0, n - 1, n)] = 1f

        assertEquals(0, CpuLife.neighbours(world, n, 0, 0, 0, FieldMode.Infinite))
        assertEquals(3, CpuLife.neighbours(world, n, 0, 0, 0, FieldMode.Torus))
    }

    @Test
    fun aShiftMovesTheWindowOverTheWorldBeforeTheGenerationRuns() {
        val world = FloatArray(n * n * n)
        for (z in 3..4) for (y in 3..4) for (x in 3..4) world[cellIndex(x, y, z, n)] = 1f

        val next = CpuLife.step(world, n, LifeRule.Reef, FieldMode.Infinite, shift = intArrayOf(2, -1, 0))

        // The window moved by (2, -1, 0) cells, so the still life now sits at (1..2, 4..5, 3..4).
        for (z in 0 until n) for (y in 0 until n) for (x in 0 until n) {
            val inside = x in 1..2 && y in 4..5 && z in 3..4
            assertEquals("($x, $y, $z)", if (inside) 2f else 0f, next[cellIndex(x, y, z, n)])
        }
    }

    @Test
    fun cellsShiftedPastAWallAreDropped() {
        val world = FloatArray(n * n * n)
        world[cellIndex(0, 0, 0, n)] = 3f

        val next = CpuLife.step(world, n, LifeRule.Reef, FieldMode.Infinite, shift = intArrayOf(1, 0, 0))

        assertEquals("the cell left the window, and one neighbour gives no birth", 0, next.count { it != 0f })
    }
}
