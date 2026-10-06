package com.example.racecore.armor;

import com.example.racecore.crafting.CraftedEquipmentManager;
import com.example.racecore.durability.BrokenItemManager;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;

public final class ArmorManager {
    private ArmorManager() {}

    public static ArmorDefinition getDefinition(ItemStack stack) {
        if (stack == null || stack.isEmpty()) return null;
        Identifier itemId = BuiltInRegistries.ITEM.getKeyOrNull(stack.getItem());
        return itemId == null ? null : ArmorRegistry.get(itemId.toString());
    }

    public static double getArmorValue(ItemStack stack) {
        ArmorDefinition definition = getDefinition(stack);

        if (definition == null
                || BrokenItemManager.isBroken(stack)) {
            return 0.0;
        }

        return definition.armor()
                + CraftedEquipmentManager
                .getArmorBonus(stack);
    }

    public static ArmorWeight getWeight(ItemStack stack) {
        ArmorDefinition definition = getDefinition(stack);
        return definition == null ? null : definition.weight();
    }

    public static ArmorStats getStats(LivingEntity entity) {
        if (entity == null) return ArmorStats.EMPTY;

        double armor = 0.0;
        int light = 0, medium = 0, heavy = 0, ultraHeavy = 0, unmapped = 0;
        EquipmentSlot[] armorSlots = {
                EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET
        };

        for (EquipmentSlot slot : armorSlots) {
            ItemStack stack = entity.getItemBySlot(slot);
            if (stack.isEmpty()) continue;

            ArmorDefinition definition = getDefinition(stack);

            if (definition == null) {
                unmapped++;
                continue;
            }

            if (!BrokenItemManager.isBroken(stack)) {
                armor += definition.armor()
                        + CraftedEquipmentManager
                        .getArmorBonus(stack);
            }

            switch (definition.weight()) {
                case LIGHT -> light++;
                case MEDIUM -> medium++;
                case HEAVY -> heavy++;
                case ULTRA_HEAVY -> ultraHeavy++;
            }
        }

        return new ArmorStats(
                armor,
                light,
                medium,
                heavy,
                ultraHeavy,
                unmapped
        );
    }

    public static double getTotalArmor(LivingEntity entity) {
        return getStats(entity).totalArmor();
    }
}
