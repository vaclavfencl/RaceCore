package com.example.racecore.crafting;

import com.example.racecore.RaceCore;
import com.example.racecore.race.RaceManager;
import net.minecraft.core.component.DataComponents;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;

@EventBusSubscriber(modid = RaceCore.MODID)
public final class CraftedEquipmentEvents {
    public static final double HOBBIT_HOE_DURABILITY_BONUS = 0.20;
    public static final double HOBBIT_HOE_EFFICIENCY_BONUS = 0.20;

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

    /**
     * Hobbit-crafted hoes get +20% durability after the shared crafted-item
     * system has tagged the finished stack. LOWEST guarantees the generic
     * crafting traits have already been applied first.
     */
    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onHobbitHoeCrafted(
            PlayerEvent.ItemCraftedEvent event
    ) {
        if (event.getEntity().level().isClientSide()
                || !"hobbit".equals(
                        RaceManager.getRaceId(event.getEntity())
                )) {
            return;
        }

        ItemStack stack = event.getCrafting();
        if (!stack.is(ItemTags.HOES)
                || !stack.isDamageableItem()) {
            return;
        }

        int currentMax = stack.getMaxDamage();
        if (currentMax <= 0) {
            return;
        }

        stack.set(
                DataComponents.MAX_DAMAGE,
                Math.max(
                        1,
                        (int) Math.round(
                                currentMax
                                        * (1.0 + HOBBIT_HOE_DURABILITY_BONUS)
                        )
                )
        );
    }

    /**
     * +20% effective hoe mining speed. The bonus belongs to the crafted item,
     * not to the Hobbit currently holding it, so traded Hobbit tools keep their
     * racial craftsmanship identity.
     */
    @SubscribeEvent
    public static void onBreakSpeed(
            PlayerEvent.BreakSpeed event
    ) {
        ItemStack stack = event.getEntity().getMainHandItem();

        if (!stack.is(ItemTags.HOES)
                || !"hobbit".equals(
                        CraftedEquipmentManager.getCraftedRaceId(stack)
                )
                || stack.getDestroySpeed(event.getState()) <= 1.0F) {
            return;
        }

        event.setNewSpeed(
                (float) (
                        event.getNewSpeed()
                                * (1.0 + HOBBIT_HOE_EFFICIENCY_BONUS)
                )
        );
    }
}
