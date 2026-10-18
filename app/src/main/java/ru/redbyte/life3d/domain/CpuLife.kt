package ru.redbyte.life3d.domain

import kotlin.math.min

/**
 * Reference implementation of one generation on the CPU.
 *
 * The game runs the same rule in a compute shader; this version exists so the shader can be
 * checked against something obviously correct, and so the rule semantics are readable in Kotlin.
 */
object CpuLife {

    private val NO_SHIFT = IntArray(3)

    /**
     * The next generation of [world]. In [FieldMode.Infinite] the window first moves by [shift]
     * cells over the world: new cell `c` continues old cell `c + shift`, and what leaves the window
     * is gone.
     */
    fun step(
        world: FloatArray,
        n: Int,
        rule: LifeRule,
        mode: FieldMode = FieldMode.Torus,
        shift: IntArray = NO_SHIFT,
    ): FloatArray {
        require(world.size == n * n * n) { "World must hold $n³ cells, was ${world.size}" }
        val next = FloatArray(world.size)
        for (z in 0 until n) for (y in 0 until n) for (x in 0 until n) {
            val sx = x + shift[0]
            val sy = y + shift[1]
            val sz = z + shift[2]
            val value = cell(world, n, sx, sy, sz, mode)
            val alive = value > ALIVE
            val count = neighbours(world, n, sx, sy, sz, mode)
            val lives = if (alive) count in rule.survive else count in rule.birth
            next[cellIndex(x, y, z, n)] = when {
                lives && alive -> min(value + 1f, MAX_AGE)
                lives -> 1f
                alive -> DYING
                else -> 0f
            }
        }
        return next
    }

    /** Live cells among the 26 neighbours of ([x], [y], [z]). */
    fun neighbours(world: FloatArray, n: Int, x: Int, y: Int, z: Int, mode: FieldMode = FieldMode.Torus): Int {
        var count = 0
        for (dz in -1..1) for (dy in -1..1) for (dx in -1..1) {
            if (dx == 0 && dy == 0 && dz == 0) continue
            if (cell(world, n, x + dx, y + dy, z + dz, mode) > ALIVE) count++
        }
        return count
    }

    /** The value at a coordinate one step or one shift outside the window at most. */
    private fun cell(world: FloatArray, n: Int, x: Int, y: Int, z: Int, mode: FieldMode): Float = when (mode) {
        FieldMode.Torus -> world[cellIndex(wrap(x, n), wrap(y, n), wrap(z, n), n)]
        FieldMode.Infinite -> if (x in 0 until n && y in 0 until n && z in 0 until n) world[cellIndex(x, y, z, n)] else 0f
    }

    private fun wrap(value: Int, n: Int): Int = when {
        value < 0 -> value + n
        value >= n -> value - n
        else -> value
    }
}
