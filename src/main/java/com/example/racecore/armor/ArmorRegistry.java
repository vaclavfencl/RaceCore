package com.example.racecore.armor;

import com.example.racecore.RaceCore;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

/**
 * Loads RaceCore armor definitions from:
 *
 *     data/racecore/armor.json
 *
 * The registry is deliberately independent from Minecraft's vanilla armor
 * attribute.
 */
public final class ArmorRegistry {
    private static final Gson GSON = new Gson();

    private static final Type ARMOR_MAP_TYPE =
            new TypeToken<LinkedHashMap<String, ArmorDefinition>>() {}.getType();

    private static final Map<String, ArmorDefinition> DEFINITIONS =
            new LinkedHashMap<>();

    private static boolean loaded = false;

    private ArmorRegistry() {
    }

    public static synchronized void load() {
        if (loaded) {
            return;
        }

        String path = "/data/" + RaceCore.MODID + "/armor.json";

        try (InputStream stream =
                     ArmorRegistry.class.getResourceAsStream(path)) {

            if (stream == null) {
                throw new IllegalStateException(
                        "Missing armor definition file: " + path
                );
            }

            try (InputStreamReader reader =
                         new InputStreamReader(
                                 stream,
                                 StandardCharsets.UTF_8
                         )) {

                Map<String, ArmorDefinition> parsed =
                        GSON.fromJson(reader, ARMOR_MAP_TYPE);

                if (parsed == null || parsed.isEmpty()) {
                    throw new IllegalStateException(
                            "No armor definitions were found in "
                                    + path
                    );
                }

                Map<String, ArmorDefinition> validated =
                        new LinkedHashMap<>();

                for (Map.Entry<String, ArmorDefinition> entry
                        : parsed.entrySet()) {

                    String itemId =
                            normalizeItemId(entry.getKey());

                    ArmorDefinition definition =
                            entry.getValue();

                    validate(itemId, definition);

                    if (validated.put(
                            itemId,
                            definition
                    ) != null) {

                        throw new IllegalStateException(
                                "Duplicate armor item id after normalization: "
                                        + itemId
                        );
                    }
                }

                DEFINITIONS.clear();
                DEFINITIONS.putAll(validated);
                loaded = true;

                RaceCore.LOGGER.info(
                        "RaceCore loaded {} armor definitions.",
                        DEFINITIONS.size()
                );
            }
        } catch (Exception exception) {
            throw new IllegalStateException(
                    "Failed to load RaceCore armor definitions.",
                    exception
            );
        }
    }

    private static void ensureLoaded() {
        if (!loaded) {
            load();
        }
    }

    private static void validate(
            String itemId,
            ArmorDefinition definition
    ) {
        if (itemId.isBlank()
                || !itemId.contains(":")) {

            throw new IllegalStateException(
                    "Invalid armor item id: '"
                            + itemId
                            + "'"
            );
        }

        if (definition == null) {
            throw new IllegalStateException(
                    "Armor item '"
                            + itemId
                            + "' has no definition."
            );
        }

        if (!Double.isFinite(definition.armor())
                || definition.armor() < 0.0) {

            throw new IllegalStateException(
                    "Armor item '"
                            + itemId
                            + "' has invalid armor value: "
                            + definition.armor()
            );
        }

        if (definition.weight() == null) {
            throw new IllegalStateException(
                    "Armor item '"
                            + itemId
                            + "' has no weight class."
            );
        }

        if (definition.protectionCapacityBonus() < 0) {
            throw new IllegalStateException(
                    "Armor item '"
                            + itemId
                            + "' has invalid protectionCapacityBonus: "
                            + definition.protectionCapacityBonus()
            );
        }
    }

    public static ArmorDefinition get(String itemId) {
        ensureLoaded();
        return DEFINITIONS.get(
                normalizeItemId(itemId)
        );
    }

    public static boolean contains(String itemId) {
        ensureLoaded();
        return DEFINITIONS.containsKey(
                normalizeItemId(itemId)
        );
    }

    public static int size() {
        ensureLoaded();
        return DEFINITIONS.size();
    }

    public static Map<String, ArmorDefinition> all() {
        ensureLoaded();
        return Collections.unmodifiableMap(
                DEFINITIONS
        );
    }

    private static String normalizeItemId(
            String itemId
    ) {
        return itemId == null
                ? ""
                : itemId.trim()
                        .toLowerCase(Locale.ROOT);
    }
}
