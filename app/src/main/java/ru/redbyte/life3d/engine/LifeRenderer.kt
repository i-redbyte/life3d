package ru.redbyte.life3d.engine

import ru.redbyte.life3d.domain.CELL_SIZE
import ru.redbyte.life3d.domain.CellView
import ru.redbyte.life3d.domain.FieldMode
import ru.redbyte.life3d.domain.Generation
import ru.redbyte.life3d.domain.LifeSettings
import ru.redbyte.life3d.domain.OrbitCamera
import ru.redbyte.life3d.domain.cellToWorld
import ru.redbyte.life3d.gl.LifeScene
import ru.redbyte.life3d.gl.backgroundModel
import ru.redbyte.redbytefx.gl.compose.CullFace
import ru.redbyte.redbytefx.gl.compose.GlFrame
import ru.redbyte.redbytefx.gl.compose.GlMesh
import ru.redbyte.redbytefx.gl.compose.GlPipeline
import ru.redbyte.redbytefx.gl.compose.quad
import ru.redbyte.redbytefx.gl.compose.sphere
import ru.redbyte.redbytefx.scene.lookAt
import ru.redbyte.redbytefx.scene.perspective
import kotlin.math.sqrt

class LifeRenderer(
    private val scene: LifeScene,
    private val engine: LifeEngine,
    private val camera: OrbitCamera,
    private val settings: LifeSettings,
    private val clock: () -> Long = System::nanoTime,
) {
    val backgroundMesh: GlMesh = quad(2f, 2f)
    private val cellMesh = sphere(1f, CELL_STACKS, CELL_SLICES)
    private val background = backgroundModel()
    private val liquidPipeline = GlPipeline(depthMask = false)
    private val cellPipeline = GlPipeline(cullFace = CullFace.Back)
    private val viewMatrix = FloatArray(MATRIX)
    private val projection = FloatArray(MATRIX)
    private val eye = FloatArray(3)
    private val focus = FloatArray(3)
    private val interval = settings.intervalMillis * NANOS_PER_MILLI
    private val decay = settings.rule.rule.decay.toFloat()
    private var lastSeconds = 0f
    private var cells: CellView? = null
    private var cellsOf: Generation? = null

    @Volatile
    var touchedAt: Long = 0L

    init {
        lookAtColony(engine.latest)
    }

    fun draw(frame: GlFrame) {
        val runtime = frame.runtime
        val now = frame.seconds
        val step = (now - lastSeconds).coerceIn(0f, MAX_STEP)
        lastSeconds = now
        if (clock() - touchedAt > IDLE_NANOS) camera.turn(step * IDLE_SPIN, 0f)

        val generation = engine.latest
        val torus = engine.mode == FieldMode.Torus
        if (!torus) keepNearColony(generation)
        val drawn = cellsAround(generation)

        camera.eye(eye)
        lookAt(eye[0], eye[1], eye[2], camera.targetX, camera.targetY, camera.targetZ, 0f, 1f, 0f, viewMatrix)
        perspective(OrbitCamera.verticalFov(frame.aspect), frame.aspect, NEAR, FAR, projection)
        runtime.set(scene.view, viewMatrix)
        runtime.set(scene.projection, projection)
        runtime.set(scene.eye, eye[0], eye[1], eye[2])
        runtime.set(scene.light, LIGHT_X, LIGHT_Y, LIGHT_Z)
        val palette = settings.palette
        runtime.set(scene.cellColor, palette.red, palette.green, palette.blue)
        runtime.set(scene.phase, ((clock() - engine.latestAt).toFloat() / interval).coerceIn(0f, 1f))
        runtime.set(scene.time, now)
        runtime.set(scene.aspect, frame.aspect)
        runtime.set(scene.size, engine.n.toFloat())
        runtime.set(scene.torus, if (torus) 1f else 0f)
        runtime.set(scene.decay, decay)
        val origin = generation.origin
        runtime.set(scene.origin, origin[0].toFloat(), origin[1].toFloat(), origin[2].toFloat())
        runtime.set(scene.viewCentre, drawn.centre[0], drawn.centre[1], drawn.centre[2])
        runtime.set(scene.viewRadius, drawn.radius)

        val reach = drawn.radius * CELL_SIZE
        runtime.set(scene.fogNear, camera.distance * FOG_NEAR_SHARE)
        runtime.set(scene.fogFar, camera.distance + reach * FOG_FAR_SHARE)
        runtime.set(scene.target, camera.targetX, camera.targetY, camera.targetZ)

        frame.draw(backgroundMesh, model = background, pipeline = liquidPipeline)
        if (drawn.count > 0) frame.draw(cellMesh, instances = drawn.instances, pipeline = cellPipeline)
    }

    private fun cellsAround(generation: Generation): CellView {
        focus[0] = camera.targetX / CELL_SIZE - 0.5f
        focus[1] = camera.targetY / CELL_SIZE - 0.5f
        focus[2] = camera.targetZ / CELL_SIZE - 0.5f
        val current = cells
        if (current != null && cellsOf === generation && distance(current.centre, focus) < REBUILD_DISTANCE) {
            return current
        }
        return CellView.around(generation, engine.mode, focus, VIEW_RADIUS, MAX_CELLS).also {
            cells = it
            cellsOf = generation
        }
    }

    private fun lookAtColony(generation: Generation) {
        val c = generation.centroid
        val o = generation.origin
        camera.moveTargetTo(cellToWorld(o[0] + c[0]), cellToWorld(o[1] + c[1]), cellToWorld(o[2] + c[2]))
    }

    private fun keepNearColony(generation: Generation) {
        val c = generation.centroid
        val o = generation.origin
        camera.keepNear(cellToWorld(o[0] + c[0]), cellToWorld(o[1] + c[1]), cellToWorld(o[2] + c[2]), ROAM_RADIUS)
    }

    private fun distance(a: FloatArray, b: FloatArray): Float {
        val dx = a[0] - b[0]
        val dy = a[1] - b[1]
        val dz = a[2] - b[2]
        return sqrt(dx * dx + dy * dy + dz * dz)
    }

    private companion object {
        const val MATRIX = 16
        const val CELL_STACKS = 6
        const val CELL_SLICES = 10
        const val NEAR = 0.02f
        const val FAR = 30f
        const val NANOS_PER_MILLI = 1_000_000L
        const val MAX_STEP = 0.1f
        const val IDLE_NANOS = 3_000_000_000L
        const val IDLE_SPIN = 0.06f
        const val LIGHT_X = 0.45f
        const val LIGHT_Y = 0.8f
        const val LIGHT_Z = 0.4f

        const val VIEW_RADIUS = 30f
        const val MAX_CELLS = 16_000

        const val REBUILD_DISTANCE = 4f
        const val FOG_NEAR_SHARE = 0.55f
        const val FOG_FAR_SHARE = 0.7f

        const val ROAM_RADIUS = 1.8f
    }
}
