package com.example.racecore.race.ability;

import com.example.racecore.RaceCore;
import com.example.racecore.race.RaceManager;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.EntityMountEvent;

/**
 * Giants are too large for living mounts.
 *
 * This intentionally blocks animals (horses, donkeys, camels, pigs, striders,
 * modded rideable animals, etc.) while leaving boats and minecarts available.
 */
@EventBusSubscriber(modid = RaceCore.MODID)
public final class GiantMountEvents {
    private GiantMountEvents() {
    }

    @SubscribeEvent
    public static void onMount(EntityMountEvent event) {
        if (!event.isMounting()
                || !(event.getEntityMounting() instanceof Player player)
                || !(event.getEntityBeingMounted() instanceof Animal)) {
            return;
        }

        if ("giant".equals(RaceManager.getRaceId(player))) {
            event.setCanceled(true);
        }
    }
}
