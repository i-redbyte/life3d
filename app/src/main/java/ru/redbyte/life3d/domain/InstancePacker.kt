package ru.redbyte.life3d.domain

/**
 * One generation ready to draw: [instances] holds [INSTANCE_FLOATS] floats per visible cell, in
 * window coordinates. [origin] is the world cell under window cell zero, and [centroid] the centre
 * of the live cells in window coordinates; both belong to this generation, so the renderer never
 * pairs cells with another generation's window.
 */
class PackedGeneration(
    val instances: FloatArray,
    val population: Int,
    val dying: Int,
    val origin: IntArray = IntArray(3),
    val centroid: FloatArray = FloatArray(3),
)

/**
 * Turns the grid read back from the GPU into instance data for one instanced draw.
 *
 * Each visible cell (alive or just died) takes one model-matrix slot. The renderer reads only the
 * first column: `(x, y, z, value)`; the other twelve floats stay zero.
 */
object InstancePacker {

    fun pack(cells: FloatArray, offset: Int, n: Int, origin: IntArray = IntArray(3)): PackedGeneration {
        val total = n * n * n
        var population = 0
        var dying = 0
        for (i in offset until offset + total) {
            val value = cells[i]
            if (value > ALIVE) population++ else if (value < -ALIVE) dying++
        }
        val instances = FloatArray((population + dying) * INSTANCE_FLOATS)
        val sum = DoubleArray(3)
        var at = 0
        for (i in 0 until total) {
            val value = cells[offset + i]
            if (value > -ALIVE && value <= ALIVE) continue
            val x = i % n
            val y = i / n % n
            val z = i / (n * n)
            instances[at] = x.toFloat()
            instances[at + 1] = y.toFloat()
            instances[at + 2] = z.toFloat()
            instances[at + 3] = value
            at += INSTANCE_FLOATS
            if (value > ALIVE) {
                sum[0] += x.toDouble()
                sum[1] += y.toDouble()
                sum[2] += z.toDouble()
            }
        }
        val centroid = if (population == 0) {
            FloatArray(3) { (n - 1) / 2f }
        } else {
            FloatArray(3) { axis -> (sum[axis] / population).toFloat() }
        }
        return PackedGeneration(instances, population, dying, origin.copyOf(), centroid)
    }
}
