package ru.redbyte.life3d.domain

import kotlin.math.atan
import kotlin.math.cos
import kotlin.math.floor
import kotlin.math.sin
import kotlin.math.sqrt
import kotlin.math.tan

class OrbitCamera {
    @Volatile
    var yaw: Float = 0f
        private set

    @Volatile
    var pitch: Float = DEFAULT_PITCH
        private set

    @Volatile
    var distance: Float = DEFAULT_DISTANCE
        private set

    @Volatile
    var targetX: Float = 0f
        private set

    @Volatile
    var targetY: Float = 0f
        private set

    @Volatile
    var targetZ: Float = 0f
        private set

    fun turn(deltaYaw: Float, deltaPitch: Float) {
        yaw += deltaYaw
        pitch = (pitch + deltaPitch).coerceIn(-MAX_PITCH, MAX_PITCH)
    }

    fun zoom(factor: Float) {
        if (factor <= 0f) return
        distance = (distance / factor).coerceIn(MIN_DISTANCE, MAX_DISTANCE)
    }

    fun pan(dx: Float, dy: Float, height: Float) {
        if (height <= 0f) return
        val worldPerPixel = 2f * distance * tan(FIELD_OF_VIEW / 2f) / height
        val basis = basis()
        val right = basis[1]
        val up = basis[2]
        targetX += (-dx * right[0] + dy * up[0]) * worldPerPixel
        targetY += (-dx * right[1] + dy * up[1]) * worldPerPixel
        targetZ += (-dx * right[2] + dy * up[2]) * worldPerPixel
    }

    fun moveTargetTo(x: Float, y: Float, z: Float) {
        targetX = x
        targetY = y
        targetZ = z
    }

    fun keepNear(centreX: Float, centreY: Float, centreZ: Float, radius: Float) {
        val dx = targetX - centreX
        val dy = targetY - centreY
        val dz = targetZ - centreZ
        val length = sqrt(dx * dx + dy * dy + dz * dz)
        if (length <= radius) return
        val scale = radius / length
        moveTargetTo(centreX + dx * scale, centreY + dy * scale, centreZ + dz * scale)
    }

    fun eye(out: FloatArray = FloatArray(3)): FloatArray {
        val d = distance
        val p = pitch
        val y = yaw
        out[0] = targetX + d * cos(p) * sin(y)
        out[1] = targetY + d * sin(p)
        out[2] = targetZ + d * cos(p) * cos(y)
        return out
    }

    fun pick(tapX: Float, tapY: Float, width: Float, height: Float): FloatArray {
        val eye = eye()
        val (forward, right, up) = basis()
        val ndcX = 2f * tapX / width - 1f
        val ndcY = 1f - 2f * tapY / height
        val aspect = width / height
        val tanHalf = tan(verticalFov(aspect) / 2f)
        val sx = ndcX * tanHalf * aspect
        val sy = ndcY * tanHalf
        val dir = normalize(
            forward[0] + right[0] * sx + up[0] * sy,
            forward[1] + right[1] * sx + up[1] * sy,
            forward[2] + right[2] * sx + up[2] * sy,
        )
        val toTarget = floatArrayOf(targetX - eye[0], targetY - eye[1], targetZ - eye[2])
        val t = toTarget[0] * dir[0] + toTarget[1] * dir[1] + toTarget[2] * dir[2]
        return FloatArray(3) { axis -> eye[axis] + dir[axis] * t }
    }

    private fun basis(): Array<FloatArray> {
        val eye = eye()
        val forward = normalize(targetX - eye[0], targetY - eye[1], targetZ - eye[2])
        val right = normalize(cross(forward, UP))
        val up = cross(right, forward)
        return arrayOf(forward, right, up)
    }

    companion object {
        const val FIELD_OF_VIEW: Float = 0.9f
        const val MAX_PITCH: Float = 1.35f
        const val MIN_DISTANCE: Float = 0.6f
        const val MAX_DISTANCE: Float = 5f
        private const val DEFAULT_PITCH = 0.35f
        private const val DEFAULT_DISTANCE = 2.4f
        private val UP = floatArrayOf(0f, 1f, 0f)

        fun verticalFov(aspect: Float): Float =
            if (aspect >= 1f) FIELD_OF_VIEW else 2f * atan(tan(FIELD_OF_VIEW / 2f) / aspect)
    }
}

fun worldToCell(point: FloatArray): FloatArray = FloatArray(3) { axis -> point[axis] / CELL_SIZE - 0.5f }

fun cellToWorld(cell: Float): Float = (cell + 0.5f) * CELL_SIZE

fun torusCell(cell: FloatArray, n: Int): FloatArray = FloatArray(3) { axis -> cell[axis] - floor(cell[axis] / n) * n }

private fun cross(a: FloatArray, b: FloatArray): FloatArray = floatArrayOf(
    a[1] * b[2] - a[2] * b[1],
    a[2] * b[0] - a[0] * b[2],
    a[0] * b[1] - a[1] * b[0],
)

private fun normalize(v: FloatArray): FloatArray = normalize(v[0], v[1], v[2])

private fun normalize(x: Float, y: Float, z: Float): FloatArray {
    val length = sqrt(x * x + y * y + z * z)
    return floatArrayOf(x / length, y / length, z / length)
}
