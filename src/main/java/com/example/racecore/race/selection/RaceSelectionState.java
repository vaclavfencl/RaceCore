package com.example.racecore.race.selection;

import net.minecraft.world.entity.player.Player;

/**
 * Persistent server-authoritative state telling RaceCore whether this player
 * has explicitly confirmed a race.
 *
 * This is deliberately separate from the race id itself:
 * "human" is a real valid race and must not mean "not selected yet".
 */
public final class RaceSelectionState {
    private static final String SELECTED_KEY =
            "racecore:race_selected";

    private RaceSelectionState() {
    }

    public static boolean hasSelectedRace(Player player) {
        return player.getPersistentData()
                .getBooleanOr(SELECTED_KEY, false);
    }

    public static void markSelected(Player player) {
        player.getPersistentData()
                .putBoolean(SELECTED_KEY, true);
    }

    public static void reset(Player player) {
        player.getPersistentData()
                .putBoolean(SELECTED_KEY, false);
    }

    public static void copy(Player from, Player to) {
        to.getPersistentData().putBoolean(
                SELECTED_KEY,
                hasSelectedRace(from)
        );
    }
}
