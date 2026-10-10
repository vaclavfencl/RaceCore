package com.example.racecore.world.encounter;

import com.example.racecore.RaceCore;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import net.minecraft.resources.Identifier;

public final class EncounterPoolRegistry {
    public static final String DAY = "day";
    public static final String NIGHT = "night";
    public static final String HUNT_DAY = "hunt_day";
    public static final String HUNT_NIGHT = "hunt_night";

    private static final Gson GSON = new Gson();
    private static final Map<String, EncounterPool> POOLS = new LinkedHashMap<>();
    private static boolean loaded;

    private EncounterPoolRegistry() {
    }

    public static synchronized EncounterPool get(String id) {
        ensureLoaded();
        return POOLS.get(id);
    }

    public static EncounterPoolEntry pick(
            String id,
            net.minecraft.server.level.ServerLevel level,
            net.minecraft.core.BlockPos pos,
            net.minecraft.util.RandomSource random
    ) {
        EncounterPool pool = get(id);
        return pool == null ? null : pool.pick(level, pos, random);
    }

    public static EncounterPoolEntry pickIncludingSpecial(
            String id,
            net.minecraft.server.level.ServerLevel level,
            net.minecraft.core.BlockPos pos,
            net.minecraft.util.RandomSource random
    ) {
        EncounterPool pool = get(id);
        return pool == null
                ? null
                : pool.pickIncludingSpecial(level, pos, random);
    }

    /**
     * Picks one member for a mixed hunt. Entries that already reached their
     * maxPerEncounter cap are excluded. The picker guarantees two different
     * mob types when the pool has enough eligible entries, then returns to the
     * normal weights for the remaining Hunt members.
     */
    public static EncounterPoolEntry pickMixedHuntMember(
            String id,
            net.minecraft.server.level.ServerLevel level,
            net.minecraft.core.BlockPos pos,
            net.minecraft.util.RandomSource random,
            Map<Identifier, Integer> currentCounts,
            java.util.Set<Identifier> usedThisCycle
    ) {
        EncounterPool pool = get(id);
        return pool == null
                ? null
                : pool.pickMixedHuntMember(
                        level,
                        pos,
                        random,
                        currentCounts,
                        usedThisCycle
                );
    }

    private static synchronized void ensureLoaded() {
        if (loaded) {
            return;
        }

        Map<String, EncounterPool> parsed = new LinkedHashMap<>();
        parsed.put(DAY, loadPool(DAY));
        parsed.put(NIGHT, loadPool(NIGHT));
        parsed.put(HUNT_DAY, loadPool(HUNT_DAY));
        parsed.put(HUNT_NIGHT, loadPool(HUNT_NIGHT));

        POOLS.clear();
        POOLS.putAll(parsed);
        loaded = true;

        RaceCore.LOGGER.info(
                "Loaded {} RaceCore encounter pools: {}",
                POOLS.size(),
                POOLS.keySet()
        );
    }

    private static EncounterPool loadPool(String id) {
        String path = "/data/" + RaceCore.MODID
                + "/encounter_pools/" + id + ".json";

        try (InputStream stream = EncounterPoolRegistry.class.getResourceAsStream(path)) {
            if (stream == null) {
                throw new IllegalStateException("Missing encounter pool file: " + path);
            }

            try (InputStreamReader reader = new InputStreamReader(stream, StandardCharsets.UTF_8)) {
                JsonObject root = GSON.fromJson(reader, JsonObject.class);
                if (root == null) {
                    throw new IllegalStateException("Encounter pool is empty: " + path);
                }

                List<EncounterPoolEntry> entries = parseEntries(
                        id,
                        "entries",
                        root.getAsJsonArray("entries")
                );

                Map<String, List<EncounterPoolEntry>> overrides = new LinkedHashMap<>();
                JsonObject overrideRoot = root.has("biomeOverrides")
                        && root.get("biomeOverrides").isJsonObject()
                        ? root.getAsJsonObject("biomeOverrides")
                        : null;

                if (overrideRoot != null) {
                    for (Map.Entry<String, JsonElement> override : overrideRoot.entrySet()) {
                        if (!override.getValue().isJsonArray()) {
                            throw new IllegalStateException(
                                    "Pool '" + id + "' biome override '"
                                            + override.getKey() + "' must be an array."
                            );
                        }

                        overrides.put(
                                override.getKey(),
                                parseEntries(
                                        id,
                                        "biomeOverrides." + override.getKey(),
                                        override.getValue().getAsJsonArray()
                                )
                        );
                    }
                }

                return new EncounterPool(id, entries, overrides);
            }
        } catch (Exception exception) {
            throw new IllegalStateException(
                    "Failed to load RaceCore encounter pool '" + id + "'.",
                    exception
            );
        }
    }

    private static List<EncounterPoolEntry> parseEntries(
            String poolId,
            String section,
            JsonArray array
    ) {
        if (array == null || array.size() == 0) {
            throw new IllegalStateException(
                    "Encounter pool '" + poolId + "' section '"
                            + section + "' has no entries."
            );
        }

        List<EncounterPoolEntry> result = new ArrayList<>();

        for (JsonElement element : array) {
            if (!element.isJsonObject()) {
                throw new IllegalStateException(
                        "Encounter pool '" + poolId + "' section '"
                                + section + "' contains a non-object entry."
                );
            }

            JsonObject object = element.getAsJsonObject();
            String entityText = requiredString(object, "entity", poolId, section);
            Identifier entityId = Identifier.tryParse(entityText);

            if (entityId == null) {
                throw new IllegalStateException(
                        "Encounter pool '" + poolId + "' has invalid entity id: "
                                + entityText
                );
            }

            int weight = requiredPositiveInt(object, "weight", poolId, section);
            int min = object.has("min") ? object.get("min").getAsInt() : 1;
            int max = object.has("max") ? object.get("max").getAsInt() : min;

            boolean huntEligible = !object.has("huntEligible")
                    || object.get("huntEligible").getAsBoolean();

            int maxPerEncounter = object.has("maxPerEncounter")
                    ? object.get("maxPerEncounter").getAsInt()
                    : Integer.MAX_VALUE;

            Identifier biomeTag = null;
            if (object.has("biomeTag")) {
                String biomeTagText = object.get("biomeTag")
                        .getAsString()
                        .trim();

                biomeTag = Identifier.tryParse(biomeTagText);
                if (biomeTag == null) {
                    throw new IllegalStateException(
                            "Encounter pool '" + poolId + "' entry '"
                                    + entityId + "' has invalid biomeTag: "
                                    + biomeTagText
                    );
                }
            }

            if (min < 1 || max < min) {
                throw new IllegalStateException(
                        "Encounter pool '" + poolId + "' entry '"
                                + entityId + "' has invalid min/max: "
                                + min + "/" + max
                );
            }

            if (maxPerEncounter < 1) {
                throw new IllegalStateException(
                        "Encounter pool '" + poolId + "' entry '"
                                + entityId + "' has invalid maxPerEncounter: "
                                + maxPerEncounter
                );
            }

            result.add(
                    new EncounterPoolEntry(
                            entityId,
                            weight,
                            min,
                            max,
                            huntEligible,
                            maxPerEncounter,
                            biomeTag
                    )
            );
        }

        return List.copyOf(result);
    }

    private static String requiredString(
            JsonObject object,
            String field,
            String poolId,
            String section
    ) {
        if (!object.has(field) || !object.get(field).isJsonPrimitive()) {
            throw new IllegalStateException(
                    "Encounter pool '" + poolId + "' section '"
                            + section + "' entry is missing '" + field + "'."
            );
        }

        String value = object.get(field).getAsString().trim();
        if (value.isEmpty()) {
            throw new IllegalStateException(
                    "Encounter pool '" + poolId + "' has blank '" + field + "'."
            );
        }

        return value;
    }

    private static int requiredPositiveInt(
            JsonObject object,
            String field,
            String poolId,
            String section
    ) {
        if (!object.has(field)) {
            throw new IllegalStateException(
                    "Encounter pool '" + poolId + "' section '"
                            + section + "' entry is missing '" + field + "'."
            );
        }

        int value = object.get(field).getAsInt();
        if (value <= 0) {
            throw new IllegalStateException(
                    "Encounter pool '" + poolId + "' has non-positive '"
                            + field + "': " + value
            );
        }

        return value;
    }
}
