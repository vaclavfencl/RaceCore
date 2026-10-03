package com.example.racecore.race;

/**
 * Fully-resolved RaceCore race definition.
 *
 * races.json owns the defaults. Individual races only override values that
 * differ from those defaults; RaceRegistry merges them before creating this
 * record.
 *
 * Neutral conventions:
 *   multiplier = 1.0
 *   bonus      = 0 / 0.0
 *   capability = false
 *   permission = true
 */
public record RaceDefinition(
        String displayName,

        // Existing physical/stat identity
        double maxHealth,
        double scale,
        double stepHeight,
        double jumpHeight,
        double movementSpeedMultiplier,
        double reachMultiplier,
        double safeFallDistance,
        int enchantmentCapacity,

        // Economy / progression
        double xpBonus,

        // Mining / drops
        double miningSpeedMultiplier,
        int fortuneBonus,
        int cropFortuneBonus,

        // Knockback / armor movement
        double knockbackResistanceBonus,
        double lightArmorPenaltyMultiplier,
        double mediumArmorPenaltyMultiplier,
        double heavyArmorPenaltyMultiplier,

        // Generic combat multipliers
        double outgoingDamageMultiplier,
        double incomingDamageMultiplier,
        double nonNetherIncomingDamageMultiplier,
        double dragonDamageMultiplier,
        double poisonDamageMultiplier,
        double fireDamageMultiplier,
        double fireworkDamageMultiplier,
        double pickaxeDamageMultiplier,
        double passiveRegenerationPerSecond,

        // Passive effects / permissions
        boolean permanentNightVision,
        boolean canLoadFireworkRockets,
        boolean canEatWhenFull,
        boolean canWearBoots,
        boolean canWearLightArmor,
        boolean canWearMediumArmor,
        boolean canWearHeavyArmor,

        // Food
        double foodHungerMultiplier,
        double foodHealing,

        // Light-based race rules
        int darkLightThreshold,
        double darkMovementMultiplier,
        double darkDamageMultiplier,
        double brightDamageMultiplier,
        boolean rangedAllowedInBrightLight,

        // Nether rules
        double netherMovementMultiplier,
        double netherDamageMultiplier,
        boolean netherMobsNeutral,

        // Ranged trajectory rules
        boolean bowNoFalloff,
        boolean crossbowNoFalloff,

        // Forest / Elf-style movement
        double forestMovementMultiplier,
        boolean leafClimbing,
        double leafClimbSpeed,

        // Temporary vulnerability mechanics
        double crackedIncomingDamageMultiplier,
        int crackedDurationTicks,

        // Gargoyle movement
        boolean chargedJumpEnabled,
        double chargedJumpMinHeight,
        double chargedJumpMaxHeight,
        int chargedJumpStartDelayTicks,
        int chargedJumpMaxChargeTicks,
        boolean glideEnabled,
        double glideFallSpeed,
        double glideForwardAcceleration,
        double glideMaxHorizontalSpeed,
        boolean rocketFlightBoostAllowed
) {
}
