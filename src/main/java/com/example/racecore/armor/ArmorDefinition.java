package com.example.racecore.armor;

/**
 * RaceCore's authoritative definition for one armor item.
 *
 * @param armor RaceCore armor value contributed by this piece
 * @param weight armor weight class used by the movement system
 * @param protectionCapacityBonus additional Protection capacity granted by
 *                                this specific armor piece/material
 */
public record ArmorDefinition(
        double armor,
        ArmorWeight weight,
        int protectionCapacityBonus
) {
}
