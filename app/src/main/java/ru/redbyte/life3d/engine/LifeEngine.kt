package ru.redbyte.life3d.engine

import ru.redbyte.life3d.domain.FieldMode
import ru.redbyte.life3d.domain.Generation
import ru.redbyte.life3d.domain.LifeSettings
import ru.redbyte.life3d.domain.Seeder
import ru.redbyte.life3d.domain.StartCells
import ru.redbyte.life3d.domain.WindowFollow
import ru.redbyte.life3d.gl.WORKGROUP_EDGE
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicReference
import kotlin.random.Random

interface LifeGpu {
    fun size(n: Int)

    fun wrap(torus: Boolean)

    fun shift(x: Int, y: Int, z: Int)

    fun rules(values: FloatArray)

    fun decay(states: Int)

    fun upload(world: FloatArray)

    fun offsets(read: Int, write: Int)

    fun spark(x: Float, y: Float, z: Float, radius: Float, seed: Float)

    fun dispatch(groups: Int)

    fun readBack(into: FloatArray, done: () -> Unit)
}

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

    @Volatile
    private var current = 0

    @Volatile
    private var epoch = 0

    @Volatile
    var latest: Generation = Generation.of(FloatArray(total), 0, n, IntArray(3), number = 0)
        private set

    @Volatile
    var latestAt: Long = 0L
        private set

    val generation: Int get() = latest.number

    fun start() {
        epoch++
        val cells = StartCells.resolve(settings.start, n, random)
        val seed = Seeder.seed(n, cells, settings.rule.seedDensity, random)
        val world = FloatArray(2 * total)
        seed.copyInto(world)
        gpu.size(n)
        gpu.wrap(mode == FieldMode.Torus)
        gpu.rules(settings.rule.rule.uniformValues())
        gpu.decay(settings.rule.rule.decay)
        gpu.upload(world)
        current = 0
        latest = Generation.of(world, 0, n, IntArray(3), number = 0)
        latestAt = clock()
        inFlight.set(false)
    }

    fun spark(cell: FloatArray) {
        pendingSpark.set(cell.copyOf())
    }

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
                latest = Generation.of(readBack, write, n, origin, previous.number + 1)
                latestAt = clock()
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
