package com.example.racecore.race;

import com.example.racecore.RaceCore;
import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

/**
 * Loads races.json using one shared "defaults" object plus per-race overrides.
 *
 * Example:
 *
 * {
 *   "defaults": {
 *      "maxHealth": 100,
 *      "xpBonus": 0,
 *      ...
 *   },
 *
 *   "human": {
 *      "displayName": "Human",
 *      "xpBonus": 0.10
 *   },
 *
 *   "dwarf": {
 *      "displayName": "Dwarf",
 *      "maxHealth": 150,
 *      "fortuneBonus": 1
 *   }
 * }
 *
 * Every RaceDefinition handed to gameplay code is fully populated after the
 * merge, so ability code never needs race-specific fallback values.
 */
public final class RaceRegistry {
    private static final Gson GSON = new Gson();

    private static final Map<String, RaceDefinition> RACES =
            new LinkedHashMap<>();

    private RaceRegistry() {
    }

    public static synchronized void load() {
        String path =
                "/data/" + RaceCore.MODID + "/races.json";

        try (InputStream stream =
                     RaceRegistry.class.getResourceAsStream(path)) {

            if (stream == null) {
                throw new IllegalStateException(
                        "Missing race definition file: " + path
                );
            }

            try (InputStreamReader reader =
                         new InputStreamReader(
                                 stream,
                                 StandardCharsets.UTF_8
                         )) {

                JsonObject root =
                        GSON.fromJson(reader, JsonObject.class);

                if (root == null || root.isEmpty()) {
                    throw new IllegalStateException(
                            "No races were defined in " + path
                    );
                }

                if (!root.has("defaults")
                        || !root.get("defaults").isJsonObject()) {

                    throw new IllegalStateException(
                            "races.json must contain a top-level "
                                    + "'defaults' object."
                    );
                }

                JsonObject defaults =
                        root.getAsJsonObject("defaults");

                Map<String, RaceDefinition> validated =
                        new LinkedHashMap<>();

                for (Map.Entry<String, JsonElement> entry
                        : root.entrySet()) {

                    if (entry.getKey().equals("defaults")) {
                        continue;
                    }

                    if (!entry.getValue().isJsonObject()) {
                        throw new IllegalStateException(
                                "Race '" + entry.getKey()
                                        + "' must be a JSON object."
                        );
                    }

                    String id =
                            normalizeId(entry.getKey());

                    JsonObject merged =
                            defaults.deepCopy();

                    for (Map.Entry<String, JsonElement> override
                            : entry.getValue()
                            .getAsJsonObject()
                            .entrySet()) {

                        merged.add(
                                override.getKey(),
                                override.getValue().deepCopy()
                        );
                    }

                    RaceDefinition race =
                            GSON.fromJson(
                                    merged,
                                    RaceDefinition.class
                            );

                    validate(id, race);

                    if (validated.put(id, race) != null) {
                        throw new IllegalStateException(
                                "Duplicate race id after normalization: "
                                        + id
                        );
                    }
                }

                if (!validated.containsKey("human")) {
                    throw new IllegalStateException(
                            "races.json must contain a 'human' race."
                    );
                }

                RACES.clear();
                RACES.putAll(validated);

                RaceCore.LOGGER.info(
                        "Loaded {} RaceCore races with shared defaults.",
                        RACES.size()
                );
            }

        } catch (Exception exception) {
            throw new IllegalStateException(
                    "Failed to load RaceCore races.",
                    exception
            );
        }
    }

    private static void validate(
            String id,
            RaceDefinition race
    ) {
        if (id.isBlank()) {
            throw new IllegalStateException(
                    "Race id cannot be blank."
            );
        }

        if (race == null) {
            throw new IllegalStateException(
                    "Race '" + id + "' has no definition."
            );
        }

        if (race.displayName() == null
                || race.displayName().isBlank()) {

            throw new IllegalStateException(
                    "Race '" + id + "' has no displayName."
            );
        }

        requirePositiveFinite(id, "maxHealth", race.maxHealth());
        requirePositiveFinite(id, "scale", race.scale());
        requirePositiveFinite(id, "stepHeight", race.stepHeight());
        requirePositiveFinite(id, "jumpHeight", race.jumpHeight());
        requirePositiveFinite(
                id,
                "movementSpeedMultiplier",
                race.movementSpeedMultiplier()
        );
        requirePositiveFinite(
                id,
                "reachMultiplier",
                race.reachMultiplier()
        );
        requireNonNegativeFinite(
                id,
                "safeFallDistance",
                race.safeFallDistance()
        );

        if (race.enchantmentCapacity() < 0) {
            fail(id, "enchantmentCapacity",
                    race.enchantmentCapacity());
        }

        requireNonNegativeFinite(id, "xpBonus", race.xpBonus());
        requirePositiveFinite(
                id,
                "miningSpeedMultiplier",
                race.miningSpeedMultiplier()
        );

        if (race.fortuneBonus() < 0) {
            fail(id, "fortuneBonus", race.fortuneBonus());
        }

        if (race.cropFortuneBonus() < 0) {
            fail(id, "cropFortuneBonus",
                    race.cropFortuneBonus());
        }

        requireRange01(
                id,
                "knockbackResistanceBonus",
                race.knockbackResistanceBonus()
        );

        requireNonNegativeFinite(
                id,
                "lightArmorPenaltyMultiplier",
                race.lightArmorPenaltyMultiplier()
        );
        requireNonNegativeFinite(
                id,
                "mediumArmorPenaltyMultiplier",
                race.mediumArmorPenaltyMultiplier()
        );
        requireNonNegativeFinite(
                id,
                "heavyArmorPenaltyMultiplier",
                race.heavyArmorPenaltyMultiplier()
        );

        requireNonNegativeFinite(
                id,
                "outgoingDamageMultiplier",
                race.outgoingDamageMultiplier()
        );
        requireNonNegativeFinite(
                id,
                "incomingDamageMultiplier",
                race.incomingDamageMultiplier()
        );
        requireNonNegativeFinite(
                id,
                "nonNetherIncomingDamageMultiplier",
                race.nonNetherIncomingDamageMultiplier()
        );
        requireNonNegativeFinite(
                id,
                "dragonDamageMultiplier",
                race.dragonDamageMultiplier()
        );
        requireNonNegativeFinite(
                id,
                "poisonDamageMultiplier",
                race.poisonDamageMultiplier()
        );
        requireNonNegativeFinite(
                id,
                "fireDamageMultiplier",
                race.fireDamageMultiplier()
        );
        requireNonNegativeFinite(
                id,
                "fireworkDamageMultiplier",
                race.fireworkDamageMultiplier()
        );
        requireNonNegativeFinite(
                id,
                "pickaxeDamageMultiplier",
                race.pickaxeDamageMultiplier()
        );
        requireNonNegativeFinite(
                id,
                "passiveRegenerationPerSecond",
                race.passiveRegenerationPerSecond()
        );

        requireNonNegativeFinite(
                id,
                "foodHungerMultiplier",
                race.foodHungerMultiplier()
        );
        requireNonNegativeFinite(
                id,
                "foodHealing",
                race.foodHealing()
        );

        if (race.darkLightThreshold() < 0
                || race.darkLightThreshold() > 15) {
            fail(
                    id,
                    "darkLightThreshold",
                    race.darkLightThreshold()
            );
        }

        requirePositiveFinite(
                id,
                "darkMovementMultiplier",
                race.darkMovementMultiplier()
        );
        requireNonNegativeFinite(
                id,
                "darkDamageMultiplier",
                race.darkDamageMultiplier()
        );
        requireNonNegativeFinite(
                id,
                "brightDamageMultiplier",
                race.brightDamageMultiplier()
        );

        requirePositiveFinite(
                id,
                "netherMovementMultiplier",
                race.netherMovementMultiplier()
        );
        requireNonNegativeFinite(
                id,
                "netherDamageMultiplier",
                race.netherDamageMultiplier()
        );

        requirePositiveFinite(
                id,
                "forestMovementMultiplier",
                race.forestMovementMultiplier()
        );
        requireNonNegativeFinite(
                id,
                "leafClimbSpeed",
                race.leafClimbSpeed()
        );

        requireNonNegativeFinite(
                id,
                "crackedIncomingDamageMultiplier",
                race.crackedIncomingDamageMultiplier()
        );

        if (race.crackedDurationTicks() < 0) {
            fail(
                    id,
                    "crackedDurationTicks",
                    race.crackedDurationTicks()
            );
        }

        requireNonNegativeFinite(
                id,
                "chargedJumpMinHeight",
                race.chargedJumpMinHeight()
        );
        requireNonNegativeFinite(
                id,
                "chargedJumpMaxHeight",
                race.chargedJumpMaxHeight()
        );

        if (race.chargedJumpMaxHeight()
                < race.chargedJumpMinHeight()) {
            fail(
                    id,
                    "chargedJumpMaxHeight",
                    race.chargedJumpMaxHeight()
            );
        }

        if (race.chargedJumpStartDelayTicks() < 0) {
            fail(
                    id,
                    "chargedJumpStartDelayTicks",
                    race.chargedJumpStartDelayTicks()
            );
        }

        if (race.chargedJumpMaxChargeTicks() < 1) {
            fail(
                    id,
                    "chargedJumpMaxChargeTicks",
                    race.chargedJumpMaxChargeTicks()
            );
        }

        requireNonNegativeFinite(
                id,
                "glideFallSpeed",
                race.glideFallSpeed()
        );
        requireNonNegativeFinite(
                id,
                "glideForwardAcceleration",
                race.glideForwardAcceleration()
        );
        requireNonNegativeFinite(
                id,
                "glideMaxHorizontalSpeed",
                race.glideMaxHorizontalSpeed()
        );
    }

    private static void requirePositiveFinite(
            String raceId,
            String field,
            double value
    ) {
        if (!Double.isFinite(value) || value <= 0.0) {
            fail(raceId, field, value);
        }
    }

    private static void requireNonNegativeFinite(
            String raceId,
            String field,
            double value
    ) {
        if (!Double.isFinite(value) || value < 0.0) {
            fail(raceId, field, value);
        }
    }

    private static void requireRange01(
            String raceId,
            String field,
            double value
    ) {
        if (!Double.isFinite(value)
                || value < 0.0
                || value > 1.0) {

            fail(raceId, field, value);
        }
    }

    private static void fail(
            String raceId,
            String field,
            Object value
    ) {
        throw new IllegalStateException(
                "Race '" + raceId
                        + "' has invalid "
                        + field
                        + ": "
                        + value
        );
    }

    public static String normalizeId(String id) {
        return id == null
                ? ""
                : id.trim().toLowerCase(Locale.ROOT);
    }

    public static RaceDefinition get(String id) {
        return RACES.get(normalizeId(id));
    }

    public static RaceDefinition getOrHuman(String id) {
        RaceDefinition race = get(id);

        return race != null
                ? race
                : RACES.get("human");
    }

    public static boolean contains(String id) {
        return RACES.containsKey(normalizeId(id));
    }

    public static Collection<String> ids() {
        return RACES.keySet();
    }

    public static Collection<RaceDefinition> all() {
        return RACES.values();
    }
}
