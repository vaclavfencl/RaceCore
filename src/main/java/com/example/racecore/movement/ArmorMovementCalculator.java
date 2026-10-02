package com.example.racecore.movement;

import com.example.racecore.armor.ArmorDefinition;
import com.example.racecore.armor.ArmorManager;
import com.example.racecore.armor.ArmorWeight;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;

/**
 * Calculates one combined movement multiplier from currently equipped RaceCore armor.
 *
 * Full-set penalties:
 *   LIGHT  -> movement.json
 *   MEDIUM -> movement.json
 *   HEAVY  -> movement.json
 *
 * Slot contribution:
 *   HEAD  15%
 *   CHEST 40%
 *   LEGS  30%
 *   FEET  15%
 *
 * Example:
 * full HEAVY at 0.15 penalty => multiplier 0.85.
 *
 * This class performs no ticking and stores no state. It is only called when
 * an entity joins or NeoForge reports an equipment change.
 */
public final class ArmorMovementCalculator {
    private static final EquipmentSlot[] ARMOR_SLOTS = {
            EquipmentSlot.HEAD,
            EquipmentSlot.CHEST,
            EquipmentSlot.LEGS,
            EquipmentSlot.FEET
    };

    private ArmorMovementCalculator() {
    }

    public static double getMultiplier(LivingEntity entity) {
        double totalPenalty = 0.0;

        for (EquipmentSlot slot : ARMOR_SLOTS) {
            ItemStack stack = entity.getItemBySlot(slot);

            if (stack.isEmpty()) {
                continue;
            }

            ArmorDefinition definition =
                    ArmorManager.getDefinition(stack);

            if (definition == null) {
                // Unknown armor has no RaceCore weight yet.
                continue;
            }

            double fullSetPenalty =
                    fullSetPenalty(definition.weight());

            totalPenalty +=
                    fullSetPenalty * slotShare(slot);
        }

        /*
         * The configured values are nowhere near this limit, but the clamp makes
         * future custom armor incapable of reducing movement to zero/negative.
         */
        return Math.max(0.05, 1.0 - totalPenalty);
    }

    private static double fullSetPenalty(ArmorWeight weight) {
        MovementSettings settings =
                MovementRegistry.settings();

        return switch (weight) {
            case LIGHT ->
                    settings.lightArmorFullSetPenalty();

            case MEDIUM ->
                    settings.mediumArmorFullSetPenalty();

            case HEAVY ->
                    settings.heavyArmorFullSetPenalty();
        };
    }

    private static double slotShare(EquipmentSlot slot) {
        return switch (slot) {
            case HEAD -> 0.15;
            case CHEST -> 0.40;
            case LEGS -> 0.30;
            case FEET -> 0.15;
            default -> 0.0;
        };
    }

    public static boolean isArmorSlot(EquipmentSlot slot) {
        return slot == EquipmentSlot.HEAD
                || slot == EquipmentSlot.CHEST
                || slot == EquipmentSlot.LEGS
                || slot == EquipmentSlot.FEET;
    }
}
