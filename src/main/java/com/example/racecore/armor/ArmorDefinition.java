package com.example.racecore.armor;

public record ArmorDefinition(
        double armor,
        ArmorWeight weight,
        int utilityCapacityBonus
) {
}
