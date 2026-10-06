package com.example.racecore.durability;

import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;

/**
 * RaceCore broken-item state.
 *
 * Minecraft normally deletes an item when durability reaches zero.
 * RaceCore reserves the final durability point as a sentinel and marks
 * the stack as broken instead.
 */
public final class BrokenItemManager {
    private static final String BROKEN_KEY =
            "racecore_broken";

    private BrokenItemManager() {
    }

    public static boolean isSupported(
            ItemStack stack
    ) {
        if (stack == null
                || stack.isEmpty()
                || !stack.isDamageableItem()) {

            return false;
        }

        return stack.has(DataComponents.EQUIPPABLE)
                || stack.has(DataComponents.TOOL)
                || stack.has(DataComponents.WEAPON)
                || stack.has(DataComponents.KINETIC_WEAPON)
                || stack.has(DataComponents.PIERCING_WEAPON)
                || stack.has(DataComponents.BLOCKS_ATTACKS);
    }

    public static boolean isBroken(
            ItemStack stack
    ) {
        if (stack == null || stack.isEmpty()) {
            return false;
        }

        return stack.getOrDefault(
                        DataComponents.CUSTOM_DATA,
                        CustomData.EMPTY
                )
                .contains(BROKEN_KEY);
    }

    public static boolean shouldBecomeBroken(
            ItemStack stack
    ) {
        if (!isSupported(stack)
                || isBroken(stack)) {

            return false;
        }

        int maxDamage =
                stack.getMaxDamage();

        return maxDamage > 0
                && stack.getDamageValue()
                >= maxDamage - 1;
    }

    public static void markBroken(
            ItemStack stack
    ) {
        if (!isSupported(stack)) {
            return;
        }

        int maxDamage =
                stack.getMaxDamage();

        if (maxDamage > 0) {
            /*
             * Keep one durability point physically present so vanilla does
             * not delete the stack. RaceCore treats the custom flag as
             * actual zero durability.
             */
            stack.setDamageValue(
                    Math.max(
                            0,
                            maxDamage - 1
                    )
            );
        }

        CustomData.update(
                DataComponents.CUSTOM_DATA,
                stack,
                tag -> tag.putBoolean(
                        BROKEN_KEY,
                        true
                )
        );
    }

    public static ItemStack brokenCopy(
            ItemStack source
    ) {
        ItemStack copy =
                source.copy();

        copy.setCount(1);
        markBroken(copy);

        return copy;
    }

    public static void clearBroken(
            ItemStack stack
    ) {
        if (stack == null || stack.isEmpty()) {
            return;
        }

        CustomData.update(
                DataComponents.CUSTOM_DATA,
                stack,
                tag -> tag.remove(
                        BROKEN_KEY
                )
        );
    }
}
