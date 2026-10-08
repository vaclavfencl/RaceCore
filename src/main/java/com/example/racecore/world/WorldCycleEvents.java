package com.example.racecore.world;

import com.example.racecore.RaceCore;
import net.minecraft.core.Holder;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.clock.WorldClock;
import net.minecraft.world.clock.WorldClocks;
import net.minecraft.world.level.Level;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.level.SleepFinishedTimeEvent;
import net.neoforged.neoforge.event.server.ServerStoppingEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

/**
 * Phase 12A world-cycle rules.
 *
 * Overworld:
 * - sleeping may still set the player's respawn point;
 * - sleeping never advances the clock to morning;
 * - the dark part of the night runs at 1.25x clock speed.
 *
 * The clock rate is changed only when crossing a day/night boundary, not every
 * tick. It is also restored to 1.0 while the server is stopping so the saved
 * world does not intentionally retain RaceCore's accelerated rate if the mod
 * is later removed.
 */
@EventBusSubscriber(modid = RaceCore.MODID)
public final class WorldCycleEvents {
    private static final long DAY_TICKS = 24_000L;

    /*
     * Minecraft's day timeline marks night at 13000. We return to vanilla
     * speed at 23000 so sunrise/day transitions remain normal-speed.
     *
     * 10000 clock ticks / 1.25 = 8000 real ticks:
     * vanilla dark-night length ~8m20s -> RaceCore ~6m40s.
     */
    private static final long NIGHT_START = 13_000L;
    private static final long NIGHT_END = 23_000L;

    private static final float DAY_RATE = 1.0F;
    private static final float NIGHT_RATE = 1.25F;

    private static MinecraftServer trackedServer;
    private static float lastAppliedRate = Float.NaN;

    private WorldCycleEvents() {
    }

    /**
     * NeoForge fires this immediately before vanilla would move the world clock
     * to the wake-up marker. Canceling it leaves the current time untouched;
     * vanilla still wakes the sleepers normally afterwards.
     *
     * This preserves beds as respawn-setting blocks while removing night skip.
     */
    @SubscribeEvent
    public static void onSleepFinished(
            SleepFinishedTimeEvent event
    ) {
        if (event.getLevel()
                instanceof ServerLevel level
                && level.dimension() == Level.OVERWORLD) {

            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void onServerTick(
            ServerTickEvent.Post event
    ) {
        MinecraftServer server =
                event.getServer();

        if (trackedServer != server) {
            trackedServer = server;
            lastAppliedRate = Float.NaN;
        }

        Holder<WorldClock> overworldClock =
                server.registryAccess()
                        .getOrThrow(
                                WorldClocks.OVERWORLD
                        );

        long totalTicks =
                server.clockManager()
                        .getInstance(
                                overworldClock
                        )
                        .totalTicks();

        long timeOfDay =
                Math.floorMod(
                        totalTicks,
                        DAY_TICKS
                );

        float desiredRate =
                timeOfDay >= NIGHT_START
                        && timeOfDay < NIGHT_END
                        ? NIGHT_RATE
                        : DAY_RATE;

        if (Float.compare(
                desiredRate,
                lastAppliedRate
        ) == 0) {
            return;
        }

        server.clockManager()
                .setRate(
                        overworldClock,
                        desiredRate
                );

        lastAppliedRate =
                desiredRate;
    }

    @SubscribeEvent
    public static void onServerStopping(
            ServerStoppingEvent event
    ) {
        MinecraftServer server =
                event.getServer();

        try {
            Holder<WorldClock> overworldClock =
                    server.registryAccess()
                            .getOrThrow(
                                    WorldClocks.OVERWORLD
                            );

            server.clockManager()
                    .setRate(
                            overworldClock,
                            DAY_RATE
                    );
        } finally {
            trackedServer = null;
            lastAppliedRate = Float.NaN;
        }
    }
}
