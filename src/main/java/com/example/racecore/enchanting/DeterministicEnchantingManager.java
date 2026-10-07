package com.example.racecore.enchanting;

import com.example.racecore.armor.ArmorManager;
import com.example.racecore.armor.ProtectionEnchantmentDefinition;
import com.example.racecore.armor.ProtectionManager;
import com.example.racecore.armor.ProtectionRegistry;
import com.example.racecore.weapon.RaceCoreWeapons;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.EnchantmentTags;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.ItemEnchantments;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Shared rules for Phase 9.0 deterministic enchanting.
 *
 * This class deliberately does not rebalance enchantments. It only exposes
 * table eligibility, preview mutation, compatibility, costs and the capacity
 * checks RaceCore already uses elsewhere.
 */
public final class DeterministicEnchantingManager {
    private static final int GENERIC_UTILITY_CAPACITY = 4;
    private static final int RACEC0RE_PROTECTION_MAX_LEVEL = 5;

    /*
     * Axes are deliberately dual-role equipment in RaceCore:
     * they retain all normal tool enchantments, while also gaining the
     * general melee enchantments below.
     *
     * Sword-specific mechanics such as Sweeping Edge are intentionally
     * excluded. The axe still uses the SAME Effective and Utility pools;
     * being both a weapon and a tool does not grant extra capacity.
     */
    private static final Set<String> AXE_COMBAT_ENCHANTMENTS =
            Set.of(
                    "minecraft:sharpness",
                    "minecraft:smite",
                    "minecraft:bane_of_arthropods",
                    "minecraft:fire_aspect",
                    "minecraft:knockback",
                    "minecraft:looting"
            );

    private DeterministicEnchantingManager() {
    }

    public static boolean isTableEligible(
            ItemStack stack,
            Holder<Enchantment> enchantment
    ) {
        if (stack == null
                || stack.isEmpty()
                || enchantment == null) {

            return false;
        }

        if (!enchantment.is(
                EnchantmentTags.IN_ENCHANTING_TABLE
        )) {
            return false;
        }

        if (enchantment.is(
                EnchantmentTags.CURSE
        )) {
            return false;
        }

        String id =
                getEnchantmentId(
                        enchantment
                );

        /*
         * RaceCore disables Mending globally. Keep it out even if another
         * datapack accidentally re-adds it to an enchanting-table tag.
         */
        if ("minecraft:mending".equals(id)) {
            return false;
        }

        if (stack.isPrimaryItemFor(
                enchantment
        )) {
            return true;
        }

        /*
         * RaceCore special-weapon table permissions.
         *
         * Glaive:
         * - Lunge is part of the weapon's intended movement/reach identity.
         *
         * Longsword:
         * - Sweeping Edge is explicitly available.
         *
         * Flail:
         * - Sweeping Edge is explicitly available and only buffs the custom
         *   fully-charged area attack. It never buffs the flail's single-target
         *   150% attack.
         *
         * Club and daggers intentionally receive no extra permissions here.
         */
        if (RaceCoreWeapons.isGlaive(stack)
                && "minecraft:lunge".equals(id)) {

            return true;
        }

        if ((RaceCoreWeapons.isLongsword(stack)
                || RaceCoreWeapons.isFlail(stack))
                && "minecraft:sweeping_edge".equals(id)) {

            return true;
        }

        /*
         * Vanilla treats axes primarily as tools at the enchanting table.
         * RaceCore treats them as both tools and melee weapons, so explicitly
         * expose the general melee enchantments while keeping the vanilla
         * tool-enchantment set as-is.
         *
         * Axes still use the SAME Effective/Utility capacity pools. Being both
         * weapon and tool does not create a second capacity pool.
         */
        return stack.is(ItemTags.AXES)
                && AXE_COMBAT_ENCHANTMENTS
                .contains(id);
    }

    public static String getEnchantmentId(
            Holder<Enchantment> enchantment
    ) {
        if (enchantment == null) {
            return "";
        }

        return enchantment.unwrapKey()
                .map(
                        key ->
                                key.identifier()
                                        .toString()
                )
                .orElse("");
    }

    /**
     * RaceCore extends every protection enchantment registered in
     * protection.json to tier V. Everything else keeps its vanilla maximum.
     *
     * This is shared by both client preview controls and server validation,
     * so the UI cannot offer a level the server later rejects.
     */
    public static int getAllowedMaxLevel(
            Holder<Enchantment> enchantment
    ) {
        if (enchantment == null) {
            return 0;
        }

        String id =
                getEnchantmentId(
                        enchantment
                );

        if (ProtectionRegistry.get(id)
                != null) {

            return RACEC0RE_PROTECTION_MAX_LEVEL;
        }

        return enchantment.value()
                .getMaxLevel();
    }

    public static int getCurrentLevel(
            ItemStack stack,
            Holder<Enchantment> enchantment
    ) {
        if (stack == null
                || stack.isEmpty()
                || enchantment == null) {

            return 0;
        }

        return stack.getOrDefault(
                        DataComponents.ENCHANTMENTS,
                        ItemEnchantments.EMPTY
                )
                .getLevel(
                        enchantment
                );
    }

    public static ItemStack applyTargetsCopy(
            ItemStack original,
            Map<? extends Holder<Enchantment>, Integer> targets
    ) {
        ItemStack result =
                original.copy();

        ItemEnchantments.Mutable mutable =
                new ItemEnchantments.Mutable(
                        result.getOrDefault(
                                DataComponents.ENCHANTMENTS,
                                ItemEnchantments.EMPTY
                        )
                );

        for (Map.Entry<? extends Holder<Enchantment>, Integer> entry
                : targets.entrySet()) {

            mutable.set(
                    entry.getKey(),
                    entry.getValue()
            );
        }

        result.set(
                DataComponents.ENCHANTMENTS,
                mutable.toImmutable()
        );

        return result;
    }

    public static int getAddedLevelCost(
            ItemStack original,
            Map<? extends Holder<Enchantment>, Integer> targets
    ) {
        int cost = 0;

        for (Map.Entry<? extends Holder<Enchantment>, Integer> entry
                : targets.entrySet()) {

            int current =
                    getCurrentLevel(
                            original,
                            entry.getKey()
                    );

            cost += Math.max(
                    0,
                    entry.getValue()
                            - current
            );
        }

        return cost;
    }

    public static boolean hasOnlyLevelIncreases(
            ItemStack original,
            Map<? extends Holder<Enchantment>, Integer> targets
    ) {
        for (Map.Entry<? extends Holder<Enchantment>, Integer> entry
                : targets.entrySet()) {

            Holder<Enchantment> enchantment =
                    entry.getKey();

            int target =
                    entry.getValue();

            int current =
                    getCurrentLevel(
                            original,
                            enchantment
                    );

            if (target <= current
                    || target
                    > getAllowedMaxLevel(
                            enchantment
                    )) {

                return false;
            }
        }

        return true;
    }

    public static boolean areEnchantmentsCompatible(
            ItemStack stack
    ) {
        ItemEnchantments enchantments =
                stack.getOrDefault(
                        DataComponents.ENCHANTMENTS,
                        ItemEnchantments.EMPTY
                );

        List<Holder<Enchantment>> holders =
                new ArrayList<>(
                        enchantments.keySet()
                );

        for (int i = 0;
             i < holders.size();
             i++) {

            for (int j = i + 1;
                 j < holders.size();
                 j++) {

                if (!Enchantment.areCompatible(
                        holders.get(i),
                        holders.get(j)
                )) {
                    return false;
                }
            }
        }

        return true;
    }

    public static boolean isWithinCapacity(
            LivingEntity entity,
            ItemStack stack
    ) {
        if (entity == null
                || stack == null
                || stack.isEmpty()) {

            return false;
        }

        if (ArmorManager.getDefinition(stack)
                != null) {

            return getProtectionUsed(stack)
                    <= getProtectionCapacity(
                    entity,
                    stack
            ) + 0.000001
                    && !UtilityEnchantingManager
                    .isOverCapacity(
                            entity,
                            stack
                    );
        }

        if (WeaponToolEnchantingManager
                .isWeaponOrTool(stack)) {

            return !WeaponToolEnchantingManager
                    .isOverCapacity(
                            entity,
                            stack
                    );
        }

        return getGenericUtilityUsed(stack)
                <= getGenericUtilityCapacity(
                entity,
                stack
        );
    }

    public static double getProtectionUsed(
            ItemStack stack
    ) {
        if (ArmorManager.getDefinition(stack)
                == null) {

            return 0.0;
        }

        ItemEnchantments enchantments =
                stack.getOrDefault(
                        DataComponents.ENCHANTMENTS,
                        ItemEnchantments.EMPTY
                );

        double used = 0.0;

        for (Object2IntMap.Entry<Holder<Enchantment>> entry
                : enchantments.entrySet()) {

            String id =
                    getEnchantmentId(
                            entry.getKey()
                    );

            ProtectionEnchantmentDefinition definition =
                    ProtectionRegistry.get(id);

            if (definition == null) {
                continue;
            }

            used +=
                    definition.pointsPerLevel()
                            * Math.max(
                            0,
                            entry.getIntValue()
                    );
        }

        return used;
    }

    public static int getProtectionCapacity(
            LivingEntity entity,
            ItemStack stack
    ) {
        if (ArmorManager.getDefinition(stack)
                == null) {

            return 0;
        }

        return ProtectionManager
                .getCapacityForPiece(
                        entity,
                        stack
                );
    }

    public static int getGenericUtilityCapacity(
            LivingEntity entity,
            ItemStack stack
    ) {
        if (entity == null
                || stack == null
                || stack.isEmpty()) {

            return 0;
        }

        int defaultRaceCapacity =
                ProtectionRegistry
                        .settings()
                        .defaultCapacityPerPiece();

        int actualRaceCapacity =
                ProtectionManager
                        .getCapacityPerPiece(
                                entity
                        );

        int raceAdjustment =
                actualRaceCapacity
                        - defaultRaceCapacity;

        return Math.max(
                0,
                GENERIC_UTILITY_CAPACITY
                        + raceAdjustment
        );
    }

    public static int getGenericUtilityUsed(
            ItemStack stack
    ) {
        if (stack == null
                || stack.isEmpty()) {

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

            Holder<Enchantment> enchantment =
                    entry.getKey();

            if (enchantment.is(
                    EnchantmentTags.CURSE
            )) {
                continue;
            }

            if ("minecraft:mending".equals(
                    getEnchantmentId(
                            enchantment
                    )
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
}
