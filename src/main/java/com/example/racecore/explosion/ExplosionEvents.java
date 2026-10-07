package com.example.racecore.explosion;

import com.example.racecore.RaceCore;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.Entity;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.level.ExplosionEvent;

/**
 * Generic data-driven terrain protection for selected explosion sources.
 *
 * Only affected blocks are removed from the detonation list. Entity damage and
 * explosion knockback remain intact.
 */
@EventBusSubscriber(modid = RaceCore.MODID)
public final class ExplosionEvents {
    private ExplosionEvents() {
    }

    @SubscribeEvent
    public static void onExplosionDetonate(
            ExplosionEvent.Detonate event
    ) {
        Entity source =
                event.getExplosion()
                        .getDirectSourceEntity();

        if (source == null) {
            return;
        }

        Identifier sourceId =
                BuiltInRegistries.ENTITY_TYPE
                        .getKey(
                                source.getType()
                        );

        if (!ExplosionRegistry
                .preventsBlockDamage(
                        sourceId.toString()
                )) {

            return;
        }

        /*
         * Keep the explosion itself: players/mobs can still take damage and
         * knockback. Only terrain destruction is suppressed.
         */
        event.getAffectedBlocks()
                .clear();
    }
}
