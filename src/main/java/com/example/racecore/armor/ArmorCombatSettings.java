package com.example.racecore.armor;

/**
 * Tunable constants for the RaceCore armor formula.
 *
 * Step 3D formula:
 *
 *     DR = A / (A + K)
 *
 * Protection is deliberately not included yet. Step 3E will extend this.
 */
public record ArmorCombatSettings(
        double armorConstant,
        double maxDamageReduction
) {
}
