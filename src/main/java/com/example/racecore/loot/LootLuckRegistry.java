package com.example.racecore.loot;

import com.example.racecore.RaceCore;
import com.google.gson.Gson;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

public final class LootLuckRegistry {
    private static final Gson GSON = new Gson();

    private static LootLuckSettings settings;
    private static boolean loaded;

    private LootLuckRegistry() {
    }

    public static synchronized void load() {
        if (loaded) {
            return;
        }

        String path =
                "/data/" + RaceCore.MODID + "/loot_luck.json";

        try (InputStream stream =
                     LootLuckRegistry.class.getResourceAsStream(path)) {

            if (stream == null) {
                throw new IllegalStateException(
                        "Missing loot Luck settings file: " + path
                );
            }

            try (InputStreamReader reader =
                         new InputStreamReader(
                                 stream,
                                 StandardCharsets.UTF_8
                         )) {

                LootLuckSettings parsed =
                        GSON.fromJson(
                                reader,
                                LootLuckSettings.class
                        );

                validate(parsed);

                settings = parsed;
                loaded = true;

                RaceCore.LOGGER.info(
                        "RaceCore loot Luck loaded. "
                                + "weightRatio={}, discoveryRollsPerLuck={}, "
                                + "quantityPerLuck={}",
                        settings.maxQualityWeightRatio(),
                        settings.bonusDiscoveryRollsPerLuck(),
                        settings.quantityBonusPerLuck()
                );
            }
        } catch (Exception exception) {
            throw new IllegalStateException(
                    "Failed to load RaceCore loot Luck settings.",
                    exception
            );
        }
    }

    private static void validate(
            LootLuckSettings parsed
    ) {
        if (parsed == null) {
            throw new IllegalStateException(
                    "loot_luck.json did not contain valid settings."
            );
        }

        requireFiniteAbove(
                "maxQualityWeightRatio",
                parsed.maxQualityWeightRatio(),
                1.0
        );

        requireNonNegativeFinite(
                "bonusDiscoveryRollsPerLuck",
                parsed.bonusDiscoveryRollsPerLuck()
        );

        requireNonNegativeFinite(
                "quantityBonusPerLuck",
                parsed.quantityBonusPerLuck()
        );

        if (parsed.maxBonusDiscoveryRolls() < 0
                || parsed.maxBonusDiscoveryRolls() > 64) {

            throw new IllegalStateException(
                    "maxBonusDiscoveryRolls must be between 0 and 64."
            );
        }

        requireFiniteAbove(
                "maxQuantityMultiplier",
                parsed.maxQuantityMultiplier(),
                1.0
        );

        if (parsed.enabledPathPrefixes() == null) {
            throw new IllegalStateException(
                    "enabledPathPrefixes cannot be null."
            );
        }

        if (parsed.qualityOverrides() == null) {
            throw new IllegalStateException(
                    "qualityOverrides cannot be null."
            );
        }

        for (Map.Entry<String, Map<String, Double>> table
                : parsed.qualityOverrides().entrySet()) {

            if (table.getValue() == null) {
                throw new IllegalStateException(
                        "qualityOverrides entry '"
                                + table.getKey()
                                + "' cannot be null."
                );
            }

            for (Map.Entry<String, Double> item
                    : table.getValue().entrySet()) {

                double value =
                        item.getValue() == null
                                ? Double.NaN
                                : item.getValue();

                if (!Double.isFinite(value)
                        || value < 0.0
                        || value > 1.0) {

                    throw new IllegalStateException(
                            "Quality override "
                                    + table.getKey()
                                    + " -> "
                                    + item.getKey()
                                    + " must be 0..1."
                    );
                }
            }
        }
    }

    private static void requireNonNegativeFinite(
            String name,
            double value
    ) {
        if (!Double.isFinite(value)
                || value < 0.0) {

            throw new IllegalStateException(
                    name
                            + " must be non-negative and finite. Got: "
                            + value
            );
        }
    }

    private static void requireFiniteAbove(
            String name,
            double value,
            double minimumExclusive
    ) {
        if (!Double.isFinite(value)
                || value <= minimumExclusive) {

            throw new IllegalStateException(
                    name
                            + " must be > "
                            + minimumExclusive
                            + ". Got: "
                            + value
            );
        }
    }

    private static void ensureLoaded() {
        if (!loaded) {
            load();
        }
    }

    public static LootLuckSettings settings() {
        ensureLoaded();
        return settings;
    }

    public static boolean enabledForPath(
            String path
    ) {
        LootLuckSettings current =
                settings();

        if (!current.enabled()) {
            return false;
        }

        for (String prefix
                : current.enabledPathPrefixes()) {

            if (path.equals(prefix)
                    || path.startsWith(prefix)) {
                return true;
            }
        }

        return false;
    }
}
