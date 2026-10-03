package com.example.racecore.armor;

/**
 * How one enchantment contributes to RaceCore Protection.
 *
 * @param type           which RaceCore protection category the enchantment belongs to
 * @param pointsPerLevel RaceCore protection points contributed by each enchantment level
 */
public record ProtectionEnchantmentDefinition(
        ProtectionType type,
        double pointsPerLevel
) {
}
