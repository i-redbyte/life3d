package ru.redbyte.life3d.domain

import org.junit.Assert.assertEquals
import org.junit.Test

class CpuLifeTest {
    private val REEF = Rule(birth = setOf(6, 7, 9, 12), survive = setOf(5, 6, 7, 8), states = 2)

    private val n = 8

    @Test
    fun aTwoByTwoByTwoBlockIsAStillLifeUnderReef() {
        val world = FloatArray(n * n * n)
        for (z in 3..4) for (y in 3..4) for (x in 3..4) world[cellIndex(x, y, z, n)] = 1f

        val next = CpuLife.step(world, n, REEF)

        for (i in world.indices) {
            val expected = if (world[i] > 0f) 2f else 0f
            assertEquals("cell $i", expected, next[i])
        }
    }

    @Test
    fun aLonelyCellDiesAndIsMarkedAsJustDied() {
        val world = FloatArray(n * n * n)
        world[cellIndex(1, 1, 1, n)] = 7f

        val next = CpuLife.step(world, n, LifeRule.Bays4555.rule)

        assertEquals(DYING, next[cellIndex(1, 1, 1, n)])
        val after = CpuLife.step(next, n, LifeRule.Bays4555.rule)
        assertEquals(0f, after[cellIndex(1, 1, 1, n)])
    }

    @Test
    fun neighboursWrapAroundEveryFaceOfTheTorus() {
        val world = FloatArray(n * n * n)

        world[cellIndex(n - 1, 0, 0, n)] = 1f
        world[cellIndex(0, n - 1, 0, n)] = 1f
        world[cellIndex(0, 0, n - 1, n)] = 1f
        world[cellIndex(n - 1, n - 1, 0, n)] = 1f
        world[cellIndex(n - 1, n - 1, n - 1, n)] = 1f

        assertEquals(5, CpuLife.neighbours(world, n, 0, 0, 0))
        val next = CpuLife.step(world, n, LifeRule.Bays4555.rule)
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

        val next = CpuLife.step(world, n, REEF, FieldMode.Infinite, shift = intArrayOf(2, -1, 0))

        for (z in 0 until n) for (y in 0 until n) for (x in 0 until n) {
            val inside = x in 1..2 && y in 4..5 && z in 3..4
            assertEquals("($x, $y, $z)", if (inside) 2f else 0f, next[cellIndex(x, y, z, n)])
        }
    }

    @Test
    fun cellsShiftedPastAWallAreDropped() {
        val world = FloatArray(n * n * n)
        world[cellIndex(0, 0, 0, n)] = 3f

        val next = CpuLife.step(world, n, REEF, FieldMode.Infinite, shift = intArrayOf(1, 0, 0))

        assertEquals("the cell left the window, and one neighbour gives no birth", 0, next.count { it != 0f })
    }

    @Test
    fun withDecayALiveCellThatFailsPassesThroughRefractoryStatesBeforeItCanBeBornAgain() {
        val rule = Rule(birth = setOf(1), survive = emptySet(), states = 4)
        val world = FloatArray(n * n * n)
        world[cellIndex(4, 4, 4, n)] = 5f
        world[cellIndex(2, 2, 2, n)] = -2f
        world[cellIndex(3, 2, 2, n)] = 1f

        val next = CpuLife.step(world, n, rule)

        assertEquals("starts decaying at -(C-2)", -2f, next[cellIndex(4, 4, 4, n)])
        assertEquals("decays toward zero and is not reborn", -1f, next[cellIndex(2, 2, 2, n)])
        val after = CpuLife.step(next, n, rule)
        assertEquals(-1f, after[cellIndex(4, 4, 4, n)])
        assertEquals("the last refractory state turns empty", 0f, after[cellIndex(2, 2, 2, n)])
    }

    @Test
    fun withoutDecayAJustDiedCellIsOnlyAMarkerAndMayBeBornAtOnce() {
        val rule = Rule(birth = setOf(1), survive = emptySet(), states = 2)
        val world = FloatArray(n * n * n)
        world[cellIndex(2, 2, 2, n)] = DYING
        world[cellIndex(3, 2, 2, n)] = 1f

        val next = CpuLife.step(world, n, rule)

        assertEquals(1f, next[cellIndex(2, 2, 2, n)])
        assertEquals(DYING, next[cellIndex(3, 2, 2, n)])
    }
}
