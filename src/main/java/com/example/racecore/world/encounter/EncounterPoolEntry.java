package com.example.racecore.world.encounter;

import java.util.Optional;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.EntityType;

/**
 * One weighted mob entry from an encounter-pool JSON file.
 *
 * min/max describe the preferred pack size when an encounter selects this
 * entry. Systems that build mixed groups (Phase 12.2 daytime groups) use only
 * the entity and weight.
 */
public record EncounterPoolEntry(
        Identifier entityId,
        int weight,
        int min,
        int max
) {
    public Optional<EntityType<?>> resolveEntityType() {
        return BuiltInRegistries.ENTITY_TYPE.getOptional(entityId);
    }
}
