package com.example.racecore.armor;

import com.example.racecore.RaceCore;
import com.google.gson.Gson;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

public final class ProtectionRegistry {
    private static final Gson GSON = new Gson();

    private static ProtectionSettings settings;
    private static boolean loaded = false;

    private ProtectionRegistry() {
    }

    public static synchronized void load() {
        if (loaded) {
            return;
        }

        String path = "/data/" + RaceCore.MODID + "/protection.json";

        try (InputStream stream =
                     ProtectionRegistry.class.getResourceAsStream(path)) {

            if (stream == null) {
                throw new IllegalStateException(
                        "Missing protection settings file: " + path);
            }

            try (InputStreamReader reader =
                         new InputStreamReader(stream, StandardCharsets.UTF_8)) {

                ProtectionSettings parsed =
                        GSON.fromJson(reader, ProtectionSettings.class);

                validate(parsed);

                Map<String, ProtectionEnchantmentDefinition> normalized =
                        new LinkedHashMap<>();

                for (Map.Entry<String, ProtectionEnchantmentDefinition> entry
                        : parsed.enchantments().entrySet()) {

                    normalized.put(
                            normalizeId(entry.getKey()),
                            entry.getValue()
                    );
                }

                settings = new ProtectionSettings(
                        parsed.defaultCapacityPerPiece(),
                        parsed.universalTypedEffectiveness(),
                        normalized
                );

                loaded = true;

                RaceCore.LOGGER.info(
                        "RaceCore protection settings loaded. Default capacity={}, universal typed effectiveness={}, enchantments={}",
                        settings.defaultCapacityPerPiece(),
                        settings.universalTypedEffectiveness(),
                        settings.enchantments().size()
                );
            }
        } catch (Exception exception) {
            throw new IllegalStateException(
                    "Failed to load RaceCore protection settings.",
                    exception
            );
        }
    }

    private static void validate(ProtectionSettings parsed) {
        if (parsed == null) {
            throw new IllegalStateException(
                    "protection.json did not contain valid settings.");
        }

        if (parsed.defaultCapacityPerPiece() < 0) {
            throw new IllegalStateException(
                    "defaultCapacityPerPiece must be >= 0.");
        }

        if (!Double.isFinite(parsed.universalTypedEffectiveness())
                || parsed.universalTypedEffectiveness() < 0.0
                || parsed.universalTypedEffectiveness() > 1.0) {

            throw new IllegalStateException(
                    "universalTypedEffectiveness must be between 0 and 1. Got: "
                            + parsed.universalTypedEffectiveness()
            );
        }

        if (parsed.enchantments() == null) {
            throw new IllegalStateException(
                    "protection.json has no enchantments section.");
        }

        for (Map.Entry<String, ProtectionEnchantmentDefinition> entry
                : parsed.enchantments().entrySet()) {

            String enchantmentId = normalizeId(entry.getKey());
            ProtectionEnchantmentDefinition definition = entry.getValue();

            if (enchantmentId.isBlank() || !enchantmentId.contains(":")) {
                throw new IllegalStateException(
                        "Invalid enchantment id: " + entry.getKey());
            }

            if (definition == null) {
                throw new IllegalStateException(
                        "Enchantment '" + enchantmentId + "' has no definition.");
            }

            if (definition.type() == null) {
                throw new IllegalStateException(
                        "Enchantment '" + enchantmentId + "' has no type.");
            }

            if (!Double.isFinite(definition.pointsPerLevel())
                    || definition.pointsPerLevel() < 0.0) {

                throw new IllegalStateException(
                        "Enchantment '" + enchantmentId
                                + "' has invalid pointsPerLevel: "
                                + definition.pointsPerLevel());
            }
        }
    }

    private static void ensureLoaded() {
        if (!loaded) {
            load();
        }
    }

    public static ProtectionSettings settings() {
        ensureLoaded();
        return settings;
    }

    public static ProtectionEnchantmentDefinition get(String enchantmentId) {
        ensureLoaded();
        return settings.enchantments().get(normalizeId(enchantmentId));
    }

    private static String normalizeId(String id) {
        return id == null ? "" : id.trim().toLowerCase(Locale.ROOT);
    }
}
