package com.example.racecore.race;

import com.example.racecore.RaceCore;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

public final class RaceRegistry {
    private static final Gson GSON = new Gson();

    private static final Type RACE_MAP_TYPE =
            new TypeToken<LinkedHashMap<String, RaceDefinition>>() {}.getType();

    private static final Map<String, RaceDefinition> RACES = new LinkedHashMap<>();

    private RaceRegistry() {
    }

    public static void load() {
        String path = "/data/" + RaceCore.MODID + "/races.json";

        try (InputStream stream = RaceRegistry.class.getResourceAsStream(path)) {
            if (stream == null) {
                throw new IllegalStateException("Missing race definition file: " + path);
            }

            try (InputStreamReader reader =
                         new InputStreamReader(stream, StandardCharsets.UTF_8)) {

                Map<String, RaceDefinition> loaded = GSON.fromJson(reader, RACE_MAP_TYPE);

                if (loaded == null || loaded.isEmpty()) {
                    throw new IllegalStateException("No races were defined in " + path);
                }

                Map<String, RaceDefinition> validated = new LinkedHashMap<>();

                for (Map.Entry<String, RaceDefinition> entry : loaded.entrySet()) {
                    String id = normalizeId(entry.getKey());
                    RaceDefinition race = entry.getValue();

                    validate(id, race);

                    if (validated.put(id, race) != null) {
                        throw new IllegalStateException(
                                "Duplicate race id after normalization: " + id);
                    }
                }

                if (!validated.containsKey("human")) {
                    throw new IllegalStateException(
                            "races.json must contain a 'human' race because it is the default.");
                }

                RACES.clear();
                RACES.putAll(validated);
            }
        } catch (Exception exception) {
            throw new IllegalStateException("Failed to load RaceCore races.", exception);
        }
    }

    private static void validate(String id, RaceDefinition race) {
        if (id.isBlank()) {
            throw new IllegalStateException("Race id cannot be blank.");
        }

        if (race == null) {
            throw new IllegalStateException("Race '" + id + "' has no definition.");
        }

        if (race.displayName() == null || race.displayName().isBlank()) {
            throw new IllegalStateException("Race '" + id + "' has no displayName.");
        }

        requirePositiveFinite(id, "maxHealth", race.maxHealth());
        requirePositiveFinite(id, "scale", race.scale());
        requirePositiveFinite(id, "stepHeight", race.stepHeight());
        requirePositiveFinite(id, "jumpHeight", race.jumpHeight());
        requirePositiveFinite(id, "movementSpeedMultiplier", race.movementSpeedMultiplier());
        requirePositiveFinite(id, "reachMultiplier", race.reachMultiplier());
    }

    private static void requirePositiveFinite(String raceId, String field, double value) {
        if (!Double.isFinite(value) || value <= 0.0) {
            throw new IllegalStateException(
                    "Race '" + raceId + "' has invalid " + field + ": " + value);
        }
    }

    public static String normalizeId(String id) {
        return id == null ? "" : id.trim().toLowerCase(Locale.ROOT);
    }

    public static RaceDefinition get(String id) {
        return RACES.get(normalizeId(id));
    }

    public static RaceDefinition getOrHuman(String id) {
        RaceDefinition race = get(id);
        return race != null ? race : RACES.get("human");
    }

    public static boolean contains(String id) {
        return RACES.containsKey(normalizeId(id));
    }

    public static Collection<String> ids() {
        return RACES.keySet();
    }
}
