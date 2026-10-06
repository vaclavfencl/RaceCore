package com.example.racecore.enchanting;

import com.example.racecore.armor.ProtectionManager;
import com.example.racecore.armor.ProtectionRegistry;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.ItemEnchantments;

import java.util.Set;

/**
 * RaceCore enchant-cap system for weapons and tools.
 *
 * Effective capacity continues to use the shared race enchant-cap system.
 *
 * Utility capacity has an item-specific base:
 * - default weapon/tool: 3
 * - crossbow: 4
 * - trident: 4
 * - fishing rod: 6
 *
 * The race enchant-cap adjustment is then applied on top, so racial
 * enchantment bonuses/penalties still affect both Effective and Utility.
 *
 * Example with the current default race cap of 4:
 * - normal race: default tool utility 3
 * - Dwarf/Dragonborn (+1 cap): default tool utility 4
 * - Troll (-1 cap): default tool utility 2
 */
public final class WeaponToolEnchantingManager {
    private static final int DEFAULT_UTILITY_CAPACITY = 3;
    private static final int CROSSBOW_UTILITY_CAPACITY = 4;
    private static final int TRIDENT_UTILITY_CAPACITY = 4;
    private static final int FISHING_ROD_UTILITY_CAPACITY = 6;

    private static final Set<String> EFFECTIVE_ENCHANTMENTS =
            Set.of(
                    // Melee / general weapon power
                    "minecraft:sharpness",
                    "minecraft:smite",
                    "minecraft:bane_of_arthropods",
                    "minecraft:fire_aspect",
                    "minecraft:looting",
                    "minecraft:sweeping_edge",
                    "minecraft:lunge",

                    // Bow / crossbow
                    "minecraft:power",
                    "minecraft:flame",
                    "minecraft:multishot",
                    "minecraft:piercing",
                    "minecraft:quick_charge",

                    // Trident / mace
                    "minecraft:impaling",
                    "minecraft:density",
                    "minecraft:breach",

                    // Mining / gathering effectiveness
                    "minecraft:efficiency",
                    "minecraft:fortune",
                    "minecraft:silk_touch"
            );

    private WeaponToolEnchantingManager() {
    }

    public static boolean isWeaponOrTool(
            ItemStack stack
    ) {
        if (stack == null || stack.isEmpty()) {
            return false;
        }

        return stack.has(DataComponents.TOOL)
                || stack.has(DataComponents.WEAPON)
                || stack.has(DataComponents.KINETIC_WEAPON)
                || stack.has(DataComponents.PIERCING_WEAPON)
                || stack.is(Items.FISHING_ROD);
    }

    public static int getEffectiveCapacity(
            LivingEntity entity,
            ItemStack stack
    ) {
        if (!isWeaponOrTool(stack)) {
            return 0;
        }

        return ProtectionManager
                .getCapacityPerPiece(entity);
    }

    public static int getUtilityCapacity(
            LivingEntity entity,
            ItemStack stack
    ) {
        if (!isWeaponOrTool(stack)) {
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
        );
    }

    private static int getBaseUtilityCapacity(
            ItemStack stack
    ) {
        if (stack.is(Items.CROSSBOW)) {
            return CROSSBOW_UTILITY_CAPACITY;
        }

        if (stack.is(Items.TRIDENT)) {
            return TRIDENT_UTILITY_CAPACITY;
        }

        if (stack.is(Items.FISHING_ROD)) {
            return FISHING_ROD_UTILITY_CAPACITY;
        }

        return DEFAULT_UTILITY_CAPACITY;
    }

    public static int getUsedEffectiveCapacity(
            ItemStack stack
    ) {
        return getUsedCapacity(
                stack,
                true
        );
    }

    public static int getUsedUtilityCapacity(
            ItemStack stack
    ) {
        return getUsedCapacity(
                stack,
                false
        );
    }

    public static boolean isOverCapacity(
            LivingEntity entity,
            ItemStack stack
    ) {
        if (!isWeaponOrTool(stack)) {
            return false;
        }

        return getUsedEffectiveCapacity(stack)
                > getEffectiveCapacity(entity, stack)
                || getUsedUtilityCapacity(stack)
                > getUtilityCapacity(entity, stack);
    }

    public static boolean isEffectiveEnchantment(
            Holder<Enchantment> enchantment
    ) {
        Identifier id =
                enchantment.unwrapKey()
                        .map(key -> key.identifier())
                        .orElse(null);

        return id != null
                && EFFECTIVE_ENCHANTMENTS
                .contains(id.toString());
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

        if (EFFECTIVE_ENCHANTMENTS
                .contains(idString)) {

            return false;
        }

        /*
         * Curses do not consume capacity.
         * Mending is disabled globally by RaceCore.
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

    private static int getUsedCapacity(
            ItemStack stack,
            boolean effective
    ) {
        if (!isWeaponOrTool(stack)) {
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

            boolean counts =
                    effective
                            ? isEffectiveEnchantment(
                                    entry.getKey()
                            )
                            : isUtilityEnchantment(
                                    entry.getKey()
                            );

            if (counts) {
                used += Math.max(
                        0,
                        entry.getIntValue()
                );
            }
        }

        return used;
    }
}
