package ru.redbyte.life3d.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CellViewTest {
    private fun generation(n: Int, origin: IntArray = IntArray(3), fill: (FloatArray) -> Unit): Generation {
        val cells = FloatArray(n * n * n).also(fill)
        return Generation.of(cells, 0, n, origin, number = 0)
    }

    private fun CellView.points(): List<List<Float>> =
        (0 until count).map { i -> (0..3).map { instances[i * INSTANCE_FLOATS + it] } }

    @Test
    fun aTorusShowsEveryCopyOfACellWithinTheRadius() {
        val n = 8
        val gen = generation(n) { it[cellIndex(1, 1, 1, n)] = 3f }

        val view = CellView.around(gen, FieldMode.Torus, focus = floatArrayOf(1f, 1f, 1f), radius = 9f, cap = 1_000)

        val points = view.points()
        assertTrue(listOf(1f, 1f, 1f, 3f) in points)
        assertTrue(listOf(9f, 1f, 1f, 3f) in points)
        assertTrue(listOf(-7f, 1f, 1f, 3f) in points)
        assertTrue(listOf(9f, 9f, 1f, 3f) !in points)
        for (p in points) {
            val d2 = (p[0] - 1f) * (p[0] - 1f) + (p[1] - 1f) * (p[1] - 1f) + (p[2] - 1f) * (p[2] - 1f)
            assertTrue(d2 <= 81f)
        }
    }

    @Test
    fun anInfiniteFieldPlacesCellsAtTheirWorldCellsWithoutCopies() {
        val n = 8
        val gen = generation(n, origin = intArrayOf(100, -50, 3)) {
            it[cellIndex(2, 3, 4, n)] = 1f
            it[cellIndex(5, 5, 5, n)] = -2f
        }

        val view = CellView.around(gen, FieldMode.Infinite, focus = floatArrayOf(103f, -47f, 7f), radius = 40f, cap = 1_000)

        assertEquals(setOf(listOf(102f, -47f, 7f, 1f), listOf(105f, -45f, 8f, -2f)), view.points().toSet())
    }

    @Test
    fun emptyCellsAreNeverDrawn() {
        val view = CellView.around(generation(4) {}, FieldMode.Torus, floatArrayOf(0f, 0f, 0f), 10f, 100)

        assertEquals(0, view.count)
        assertEquals(0, view.instances.size)
    }

    @Test
    fun tooManyCellsShrinkTheRadiusSoTheNearestOnesStay() {
        val n = 16
        val gen = generation(n) { cells -> cells.fill(1f) }

        val view = CellView.around(gen, FieldMode.Infinite, floatArrayOf(8f, 8f, 8f), radius = 20f, cap = 500)

        assertTrue("${view.count}", view.count in 1..500)
        assertTrue(view.radius < 20f)
        for (p in view.points()) {
            val d2 = (p[0] - 8f) * (p[0] - 8f) + (p[1] - 8f) * (p[1] - 8f) + (p[2] - 8f) * (p[2] - 8f)
            assertTrue(d2 <= view.radius * view.radius)
        }
    }
}
