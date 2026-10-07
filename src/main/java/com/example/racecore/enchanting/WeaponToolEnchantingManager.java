package com.example.racecore.enchanting;

import com.example.racecore.armor.ProtectionManager;
import com.example.racecore.armor.ProtectionRegistry;
import com.example.racecore.weapon.RaceCoreWeapons;
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
 * - default weapon/tool: 4
 * - crossbow: 5
 * - trident: 5
 * - fishing rod: 7
 *
 * The race enchant-cap adjustment is then applied on top, so racial
 * enchantment bonuses/penalties still affect both Effective and Utility.
 *
 * Gold material bonus:
 * - all vanilla golden weapons/tools: +2 Utility
 * - RaceCore golden dagger/glaive/longsword: +2 Utility
 * - this bonus does NOT affect Effective capacity
 *
 * Category rule:
 * - Effective: numerical combat/work output
 * - Utility: behavior/functionality changes
 * - Both: consumes BOTH pools at its enchantment level
 *
 * Current notable category choices:
 * - Fire Aspect, Flame: Both
 * - Piercing: Effective
 * - Multishot, Quick Charge: Utility
 * - Lunge: Utility
 * - Impaling, Density, Breach: Effective
 * - Loyalty, Channeling, Riptide, Wind Burst: Utility by fallback
 * - Fortune: Effective
 * - Silk Touch: Utility
 *
 * Example with the current default race cap of 4:
 * - normal race: default tool utility 4
 * - Dwarf/Dragonborn (+1 cap): default tool utility 5
 * - Troll (-1 cap): default tool utility 3
 * - normal-race golden tool: default tool utility 6
 */
public final class WeaponToolEnchantingManager {
    private static final int DEFAULT_UTILITY_CAPACITY = 4;
    private static final int CROSSBOW_UTILITY_CAPACITY = 4;
    private static final int TRIDENT_UTILITY_CAPACITY = 4;
    private static final int FISHING_ROD_UTILITY_CAPACITY = 6;

    private static final int GOLD_UTILITY_BONUS = 2;

    private static final Set<String> EFFECTIVE_ENCHANTMENTS =
            Set.of(
                    // Direct melee combat output
                    "minecraft:sharpness",
                    "minecraft:smite",
                    "minecraft:bane_of_arthropods",
                    "minecraft:sweeping_edge",

                    // Bow / crossbow direct output
                    "minecraft:power",
                    "minecraft:piercing",

                    // Trident / mace direct output
                    "minecraft:impaling",
                    "minecraft:density",
                    "minecraft:breach",

                    // Tool / gathering output
                    "minecraft:efficiency"
            );

    /*
     * BOTH means the enchantment consumes its level from BOTH capacity pools.
     *
     * Example:
     * Fire Aspect II = 2 Effective + 2 Utility.
     */
    private static final Set<String> BOTH_ENCHANTMENTS =
            Set.of(
                    "minecraft:fire_aspect",
                    "minecraft:flame"
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
                        + getMaterialUtilityBonus(stack)
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

    private static int getMaterialUtilityBonus(
            ItemStack stack
    ) {
        return isGoldWeaponOrTool(stack)
                ? GOLD_UTILITY_BONUS
                : 0;
    }

    private static boolean isGoldWeaponOrTool(
            ItemStack stack
    ) {
        if (stack == null || stack.isEmpty()) {
            return false;
        }

        return stack.is(Items.GOLDEN_SWORD)
                || stack.is(Items.GOLDEN_AXE)
                || stack.is(Items.GOLDEN_PICKAXE)
                || stack.is(Items.GOLDEN_SHOVEL)
                || stack.is(Items.GOLDEN_HOE)
                || stack.is(RaceCoreWeapons.GOLDEN_DAGGER.get())
                || stack.is(RaceCoreWeapons.GOLDEN_GLAIVE.get())
                || stack.is(RaceCoreWeapons.GOLDEN_LONGSWORD.get());
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

        if (id == null) {
            return false;
        }

        String idString =
                id.toString();

        return EFFECTIVE_ENCHANTMENTS
                .contains(idString)
                || BOTH_ENCHANTMENTS
                .contains(idString);
    }

    public static boolean isBothEnchantment(
            Holder<Enchantment> enchantment
    ) {
        Identifier id =
                enchantment.unwrapKey()
                        .map(key -> key.identifier())
                        .orElse(null);

        return id != null
                && BOTH_ENCHANTMENTS
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

        /*
         * BOTH enchantments deliberately count in Utility as well as
         * Effective. getUsedCapacity() calls both category checks separately,
         * so their level is charged to both pools.
         */
        if (BOTH_ENCHANTMENTS
                .contains(idString)) {

            return true;
        }

        if (EFFECTIVE_ENCHANTMENTS
                .contains(idString)) {

            return false;
        }

        /*
         * Everything else defaults to Utility: Unbreaking, Looting,
         * Knockback, Punch, Infinity, Multishot, Quick Charge, Lunge,
         * Silk Touch, Loyalty, Channeling, Riptide, Wind Burst, etc.
         *
         * Curses consume no capacity and Mending is disabled globally.
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
