package ru.redbyte.life3d.domain

enum class LifeRule(
    val rule: Rule,
    val seedDensity: Float,
) {
    Lava(Rule(birth = setOf(6, 7, 8), survive = setOf(4, 5, 6, 7), states = 10), seedDensity = 0.4f),

    Reef(Rule(birth = setOf(6, 7, 9, 12), survive = setOf(5, 6, 7, 8), states = 4), seedDensity = 0.4f),

    Amoeba(Rule(birth = setOf(5, 6, 7, 12, 13, 15), survive = (9..26).toSet(), states = 5), seedDensity = 0.4f),

    Waves(Rule(birth = setOf(4), survive = setOf(4), states = 5), seedDensity = 0.4f),

    Builder(Rule(birth = setOf(4, 6, 8, 9), survive = setOf(2, 6, 9), states = 10), seedDensity = 0.5f),

    Moss(Rule(birth = setOf(6, 7, 8), survive = setOf(6, 7, 8), states = 3), seedDensity = 0.4f),

    Bays4555(Rule(birth = setOf(5), survive = setOf(4, 5), states = 2), seedDensity = 0.2f),
    ;

    val notation: String get() = rule.notation
}
