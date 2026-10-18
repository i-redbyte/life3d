package ru.redbyte.life3d.domain

const val DYING: Float = -1f
const val ALIVE: Float = 0.5f
const val MAX_AGE: Float = 255f

const val INSTANCE_FLOATS: Int = 16

fun cellIndex(x: Int, y: Int, z: Int, n: Int): Int = (z * n + y) * n + x

const val CELL_SIZE: Float = 0.1f
