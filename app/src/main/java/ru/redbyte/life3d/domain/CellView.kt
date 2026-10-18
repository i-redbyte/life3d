package ru.redbyte.life3d.domain

import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.sqrt

class CellView(
    val instances: FloatArray,
    val count: Int,

    val centre: FloatArray,

    val radius: Float,
) {
    companion object {
        fun around(generation: Generation, mode: FieldMode, focus: FloatArray, radius: Float, cap: Int): CellView {
            val histogram = IntArray(ceil(radius).toInt() + 1)
            visit(generation, mode, focus, radius) { _, _, _, _, distance -> histogram[distance.toInt()]++ }
            var shell = histogram.size - 1
            var total = histogram.sum()
            while (total > cap && shell > 0) {
                total -= histogram[shell]
                shell--
            }
            val used = minOf(radius, shell + 1f)
            val instances = FloatArray(total * INSTANCE_FLOATS)
            var at = 0
            visit(generation, mode, focus, used) { x, y, z, value, distance ->
                if (distance.toInt() <= shell && at < instances.size) {
                    instances[at] = x
                    instances[at + 1] = y
                    instances[at + 2] = z
                    instances[at + 3] = value
                    at += INSTANCE_FLOATS
                }
            }
            return CellView(instances, at / INSTANCE_FLOATS, focus.copyOf(), used)
        }

        private inline fun visit(
            generation: Generation,
            mode: FieldMode,
            focus: FloatArray,
            radius: Float,
            emit: (Float, Float, Float, Float, Float) -> Unit,
        ) {
            val n = generation.n
            val cells = generation.cells
            val origin = generation.origin
            val limit = radius * radius
            for (z in 0 until n) for (y in 0 until n) for (x in 0 until n) {
                val value = cells[cellIndex(x, y, z, n)]
                if (value > -ALIVE && value <= ALIVE) continue
                if (mode == FieldMode.Infinite) {
                    val wx = (origin[0] + x).toFloat()
                    val wy = (origin[1] + y).toFloat()
                    val wz = (origin[2] + z).toFloat()
                    val d2 = square(wx - focus[0]) + square(wy - focus[1]) + square(wz - focus[2])
                    if (d2 <= limit) emit(wx, wy, wz, value, sqrt(d2))
                } else {
                    copies(x, focus[0], radius, n) { wx ->
                        val dx2 = square(wx - focus[0])
                        if (dx2 <= limit) {
                            copies(y, focus[1], radius, n) { wy ->
                                val dxy2 = dx2 + square(wy - focus[1])
                                if (dxy2 <= limit) {
                                    copies(z, focus[2], radius, n) { wz ->
                                        val d2 = dxy2 + square(wz - focus[2])
                                        if (d2 <= limit) emit(wx, wy, wz, value, sqrt(d2))
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        private inline fun copies(cell: Int, focus: Float, radius: Float, n: Int, each: (Float) -> Unit) {
            val first = ceil((focus - radius - cell) / n).toInt()
            val last = floor((focus + radius - cell) / n).toInt()
            for (k in first..last) each((cell + k * n).toFloat())
        }

        private fun square(value: Float): Float = value * value
    }
}
