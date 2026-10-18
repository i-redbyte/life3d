package ru.redbyte.life3d.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test
import ru.redbyte.life3d.gl.WORKGROUP_EDGE

class LifeSettingsTest {

    @Test
    fun everyFieldSizeSplitsIntoWholeWorkgroups() {
        for (size in LifeSettings.GRID_SIZES) assertEquals(0, size % WORKGROUP_EDGE)
    }

    @Test
    fun speedIsGenerationsPerSecond() {
        assertEquals(100L, LifeSettings(speed = 10).intervalMillis)
        assertEquals(1_000L, LifeSettings(speed = 1).intervalMillis)
    }

    @Test
    fun aFieldSizeTheComputeShaderCannotSplitIsRejected() {
        assertThrows(IllegalArgumentException::class.java) { LifeSettings(gridSize = 18) }
        assertThrows(IllegalArgumentException::class.java) { LifeSettings(speed = 0) }
    }
}
