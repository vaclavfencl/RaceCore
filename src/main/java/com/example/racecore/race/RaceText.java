package com.example.racecore.race;

import net.minecraft.network.chat.Component;

public final class RaceText {
    private RaceText() {
    }

    public static Component name(
            String raceId
    ) {
        return Component.translatable(
                "race.racecore." + raceId
        );
    }
}
