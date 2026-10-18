package ru.redbyte.life3d.domain

/** How the edges of the simulated window behave. */
enum class FieldMode {
    /** The window is the whole world: each face wraps around to the opposite one. */
    Torus,

    /**
     * The window is a moving view of an unbounded world: nothing lives beyond its walls, and the
     * window slides after the colony so it never reaches them for long.
     */
    Infinite,
}
