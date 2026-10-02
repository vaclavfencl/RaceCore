package com.example.racecore.armor;

/**
 * Pure RaceCore armor math.
 *
 * Step 3D:
 *
 *     DR = A / (A + K)
 *
 * where:
 *     A = total RaceCore armor
 *     K = armorConstant from armor_combat.json
 *
 * Step 3E will extend this class with typed Protection.
 */
public final class ArmorFormula {
    private ArmorFormula() {
    }

    public static double damageReduction(
            double armor
    ) {
        if (!Double.isFinite(armor) || armor <= 0.0) {
            return 0.0;
        }

        ArmorCombatSettings settings =
                ArmorCombatRegistry.settings();

        double k = settings.armorConstant();

        double reduction =
                armor / (armor + k);

        return Math.max(
                0.0,
                Math.min(
                        settings.maxDamageReduction(),
                        reduction
                )
        );
    }

    public static double damageMultiplier(
            double armor
    ) {
        return 1.0 - damageReduction(armor);
    }
}
