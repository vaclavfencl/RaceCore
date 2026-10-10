package com.example.racecore.world;

import com.example.racecore.RaceCore;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityTypes;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.FinalizeSpawnEvent;

/**
 * Removes renewable village/natural Iron Golem spawning while preserving
 * player-built golems and explicit command/structure/event spawning routes.
 */
@EventBusSubscriber(modid = RaceCore.MODID)
public final class IronGolemSpawnEvents {
    private IronGolemSpawnEvents() {
    }

    @SubscribeEvent
    public static void onFinalizeSpawn(
            FinalizeSpawnEvent event
    ) {
        if (event.getEntity().getType() != EntityTypes.IRON_GOLEM) {
            return;
        }

        EntitySpawnReason reason = event.getSpawnType();

        /*
         * Village villagers summon their renewable Iron Golems as
         * MOB_SUMMONED. NATURAL is blocked as a safety net for any ordinary
         * natural route. Player-built pumpkin-pattern golems use a different
         * spawn reason and are intentionally left untouched.
         */
        if (reason == EntitySpawnReason.NATURAL
                || reason == EntitySpawnReason.MOB_SUMMONED) {
            event.setSpawnCancelled(true);
        }
    }
}
