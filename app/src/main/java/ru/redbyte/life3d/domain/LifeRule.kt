package ru.redbyte.life3d.domain

/** A cell has 26 neighbours, so a neighbour count is one of 0..26. */
const val NEIGHBOUR_COUNTS: Int = 27

/**
 * A 3D Life rule in B/S notation: a dead cell is born with a neighbour count from [birth],
 * a live cell survives with a count from [survive]. [seedDensity] is how densely the starting
 * cloud should be packed for this rule to come alive instead of dying out or exploding.
 */
enum class LifeRule(
    val birth: Set<Int>,
    val survive: Set<Int>,
    val seedDensity: Float,
) {
    /** Flickering chaos that settles at about a sixth of the field: B4/S4. */
    Pulse(birth = setOf(4), survive = setOf(4), seedDensity = 0.3f),

    /** A porous, constantly reshaping mass. */
    Sponge(birth = setOf(5, 6, 7), survive = setOf(4, 5, 6, 7), seedDensity = 0.35f),

    /** Branching growth that keeps rebuilding itself. */
    Reef(birth = setOf(6, 7, 9, 12), survive = setOf(5, 6, 7, 8), seedDensity = 0.35f),

    /** Hollow shells that bloom and burst. */
    Shells(birth = setOf(6, 7, 8, 9), survive = setOf(6), seedDensity = 0.35f),

    /** Grows a lattice outward from a small seed. */
    Crystal(birth = setOf(1, 3), survive = (0..6).toSet(), seedDensity = 0.2f),

    /**
     * Carter Bays' 4555, the classic 3D Life with gliders. From a random cloud it soon settles into
     * a few still lifes; taps bring it back.
     */
    Bays4555(birth = setOf(5), survive = setOf(4, 5), seedDensity = 0.2f),
    ;

    /** Standard B/S notation with ranges, such as `B13-14,17-19/S13-26`. */
    val notation: String = "B${compress(birth)}/S${compress(survive)}"

    /**
     * Uniform block contents for the compute shader: 27 birth flags, then 27 survival flags.
     * A flag is 1 when that neighbour count applies.
     */
    fun uniformValues(): FloatArray = FloatArray(2 * NEIGHBOUR_COUNTS) { index ->
        val count = index % NEIGHBOUR_COUNTS
        val set = if (index < NEIGHBOUR_COUNTS) birth else survive
        if (count in set) 1f else 0f
    }
}

private fun compress(counts: Set<Int>): String {
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
