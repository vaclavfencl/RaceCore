package com.example.racecore.armor;

/**
 * RaceCore's authoritative definition for one armor item.
 *
 * @param armor  RaceCore armor value contributed by this piece
 * @param weight armor weight class used later by the movement system
 */
public record ArmorDefinition(
        double armor,
        ArmorWeight weight
) {
}
