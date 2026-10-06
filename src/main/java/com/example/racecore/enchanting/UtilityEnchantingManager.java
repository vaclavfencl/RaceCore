package com.example.racecore.enchanting;

import com.example.racecore.armor.ArmorDefinition;
import com.example.racecore.armor.ArmorManager;
import com.example.racecore.armor.ProtectionManager;
import com.example.racecore.armor.ProtectionRegistry;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.ItemEnchantments;
import net.minecraft.world.item.equipment.Equippable;

/**
 * Utility enchant capacity for RaceCore armor.
 *
 * Base Utility capacity is slot-specific:
 * - Helmet:     4
 * - Chestplate: 3
 * - Leggings:   3
 * - Boots:      5
 *
 * Race enchant-capacity modifiers apply to BOTH Protection and Utility.
 * Material Utility bonuses (currently Gold +2) apply only to Utility.
 *
 * Example:
 * normal race Gold boots = 5 + 0 + 2 = 7 Utility
 * +1 enchant-cap race    = 5 + 1 + 2 = 8 Utility
 */
public final class UtilityEnchantingManager {
    private static final int HELMET_UTILITY_CAPACITY = 4;
    private static final int CHESTPLATE_UTILITY_CAPACITY = 3;
    private static final int LEGGINGS_UTILITY_CAPACITY = 3;
    private static final int BOOTS_UTILITY_CAPACITY = 5;

    private UtilityEnchantingManager() {
    }

    public static int getCapacity(
            LivingEntity entity,
            ItemStack stack
    ) {
        ArmorDefinition definition =
                ArmorManager.getDefinition(stack);

        if (definition == null) {
            return 0;
        }

        int baseCapacity =
                getBaseUtilityCapacity(stack);

        int defaultRaceCapacity =
                ProtectionRegistry
                        .settings()
                        .defaultCapacityPerPiece();

        int actualRaceCapacity =
                ProtectionManager
                        .getCapacityPerPiece(entity);

        int raceAdjustment =
                actualRaceCapacity
                        - defaultRaceCapacity;

        return Math.max(
                0,
                baseCapacity
                        + raceAdjustment
                        + Math.max(
                                0,
                                definition.utilityCapacityBonus()
                        )
        );
    }

    private static int getBaseUtilityCapacity(
            ItemStack stack
    ) {
        Equippable equippable =
                stack.get(
                        DataComponents.EQUIPPABLE
                );

        if (equippable == null) {
            return 0;
        }

        EquipmentSlot slot =
                equippable.slot();

        return switch (slot) {
            case HEAD ->
                    HELMET_UTILITY_CAPACITY;

            case CHEST ->
                    CHESTPLATE_UTILITY_CAPACITY;

            case LEGS ->
                    LEGGINGS_UTILITY_CAPACITY;

            case FEET ->
                    BOOTS_UTILITY_CAPACITY;

            default -> 0;
        };
    }

    public static int getUsedCapacity(
            ItemStack stack
    ) {
        if (ArmorManager.getDefinition(stack) == null) {
            return 0;
        }

        ItemEnchantments enchantments =
                stack.getOrDefault(
                        DataComponents.ENCHANTMENTS,
                        ItemEnchantments.EMPTY
                );

        int used = 0;

        for (Object2IntMap.Entry<Holder<Enchantment>> entry
                : enchantments.entrySet()) {

            if (!isUtilityEnchantment(
                    entry.getKey()
            )) {
                continue;
            }

            used += Math.max(
                    0,
                    entry.getIntValue()
            );
        }

        return used;
    }

    public static boolean isOverCapacity(
            LivingEntity entity,
            ItemStack stack
    ) {
        return ArmorManager.getDefinition(stack) != null
                && getUsedCapacity(stack)
                > getCapacity(entity, stack);
    }

    public static boolean isUtilityEnchantment(
            Holder<Enchantment> enchantment
    ) {
        Identifier id =
                enchantment.unwrapKey()
                        .map(key -> key.identifier())
                        .orElse(null);

        if (id == null) {
            return false;
        }

        String idString =
                id.toString();

        /*
         * Protection enchantments use the separate Protection pool.
         */
        if (ProtectionRegistry.get(idString) != null) {
            return false;
        }

        /*
         * Curses do not consume Utility capacity.
         * Mending is disabled globally in RaceCore.
         */
        return !idString.equals(
                        "minecraft:binding_curse"
                )
                && !idString.equals(
                        "minecraft:vanishing_curse"
                )
                && !idString.equals(
                        "minecraft:mending"
                );
    }
}
