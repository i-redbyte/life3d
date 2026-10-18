package ru.redbyte.life3d.engine

import ru.redbyte.life3d.domain.FieldMode
import ru.redbyte.life3d.domain.LifeSettings
import ru.redbyte.life3d.domain.OrbitCamera
import ru.redbyte.life3d.domain.PackedGeneration
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

/**
 * Draws one frame on the GL thread: the liquid first, without writing depth, then every visible
 * cell of [LifeEngine.latest] in a single instanced draw.
 *
 * Nothing here allocates per frame: the matrices are written into arrays kept for the whole run,
 * and the instance array is the one the engine published for the current generation.
 */
class LifeRenderer(
    private val scene: LifeScene,
    private val engine: LifeEngine,
    private val camera: OrbitCamera,
    private val settings: LifeSettings,
    private val clock: () -> Long = System::nanoTime,
) {
    /** The surface mesh: a quad covering the screen, drawn as the liquid. */
    val backgroundMesh: GlMesh = quad(2f, 2f)
    private val cellMesh = sphere(1f, CELL_STACKS, CELL_SLICES)
    private val background = backgroundModel()
    private val liquidPipeline = GlPipeline(depthMask = false)
    private val cellPipeline = GlPipeline(cullFace = CullFace.Back)
    private val view = FloatArray(MATRIX)
    private val projection = FloatArray(MATRIX)
    private val eye = FloatArray(3)
    private val interval = settings.intervalMillis * NANOS_PER_MILLI
    private var lastSeconds = 0f

    /** Nanoseconds of the last gesture; the camera drifts around the field when left alone. */
    @Volatile
    var touchedAt: Long = 0L

    @Volatile
    private var seconds = 0f

    fun draw(frame: GlFrame) {
        val runtime = frame.runtime
        val now = frame.seconds
        val step = (now - lastSeconds).coerceIn(0f, MAX_STEP)
        lastSeconds = now
        seconds = now
        if (clock() - touchedAt > IDLE_NANOS) camera.turn(step * IDLE_SPIN, 0f)

        val generation = engine.latest
        val torus = engine.mode == FieldMode.Torus
        if (!torus) keepNearColony(generation)

        camera.eye(eye)
        lookAt(eye[0], eye[1], eye[2], camera.targetX, camera.targetY, camera.targetZ, 0f, 1f, 0f, view)
        perspective(OrbitCamera.verticalFov(frame.aspect), frame.aspect, NEAR, FAR, projection)
        runtime.set(scene.view, view)
        runtime.set(scene.projection, projection)
        runtime.set(scene.eye, eye[0], eye[1], eye[2])
        runtime.set(scene.light, LIGHT_X, LIGHT_Y, LIGHT_Z)
        val palette = settings.palette
        runtime.set(scene.cellColor, palette.red, palette.green, palette.blue)
        runtime.set(scene.size, engine.n.toFloat())
        val drift = driftAt(now)
        runtime.set(scene.drift, drift * DRIFT_X, drift * DRIFT_Y, drift * DRIFT_Z)
        runtime.set(scene.phase, ((clock() - engine.latestAt).toFloat() / interval).coerceIn(0f, 1f))
        runtime.set(scene.time, now)
        runtime.set(scene.aspect, frame.aspect)
        runtime.set(scene.torus, if (torus) 1f else 0f)
        val origin = generation.origin
        runtime.set(scene.origin, origin[0].toFloat(), origin[1].toFloat(), origin[2].toFloat())
        runtime.set(scene.target, camera.targetX, camera.targetY, camera.targetZ)
        runtime.set(scene.focus, camera.distance)

        frame.draw(backgroundMesh, model = background, pipeline = liquidPipeline)
        val instances = generation.instances
        if (instances.isNotEmpty()) frame.draw(cellMesh, instances = instances, pipeline = cellPipeline)
    }

    /**
     * In an infinite field the camera may roam, but only so far from the colony: the target is
     * held within a little more than half a window of the live cells' centre.
     */
    private fun keepNearColony(generation: PackedGeneration) {
        val n = engine.n
        val centroid = generation.centroid
        val origin = generation.origin
        camera.keepNear(
            cellToWorld(origin[0] + centroid[0], n),
            cellToWorld(origin[1] + centroid[1], n),
            cellToWorld(origin[2] + centroid[2], n),
            ROAM_RADIUS,
        )
    }

    /** The current drift in cells, for turning a tap into the cell under it. */
    fun drift(): FloatArray {
        val drift = driftAt(seconds)
        return floatArrayOf(drift * DRIFT_X, drift * DRIFT_Y, drift * DRIFT_Z)
    }

    private fun driftAt(seconds: Float): Float =
        if (settings.flow && engine.mode == FieldMode.Torus) seconds * FLOW_CELLS_PER_SECOND else 0f

    private companion object {
        const val MATRIX = 16
        const val CELL_STACKS = 8
        const val CELL_SLICES = 12
        const val NEAR = 0.05f
        const val FAR = 20f
        const val NANOS_PER_MILLI = 1_000_000L
        const val MAX_STEP = 0.1f
        const val IDLE_NANOS = 3_000_000_000L
        const val IDLE_SPIN = 0.08f
        const val LIGHT_X = 0.45f
        const val LIGHT_Y = 0.8f
        const val LIGHT_Z = 0.4f
        const val FLOW_CELLS_PER_SECOND = 0.35f
        const val DRIFT_X = 0.8f
        const val DRIFT_Y = 0.35f
        const val DRIFT_Z = 0.5f
        const val ROAM_RADIUS = 1f
    }
}
