package com.example.racecore.loot;

import com.example.racecore.RaceCore;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Items;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingDropsEvent;

/**
 * RaceCore economy rule: living non-player mobs never drop Emeralds or
 * Totems of Undying. Raids and mansion rewards will grant them through
 * separate world/container rewards, not entity loot.
 *
 * This runs after normal loot tables (and RaceCore's Loot Luck rewrite)
 * so it also catches equipped-item drops and loot modifier additions.
 */
@EventBusSubscriber(modid = RaceCore.MODID)
public final class IllagerLootEvents {
    private IllagerLootEvents() {
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onLivingDrops(LivingDropsEvent event) {
        // Player inventory drops must never be treated like hostile-mob loot.
        if (event.getEntity() instanceof Player || event.getEntity().level().isClientSide()) {
            return;
        }

        event.getDrops().removeIf(drop ->
                drop.getItem().is(Items.EMERALD)
                        || drop.getItem().is(Items.TOTEM_OF_UNDYING));
    }
}
