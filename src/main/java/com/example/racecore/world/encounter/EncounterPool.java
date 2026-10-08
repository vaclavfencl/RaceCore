package com.example.racecore.world.encounter;

import java.util.List;
import java.util.Map;
import net.minecraft.core.Holder;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.biome.Biome;

/**
 * Immutable, data-driven mob pool.
 *
 * A pool has normal entries plus optional exact-biome overrides. The override
 * replaces the default list for that biome, which lets the day pool preserve
 * the old desert Husk ratios without putting desert logic in Java.
 */
public final class EncounterPool {
    private final String id;
    private final List<EncounterPoolEntry> entries;
    private final Map<String, List<EncounterPoolEntry>> biomeOverrides;

    EncounterPool(
            String id,
            List<EncounterPoolEntry> entries,
            Map<String, List<EncounterPoolEntry>> biomeOverrides
    ) {
        this.id = id;
        this.entries = List.copyOf(entries);
        this.biomeOverrides = Map.copyOf(biomeOverrides);
    }

    public String id() {
        return id;
    }

    public EncounterPoolEntry pick(
            ServerLevel level,
            net.minecraft.core.BlockPos pos,
            RandomSource random
    ) {
        List<EncounterPoolEntry> candidates = entriesFor(level, pos);

        int totalWeight = 0;
        for (EncounterPoolEntry entry : candidates) {
            totalWeight += entry.weight();
        }

        if (totalWeight <= 0) {
            return null;
        }

        int roll = random.nextInt(totalWeight);

        for (EncounterPoolEntry entry : candidates) {
            roll -= entry.weight();
            if (roll < 0) {
                return entry;
            }
        }

        return candidates.getLast();
    }

    private List<EncounterPoolEntry> entriesFor(
            ServerLevel level,
            net.minecraft.core.BlockPos pos
    ) {
        Holder<Biome> biome = level.getBiome(pos);

        String biomeId = biome.unwrapKey()
                .map(ResourceKey::identifier)
                .map(Object::toString)
                .orElse("");

        List<EncounterPoolEntry> override = biomeOverrides.get(biomeId);
        return override == null || override.isEmpty()
                ? entries
                : override;
    }
}
