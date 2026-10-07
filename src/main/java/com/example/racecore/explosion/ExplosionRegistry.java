package com.example.racecore.explosion;

import com.example.racecore.RaceCore;
import com.google.gson.Gson;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

public final class ExplosionRegistry {
    private static final Gson GSON = new Gson();

    private static ExplosionSettings settings;
    private static Set<String> noBlockDamageEntityTypes = Set.of();
    private static boolean loaded;

    private ExplosionRegistry() {
    }

    public static synchronized void load() {
        if (loaded) {
            return;
        }

        String path =
                "/data/" + RaceCore.MODID + "/explosion.json";

        try (InputStream stream =
                     ExplosionRegistry.class.getResourceAsStream(path)) {

            if (stream == null) {
                throw new IllegalStateException(
                        "Missing explosion settings file: " + path
                );
            }

            try (InputStreamReader reader =
                         new InputStreamReader(
                                 stream,
                                 StandardCharsets.UTF_8
                         )) {

                ExplosionSettings parsed =
                        GSON.fromJson(
                                reader,
                                ExplosionSettings.class
                        );

                validate(parsed);

                Set<String> normalized =
                        new HashSet<>();

                for (String id
                        : parsed.noBlockDamageEntityTypes()) {

                    String value =
                            normalize(id);

                    if (!value.isEmpty()) {
                        normalized.add(value);
                    }
                }

                settings = parsed;
                noBlockDamageEntityTypes =
                        Set.copyOf(normalized);
                loaded = true;

                RaceCore.LOGGER.info(
                        "RaceCore explosion rules loaded. "
                                + "crossbowRocketMinimumExplosions={}, "
                                + "noBlockDamageEntityTypes={}",
                        settings.crossbowRocketMinimumExplosions(),
                        noBlockDamageEntityTypes
                );
            }
        } catch (Exception exception) {
            throw new IllegalStateException(
                    "Failed to load RaceCore explosion settings.",
                    exception
            );
        }
    }

    private static void validate(
            ExplosionSettings parsed
    ) {
        if (parsed == null) {
            throw new IllegalStateException(
                    "explosion.json did not contain valid settings."
            );
        }

        if (parsed.crossbowRocketMinimumExplosions() < 0
                || parsed.crossbowRocketMinimumExplosions() > 7) {

            throw new IllegalStateException(
                    "crossbowRocketMinimumExplosions must be between 0 and 7."
            );
        }

        if (parsed.noBlockDamageEntityTypes() == null) {
            throw new IllegalStateException(
                    "noBlockDamageEntityTypes cannot be null."
            );
        }
    }

    public static ExplosionSettings settings() {
        if (!loaded) {
            load();
        }

        return settings;
    }

    public static boolean preventsBlockDamage(
            String entityTypeId
    ) {
        if (!loaded) {
            load();
        }

        return noBlockDamageEntityTypes.contains(
                normalize(entityTypeId)
        );
    }

    private static String normalize(
            String value
    ) {
        return value == null
                ? ""
                : value.trim()
                        .toLowerCase(Locale.ROOT);
    }
}
