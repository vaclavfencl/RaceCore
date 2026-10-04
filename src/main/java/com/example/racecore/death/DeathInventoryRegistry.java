package com.example.racecore.death;

import com.example.racecore.RaceCore;
import com.google.gson.Gson;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

public final class DeathInventoryRegistry {
    private static final Gson GSON = new Gson();

    private static DeathInventorySettings settings;
    private static boolean loaded = false;

    private DeathInventoryRegistry() {
    }

    public static synchronized void load() {
        if (loaded) {
            return;
        }

        String path = "/data/" + RaceCore.MODID + "/death_inventory.json";

        try (InputStream stream =
                     DeathInventoryRegistry.class.getResourceAsStream(path)) {

            if (stream == null) {
                throw new IllegalStateException(
                        "Missing death inventory settings file: " + path
                );
            }

            try (InputStreamReader reader =
                         new InputStreamReader(
                                 stream,
                                 StandardCharsets.UTF_8
                         )) {

                DeathInventorySettings parsed =
                        GSON.fromJson(
                                reader,
                                DeathInventorySettings.class
                        );

                validate(parsed);

                settings = parsed;
                loaded = true;

                RaceCore.LOGGER.info(
                        "RaceCore death inventory settings loaded. "
                                + "enabled={}, horizontalRadius={}, verticalRadius={}",
                        settings.enabled(),
                        settings.horizontalSearchRadius(),
                        settings.verticalSearchRadius()
                );
            }
        } catch (Exception exception) {
            throw new IllegalStateException(
                    "Failed to load RaceCore death inventory settings.",
                    exception
            );
        }
    }

    private static void validate(
            DeathInventorySettings parsed
    ) {
        if (parsed == null) {
            throw new IllegalStateException(
                    "death_inventory.json did not contain valid settings."
            );
        }

        if (parsed.horizontalSearchRadius() < 0
                || parsed.horizontalSearchRadius() > 16) {
            throw new IllegalStateException(
                    "horizontalSearchRadius must be between 0 and 16. Got: "
                            + parsed.horizontalSearchRadius()
            );
        }

        if (parsed.verticalSearchRadius() < 0
                || parsed.verticalSearchRadius() > 16) {
            throw new IllegalStateException(
                    "verticalSearchRadius must be between 0 and 16. Got: "
                            + parsed.verticalSearchRadius()
            );
        }
    }

    private static void ensureLoaded() {
        if (!loaded) {
            load();
        }
    }

    public static DeathInventorySettings settings() {
        ensureLoaded();
        return settings;
    }
}
