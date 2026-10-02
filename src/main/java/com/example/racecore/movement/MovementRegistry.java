package com.example.racecore.movement;

import com.example.racecore.RaceCore;
import com.google.gson.Gson;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

public final class MovementRegistry {
    private static final Gson GSON = new Gson();

    private static MovementSettings settings;
    private static boolean loaded = false;

    private MovementRegistry() {
    }

    public static synchronized void load() {
        if (loaded) {
            return;
        }

        String path = "/data/" + RaceCore.MODID + "/movement.json";

        try (InputStream stream = MovementRegistry.class.getResourceAsStream(path)) {
            if (stream == null) {
                throw new IllegalStateException(
                        "Missing movement settings file: " + path);
            }

            try (InputStreamReader reader =
                         new InputStreamReader(stream, StandardCharsets.UTF_8)) {

                MovementSettings parsed =
                        GSON.fromJson(reader, MovementSettings.class);

                validate(parsed);

                settings = parsed;
                loaded = true;

                RaceCore.LOGGER.info(
                        "RaceCore movement settings loaded. Global={}, lightPenalty={}, mediumPenalty={}, heavyPenalty={}",
                        settings.globalMovementMultiplier(),
                        settings.lightArmorFullSetPenalty(),
                        settings.mediumArmorFullSetPenalty(),
                        settings.heavyArmorFullSetPenalty()
                );
            }
        } catch (Exception exception) {
            throw new IllegalStateException(
                    "Failed to load RaceCore movement settings.",
                    exception
            );
        }
    }

    private static void validate(MovementSettings parsed) {
        if (parsed == null) {
            throw new IllegalStateException(
                    "movement.json did not contain valid settings.");
        }

        validatePositiveMultiplier(
                "globalMovementMultiplier",
                parsed.globalMovementMultiplier()
        );

        validatePenalty(
                "lightArmorFullSetPenalty",
                parsed.lightArmorFullSetPenalty()
        );

        validatePenalty(
                "mediumArmorFullSetPenalty",
                parsed.mediumArmorFullSetPenalty()
        );

        validatePenalty(
                "heavyArmorFullSetPenalty",
                parsed.heavyArmorFullSetPenalty()
        );
    }

    private static void validatePositiveMultiplier(
            String name,
            double value
    ) {
        if (!Double.isFinite(value) || value <= 0.0) {
            throw new IllegalStateException(
                    name + " must be a positive finite number. Got: " + value);
        }
    }

    private static void validatePenalty(
            String name,
            double value
    ) {
        if (!Double.isFinite(value) || value < 0.0 || value >= 1.0) {
            throw new IllegalStateException(
                    name + " must be >= 0 and < 1. Got: " + value);
        }
    }

    private static void ensureLoaded() {
        if (!loaded) {
            load();
        }
    }

    public static MovementSettings settings() {
        ensureLoaded();
        return settings;
    }

    public static double globalMovementMultiplier() {
        return settings().globalMovementMultiplier();
    }
}
