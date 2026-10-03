package com.example.racecore.armor;

/**
 * Pure RaceCore armor math.
 *
 * Step 3E:
 *
 *     DR = A * (1 + Prot / 10)
 *          --------------------
 *          A * (1 + Prot / 20) + K
 *
 * where:
 *     A    = total RaceCore armor
 *     Prot = relevant RaceCore protection points for this damage type
 *     K    = armorConstant from armor_combat.json
 *
 * Final DR is clamped to the configured cap.
 */
public final class ArmorFormula {
    private ArmorFormula() {
    }

    public static double damageReduction(
            double armor
    ) {
        return damageReduction(armor, 0.0);
    }

    public static double damageReduction(
            double armor,
            double protection
    ) {
        if (!Double.isFinite(armor) || armor <= 0.0) {
            return 0.0;
        }

        ArmorCombatSettings settings =
                ArmorCombatRegistry.settings();

        double prot = (!Double.isFinite(protection) || protection < 0.0)
                ? 0.0
                : protection;

        double numerator =
                armor * (1.0 + prot / 10.0);

        double denominator =
                armor * (1.0 + prot / 20.0)
                        + settings.armorConstant();

        if (!Double.isFinite(denominator) || denominator <= 0.0) {
            return 0.0;
        }

        double reduction =
                numerator / denominator;

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
        return damageMultiplier(armor, 0.0);
    }

    public static double damageMultiplier(
            double armor,
            double protection
    ) {
        return 1.0 - damageReduction(armor, protection);
    }
}
