package ru.redbyte.life3d.domain

import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.random.Random

class SeederTest {

    @Test
    fun seedsExactlyTheRequestedNumberOfCellsAroundTheCentre() {
        val n = 24
        val world = Seeder.seed(n, cells = 600, density = 0.3f, random = Random(7))

        assertEquals(n * n * n, world.size)
        assertEquals(600, world.count { it == 1f })
        val centre = (n - 1) / 2f
        val radius = Seeder.ballRadius(600, 0.3f, n)
        for (z in 0 until n) for (y in 0 until n) for (x in 0 until n) {
            if (world[cellIndex(x, y, z, n)] == 0f) continue
            val dx = x - centre
            val dy = y - centre
            val dz = z - centre
            assertTrue(dx * dx + dy * dy + dz * dz <= (radius + 1f) * (radius + 1f))
        }
    }

    @Test
    fun aRequestLargerThanTheFieldFillsItWithoutLooping() {
        val n = 4
        val world = Seeder.seed(n, cells = 1_000, density = 0.3f, random = Random(1))

        assertEquals(n * n * n, world.count { it == 1f })
    }

    @Test
    fun theSameRandomGivesTheSameWorld() {
        val first = Seeder.seed(16, cells = 200, density = 0.4f, random = Random(42))
        val second = Seeder.seed(16, cells = 200, density = 0.4f, random = Random(42))

        assertArrayEquals(first, second, 0f)
    }

    @Test
    fun aRandomStartPicksACountInsideTheAllowedRange() {
        val range = StartCells.range(24)
        repeat(50) { seed ->
            val count = StartCells.resolve(StartCells(count = 10, random = true), 24, Random(seed))
            assertTrue(count in range)
        }
        assertEquals(range.first, StartCells.resolve(StartCells(count = 1, random = false), 24, Random(0)))
    }
}
