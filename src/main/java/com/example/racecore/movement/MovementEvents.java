package com.example.racecore.movement;

import com.example.racecore.RaceCore;
import net.minecraft.world.entity.LivingEntity;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.entity.living.LivingEquipmentChangeEvent;

/**
 * Event-driven movement updates.
 *
 * There is deliberately NO tick handler.
 */
@EventBusSubscriber(modid = RaceCore.MODID)
public final class MovementEvents {
    private MovementEvents() {
    }

    /**
     * Every living entity receives global movement scaling when it enters a level.
     *
     * Armor is also calculated here so an entity loaded from disk with armor
     * immediately gets the correct weight penalty.
     */
    @SubscribeEvent
    public static void onEntityJoin(
            EntityJoinLevelEvent event
    ) {
        if (event.getLevel().isClientSide()) {
            return;
        }

        if (!(event.getEntity() instanceof LivingEntity living)) {
            return;
        }

        MovementManager.applyGlobalMovement(living);
        MovementManager.applyArmorMovement(living);
    }

    /**
     * NeoForge fires this only when equipment actually changes.
     *
     * That means equipping, removing, swapping or breaking armor recalculates
     * the armor modifier once, rather than checking all entities every tick.
     */
    @SubscribeEvent
    public static void onEquipmentChange(
            LivingEquipmentChangeEvent event
    ) {
        if (!ArmorMovementCalculator.isArmorSlot(event.getSlot())) {
            return;
        }

        MovementManager.applyArmorMovement(
                event.getEntity()
        );
    }
}
