package ru.redbyte.life3d.gl

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import ru.redbyte.life3d.domain.NEIGHBOUR_COUNTS

class LifeComputeTest {
    private val compute = lifeCompute()

    @Test
    fun theGenerationRunsInFourCubedWorkgroups() {
        val source = compute.program.computeSource()

        assertTrue(source, source.contains("local_size_x = 4, local_size_y = 4, local_size_z = 4"))
        assertTrue(source, source.contains("gl_GlobalInvocationID"))
    }

    @Test
    fun theRuleBlockHoldsABirthAndASurvivalFlagForEveryNeighbourCount() {
        assertEquals(0, compute.world.binding)
        assertTrue(compute.rules.name.contains("rules"))
        val source = compute.program.computeSource()
        assertTrue(source, source.contains("[$NEIGHBOUR_COUNTS]"))
    }
}
