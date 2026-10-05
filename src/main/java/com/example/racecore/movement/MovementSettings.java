package com.example.racecore.movement;

public record MovementSettings(
        double globalMovementMultiplier,
        double lightArmorFullSetPenalty,
        double mediumArmorFullSetPenalty,
        double heavyArmorFullSetPenalty,
        double ultraHeavyArmorFullSetPenalty
) {
}
