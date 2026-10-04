package com.example.racecore.death;

public record DeathInventorySettings(
        boolean enabled,
        int horizontalSearchRadius,
        int verticalSearchRadius,
        boolean requireAirAbove,
        boolean requireSupportBelow
) {
}
