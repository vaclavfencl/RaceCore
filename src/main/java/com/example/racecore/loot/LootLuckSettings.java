package com.example.racecore.loot;

import java.util.List;
import java.util.Map;

/**
 * Phase 7 balance knobs.
 *
 * quality is contextual and derived automatically per loot-table entry:
 *   0.0 = common / little discovery help
 *   1.0 = extremely rare / maximum discovery help
 */
public record LootLuckSettings(
        boolean enabled,
        double maxQualityWeightRatio,
        double bonusDiscoveryRollsPerLuck,
        double quantityBonusPerLuck,
        int maxBonusDiscoveryRolls,
        double maxQuantityMultiplier,
        List<String> enabledPathPrefixes,
        Map<String, Map<String, Double>> qualityOverrides
) {
}
