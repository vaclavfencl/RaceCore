package com.example.racecore.armor;

public final class ArmorFormula {
    private ArmorFormula() {}

    public static double damageReduction(double armor) {
        return damageReduction(armor, 0.0);
    }

    public static double damageReduction(double armor, double protection) {
        if (!Double.isFinite(armor) || armor <= 0.0) return 0.0;
        ArmorCombatSettings settings = ArmorCombatRegistry.settings();
        double prot = (!Double.isFinite(protection) || protection < 0.0) ? 0.0 : protection;
        double effectiveArmor = armor * Math.pow(1.07, prot);
        double denominator = effectiveArmor + settings.armorConstant();
        if (!Double.isFinite(denominator) || denominator <= 0.0) return 0.0;
        double reduction = effectiveArmor / denominator;
        return Math.max(0.0, Math.min(settings.maxDamageReduction(), reduction));
    }

    public static double damageMultiplier(double armor) {
        return damageMultiplier(armor, 0.0);
    }

    public static double damageMultiplier(double armor, double protection) {
        return 1.0 - damageReduction(armor, protection);
    }
}
