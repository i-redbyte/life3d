package ru.redbyte.life3d.domain

import org.junit.Assert.assertEquals
import org.junit.Test

class InstancePackerTest {

    @Test
    fun everyLivingOrJustDiedCellBecomesOneInstanceWithItsCoordinatesAndState() {
        val n = 4
        val cells = FloatArray(2 * n * n * n)
        val half = n * n * n
        cells[half + cellIndex(1, 2, 3, n)] = 5f
        cells[half + cellIndex(3, 0, 0, n)] = DYING
        cells[cellIndex(0, 0, 0, n)] = 1f // the other half is ignored

        val packed = InstancePacker.pack(cells, offset = half, n = n)

        assertEquals(2, packed.population + packed.dying)
        assertEquals(1, packed.population)
        assertEquals(2 * INSTANCE_FLOATS, packed.instances.size)
        val first = packed.instances.copyOfRange(0, 4).toList()
        val second = packed.instances.copyOfRange(INSTANCE_FLOATS, INSTANCE_FLOATS + 4).toList()
        assertEquals(listOf(3f, 0f, 0f, DYING), first)
        assertEquals(listOf(1f, 2f, 3f, 5f), second)
    }

    @Test
    fun anEmptyWorldPacksToNothing() {
        val packed = InstancePacker.pack(FloatArray(2 * 8), offset = 8, n = 2)

        assertEquals(0, packed.instances.size)
        assertEquals(0, packed.population)
    }
}
