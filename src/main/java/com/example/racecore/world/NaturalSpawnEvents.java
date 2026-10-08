package com.example.racecore.world;

import com.example.racecore.RaceCore;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.level.Level;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.MobSpawnEvent;

/**
 * Phase 12.1: reduced ordinary Overworld hostile natural spawning.
 *
 * Vanilla-valid NATURAL monster placement checks are allowed 60% of the time.
 * This reduces successful natural hostile spawning without touching:
 * - Nether / End
 * - mob spawners
 * - trial spawners
 * - raids / event spawns
 * - summoned / command-spawned mobs
 *
 * The vanilla mob cap is intentionally left unchanged. This is a spawn-rate
 * reduction rather than a hard population-cap reduction.
 */
@EventBusSubscriber(modid = RaceCore.MODID)
public final class NaturalSpawnEvents {
    private static final float OVERWORLD_NATURAL_HOSTILE_MULTIPLIER = 0.60F;

    private NaturalSpawnEvents() {
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onSpawnPlacementCheck(
            MobSpawnEvent.SpawnPlacementCheck event
    ) {
        if (event.getSpawnType()
                != EntitySpawnReason.NATURAL) {

            return;
        }

        if (event.getEntityType()
                .getCategory()
                != MobCategory.MONSTER) {

            return;
        }

        if (event.getLevel()
                .getLevel()
                .dimension()
                != Level.OVERWORLD) {

            return;
        }

        /*
         * Do not override another mod's explicit decision.
         * Also do not spend RNG on candidates vanilla already rejected.
         */
        if (event.getResult()
                != MobSpawnEvent.SpawnPlacementCheck.Result.DEFAULT
                || !event.getDefaultResult()) {

            return;
        }

        if (event.getRandom().nextFloat()
                >= OVERWORLD_NATURAL_HOSTILE_MULTIPLIER) {

            event.setResult(
                    MobSpawnEvent.SpawnPlacementCheck.Result.FAIL
            );
        }
    }
}
