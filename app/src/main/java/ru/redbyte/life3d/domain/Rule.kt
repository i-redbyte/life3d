package ru.redbyte.life3d.domain

const val NEIGHBOUR_COUNTS: Int = 27

data class Rule(val birth: Set<Int>, val survive: Set<Int>, val states: Int) {
    init {
        require(states >= 2) { "A rule needs at least two states, was $states" }
        require((birth + survive).all { it in 0 until NEIGHBOUR_COUNTS }) { "Counts must be 0..26" }
    }

    val decay: Int get() = states - 2

    val notation: String get() = "B${compress(birth)}/S${compress(survive)}/C$states"

    fun uniformValues(): FloatArray = FloatArray(2 * NEIGHBOUR_COUNTS) { index ->
        val count = index % NEIGHBOUR_COUNTS
        val set = if (index < NEIGHBOUR_COUNTS) birth else survive
        if (count in set) 1f else 0f
    }
}

private fun compress(counts: Set<Int>): String {
    if (counts.isEmpty()) return "-"
    val sorted = counts.sorted()
    val parts = mutableListOf<String>()
    var start = 0
    while (start < sorted.size) {
        var end = start
        while (end + 1 < sorted.size && sorted[end + 1] == sorted[end] + 1) end++
        parts += if (end == start) "${sorted[start]}" else "${sorted[start]}-${sorted[end]}"
        start = end + 1
    }
    return parts.joinToString(",")
}
