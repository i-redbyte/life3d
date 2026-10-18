package ru.redbyte.life3d.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.abs
import kotlin.math.sqrt

class OrbitCameraTest {

    @Test
    fun aTapInTheMiddleOfTheScreenAimsAtTheTarget() {
        val camera = OrbitCamera()
        camera.turn(0.7f, -0.3f)

        val centre = camera.pick(tapX = 540f, tapY = 960f, width = 1080f, height = 1920f)
        assertEquals(0f, centre[0], 1e-3f)
        assertEquals(0f, centre[1], 1e-3f)
        assertEquals(0f, centre[2], 1e-3f)

        camera.pan(dx = 0f, dy = 0f, height = 1920f)
        camera.moveTargetTo(0.4f, -0.2f, 1.5f)
        val moved = camera.pick(tapX = 540f, tapY = 960f, width = 1080f, height = 1920f)
        assertEquals(0.4f, moved[0], 1e-3f)
        assertEquals(-0.2f, moved[1], 1e-3f)
        assertEquals(1.5f, moved[2], 1e-3f)
    }

    @Test
    fun aTapAwayFromTheCentreLandsOnTheMatchingSide() {
        val camera = OrbitCamera()

        val target = camera.pick(tapX = 0f, tapY = 0f, width = 1080f, height = 1920f)

        assertTrue("left of centre", target[0] < 0f)
        assertTrue("above centre", target[1] > 0f)
    }

    @Test
    fun draggingRightMovesTheWorldRightSoTheTargetGoesLeft() {
        val camera = OrbitCamera()

        camera.pan(dx = 200f, dy = 0f, height = 1920f)

        assertTrue(camera.targetX < 0f)
        assertEquals(0f, camera.targetY, 1e-4f)
    }

    @Test
    fun theTargetCannotWanderFarFromTheColony() {
        val camera = OrbitCamera()
        camera.moveTargetTo(5f, 0f, 0f)

        camera.keepNear(centreX = 1f, centreY = 0f, centreZ = 0f, radius = 0.5f)

        val dx = camera.targetX - 1f
        assertEquals(0.5f, sqrt(dx * dx + camera.targetY * camera.targetY + camera.targetZ * camera.targetZ), 1e-4f)
    }

    @Test
    fun aPortraitScreenWidensTheVerticalFieldOfViewSoTheFieldFitsAcross() {
        assertEquals(OrbitCamera.FIELD_OF_VIEW, OrbitCamera.verticalFov(aspect = 1.6f), 1e-6f)
        assertTrue(OrbitCamera.verticalFov(aspect = 0.5f) > OrbitCamera.FIELD_OF_VIEW)
    }

    @Test
    fun pitchAndZoomAreClamped() {
        val camera = OrbitCamera()
        camera.turn(0f, 100f)
        camera.zoom(100f)

        assertTrue(camera.pitch <= OrbitCamera.MAX_PITCH)
        assertTrue(camera.distance >= OrbitCamera.MIN_DISTANCE)
        camera.zoom(0.0001f)
        assertTrue(camera.distance <= OrbitCamera.MAX_DISTANCE)
    }

    @Test
    fun aFieldPointMapsToTheCellUnderItAfterTheDrift() {
        val cell = fieldToCell(floatArrayOf(-0.9f, 0.1f, 0.5f), n = 10, drift = floatArrayOf(0f, 0f, 0f))
        assertEquals(0f, cell[0], 1e-4f)
        assertEquals(5f, cell[1], 1e-4f)
        assertEquals(7f, cell[2], 1e-4f)

        val drifted = fieldToCell(floatArrayOf(-0.9f, 0.1f, 0.1f), n = 10, drift = floatArrayOf(3f, 0f, 0f))
        assertEquals("wraps below zero", 7f, drifted[0], 1e-4f)
    }

    @Test
    fun aWorldPointOfAnInfiniteFieldMapsToAWorldCellWithoutWrapping() {
        val cell = worldToCell(floatArrayOf(-2.9f, 0.1f, 3.1f), n = 10)

        assertEquals(-10f, cell[0], 1e-4f)
        assertEquals(5f, cell[1], 1e-4f)
        assertEquals(20f, cell[2], 1e-4f)
        assertTrue(abs(cellToWorld(-10f, 10) + 2.9f) < 1e-4f)
    }
}
