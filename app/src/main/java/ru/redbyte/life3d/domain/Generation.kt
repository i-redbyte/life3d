package ru.redbyte.life3d.domain

class Generation private constructor(
    val cells: FloatArray,
    val n: Int,
    val origin: IntArray,

    val centroid: FloatArray,
    val population: Int,

    val fading: Int,
    val number: Int,
) {
    companion object {
        fun of(source: FloatArray, offset: Int, n: Int, origin: IntArray, number: Int): Generation {
            val total = n * n * n
            val cells = source.copyOfRange(offset, offset + total)
            var population = 0
            var fading = 0
            var sx = 0.0
            var sy = 0.0
            var sz = 0.0
            for (i in 0 until total) {
                val value = cells[i]
                if (value > ALIVE) {
                    population++
                    sx += i % n
                    sy += i / n % n
                    sz += i / (n * n)
                } else if (value < -ALIVE) {
                    fading++
                }
            }
            val centroid = if (population == 0) {
                FloatArray(3) { (n - 1) / 2f }
            } else {
                floatArrayOf((sx / population).toFloat(), (sy / population).toFloat(), (sz / population).toFloat())
            }
            return Generation(cells, n, origin.copyOf(), centroid, population, fading, number)
        }
    }
}
