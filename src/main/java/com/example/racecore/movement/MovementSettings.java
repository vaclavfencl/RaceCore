package com.example.racecore.movement;

/**
 * Central movement tuning values.
 *
 * Armor penalties are expressed as the penalty of a complete four-piece set.
 * Individual pieces use the same 15/40/30/15 slot weighting as RaceCore armor.
 */
public record MovementSettings(
        double globalMovementMultiplier,
        double lightArmorFullSetPenalty,
        double mediumArmorFullSetPenalty,
        double heavyArmorFullSetPenalty
) {
}
