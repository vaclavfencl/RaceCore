package com.example.racecore.enchanting;

import com.example.racecore.RaceCore;
import com.example.racecore.armor.ArmorManager;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.EnchantmentInstance;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.AnvilUpdateEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEnchantItemEvent;

import java.util.List;

/**
 * Enforces Utility capacity using the same race/base cap as Protection.
 */
@EventBusSubscriber(modid = RaceCore.MODID)
public final class UtilityEnchantingEvents {
    private UtilityEnchantingEvents() {
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onAnvilUpdate(
            AnvilUpdateEvent event
    ) {
        ItemStack output =
                event.getOutput();

        if (output.isEmpty()
                || ArmorManager.getDefinition(output) == null) {

            return;
        }

        if (UtilityEnchantingManager
                .isOverCapacity(
                        event.getPlayer(),
                        output
                )) {

            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void onPlayerEnchantItem(
            PlayerEnchantItemEvent event
    ) {
        ItemStack stack =
                event.getEnchantedItem();

        if (ArmorManager.getDefinition(stack) == null) {
            return;
        }

        int capacity =
                UtilityEnchantingManager
                        .getCapacity(
                                event.getEntity(),
                                stack
                        );

        int over =
                UtilityEnchantingManager
                        .getUsedCapacity(stack)
                        - capacity;

        if (over <= 0) {
            return;
        }

        List<EnchantmentInstance> applied =
                event.getEnchantments();

        for (int i = applied.size() - 1;
             i >= 0 && over > 0;
             i--) {

            EnchantmentInstance instance =
                    applied.get(i);

            if (!UtilityEnchantingManager
                    .isUtilityEnchantment(
                            instance.enchantment()
                    )) {

                continue;
            }

            int currentLevel =
                    EnchantmentHelper
                            .getEnchantmentsForCrafting(
                                    stack
                            )
                            .getLevel(
                                    instance.enchantment()
                            );

            if (currentLevel <= 0) {
                continue;
            }

            int reduction =
                    Math.min(
                            over,
                            currentLevel
                    );

            int newLevel =
                    currentLevel - reduction;

            EnchantmentHelper.updateEnchantments(
                    stack,
                    mutable -> {
                        if (newLevel <= 0) {
                            mutable.removeIf(
                                    holder ->
                                            holder.equals(
                                                    instance.enchantment()
                                            )
                            );
                        } else {
                            mutable.set(
                                    instance.enchantment(),
                                    newLevel
                            );
                        }
                    }
            );

            over -= reduction;
        }

        event.getEntity()
                .sendOverlayMessage(
                        Component.literal(
                                "Utility enchant capacity: "
                                        + capacity
                                        + ". Extra utility levels were removed."
                        )
                );
    }
}
