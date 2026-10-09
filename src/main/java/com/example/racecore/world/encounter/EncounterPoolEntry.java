package com.example.racecore.world.encounter;

import java.util.Optional;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.EntityType;

public record EncounterPoolEntry(
        Identifier entityId,
        int weight,
        int min,
        int max,
        boolean huntEligible,
        int maxPerEncounter
) {
    public Optional<EntityType<?>> resolveEntityType() {
        return BuiltInRegistries.ENTITY_TYPE.getOptional(entityId);
    }
}
