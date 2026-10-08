package com.example.racecore.world;

import com.example.racecore.RaceCore;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.Difficulty;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.level.levelgen.Heightmap;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.server.ServerStoppingEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

/**
 * Phase 12.2: rare daytime hostile groups.
 *
 * This is intentionally separate from vanilla NATURAL spawning.
 * Daytime should usually be calm, then occasionally create one actual pack
 * rather than dribbling isolated mobs into the world.
 *
 * Current tuning target:
 * - one attempt per nearby player group every 2-5 real minutes;
 * - 3-6 mobs per successful group;
 * - surface only;
 * - approximately 32-48 blocks from a player;
 * - mobs that do not burn in daylight.
 *
 * Spawned mobs are ordinary non-persistent mobs and therefore despawn using
 * vanilla rules.
 */
@EventBusSubscriber(modid = RaceCore.MODID)
public final class DaytimeGroupSpawnEvents {
    public static final String DAY_GROUP_TAG =
            "racecore_day_group";

    private static final long DAY_TICKS =
            24_000L;

    private static final long NIGHT_START =
            13_000L;

    private static final long NIGHT_END =
            23_000L;

    private static final int TICK_INTERVAL =
            20;

    private static final long MIN_COOLDOWN_TICKS =
            2L * 60L * 20L;

    private static final long MAX_COOLDOWN_TICKS =
            5L * 60L * 20L;

    private static final long FAILED_RETRY_MIN_TICKS =
            30L * 20L;

    private static final long FAILED_RETRY_MAX_TICKS =
            60L * 20L;

    private static final double PLAYER_GROUP_RADIUS =
            96.0;

    private static final double PLAYER_GROUP_RADIUS_SQR =
            PLAYER_GROUP_RADIUS
                    * PLAYER_GROUP_RADIUS;

    private static final double SPAWN_MIN_DISTANCE =
            32.0;

    private static final double SPAWN_MAX_DISTANCE =
            48.0;

    private static final double ABSOLUTE_MIN_PLAYER_DISTANCE =
            24.0;

    private static final double ABSOLUTE_MIN_PLAYER_DISTANCE_SQR =
            ABSOLUTE_MIN_PLAYER_DISTANCE
                    * ABSOLUTE_MIN_PLAYER_DISTANCE;

    private static final int MIN_GROUP_SIZE =
            3;

    private static final int MAX_GROUP_SIZE =
            6;

    private static final int MAX_GROUP_SEARCH_ATTEMPTS =
            18;

    private static final int MAX_MEMBER_SEARCH_ATTEMPTS =
            10;

    /*
     * Prevent multiple RaceCore daytime groups from stacking around the same
     * players. Existing vanilla monsters are intentionally not counted here.
     */
    private static final int MAX_ACTIVE_DAY_GROUP_MOBS_NEAR_PLAYERS =
            8;

    private static final double ACTIVE_DAY_GROUP_CHECK_RADIUS =
            72.0;

    private static final Map<UUID, Long> NEXT_ATTEMPT =
            new HashMap<>();

    private static int tickCounter;

    private DaytimeGroupSpawnEvents() {
    }

    @SubscribeEvent
    public static void onServerTick(
            ServerTickEvent.Post event
    ) {
        tickCounter++;

        if (tickCounter % TICK_INTERVAL != 0) {
            return;
        }

        MinecraftServer server =
                event.getServer();

        ServerLevel level =
                server.overworld();

        List<ServerPlayer> players =
                getEligiblePlayers(level);

        removeOfflineCooldowns(players);

        if (players.isEmpty()) {
            return;
        }

        long now =
                level.getGameTime();

        RandomSource random =
                level.getRandom();

        /*
         * Keep expired timers rolling during night. Otherwise every player
         * whose timer expired overnight would get a group immediately at
         * sunrise.
         */
        if (!isDaytime(server)
                || level.getDifficulty()
                == Difficulty.PEACEFUL) {

            for (ServerPlayer player : players) {
                NEXT_ATTEMPT.compute(
                        player.getUUID(),
                        (uuid, existing) ->
                                existing == null
                                        || existing <= now
                                        ? now
                                        + randomCooldown(random)
                                        : existing
                );
            }

            return;
        }

        Set<UUID> processed =
                new HashSet<>();

        for (ServerPlayer player : players) {
            if (processed.contains(
                    player.getUUID()
            )) {
                continue;
            }

            List<ServerPlayer> group =
                    collectPlayerGroup(
                            player,
                            players,
                            processed
                    );

            long readyAt =
                    initializeAndGetGroupReadyTime(
                            group,
                            now,
                            random
                    );

            if (now < readyAt) {
                continue;
            }

            if (countNearbyDayGroupMobs(
                    level,
                    group
            ) >= MAX_ACTIVE_DAY_GROUP_MOBS_NEAR_PLAYERS) {

                scheduleGroup(
                        group,
                        now
                                + randomBetween(
                                        random,
                                        FAILED_RETRY_MIN_TICKS,
                                        FAILED_RETRY_MAX_TICKS
                                )
                );

                continue;
            }

            ServerPlayer target =
                    group.get(
                            random.nextInt(
                                    group.size()
                            )
                    );

            boolean spawned =
                    trySpawnGroup(
                            level,
                            group,
                            target,
                            random
                    );

            long delay =
                    spawned
                            ? randomCooldown(random)
                            : randomBetween(
                                    random,
                                    FAILED_RETRY_MIN_TICKS,
                                    FAILED_RETRY_MAX_TICKS
                            );

            scheduleGroup(
                    group,
                    now + delay
            );
        }
    }

    @SubscribeEvent
    public static void onServerStopping(
            ServerStoppingEvent event
    ) {
        NEXT_ATTEMPT.clear();
        tickCounter = 0;
    }

    private static List<ServerPlayer> getEligiblePlayers(
            ServerLevel level
    ) {
        List<ServerPlayer> result =
                new ArrayList<>();

        for (ServerPlayer player : level.players()) {
            if (player.isAlive()
                    && !player.isSpectator()) {

                result.add(player);
            }
        }

        return result;
    }

    /**
     * Players within 96 blocks are treated as one spawn group. The breadth
     * first walk also handles chains of nearby players without spawning one
     * daytime pack for every person standing in the same settlement.
     */
    private static List<ServerPlayer> collectPlayerGroup(
            ServerPlayer start,
            List<ServerPlayer> players,
            Set<UUID> processed
    ) {
        List<ServerPlayer> group =
                new ArrayList<>();

        ArrayDeque<ServerPlayer> open =
                new ArrayDeque<>();

        open.add(start);
        processed.add(start.getUUID());

        while (!open.isEmpty()) {
            ServerPlayer current =
                    open.removeFirst();

            group.add(current);

            for (ServerPlayer candidate : players) {
                if (processed.contains(
                        candidate.getUUID()
                )) {
                    continue;
                }

                if (current.distanceToSqr(
                        candidate
                ) <= PLAYER_GROUP_RADIUS_SQR) {

                    processed.add(
                            candidate.getUUID()
                    );

                    open.addLast(candidate);
                }
            }
        }

        return group;
    }

    private static long initializeAndGetGroupReadyTime(
            List<ServerPlayer> group,
            long now,
            RandomSource random
    ) {
        long readyAt =
                Long.MIN_VALUE;

        for (ServerPlayer player : group) {
            long playerReady =
                    NEXT_ATTEMPT.computeIfAbsent(
                            player.getUUID(),
                            uuid ->
                                    now
                                    + randomCooldown(
                                            random
                                    )
                    );

            /*
             * Use the latest timer when groups merge. A player cannot walk
             * into another group to force an immediate extra spawn.
             */
            readyAt =
                    Math.max(
                            readyAt,
                            playerReady
                    );
        }

        return readyAt;
    }

    private static void scheduleGroup(
            List<ServerPlayer> group,
            long nextAttempt
    ) {
        for (ServerPlayer player : group) {
            NEXT_ATTEMPT.put(
                    player.getUUID(),
                    nextAttempt
            );
        }
    }

    private static boolean trySpawnGroup(
            ServerLevel level,
            List<ServerPlayer> playerGroup,
            ServerPlayer target,
            RandomSource random
    ) {
        BlockPos center =
                findGroupCenter(
                        level,
                        playerGroup,
                        target,
                        random
                );

        if (center == null) {
            return false;
        }

        int requested =
                MIN_GROUP_SIZE
                        + random.nextInt(
                                MAX_GROUP_SIZE
                                        - MIN_GROUP_SIZE
                                        + 1
                        );

        int spawned =
                0;

        for (int i = 0;
             i < requested;
             i++) {

            if (trySpawnMember(
                    level,
                    playerGroup,
                    center,
                    random
            )) {
                spawned++;
            }
        }

        /*
         * One stray mob is not the intended daytime behavior. A normal
         * successful group should contain at least two members; with the
         * search counts above it will normally reach the requested 3-6.
         */
        return spawned >= 2;
    }

    private static BlockPos findGroupCenter(
            ServerLevel level,
            List<ServerPlayer> playerGroup,
            ServerPlayer target,
            RandomSource random
    ) {
        for (int attempt = 0;
             attempt < MAX_GROUP_SEARCH_ATTEMPTS;
             attempt++) {

            double angle =
                    random.nextDouble()
                            * Math.PI
                            * 2.0;

            double distance =
                    SPAWN_MIN_DISTANCE
                            + random.nextDouble()
                            * (
                                    SPAWN_MAX_DISTANCE
                                    - SPAWN_MIN_DISTANCE
                            );

            int x =
                    (int) Math.floor(
                            target.getX()
                                    + Math.cos(angle)
                                    * distance
                    );

            int z =
                    (int) Math.floor(
                            target.getZ()
                                    + Math.sin(angle)
                                    * distance
                    );

            int y =
                    level.getHeight(
                            Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                            x,
                            z
                    );

            BlockPos pos =
                    new BlockPos(
                            x,
                            y,
                            z
                    );

            /*
             * Don't create surface packs far above a player who is mining
             * underground or far below a player on a mountain/cliff.
             */
            if (Math.abs(
                    y - target.getY()
            ) > 12.0) {
                continue;
            }

            if (!isValidSurfacePosition(
                    level,
                    pos
            )) {
                continue;
            }

            if (level.getBiome(pos)
                    .is(Biomes.MUSHROOM_FIELDS)) {
                continue;
            }

            if (isTooCloseToAnyPlayer(
                    playerGroup,
                    pos
            )) {
                continue;
            }

            return pos;
        }

        return null;
    }

    private static boolean trySpawnMember(
            ServerLevel level,
            List<ServerPlayer> playerGroup,
            BlockPos center,
            RandomSource random
    ) {
        for (int attempt = 0;
             attempt < MAX_MEMBER_SEARCH_ATTEMPTS;
             attempt++) {

            int x =
                    center.getX()
                            + random.nextInt(9)
                            - 4;

            int z =
                    center.getZ()
                            + random.nextInt(9)
                            - 4;

            int y =
                    level.getHeight(
                            Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                            x,
                            z
                    );

            BlockPos pos =
                    new BlockPos(
                            x,
                            y,
                            z
                    );

            if (Math.abs(
                    y - center.getY()
            ) > 4) {
                continue;
            }

            if (!isValidSurfacePosition(
                    level,
                    pos
            )) {
                continue;
            }

            if (level.getBiome(pos)
                    .is(Biomes.MUSHROOM_FIELDS)) {
                continue;
            }

            if (isTooCloseToAnyPlayer(
                    playerGroup,
                    pos
            )) {
                continue;
            }

            EntityType<? extends Mob> type =
                    selectDayMob(
                            level,
                            pos,
                            random
                    );

            Mob mob =
                    type.create(
                            level,
                            EntitySpawnReason.EVENT
                    );

            if (mob == null) {
                continue;
            }

            mob.snapTo(
                    pos.getX() + 0.5,
                    pos.getY(),
                    pos.getZ() + 0.5,
                    random.nextFloat()
                            * 360.0F,
                    0.0F
            );

            if (!level.noCollision(mob)
                    || level.containsAnyLiquid(
                            mob.getBoundingBox()
                    )) {

                mob.discard();
                continue;
            }

            mob.finalizeSpawn(
                    level,
                    level.getCurrentDifficultyAt(
                            pos
                    ),
                    EntitySpawnReason.EVENT,
                    null
            );

            if (mob.isSpawnCancelled()) {
                mob.discard();
                continue;
            }

            ServerPlayer target =
                    nearestPlayer(
                            playerGroup,
                            mob
                    );

            if (target != null) {
                mob.setTarget(target);
            }

            mob.addTag(DAY_GROUP_TAG);

            level.addFreshEntityWithPassengers(
                    mob
            );

            return true;
        }

        return false;
    }

    private static EntityType<? extends Mob> selectDayMob(
            ServerLevel level,
            BlockPos pos,
            RandomSource random
    ) {
        int roll =
                random.nextInt(100);

        /*
         * Every mob in these pools survives daylight.
         *
         * Desert:
         * 45% Husk, 30% Spider, 20% Creeper, 5% Witch.
         *
         * Elsewhere:
         * 55% Spider, 35% Creeper, 10% Witch.
         *
         * Spiders are explicitly assigned a player target on spawn so the
         * initial group is still hostile even in bright light.
         */
        if (level.getBiome(pos)
                .is(Biomes.DESERT)) {

            if (roll < 45) {
                return EntityTypes.HUSK;
            }

            if (roll < 75) {
                return EntityTypes.SPIDER;
            }

            if (roll < 95) {
                return EntityTypes.CREEPER;
            }

            return EntityTypes.WITCH;
        }

        if (roll < 55) {
            return EntityTypes.SPIDER;
        }

        if (roll < 90) {
            return EntityTypes.CREEPER;
        }

        return EntityTypes.WITCH;
    }

    private static boolean isValidSurfacePosition(
            ServerLevel level,
            BlockPos pos
    ) {
        if (!level.getWorldBorder()
                .isWithinBounds(pos)) {

            return false;
        }

        if (!level.getFluidState(pos)
                .isEmpty()) {

            return false;
        }

        if (!level.getFluidState(
                pos.below()
        ).isEmpty()) {

            return false;
        }

        if (!level.getBlockState(pos)
                .isAir()) {

            return false;
        }

        if (!level.getBlockState(
                pos.above()
        ).isAir()) {

            return false;
        }

        return level.getBlockState(
                        pos.below()
                )
                .isFaceSturdy(
                        level,
                        pos.below(),
                        Direction.UP
                );
    }

    private static boolean isTooCloseToAnyPlayer(
            List<ServerPlayer> players,
            BlockPos pos
    ) {
        double x =
                pos.getX() + 0.5;

        double y =
                pos.getY();

        double z =
                pos.getZ() + 0.5;

        for (ServerPlayer player : players) {
            if (player.distanceToSqr(
                    x,
                    y,
                    z
            ) < ABSOLUTE_MIN_PLAYER_DISTANCE_SQR) {

                return true;
            }
        }

        return false;
    }

    private static ServerPlayer nearestPlayer(
            List<ServerPlayer> players,
            Mob mob
    ) {
        ServerPlayer nearest =
                null;

        double bestDistance =
                Double.MAX_VALUE;

        for (ServerPlayer player : players) {
            double distance =
                    player.distanceToSqr(
                            mob
                    );

            if (distance < bestDistance) {
                bestDistance =
                        distance;

                nearest =
                        player;
            }
        }

        return nearest;
    }

    private static int countNearbyDayGroupMobs(
            ServerLevel level,
            List<ServerPlayer> group
    ) {
        Set<UUID> counted =
                new HashSet<>();

        for (ServerPlayer player : group) {
            for (Mob mob : level.getEntitiesOfClass(
                    Mob.class,
                    player.getBoundingBox()
                            .inflate(
                                    ACTIVE_DAY_GROUP_CHECK_RADIUS
                            ),
                    mob ->
                            mob.entityTags()
                                    .contains(
                                            DAY_GROUP_TAG
                                    )
            )) {
                counted.add(
                        mob.getUUID()
                );
            }
        }

        return counted.size();
    }

    private static boolean isDaytime(
            MinecraftServer server
    ) {
        long totalTicks =
                server.clockManager()
                        .getInstance(
                                server.registryAccess()
                                        .getOrThrow(
                                                net.minecraft.world.clock.WorldClocks.OVERWORLD
                                        )
                        )
                        .totalTicks();

        long timeOfDay =
                Math.floorMod(
                        totalTicks,
                        DAY_TICKS
                );

        return timeOfDay < NIGHT_START
                || timeOfDay >= NIGHT_END;
    }

    private static long randomCooldown(
            RandomSource random
    ) {
        return randomBetween(
                random,
                MIN_COOLDOWN_TICKS,
                MAX_COOLDOWN_TICKS
        );
    }

    private static long randomBetween(
            RandomSource random,
            long min,
            long max
    ) {
        if (max <= min) {
            return min;
        }

        long bound =
                max - min + 1;

        if (bound <= Integer.MAX_VALUE) {
            return min
                    + random.nextInt(
                            (int) bound
                    );
        }

        return min
                + Math.floorMod(
                        random.nextLong(),
                        bound
                );
    }

    private static void removeOfflineCooldowns(
            List<ServerPlayer> players
    ) {
        Set<UUID> active =
                new HashSet<>();

        for (ServerPlayer player : players) {
            active.add(
                    player.getUUID()
            );
        }

        NEXT_ATTEMPT.keySet()
                .removeIf(
                        uuid ->
                                !active.contains(
                                        uuid
                                )
                );
    }
}
