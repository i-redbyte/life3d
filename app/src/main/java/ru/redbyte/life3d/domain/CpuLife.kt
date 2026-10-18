package ru.redbyte.life3d.domain

import kotlin.math.min

object CpuLife {
    private val NO_SHIFT = IntArray(3)

    fun step(
        world: FloatArray,
        n: Int,
        rule: Rule,
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
            next[cellIndex(x, y, z, n)] = nextValue(value, neighbours(world, n, sx, sy, sz, mode), rule)
        }
        return next
    }

    fun nextValue(value: Float, count: Int, rule: Rule): Float {
        val alive = value > ALIVE
        val refractory = value < -ALIVE && rule.decay > 0
        return when {
            alive && count in rule.survive -> min(value + 1f, MAX_AGE)
            alive -> if (rule.decay > 0) -rule.decay.toFloat() else DYING
            refractory -> value + 1f
            count in rule.birth -> 1f
            else -> 0f
        }
    }

    fun neighbours(world: FloatArray, n: Int, x: Int, y: Int, z: Int, mode: FieldMode = FieldMode.Torus): Int {
        var count = 0
        for (dz in -1..1) for (dy in -1..1) for (dx in -1..1) {
            if (dx == 0 && dy == 0 && dz == 0) continue
            if (cell(world, n, x + dx, y + dy, z + dz, mode) > ALIVE) count++
        }
        return count
    }

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
