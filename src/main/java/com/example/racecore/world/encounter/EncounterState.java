package com.example.racecore.world.encounter;

/**
 * Shared lifecycle states for world encounters.
 *
 * Hunts use ACTIVE -> SEARCHING -> FINISHED. Dungeon rooms and bosses can
 * reuse the remaining states later without needing a separate lifecycle.
 */
public enum EncounterState {
    SPAWNING,
    ACTIVE,
    SEARCHING,
    RETURNING,
    FINISHED
}
