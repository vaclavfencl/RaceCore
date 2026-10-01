package com.example.racecore.race;

import com.example.racecore.RaceCore;
import com.example.racecore.stats.VanillaStatScaling;
import net.minecraft.core.Holder;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;

public final class RaceManager {
    private static final Identifier RACE_MOVEMENT_SPEED_ID =
            Identifier.fromNamespaceAndPath(RaceCore.MODID, "race_movement_speed");

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

    private static void applyRaceAttributes(ServerPlayer player, RaceDefinition race) {
        AttributeInstance maxHealth =
                requireAttribute(player, Attributes.MAX_HEALTH, "max_health");

        AttributeInstance scale =
                requireAttribute(player, Attributes.SCALE, "scale");

        AttributeInstance stepHeight =
                requireAttribute(player, Attributes.STEP_HEIGHT, "step_height");

        AttributeInstance movementSpeed =
                requireAttribute(player, Attributes.MOVEMENT_SPEED, "movement_speed");

        AttributeInstance blockReach =
                requireAttribute(player, Attributes.BLOCK_INTERACTION_RANGE,
                        "block_interaction_range");

        AttributeInstance entityReach =
                requireAttribute(player, Attributes.ENTITY_INTERACTION_RANGE,
                        "entity_interaction_range");

        double oldMaxHealth = player.getMaxHealth();
        float oldHealth = player.getHealth();

        /*
         * races.json stores the REAL RaceCore HP value (100, 150, 350...).
         *
         * VanillaStatScaling supplies the global x5 modifier, therefore the
         * underlying base value is divided by 5 here:
         *
         * Human: 20 base * 5 = 100 final HP
         * Dwarf: 30 base * 5 = 150 final HP
         * Giant: 70 base * 5 = 350 final HP
         *
         * This also lets vanilla Health Boost and other max-health modifiers
         * participate in the same global x5 scale.
         */
        maxHealth.setBaseValue(
                race.maxHealth() / VanillaStatScaling.SCALE
        );

        scale.setBaseValue(race.scale());
        stepHeight.setBaseValue(race.stepHeight());

        movementSpeed.removeModifier(RACE_MOVEMENT_SPEED_ID);

        if (Math.abs(race.movementSpeedMultiplier() - 1.0) > 0.000001) {
            movementSpeed.addOrUpdateTransientModifier(
                    new AttributeModifier(
                            RACE_MOVEMENT_SPEED_ID,
                            race.movementSpeedMultiplier() - 1.0,
                            AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL
                    )
            );
        }

        blockReach.setBaseValue(
                Player.DEFAULT_BLOCK_INTERACTION_RANGE * race.reachMultiplier()
        );

        entityReach.setBaseValue(
                Player.DEFAULT_ENTITY_INTERACTION_RANGE * race.reachMultiplier()
        );

        // Changing race is not a free heal.
        if (oldMaxHealth > 0.0) {
            double healthRatio = oldHealth / oldMaxHealth;
            float newHealth = (float) (player.getMaxHealth() * healthRatio);

            player.setHealth(Math.max(
                    0.0F,
                    Math.min(player.getMaxHealth(), newHealth)
            ));
        }

        player.refreshDimensions();
    }

    private static AttributeInstance requireAttribute(
            ServerPlayer player,
            Holder<Attribute> attribute,
            String name
    ) {
        AttributeInstance instance = player.getAttribute(attribute);

        if (instance == null) {
            throw new IllegalStateException(
                    "Player is missing minecraft:" + name + " attribute.");
        }

        return instance;
    }
}
