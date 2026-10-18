package ru.redbyte.life3d.domain

import kotlin.math.abs
import kotlin.math.roundToInt

object WindowFollow {
    fun shiftFor(centroid: FloatArray, population: Int, n: Int): IntArray {
        val shift = IntArray(3)
        if (population == 0) return shift
        val middle = (n - 1) / 2f
        val slack = n * SLACK
        val step = n / MAX_STEP_DIVISOR
        for (axis in 0..2) {
            val offset = centroid[axis] - middle
            if (abs(offset) > slack) shift[axis] = offset.roundToInt().coerceIn(-step, step)
        }
        return shift
    }

    private const val SLACK = 0.125f
    private const val MAX_STEP_DIVISOR = 4
}
