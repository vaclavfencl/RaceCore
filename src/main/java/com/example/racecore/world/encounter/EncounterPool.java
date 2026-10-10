package com.example.racecore.world.encounter;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.tags.TagKey;
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
            if (entry.huntEligible()
                    && isBiomeEligible(entry, level, pos)) {
                candidates.add(entry);
            }
        }

        return pickWeighted(candidates, random);
    }

    /**
     * Mixed-hunt picker. It keeps the pool weights and per-entry caps while
     * guaranteeing variety at the start of a multi-member Hunt. Until two
     * different mob types have been selected, already-used types are excluded.
     * After that, the full weighted pool is used normally again.
     */
    public EncounterPoolEntry pickMixedHuntMember(
            ServerLevel level,
            net.minecraft.core.BlockPos pos,
            RandomSource random,
            Map<Identifier, Integer> currentCounts,
            Set<Identifier> usedThisCycle
    ) {
        List<EncounterPoolEntry> eligible = new ArrayList<>();

        for (EncounterPoolEntry entry : entriesFor(level, pos)) {
            if (!entry.huntEligible()
                    || !isBiomeEligible(entry, level, pos)) {
                continue;
            }

            int current = currentCounts.getOrDefault(
                    entry.entityId(),
                    0
            );

            if (current < entry.maxPerEncounter()) {
                eligible.add(entry);
            }
        }

        if (eligible.isEmpty()) {
            return null;
        }

        if (usedThisCycle.size() < 2) {
            List<EncounterPoolEntry> diverse = new ArrayList<>();

            for (EncounterPoolEntry entry : eligible) {
                if (!usedThisCycle.contains(entry.entityId())) {
                    diverse.add(entry);
                }
            }

            if (!diverse.isEmpty()) {
                return pickWeighted(diverse, random);
            }
        }

        return pickWeighted(eligible, random);
    }

    /**
     * Full weighted pool used only by RarePoolSpawnEvents.
     */
    public EncounterPoolEntry pickIncludingSpecial(
            ServerLevel level,
            net.minecraft.core.BlockPos pos,
            RandomSource random
    ) {
        List<EncounterPoolEntry> candidates = new ArrayList<>();

        for (EncounterPoolEntry entry : entriesFor(level, pos)) {
            if (isBiomeEligible(entry, level, pos)) {
                candidates.add(entry);
            }
        }

        return pickWeighted(candidates, random);
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

    private static boolean isBiomeEligible(
            EncounterPoolEntry entry,
            ServerLevel level,
            net.minecraft.core.BlockPos pos
    ) {
        Identifier biomeTagId = entry.biomeTag();
        if (biomeTagId == null) {
            return true;
        }

        TagKey<Biome> biomeTag = TagKey.create(
                Registries.BIOME,
                biomeTagId
        );

        return level.getBiome(pos).is(biomeTag);
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
