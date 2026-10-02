package com.example.racecore.armor;

import com.example.racecore.RaceCore;
import com.google.gson.Gson;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

public final class ArmorCombatRegistry {
    private static final Gson GSON = new Gson();

    private static ArmorCombatSettings settings;
    private static boolean loaded = false;

    private ArmorCombatRegistry() {
    }

    public static synchronized void load() {
        if (loaded) {
            return;
        }

        String path =
                "/data/" + RaceCore.MODID + "/armor_combat.json";

        try (InputStream stream =
                     ArmorCombatRegistry.class.getResourceAsStream(path)) {

            if (stream == null) {
                throw new IllegalStateException(
                        "Missing armor combat settings file: " + path);
            }

            try (InputStreamReader reader =
                         new InputStreamReader(
                                 stream,
                                 StandardCharsets.UTF_8
                         )) {

                ArmorCombatSettings parsed =
                        GSON.fromJson(
                                reader,
                                ArmorCombatSettings.class
                        );

                validate(parsed);

                settings = parsed;
                loaded = true;

                RaceCore.LOGGER.info(
                        "RaceCore armor combat settings loaded. K={}, cap={}",
                        settings.armorConstant(),
                        settings.maxDamageReduction()
                );
            }
        } catch (Exception exception) {
            throw new IllegalStateException(
                    "Failed to load RaceCore armor combat settings.",
                    exception
            );
        }
    }

    private static void validate(
            ArmorCombatSettings parsed
    ) {
        if (parsed == null) {
            throw new IllegalStateException(
                    "armor_combat.json did not contain valid settings.");
        }

        if (!Double.isFinite(parsed.armorConstant())
                || parsed.armorConstant() <= 0.0) {

            throw new IllegalStateException(
                    "armorConstant must be positive and finite. Got: "
                            + parsed.armorConstant()
            );
        }

        if (!Double.isFinite(parsed.maxDamageReduction())
                || parsed.maxDamageReduction() < 0.0
                || parsed.maxDamageReduction() > 1.0) {

            throw new IllegalStateException(
                    "maxDamageReduction must be between 0 and 1. Got: "
                            + parsed.maxDamageReduction()
            );
        }
    }

    private static void ensureLoaded() {
        if (!loaded) {
            load();
        }
    }

    public static ArmorCombatSettings settings() {
        ensureLoaded();
        return settings;
    }
}
