package com.example.racecore.crafting;

import com.example.racecore.RaceCore;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;

@EventBusSubscriber(modid = RaceCore.MODID)
public final class CraftedEquipmentEvents {
    private CraftedEquipmentEvents() {
    }

    @SubscribeEvent
    public static void onItemCrafted(
            PlayerEvent.ItemCraftedEvent event
    ) {
        if (event.getEntity()
                .level()
                .isClientSide()) {
            return;
        }

        CraftedEquipmentManager
                .applyCraftingTraits(
                        event.getEntity(),
                        event.getCrafting()
                );
    }
}
