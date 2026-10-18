package ru.redbyte.life3d.engine

import ru.redbyte.life3d.domain.FieldMode
import ru.redbyte.life3d.domain.InstancePacker
import ru.redbyte.life3d.domain.LifeSettings
import ru.redbyte.life3d.domain.PackedGeneration
import ru.redbyte.life3d.domain.Seeder
import ru.redbyte.life3d.domain.StartCells
import ru.redbyte.life3d.domain.WindowFollow
import ru.redbyte.life3d.gl.WORKGROUP_EDGE
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicReference
import kotlin.random.Random

/**
 * What the engine needs from the GPU. The app implements it with a RedByteFX `GlController`;
 * tests implement it on the CPU.
 */
interface LifeGpu {
    fun size(n: Int)

    /** True wraps every face around (a torus); false leaves nothing beyond the walls. */
    fun wrap(torus: Boolean)

    /** Moves the window by this many cells over the world before the next generation. */
    fun shift(x: Int, y: Int, z: Int)

    fun rules(values: FloatArray)

    /** Replaces both grids of the world. */
    fun upload(world: FloatArray)

    fun offsets(read: Int, write: Int)

    /** A radius of zero switches the spark off. */
    fun spark(x: Float, y: Float, z: Float, radius: Float, seed: Float)

    /** Runs one generation over [groups]³ workgroups. */
    fun dispatch(groups: Int)

    /** Copies the world into [into], then calls [done]; both may happen on another thread. */
    fun readBack(into: FloatArray, done: () -> Unit)
}

/**
 * Drives the game: seeds the world, runs one generation per [tick], and publishes each
 * generation, packed for drawing, as [latest].
 *
 * [tick] is called from the UI thread; read backs complete on the GL thread. A tick is refused
 * while the previous generation is still on its way back, so uniform writes for one generation
 * can never be merged with the next one's.
 */
class LifeEngine(
    private val settings: LifeSettings,
    private val gpu: LifeGpu,
    private val random: Random = Random.Default,
    private val clock: () -> Long = System::nanoTime,
) {
    val n: Int = settings.gridSize
    val mode: FieldMode = settings.mode
    private val total = n * n * n
    private val readBack = FloatArray(2 * total)
    private val inFlight = AtomicBoolean(false)
    private val pendingSpark = AtomicReference<FloatArray?>(null)

    /** Offset of the grid holding the latest generation. */
    @Volatile
    private var current = 0

    /** Bumped by [start], so a generation started before a reset is dropped when it returns. */
    @Volatile
    private var epoch = 0

    @Volatile
    var latest: PackedGeneration = PackedGeneration(FloatArray(0), 0, 0)
        private set

    /** [clock] time when [latest] arrived; the renderer animates births and deaths from it. */
    @Volatile
    var latestAt: Long = 0L
        private set

    @Volatile
    var generation: Int = 0
        private set

    /** Seeds a new world and shows it at once. Safe to call again to restart. */
    fun start() {
        epoch++
        val cells = StartCells.resolve(settings.start, n, random)
        val seed = Seeder.seed(n, cells, settings.rule.seedDensity, random)
        val world = FloatArray(2 * total)
        seed.copyInto(world)
        gpu.size(n)
        gpu.wrap(mode == FieldMode.Torus)
        gpu.rules(settings.rule.uniformValues())
        gpu.upload(world)
        current = 0
        generation = 0
        latest = InstancePacker.pack(world, 0, n)
        latestAt = clock()
        inFlight.set(false)
    }

    /**
     * Asks the next generation to bring cells to life around [cell], in world cell coordinates;
     * on a torus, world and window coordinates are the same.
     */
    fun spark(cell: FloatArray) {
        pendingSpark.set(cell.copyOf())
    }

    /** Starts the next generation; returns false while the previous one is still running. */
    fun tick(): Boolean {
        if (!inFlight.compareAndSet(false, true)) return false
        val started = epoch
        val previous = latest
        val read = current
        val write = if (read == 0) total else 0
        val shift = if (mode == FieldMode.Infinite) {
            WindowFollow.shiftFor(previous.centroid, previous.population, n)
        } else {
            IntArray(3)
        }
        val origin = IntArray(3) { axis -> previous.origin[axis] + shift[axis] }
        gpu.offsets(read, write)
        gpu.shift(shift[0], shift[1], shift[2])
        val spark = pendingSpark.getAndSet(null)
        if (spark != null) {
            gpu.spark(
                spark[0] - origin[0],
                spark[1] - origin[1],
                spark[2] - origin[2],
                sparkRadius(),
                random.nextFloat() * SPARK_SEED_RANGE,
            )
        } else {
            gpu.spark(0f, 0f, 0f, 0f, 0f)
        }
        gpu.dispatch(n / WORKGROUP_EDGE)
        gpu.readBack(readBack) {
            if (started == epoch) {
                latest = InstancePacker.pack(readBack, write, n, origin)
                latestAt = clock()
                generation++
                current = write
                inFlight.set(false)
            }
        }
        return true
    }

    private fun sparkRadius(): Float = maxOf(MIN_SPARK_RADIUS, n * SPARK_SHARE)

    private companion object {
        const val MIN_SPARK_RADIUS = 2.5f
        const val SPARK_SHARE = 0.12f
        const val SPARK_SEED_RANGE = 1_000f
    }
}
