package com.example.racecore.armor;

import java.util.Map;

/**
 * Global Protection defaults.
 *
 * Race-specific player capacity now lives in races.json.
 */
public record ProtectionSettings(
        int defaultCapacityPerPiece,
        double universalTypedEffectiveness,
        Map<String, ProtectionEnchantmentDefinition> enchantments
) {
}
