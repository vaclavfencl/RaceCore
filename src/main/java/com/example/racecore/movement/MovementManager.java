package com.example.racecore.movement;

import com.example.racecore.RaceCore;
import com.example.racecore.race.RaceDefinition;
import com.example.racecore.race.RaceManager;
import net.minecraft.core.Holder;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.neoforged.neoforge.common.NeoForgeMod;

/**
 * Central RaceCore movement-speed system.
 *
 * Universal layers:
 *   x global multiplier
 *   x armor multiplier
 *
 * Player-only layer:
 *   x race multiplier
 *
 * Nothing here runs every tick.
 */
public final class MovementManager {
    private static final Identifier GLOBAL_MOVEMENT_ID =
            Identifier.fromNamespaceAndPath(
                    RaceCore.MODID,
                    "global_movement_speed"
            );

    private static final Identifier GLOBAL_FLYING_ID =
            Identifier.fromNamespaceAndPath(
                    RaceCore.MODID,
                    "global_flying_speed"
            );

    private static final Identifier RACE_MOVEMENT_ID =
            Identifier.fromNamespaceAndPath(
                    RaceCore.MODID,
                    "race_movement_speed"
            );

    private static final Identifier RACE_SWIM_SPEED_ID =
            Identifier.fromNamespaceAndPath(
                    RaceCore.MODID,
                    "race_swim_speed"
            );

    private static final Identifier ARMOR_MOVEMENT_ID =
            Identifier.fromNamespaceAndPath(
                    RaceCore.MODID,
                    "armor_movement_speed"
            );

    private static final Identifier ARMOR_FLYING_ID =
            Identifier.fromNamespaceAndPath(
                    RaceCore.MODID,
                    "armor_flying_speed"
            );

    /**
     * Giant already has 1.20 race movement and RaceCore applies the global
     * 1.07 movement layer. A further ~1.11 underwater multiplier puts normal
     * Giant swimming around vanilla boat travel speed without turning it into
     * Dolphin's-Grace-style water flight.
     */
    public static final double GIANT_SWIM_SPEED_MULTIPLIER = 1.11;

    private MovementManager() {
    }

    /**
     * Compatibility entry point used by the existing RaceManager from Step 3B.
     *
     * It is safe for this to reapply universal modifiers because each modifier
     * has a stable ID and replaces its previous value rather than stacking.
     */
    public static void applyMovement(
            ServerPlayer player,
            RaceDefinition race
    ) {
        applyGlobalMovement(player);
        applyRaceMovement(player, race);
        applyArmorMovement(player);
    }

    /**
     * Applies RaceCore's server-wide speed multiplier to any living entity.
     *
     * MOVEMENT_SPEED handles normal ground movement.
     * FLYING_SPEED is also modified when that entity actually owns the attribute.
     */
    public static void applyGlobalMovement(
            LivingEntity entity
    ) {
        double multiplier =
                MovementRegistry.globalMovementMultiplier();

        applyMultiplierIfPresent(
                entity,
                Attributes.MOVEMENT_SPEED,
                GLOBAL_MOVEMENT_ID,
                multiplier
        );

        applyMultiplierIfPresent(
                entity,
                Attributes.FLYING_SPEED,
                GLOBAL_FLYING_ID,
                multiplier
        );
    }

    /**
     * Race movement remains player-only.
     */
    public static void applyRaceMovement(
            ServerPlayer player,
            RaceDefinition race
    ) {
        applyMultiplierIfPresent(
                player,
                Attributes.MOVEMENT_SPEED,
                RACE_MOVEMENT_ID,
                race.movementSpeedMultiplier()
        );

        double swimMultiplier =
                "giant".equals(RaceManager.getRaceId(player))
                        ? GIANT_SWIM_SPEED_MULTIPLIER
                        : 1.0;

        applyMultiplierIfPresent(
                player,
                NeoForgeMod.SWIM_SPEED,
                RACE_SWIM_SPEED_ID,
                swimMultiplier
        );
    }

    /**
     * Armor weight applies to every living entity, not just players.
     */
    public static void applyArmorMovement(
            LivingEntity entity
    ) {
        double multiplier =
                ArmorMovementCalculator.getMultiplier(entity);

        applyMultiplierIfPresent(
                entity,
                Attributes.MOVEMENT_SPEED,
                ARMOR_MOVEMENT_ID,
                multiplier
        );

        applyMultiplierIfPresent(
                entity,
                Attributes.FLYING_SPEED,
                ARMOR_FLYING_ID,
                multiplier
        );
    }

    private static void applyMultiplierIfPresent(
            LivingEntity entity,
            Holder<Attribute> attributeType,
            Identifier modifierId,
            double multiplier
    ) {
        AttributeInstance attribute =
                entity.getAttribute(attributeType);

        if (attribute == null) {
            return;
        }

        attribute.removeModifier(modifierId);

        if (Math.abs(multiplier - 1.0) <= 0.000001) {
            return;
        }

        attribute.addOrUpdateTransientModifier(
                new AttributeModifier(
                        modifierId,
                        multiplier - 1.0,
                        AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL
                )
        );
    }

    public static void clearMovementModifiers(
            LivingEntity entity
    ) {
        removeIfPresent(
                entity,
                Attributes.MOVEMENT_SPEED,
                GLOBAL_MOVEMENT_ID
        );

        removeIfPresent(
                entity,
                Attributes.FLYING_SPEED,
                GLOBAL_FLYING_ID
        );

        removeIfPresent(
                entity,
                Attributes.MOVEMENT_SPEED,
                ARMOR_MOVEMENT_ID
        );

        removeIfPresent(
                entity,
                Attributes.FLYING_SPEED,
                ARMOR_FLYING_ID
        );

        if (entity instanceof ServerPlayer) {
            removeIfPresent(
                    entity,
                    Attributes.MOVEMENT_SPEED,
                    RACE_MOVEMENT_ID
            );

            removeIfPresent(
                    entity,
                    NeoForgeMod.SWIM_SPEED,
                    RACE_SWIM_SPEED_ID
            );
        }
    }

    private static void removeIfPresent(
            LivingEntity entity,
            Holder<Attribute> attributeType,
            Identifier modifierId
    ) {
        AttributeInstance attribute =
                entity.getAttribute(attributeType);

        if (attribute != null) {
            attribute.removeModifier(modifierId);
        }
    }
}
