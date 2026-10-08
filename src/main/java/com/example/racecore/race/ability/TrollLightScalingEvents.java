package com.example.racecore.race.ability;

import com.example.racecore.RaceCore;
import com.example.racecore.race.RaceDefinition;
import com.example.racecore.race.RaceManager;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biome;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

/**
 * Troll light scaling.
 *
 * Troll is intentionally the only RaceCore race whose combat power changes
 * continuously with local light:
 *
 * light 0  -> +40% outgoing damage, +15% movement
 * light 15 -> -10% outgoing damage, +0% movement
 *
 * Values between 0 and 15 interpolate linearly.
 *
 * The existing RaceAbilityEvents still owns the generic light fields and
 * ranged restriction. This class runs at LOWEST priority and converts Troll's
 * old threshold-based damage/movement result into the desired continuous
 * curve while leaving all other races untouched.
 */
@EventBusSubscriber(modid = RaceCore.MODID)
public final class TrollLightScalingEvents {
    private static final String TROLL_ID = "troll";

    /*
     * RaceAbilityEvents uses this exact modifier ID for its environmental
     * movement modifier. Replacing the value at LOWEST priority lets Troll use
     * the smooth light curve without stacking a second permanent movement
     * modifier.
     */
    private static final Identifier ENVIRONMENT_MOVEMENT_ID =
            Identifier.fromNamespaceAndPath(
                    RaceCore.MODID,
                    "environment_movement"
            );

    private static final TagKey<Biome> FOREST_BIOMES =
            TagKey.create(
                    Registries.BIOME,
                    Identifier.fromNamespaceAndPath(
                            RaceCore.MODID,
                            "forest_biomes"
                    )
            );

    private TrollLightScalingEvents() {
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onLivingDamage(
            LivingDamageEvent.Pre event
    ) {
        if (!(event.getSource().getEntity()
                instanceof Player attacker)) {

            return;
        }

        if (!TROLL_ID.equals(
                RaceManager.getRaceId(attacker)
        )) {
            return;
        }

        RaceDefinition race =
                RaceManager.getRace(attacker);

        int light =
                clampLight(
                        attacker.level()
                                .getMaxLocalRawBrightness(
                                        attacker.blockPosition()
                                )
                );

        /*
         * Generic RaceAbilityEvents has already applied one of these two
         * threshold multipliers:
         *
         * light <= darkLightThreshold -> darkDamageMultiplier
         * otherwise                   -> brightDamageMultiplier
         *
         * Correct only that factor to the continuous Troll value.
         */
        double previouslyApplied =
                light <= race.darkLightThreshold()
                        ? race.darkDamageMultiplier()
                        : race.brightDamageMultiplier();

        if (previouslyApplied <= 0.0) {
            return;
        }

        double desired =
                lerpByLight(
                        light,
                        race.darkDamageMultiplier(),
                        race.brightDamageMultiplier()
                );

        double correction =
                desired / previouslyApplied;

        event.setNewDamage(
                (float) Math.max(
                        0.0,
                        event.getNewDamage()
                                * correction
                )
        );
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onPlayerTick(
            PlayerTickEvent.Post event
    ) {
        Player player = event.getEntity();

        if (player.level().isClientSide()
                || player.tickCount % 10 != 0) {

            return;
        }

        if (!TROLL_ID.equals(
                RaceManager.getRaceId(player)
        )) {
            return;
        }

        RaceDefinition race =
                RaceManager.getRace(player);

        var movement =
                player.getAttribute(
                        Attributes.MOVEMENT_SPEED
                );

        if (movement == null) {
            return;
        }

        int light =
                clampLight(
                        player.level()
                                .getMaxLocalRawBrightness(
                                        player.blockPosition()
                                )
                );

        double multiplier =
                lerpByLight(
                        light,
                        race.darkMovementMultiplier(),
                        1.0
                );

        /*
         * Preserve any future Troll forest/Nether movement modifiers that use
         * the existing generic race fields.
         */
        if (player.level()
                .getBiome(player.blockPosition())
                .is(FOREST_BIOMES)) {

            multiplier *=
                    race.forestMovementMultiplier();
        }

        if (player.level().dimension()
                == Level.NETHER) {

            multiplier *=
                    race.netherMovementMultiplier();
        }

        movement.removeModifier(
                ENVIRONMENT_MOVEMENT_ID
        );

        if (Math.abs(multiplier - 1.0)
                <= 0.000001) {

            return;
        }

        movement.addOrUpdateTransientModifier(
                new AttributeModifier(
                        ENVIRONMENT_MOVEMENT_ID,
                        multiplier - 1.0,
                        AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL
                )
        );
    }

    /**
     * light 0 returns darkValue.
     * light 15 returns brightValue.
     */
    private static double lerpByLight(
            int light,
            double darkValue,
            double brightValue
    ) {
        double t =
                clampLight(light) / 15.0;

        return darkValue
                + (brightValue - darkValue)
                * t;
    }

    private static int clampLight(
            int light
    ) {
        return Math.max(
                0,
                Math.min(15, light)
        );
    }
}
