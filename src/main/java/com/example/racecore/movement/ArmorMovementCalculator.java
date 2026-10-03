package com.example.racecore.movement;

import com.example.racecore.armor.ArmorDefinition;
import com.example.racecore.armor.ArmorManager;
import com.example.racecore.armor.ArmorWeight;
import com.example.racecore.race.RaceDefinition;
import com.example.racecore.race.RaceManager;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

/**
 * Calculates one combined movement multiplier from equipped RaceCore armor.
 *
 * Race-specific penalty multipliers are applied here, at the actual source of
 * the armor slowdown. This avoids adding a second compensating speed modifier.
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

    public static double getMultiplier(
            LivingEntity entity
    ) {
        double totalPenalty = 0.0;

        RaceDefinition race =
                entity instanceof Player player
                        ? RaceManager.getRace(player)
                        : null;

        for (EquipmentSlot slot : ARMOR_SLOTS) {
            ItemStack stack =
                    entity.getItemBySlot(slot);

            if (stack.isEmpty()) {
                continue;
            }

            ArmorDefinition definition =
                    ArmorManager.getDefinition(stack);

            if (definition == null) {
                continue;
            }

            double fullSetPenalty =
                    fullSetPenalty(definition.weight());

            double racialPenaltyMultiplier =
                    race == null
                            ? 1.0
                            : racialPenaltyMultiplier(
                                    race,
                                    definition.weight()
                            );

            totalPenalty +=
                    fullSetPenalty
                            * racialPenaltyMultiplier
                            * slotShare(slot);
        }

        return Math.max(
                0.05,
                1.0 - totalPenalty
        );
    }

    private static double racialPenaltyMultiplier(
            RaceDefinition race,
            ArmorWeight weight
    ) {
        return switch (weight) {
            case LIGHT ->
                    race.lightArmorPenaltyMultiplier();

            case MEDIUM ->
                    race.mediumArmorPenaltyMultiplier();

            case HEAVY ->
                    race.heavyArmorPenaltyMultiplier();
        };
    }

    private static double fullSetPenalty(
            ArmorWeight weight
    ) {
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

    private static double slotShare(
            EquipmentSlot slot
    ) {
        return switch (slot) {
            case HEAD -> 0.15;
            case CHEST -> 0.40;
            case LEGS -> 0.30;
            case FEET -> 0.15;
            default -> 0.0;
        };
    }

    public static boolean isArmorSlot(
            EquipmentSlot slot
    ) {
        return slot == EquipmentSlot.HEAD
                || slot == EquipmentSlot.CHEST
                || slot == EquipmentSlot.LEGS
                || slot == EquipmentSlot.FEET;
    }
}
