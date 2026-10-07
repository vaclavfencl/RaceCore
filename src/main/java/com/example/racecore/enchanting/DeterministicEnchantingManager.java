package com.example.racecore.enchanting;

import com.example.racecore.armor.ArmorManager;
import com.example.racecore.armor.ProtectionEnchantmentDefinition;
import com.example.racecore.armor.ProtectionManager;
import com.example.racecore.armor.ProtectionRegistry;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.EnchantmentTags;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.ItemEnchantments;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Shared rules for Phase 9.0 deterministic enchanting.
 *
 * This class deliberately does not rebalance enchantments. It only exposes
 * table eligibility, preview mutation, compatibility, costs and the capacity
 * checks RaceCore already uses elsewhere.
 */
public final class DeterministicEnchantingManager {
    private static final int GENERIC_UTILITY_CAPACITY = 4;

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

        return stack.isPrimaryItemFor(
                enchantment
        );
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
                    > enchantment.value()
                    .getMaxLevel()) {

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
