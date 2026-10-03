package com.example.racecore.race;

import com.example.racecore.movement.MovementManager;
import com.example.racecore.stats.VanillaStatScaling;
import net.minecraft.core.Holder;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;

public final class RaceManager {
    private RaceManager() {
    }

    public static String getRaceId(Player player) {
        String raceId = player.getData(RaceAttachments.RACE);

        if (!RaceRegistry.contains(raceId)) {
            if (!player.level().isClientSide()) {
                raceId = "human";
                player.setData(RaceAttachments.RACE, raceId);
            } else {
                raceId = "human";
            }
        }

        return raceId;
    }

    public static RaceDefinition getRace(Player player) {
        return RaceRegistry.getOrHuman(getRaceId(player));
    }

    public static boolean setRace(ServerPlayer player, String requestedRaceId) {
        String raceId = RaceRegistry.normalizeId(requestedRaceId);
        RaceDefinition race = RaceRegistry.get(raceId);

        if (race == null) {
            return false;
        }

        player.setData(RaceAttachments.RACE, raceId);
        applyRaceAttributes(player, race);
        return true;
    }

    public static void applyCurrentRace(ServerPlayer player) {
        applyRaceAttributes(player, getRace(player));
    }

    private static void applyRaceAttributes(
            ServerPlayer player,
            RaceDefinition race
    ) {
        AttributeInstance maxHealth =
                requireAttribute(
                        player,
                        Attributes.MAX_HEALTH,
                        "max_health"
                );

        AttributeInstance scale =
                requireAttribute(
                        player,
                        Attributes.SCALE,
                        "scale"
                );

        AttributeInstance stepHeight =
                requireAttribute(
                        player,
                        Attributes.STEP_HEIGHT,
                        "step_height"
                );

        AttributeInstance blockReach =
                requireAttribute(
                        player,
                        Attributes.BLOCK_INTERACTION_RANGE,
                        "block_interaction_range"
                );

        AttributeInstance entityReach =
                requireAttribute(
                        player,
                        Attributes.ENTITY_INTERACTION_RANGE,
                        "entity_interaction_range"
                );

        AttributeInstance safeFallDistance =
                requireAttribute(
                        player,
                        Attributes.SAFE_FALL_DISTANCE,
                        "safe_fall_distance"
                );

        double oldMaxHealth = player.getMaxHealth();
        float oldHealth = player.getHealth();

        /*
         * races.json stores final RaceCore HP.
         * VanillaStatScaling supplies the global x5 max-health modifier.
         */
        maxHealth.setBaseValue(
                race.maxHealth() / VanillaStatScaling.SCALE
        );

        scale.setBaseValue(race.scale());
        stepHeight.setBaseValue(race.stepHeight());

        /*
         * Movement is now owned by MovementManager.
         *
         * It applies:
         *   global multiplier x race multiplier
         *
         * Step 3C will add armor weight as another independent layer.
         */
        MovementManager.applyMovement(player, race);

        blockReach.setBaseValue(
                Player.DEFAULT_BLOCK_INTERACTION_RANGE
                        * race.reachMultiplier()
        );

        entityReach.setBaseValue(
                Player.DEFAULT_ENTITY_INTERACTION_RANGE
                        * race.reachMultiplier()
        );

        /*
         * Native Minecraft fall-damage threshold.
         *
         * Vanilla baseline is 3.0:
         *   <= 3 blocks safe
         *   4+ blocks starts dealing fall damage
         *
         * RaceCore changes the BASE value, so equipment/effects using
         * SAFE_FALL_DISTANCE attribute modifiers can still stack normally.
         */
        safeFallDistance.setBaseValue(
                race.safeFallDistance()
        );

        // Changing race is not a free heal.
        if (oldMaxHealth > 0.0) {
            double healthRatio = oldHealth / oldMaxHealth;

            float newHealth =
                    (float) (
                            player.getMaxHealth()
                                    * healthRatio
                    );

            player.setHealth(
                    Math.max(
                            0.0F,
                            Math.min(
                                    player.getMaxHealth(),
                                    newHealth
                            )
                    )
            );
        }

        player.refreshDimensions();
    }

    private static AttributeInstance requireAttribute(
            ServerPlayer player,
            Holder<Attribute> attribute,
            String name
    ) {
        AttributeInstance instance =
                player.getAttribute(attribute);

        if (instance == null) {
            throw new IllegalStateException(
                    "Player is missing minecraft:"
                            + name
                            + " attribute."
            );
        }

        return instance;
    }
}
