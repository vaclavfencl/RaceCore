package com.example.racecore.race;

public record RaceDefinition(
        String displayName,
        double maxHealth,
        double scale,
        double stepHeight,
        double jumpHeight,
        double movementSpeedMultiplier,
        double reachMultiplier
) {
}
