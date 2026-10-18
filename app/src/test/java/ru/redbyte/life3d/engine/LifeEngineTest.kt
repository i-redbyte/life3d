package ru.redbyte.life3d.engine

import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import ru.redbyte.life3d.domain.CpuLife
import ru.redbyte.life3d.domain.FieldMode
import ru.redbyte.life3d.domain.cellIndex
import ru.redbyte.life3d.domain.LifeRule
import ru.redbyte.life3d.domain.LifeSettings
import ru.redbyte.life3d.domain.StartCells
import kotlin.random.Random

class LifeEngineTest {
    private val settings = LifeSettings(gridSize = 24, rule = LifeRule.Bays4555, start = StartCells(300, false))
    private val n = settings.gridSize
    private val total = n * n * n

    @Test
    fun theFirstGenerationIsTheSeedAndIsVisibleBeforeAnyDispatch() {
        val gpu = CpuGpu(deferred = false)
        val engine = LifeEngine(settings, gpu, Random(3))

        engine.start()

        assertEquals(0, engine.generation)
        assertEquals(300, engine.latest.population)
        assertEquals(n, gpu.size)
        assertArrayEquals(settings.rule.rule.uniformValues(), gpu.rules, 0f)
        assertEquals(2 * total, gpu.world.size)
    }

    @Test
    fun eachTickRunsOneGenerationAndSwapsTheHalves() {
        val gpu = CpuGpu(deferred = false)
        val engine = LifeEngine(settings, gpu, Random(3))
        engine.start()
        val seed = gpu.world.copyOfRange(0, total)

        assertTrue(engine.tick())

        assertEquals(listOf(0 to total), gpu.offsets)
        assertEquals(n / 4, gpu.groups)
        val expected = CpuLife.step(seed, n, settings.rule.rule)
        assertArrayEquals(expected, engine.latest.cells, 0f)
        assertEquals(expected.count { it > 0.5f }, engine.latest.population)
        assertEquals(1, engine.generation)

        engine.tick()
        assertEquals(total to 0, gpu.offsets.last())
        assertEquals(2, engine.generation)
    }

    @Test
    fun aTickWaitsUntilThePreviousGenerationHasComeBack() {
        val gpu = CpuGpu(deferred = true)
        val engine = LifeEngine(settings, gpu, Random(3))
        engine.start()

        assertTrue(engine.tick())
        assertFalse(engine.tick())
        assertEquals(1, gpu.offsets.size)

        gpu.finish()
        assertTrue(engine.tick())
        assertEquals(2, gpu.offsets.size)
    }

    @Test
    fun aSparkIsSentWithOneGenerationAndThenSwitchedOff() {
        val gpu = CpuGpu(deferred = false)
        val engine = LifeEngine(settings, gpu, Random(3))
        engine.start()

        engine.spark(floatArrayOf(3f, 4f, 5f))
        engine.tick()
        val sent = gpu.sparks.last()
        assertEquals(listOf(3f, 4f, 5f), sent.take(3))
        assertTrue("radius", sent[3] > 0f)

        engine.tick()
        assertEquals(0f, gpu.sparks.last()[3])
    }

    @Test
    fun inAnInfiniteFieldTheWindowFollowsTheColonyAndKeepsItsWorldPlace() {
        val open = settings.copy(mode = FieldMode.Infinite, rule = LifeRule.Reef)
        val gpu = CpuGpu(deferred = false)
        val engine = LifeEngine(open, gpu, Random(3))
        engine.start()
        assertEquals(listOf(false), gpu.wraps)

        val corner = FloatArray(2 * total)
        for (z in 0..1) for (y in 0..1) for (x in 0..1) corner[cellIndex(x, y, z, n)] = 1f
        gpu.upload(corner)
        engine.tick()
        assertEquals(listOf(0, 0, 0), engine.latest.origin.toList())
        assertEquals(listOf(0, 0, 0), gpu.shifts.last())

        engine.tick()

        assertEquals(listOf(-6, -6, -6), gpu.shifts.last())
        assertEquals(listOf(-6, -6, -6), engine.latest.origin.toList())

        assertEquals(listOf(6.5f, 6.5f, 6.5f), engine.latest.centroid.toList())
    }

    @Test
    fun aSparkIsGivenInWorldCellsAndLandsInTheWindowAfterItsShift() {
        val open = settings.copy(mode = FieldMode.Infinite, rule = LifeRule.Reef)
        val gpu = CpuGpu(deferred = false)
        val engine = LifeEngine(open, gpu, Random(3))
        engine.start()
        val corner = FloatArray(2 * total)
        for (z in 0..1) for (y in 0..1) for (x in 0..1) corner[cellIndex(x, y, z, n)] = 1f
        gpu.upload(corner)
        engine.tick()

        engine.spark(floatArrayOf(1f, 1f, 1f))
        engine.tick()

        assertEquals(listOf(7f, 7f, 7f), gpu.sparks.last().take(3))
    }

    @Test
    fun aGenerationThatComesBackAfterAResetIsDropped() {
        val gpu = CpuGpu(deferred = true)
        val engine = LifeEngine(settings, gpu, Random(3))
        engine.start()
        engine.tick()

        engine.start()
        val restarted = engine.latest
        gpu.finish()

        assertEquals(0, engine.generation)
        assertTrue(restarted === engine.latest)
        assertTrue("a new tick may start at once", engine.tick())
        assertEquals(0 to total, gpu.offsets.last())
    }
}

private class CpuGpu(private val deferred: Boolean) : LifeGpu {
    var size = 0
    val wraps = mutableListOf<Boolean>()
    val shifts = mutableListOf<List<Int>>()
    private var shift = IntArray(3)
    var rules = FloatArray(0)
    var world = FloatArray(0)
    var groups = 0
    val offsets = mutableListOf<Pair<Int, Int>>()
    val sparks = mutableListOf<List<Float>>()
    private var pending: (() -> Unit)? = null
    private var rule = LifeRule.Bays4555

    override fun size(n: Int) {
        size = n
    }

    override fun rules(values: FloatArray) {
        rules = values.copyOf()
        rule = LifeRule.entries.first { it.rule.uniformValues().contentEquals(values) }
    }

    override fun decay(states: Int) = Unit

    override fun wrap(torus: Boolean) {
        wraps += torus
    }

    override fun shift(x: Int, y: Int, z: Int) {
        shift = intArrayOf(x, y, z)
        shifts += listOf(x, y, z)
    }

    override fun upload(world: FloatArray) {
        this.world = world.copyOf()
    }

    override fun offsets(read: Int, write: Int) {
        offsets += read to write
    }

    override fun spark(x: Float, y: Float, z: Float, radius: Float, seed: Float) {
        sparks += listOf(x, y, z, radius, seed)
    }

    override fun dispatch(groups: Int) {
        this.groups = groups
        val (read, write) = offsets.last()
        val total = size * size * size
        val mode = if (wraps.last()) FieldMode.Torus else FieldMode.Infinite
        val next = CpuLife.step(world.copyOfRange(read, read + total), size, rule.rule, mode, shift)
        next.copyInto(world, write)
    }

    override fun readBack(into: FloatArray, done: () -> Unit) {
        val complete = {
            world.copyInto(into)
            done()
        }
        if (deferred) pending = complete else complete()
    }

    fun finish() {
        pending?.invoke()
        pending = null
    }
}
