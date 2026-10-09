package com.example.racecore.world.encounter;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import net.minecraft.core.Holder;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.biome.Biome;

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

    /**
     * Standard existing systems use this picker.
     * huntEligible=false entries are deliberately hidden from it, so special
     * ambient mobs can never become hunt mobs.
     */
    public EncounterPoolEntry pick(
            ServerLevel level,
            net.minecraft.core.BlockPos pos,
            RandomSource random
    ) {
        List<EncounterPoolEntry> candidates = new ArrayList<>();

        for (EncounterPoolEntry entry : entriesFor(level, pos)) {
            if (entry.huntEligible()) {
                candidates.add(entry);
            }
        }

        return pickWeighted(candidates, random);
    }

    /**
     * Full weighted pool used only by RarePoolSpawnEvents.
     */
    public EncounterPoolEntry pickIncludingSpecial(
            ServerLevel level,
            net.minecraft.core.BlockPos pos,
            RandomSource random
    ) {
        return pickWeighted(entriesFor(level, pos), random);
    }

    private static EncounterPoolEntry pickWeighted(
            List<EncounterPoolEntry> candidates,
            RandomSource random
    ) {
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

        return candidates.isEmpty() ? null : candidates.getLast();
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
