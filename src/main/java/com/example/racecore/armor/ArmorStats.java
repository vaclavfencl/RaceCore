package com.example.racecore.armor;

public record ArmorStats(
        double totalArmor,
        int lightPieces,
        int mediumPieces,
        int heavyPieces,
        int ultraHeavyPieces,
        int unmappedPieces
) {
    public static final ArmorStats EMPTY = new ArmorStats(0.0, 0, 0, 0, 0, 0);

    public int mappedPieces() {
        return lightPieces + mediumPieces + heavyPieces + ultraHeavyPieces;
    }

    public int totalPieces() {
        return mappedPieces() + unmappedPieces;
    }
}
