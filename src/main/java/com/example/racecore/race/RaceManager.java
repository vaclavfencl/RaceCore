package com.example.racecore.race;

import com.example.racecore.movement.MovementManager;
import com.example.racecore.stats.VanillaStatScaling;
import net.minecraft.core.Holder;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
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

        String oldRaceId = getRaceId(player);
        boolean raceChanged = !oldRaceId.equals(raceId);

        player.setData(RaceAttachments.RACE, raceId);
        applyRaceAttributes(player, race);

        /*
         * On an actual race change:
         * - a non-Night-Vision race removes Night Vision;
         * - a Night-Vision race receives the infinite racial passive.
         *
         * Re-selecting the same race does not delete a normal potion effect.
         */
        syncPermanentNightVision(
                player,
                race,
                raceChanged
        );

        return true;
    }

    public static void applyCurrentRace(ServerPlayer player) {
        RaceDefinition race =
                getRace(player);

        applyRaceAttributes(
                player,
                race
        );

        /*
         * Login / respawn / dimension change:
         * make sure racial Night Vision exists, but do not remove legitimate
         * potion Night Vision from races that do not own the passive.
         */
        syncPermanentNightVision(
                player,
                race,
                false
        );
    }

    /**
     * Adds the racial Night Vision passive only when it is missing.
     *
     * The effect uses Minecraft's real INFINITE_DURATION, so there is no
     * low-duration flashing/flicker.
     */
    public static void ensurePermanentNightVision(
            Player player,
            RaceDefinition race
    ) {
        if (!race.permanentNightVision()
                || player.hasEffect(
                        MobEffects.NIGHT_VISION
                )) {
            return;
        }

        player.addEffect(
                new MobEffectInstance(
                        MobEffects.NIGHT_VISION,
                        MobEffectInstance.INFINITE_DURATION,
                        0,
                        true,
                        false,
                        false
                )
        );
    }

    private static void syncPermanentNightVision(
            ServerPlayer player,
            RaceDefinition race,
            boolean removeIfDisabled
    ) {
        if (race.permanentNightVision()) {
            ensurePermanentNightVision(
                    player,
                    race
            );
            return;
        }

        if (removeIfDisabled) {
            player.removeEffect(
                    MobEffects.NIGHT_VISION
            );
        }
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

        maxHealth.setBaseValue(
                race.maxHealth() / VanillaStatScaling.SCALE
        );

        scale.setBaseValue(race.scale());
        stepHeight.setBaseValue(race.stepHeight());

        MovementManager.applyMovement(
                player,
                race
        );

        blockReach.setBaseValue(
                Player.DEFAULT_BLOCK_INTERACTION_RANGE
                        * race.reachMultiplier()
        );

        entityReach.setBaseValue(
                Player.DEFAULT_ENTITY_INTERACTION_RANGE
                        * race.reachMultiplier()
        );

        safeFallDistance.setBaseValue(
                race.safeFallDistance()
        );

        // Changing race is not a free heal.
        if (oldMaxHealth > 0.0) {
            double healthRatio =
                    oldHealth / oldMaxHealth;

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
