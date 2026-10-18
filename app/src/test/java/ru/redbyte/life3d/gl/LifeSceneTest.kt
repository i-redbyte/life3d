package ru.redbyte.life3d.gl

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class LifeSceneTest {
    private val scene = lifeScene()

    @Test
    fun cellsAreReadFromTheFirstColumnOfTheInstanceMatrix() {
        val vertex = scene.program.vertexSource()

        assertTrue(vertex, vertex.contains("a_model0"))
        assertTrue(vertex, vertex.contains("distance("))
    }

    @Test
    fun theBackgroundIsMarkedBelowAnyCellValue() {
        val model = backgroundModel()

        assertEquals(BACKGROUND_MARK, model[3])
        assertTrue(BACKGROUND_MARK < -500f)
        assertTrue(scene.program.fragmentSource().contains("sin("))
    }
}
