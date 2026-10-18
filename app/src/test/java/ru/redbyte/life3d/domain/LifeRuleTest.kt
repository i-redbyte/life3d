package ru.redbyte.life3d.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class LifeRuleTest {
    @Test
    fun ruleUniformsListBirthThenSurvivalForEveryNeighbourCount() {
        val rule = LifeRule.Bays4555.rule

        val values = rule.uniformValues()

        assertEquals(2 * NEIGHBOUR_COUNTS, values.size)
        for (count in 0 until NEIGHBOUR_COUNTS) {
            assertEquals(if (count == 5) 1f else 0f, values[count])
            assertEquals(if (count == 4 || count == 5) 1f else 0f, values[NEIGHBOUR_COUNTS + count])
        }
    }

    @Test
    fun everyPresetHasANameInBothNotationsAndASensibleSeedDensity() {
        for (rule in LifeRule.entries) {
            assertTrue(rule.notation, rule.notation.startsWith("B") && "/S" in rule.notation)
            assertTrue(rule.rule.birth.isNotEmpty())
            assertTrue(rule.seedDensity in 0.05f..0.9f)
        }
        assertEquals("B5/S4-5/C2", LifeRule.Bays4555.notation)
        assertEquals("B6-7,9,12/S5-8/C4", LifeRule.Reef.notation)
        assertEquals("B4/S4/C5", LifeRule.Waves.notation)
        assertEquals("B5-7,12-13,15/S9-26/C5", LifeRule.Amoeba.notation)
    }
}
