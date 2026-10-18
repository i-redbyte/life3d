package ru.redbyte.life3d.domain

/**
 * Cell values shared by the compute shader, the CPU reference, and the renderer:
 * `0` is empty, [DYING] died in the last generation, and a positive value is the age of a
 * live cell in generations, capped at [MAX_AGE]. A cell is alive when its value exceeds [ALIVE].
 */
const val DYING: Float = -1f
const val ALIVE: Float = 0.5f
const val MAX_AGE: Float = 255f

/** Floats per instance: one model matrix, as `GlFrame.draw(instances = …)` expects. */
const val INSTANCE_FLOATS: Int = 16

/** Row-major index of cell ([x], [y], [z]) in an [n]³ grid; x changes fastest. */
fun cellIndex(x: Int, y: Int, z: Int, n: Int): Int = (z * n + y) * n + x
