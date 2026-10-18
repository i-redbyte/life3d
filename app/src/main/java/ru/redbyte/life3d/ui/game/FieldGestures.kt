package ru.redbyte.life3d.ui.game

import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.calculateCentroidSize
import androidx.compose.foundation.gestures.calculatePan
import androidx.compose.foundation.gestures.calculateRotation
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChanged
import ru.redbyte.life3d.domain.FieldMode
import ru.redbyte.life3d.domain.OrbitCamera
import kotlin.math.abs

/**
 * Camera gestures for the field.
 *
 * - One finger: turns the field on a torus, moves through the world in an infinite field.
 * - Two fingers: twist turns, moving both up or down tilts, pinch zooms, in both modes.
 *
 * Movement counts only after the touch slop, so a tap with a trembling finger still reaches the
 * tap detector instead of nudging the camera.
 */
fun Modifier.fieldGestures(camera: OrbitCamera, mode: FieldMode, onTouch: () -> Unit): Modifier =
    pointerInput(camera, mode) {
        awaitEachGesture {
            awaitFirstDown(requireUnconsumed = false)
            var travelled = 0f
            var moving = false
            do {
                val event = awaitPointerEvent()
                val fingers = event.changes.count { it.pressed }
                val pan = event.calculatePan()
                if (!moving) {
                    travelled += abs(pan.x) + abs(pan.y) + abs(1f - event.calculateZoom()) * event.calculateCentroidSize()
                    moving = travelled > viewConfiguration.touchSlop
                }
                if (moving && fingers > 0) {
                    onTouch()
                    if (fingers >= 2) {
                        camera.turn(event.calculateRotation() * -RADIANS_PER_DEGREE, pan.y * TILT_PER_PIXEL)
                        camera.zoom(event.calculateZoom())
                    } else if (mode == FieldMode.Infinite) {
                        camera.pan(pan.x, pan.y, size.height.toFloat())
                    } else {
                        camera.turn(-pan.x * TURN_PER_PIXEL, pan.y * TURN_PER_PIXEL)
                    }
                    event.changes.forEach { if (it.positionChanged()) it.consume() }
                }
            } while (event.changes.any { it.pressed })
        }
    }

private const val TURN_PER_PIXEL = 0.008f
private const val TILT_PER_PIXEL = 0.006f
private const val RADIANS_PER_DEGREE = (Math.PI / 180.0).toFloat()
