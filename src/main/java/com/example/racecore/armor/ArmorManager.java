package com.example.racecore.armor;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.entity.EquipmentSlot;

/**
 * Read-only interface between equipped items and RaceCore armor systems.
 *
 * Step 3A does NOT change damage reduction or movement speed.
 */
public final class ArmorManager {
    private ArmorManager() {
    }

    public static ArmorDefinition getDefinition(ItemStack stack) {
        if (stack == null || stack.isEmpty()) {
            return null;
        }

        Identifier itemId =
                BuiltInRegistries.ITEM.getKeyOrNull(stack.getItem());

        if (itemId == null) {
            return null;
        }

        return ArmorRegistry.get(itemId.toString());
    }

    public static double getArmorValue(ItemStack stack) {
        ArmorDefinition definition = getDefinition(stack);
        return definition == null ? 0.0 : definition.armor();
    }

    public static ArmorWeight getWeight(ItemStack stack) {
        ArmorDefinition definition = getDefinition(stack);
        return definition == null ? null : definition.weight();
    }

    /**
     * Aggregates only the four normal armor slots returned by getArmorSlots().
     *
     * Unmapped equipped items are counted separately rather than silently being
     * treated as a known armor category. This will make it easy to notice when
     * custom/modded armor needs a RaceCore definition later.
     */
    public static ArmorStats getStats(LivingEntity entity) {
        if (entity == null) {
            return ArmorStats.EMPTY;
        }

        double armor = 0.0;
        int light = 0;
        int medium = 0;
        int heavy = 0;
        int unmapped = 0;

        EquipmentSlot[] armorSlots = {
                EquipmentSlot.HEAD,
                EquipmentSlot.CHEST,
                EquipmentSlot.LEGS,
                EquipmentSlot.FEET
        };

        for (EquipmentSlot slot : armorSlots) {
            ItemStack stack = entity.getItemBySlot(slot);

            if (stack.isEmpty()) {
                continue;
            }

            ArmorDefinition definition = getDefinition(stack);

            if (definition == null) {
                unmapped++;
                continue;
            }

            armor += definition.armor();

            switch (definition.weight()) {
                case LIGHT -> light++;
                case MEDIUM -> medium++;
                case HEAVY -> heavy++;
            }
        }

        return new ArmorStats(
                armor,
                light,
                medium,
                heavy,
                unmapped
        );
    }

    public static double getTotalArmor(LivingEntity entity) {
        return getStats(entity).totalArmor();
    }
}
