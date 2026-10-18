package ru.redbyte.life3d.domain

import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Test

class WindowFollowTest {
    @Test
    fun aColonyNearTheMiddleKeepsTheWindowInPlace() {
        assertArrayEquals(intArrayOf(0, 0, 0), WindowFollow.shiftFor(floatArrayOf(12.5f, 10f, 13f), population = 50, n = 24))
    }

    @Test
    fun aColonyDriftingAwayPullsTheWindowAfterItAtMostAQuarterAtATime() {
        val shift = WindowFollow.shiftFor(floatArrayOf(18f, 11.5f, 1f), population = 50, n = 24)

        assertArrayEquals(intArrayOf(6, 0, -6), shift)
    }

    @Test
    fun anEmptyWorldNeverMovesTheWindow() {
        assertArrayEquals(intArrayOf(0, 0, 0), WindowFollow.shiftFor(floatArrayOf(0f, 0f, 0f), population = 0, n = 24))
    }

    @Test
    fun aGenerationRecordsTheCentroidOfLiveCellsAndItsCounts() {
        val n = 4
        val cells = FloatArray(n * n * n)
        cells[cellIndex(0, 0, 0, n)] = 1f
        cells[cellIndex(2, 2, 0, n)] = 4f
        cells[cellIndex(3, 3, 3, n)] = DYING

        val generation = Generation.of(cells, offset = 0, n = n, origin = intArrayOf(5, -2, 7), number = 3)

        assertArrayEquals(floatArrayOf(1f, 1f, 0f), generation.centroid, 1e-6f)
        assertArrayEquals(intArrayOf(5, -2, 7), generation.origin)
        assertEquals(2, generation.population)
        assertEquals(1, generation.fading)
        assertEquals(3, generation.number)
    }
}
