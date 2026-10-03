package com.example.racecore.race.ability;

import com.example.racecore.RaceCore;
import com.example.racecore.race.RaceDefinition;
import com.example.racecore.race.RaceManager;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingChangeTargetEvent;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;

/**
 * Generic "Nether mobs are neutral until attacked" behavior.
 *
 * The mob list is data-driven through racecore:nether_neutral_mobs.
 * Provocation is tracked per mob + player, so attacking one mob does not
 * globally anger every mob of that species.
 */
@EventBusSubscriber(modid = RaceCore.MODID)
public final class RaceNetherNeutralityEvents {
    private static final TagKey<EntityType<?>> NETHER_NEUTRAL_MOBS =
            TagKey.create(
                    Registries.ENTITY_TYPE,
                    Identifier.fromNamespaceAndPath(
                            RaceCore.MODID,
                            "nether_neutral_mobs"
                    )
            );

    private RaceNetherNeutralityEvents() {
    }

    @SubscribeEvent
    public static void onTargetChange(
            LivingChangeTargetEvent event
    ) {
        if (!(event.getEntity()
                instanceof Mob mob)) {

            return;
        }

        if (!mob.is(NETHER_NEUTRAL_MOBS)) {
            return;
        }

        if (!(event.getNewAboutToBeSetTarget()
                instanceof Player player)) {

            return;
        }

        RaceDefinition race =
                RaceManager.getRace(player);

        if (!race.netherMobsNeutral()) {
            return;
        }

        if (isProvokedBy(mob, player)) {
            return;
        }

        event.setNewAboutToBeSetTarget(null);
    }

    @SubscribeEvent
    public static void onMobDamaged(
            LivingDamageEvent.Pre event
    ) {
        if (!(event.getEntity()
                instanceof Mob mob)
                || !mob.is(NETHER_NEUTRAL_MOBS)) {

            return;
        }

        if (!(event.getSource().getEntity()
                instanceof Player player)) {

            return;
        }

        RaceDefinition race =
                RaceManager.getRace(player);

        if (!race.netherMobsNeutral()) {
            return;
        }

        mob.getPersistentData().putBoolean(
                provokedKey(player),
                true
        );
    }

    private static boolean isProvokedBy(
            Mob mob,
            Player player
    ) {
        return mob.getPersistentData()
                .getBooleanOr(
                        provokedKey(player),
                        false
                );
    }

    private static String provokedKey(
            Player player
    ) {
        return "racecore:provoked_by_"
                + player.getStringUUID();
    }
}
