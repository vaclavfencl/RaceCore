package com.example.racecore.armor;

/**
 * Aggregated RaceCore armor currently worn by an entity.
 *
 * The piece counts are intentionally kept separate so the future movement
 * system can decide how LIGHT / MEDIUM / HEAVY armor should affect speed
 * without changing the armor registry itself.
 */
public record ArmorStats(
        double totalArmor,
        int lightPieces,
        int mediumPieces,
        int heavyPieces,
        int unmappedPieces
) {
    public static final ArmorStats EMPTY =
            new ArmorStats(0.0, 0, 0, 0, 0);

    public int mappedPieces() {
        return lightPieces + mediumPieces + heavyPieces;
    }

    public int totalPieces() {
        return mappedPieces() + unmappedPieces;
    }

    public boolean hasHeavyArmor() {
        return heavyPieces > 0;
    }

    public boolean hasMediumArmor() {
        return mediumPieces > 0;
    }

    public boolean hasLightArmor() {
        return lightPieces > 0;
    }
}
