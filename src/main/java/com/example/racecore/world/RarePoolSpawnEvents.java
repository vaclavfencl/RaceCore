package com.example.racecore.world;

import com.example.racecore.RaceCore;
import com.example.racecore.world.encounter.EncounterPoolEntry;
import com.example.racecore.world.encounter.EncounterPoolRegistry;
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
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.level.levelgen.Heightmap;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.server.ServerStoppingEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

/**
 * Rare ambient mobs share RaceCore's day/night weighted pools but do not join
 * the ordinary packs or player hunts.
 *
 * Every 3-5 minutes a nearby player group gets 3-6 virtual weighted rolls.
 * Normal pool entries consume a roll but do not spawn here. Only entries with
 * huntEligible=false spawn. With Shulker weight 5 and Enderman weight 5 in the
 * Night pool, their per-roll rarity is the same.
 */
@EventBusSubscriber(modid = RaceCore.MODID)
public final class RarePoolSpawnEvents {
    public static final String RARE_POOL_TAG = "racecore_rare_pool_spawn";

    private static final long DAY_TICKS = 24_000L;
    private static final long NIGHT_START = 13_000L;
    private static final long NIGHT_END = 23_000L;

    private static final int TICK_INTERVAL = 20;
    private static final long MIN_COOLDOWN_TICKS = 3L * 60L * 20L;
    private static final long MAX_COOLDOWN_TICKS = 5L * 60L * 20L;

    private static final double PLAYER_GROUP_RADIUS = 96.0;
    private static final double PLAYER_GROUP_RADIUS_SQR =
            PLAYER_GROUP_RADIUS * PLAYER_GROUP_RADIUS;

    private static final double SPAWN_MIN_DISTANCE = 32.0;
    private static final double SPAWN_MAX_DISTANCE = 48.0;

    private static final double ABSOLUTE_MIN_PLAYER_DISTANCE = 24.0;
    private static final double ABSOLUTE_MIN_PLAYER_DISTANCE_SQR =
            ABSOLUTE_MIN_PLAYER_DISTANCE * ABSOLUTE_MIN_PLAYER_DISTANCE;

    private static final int MIN_VIRTUAL_GROUP_SIZE = 3;
    private static final int MAX_VIRTUAL_GROUP_SIZE = 6;
    private static final int MAX_CENTER_SEARCH_ATTEMPTS = 18;
    private static final int MAX_SPAWN_SEARCH_ATTEMPTS = 12;

    private static final Map<UUID, Long> NEXT_ATTEMPT = new HashMap<>();
    private static int tickCounter;

    private RarePoolSpawnEvents() {
    }

    @SubscribeEvent
    public static void onServerTick(ServerTickEvent.Post event) {
        tickCounter++;
        if (tickCounter % TICK_INTERVAL != 0) {
            return;
        }

        MinecraftServer server = event.getServer();
        ServerLevel level = server.overworld();
        List<ServerPlayer> players = getEligiblePlayers(level);

        removeOfflineCooldowns(players);

        if (players.isEmpty()) {
            return;
        }

        long now = level.getGameTime();
        RandomSource random = level.getRandom();
        Set<UUID> processed = new HashSet<>();

        for (ServerPlayer player : players) {
            if (processed.contains(player.getUUID())) {
                continue;
            }

            List<ServerPlayer> group = collectPlayerGroup(
                    player,
                    players,
                    processed
            );

            long readyAt = initializeAndGetGroupReadyTime(
                    group,
                    now,
                    random
            );

            if (now < readyAt) {
                continue;
            }

            scheduleGroup(
                    group,
                    now + randomCooldown(random)
            );

            if (level.getDifficulty() == Difficulty.PEACEFUL) {
                continue;
            }

            ServerPlayer target = group.get(random.nextInt(group.size()));

            if (BannerClaimEvents.blocksSurfaceHostileSpawn(
                    level,
                    target.blockPosition()
            )) {
                continue;
            }

            trySpawnRarePoolEntry(
                    server,
                    level,
                    group,
                    target,
                    random
            );
        }
    }

    @SubscribeEvent
    public static void onServerStopping(ServerStoppingEvent event) {
        NEXT_ATTEMPT.clear();
        tickCounter = 0;
    }

    private static void trySpawnRarePoolEntry(
            MinecraftServer server,
            ServerLevel level,
            List<ServerPlayer> group,
            ServerPlayer target,
            RandomSource random
    ) {
        BlockPos center = findGroupCenter(
                level,
                group,
                target,
                random
        );

        if (center == null) {
            return;
        }

        String poolId = isDaytime(server)
                ? EncounterPoolRegistry.DAY
                : EncounterPoolRegistry.NIGHT;

        int rolls = MIN_VIRTUAL_GROUP_SIZE
                + random.nextInt(
                        MAX_VIRTUAL_GROUP_SIZE
                                - MIN_VIRTUAL_GROUP_SIZE
                                + 1
                );

        Map<String, Integer> spawnedByType = new HashMap<>();

        for (int i = 0; i < rolls; i++) {
            EncounterPoolEntry entry =
                    EncounterPoolRegistry.pickIncludingSpecial(
                            poolId,
                            level,
                            center,
                            random
                    );

            if (entry == null || entry.huntEligible()) {
                continue;
            }

            String key = entry.entityId().toString();
            int alreadySpawned = spawnedByType.getOrDefault(key, 0);

            if (alreadySpawned >= entry.maxPerEncounter()) {
                continue;
            }

            Mob mob = trySpawnSpecialMob(
                    level,
                    group,
                    target,
                    center,
                    entry,
                    random
            );

            if (mob == null) {
                continue;
            }

            mob.addTag(RARE_POOL_TAG);
            level.addFreshEntityWithPassengers(mob);
            spawnedByType.put(key, alreadySpawned + 1);
        }
    }

    private static Mob trySpawnSpecialMob(
            ServerLevel level,
            List<ServerPlayer> group,
            ServerPlayer target,
            BlockPos center,
            EncounterPoolEntry entry,
            RandomSource random
    ) {
        EntityType<?> type = entry.resolveEntityType().orElse(null);

        if (type == null) {
            RaceCore.LOGGER.warn(
                    "Rare encounter pool entry '{}' is not a registered entity type.",
                    entry.entityId()
            );
            return null;
        }

        for (int attempt = 0; attempt < MAX_SPAWN_SEARCH_ATTEMPTS; attempt++) {
            int x = center.getX() + random.nextInt(9) - 4;
            int z = center.getZ() + random.nextInt(9) - 4;
            int y = level.getHeight(
                    Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                    x,
                    z
            );

            BlockPos pos = new BlockPos(x, y, z);

            if (Math.abs(y - center.getY()) > 4
                    || !isValidSurfacePosition(level, pos)
                    || level.getBiome(pos).is(Biomes.MUSHROOM_FIELDS)
                    || isTooCloseToAnyPlayer(group, pos)
                    || BannerClaimEvents.blocksSurfaceHostileSpawn(level, pos)) {
                continue;
            }

            Entity created = type.create(level, EntitySpawnReason.EVENT);

            if (!(created instanceof Mob mob)) {
                if (created != null) {
                    created.discard();
                }
                return null;
            }

            mob.snapTo(
                    pos.getX() + 0.5,
                    pos.getY(),
                    pos.getZ() + 0.5,
                    random.nextFloat() * 360.0F,
                    0.0F
            );

            if (!level.noCollision(mob)
                    || level.containsAnyLiquid(mob.getBoundingBox())) {
                mob.discard();
                continue;
            }

            mob.finalizeSpawn(
                    level,
                    level.getCurrentDifficultyAt(pos),
                    EntitySpawnReason.EVENT,
                    null
            );

            if (mob.isSpawnCancelled()) {
                mob.discard();
                continue;
            }

            mob.setTarget(target);
            return mob;
        }

        return null;
    }

    private static BlockPos findGroupCenter(
            ServerLevel level,
            List<ServerPlayer> group,
            ServerPlayer target,
            RandomSource random
    ) {
        for (int attempt = 0; attempt < MAX_CENTER_SEARCH_ATTEMPTS; attempt++) {
            double angle = random.nextDouble() * Math.PI * 2.0;
            double distance = SPAWN_MIN_DISTANCE
                    + random.nextDouble()
                    * (SPAWN_MAX_DISTANCE - SPAWN_MIN_DISTANCE);

            int x = (int) Math.floor(
                    target.getX() + Math.cos(angle) * distance
            );

            int z = (int) Math.floor(
                    target.getZ() + Math.sin(angle) * distance
            );

            int y = level.getHeight(
                    Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                    x,
                    z
            );

            BlockPos pos = new BlockPos(x, y, z);

            if (Math.abs(y - target.getY()) > 12.0
                    || !isValidSurfacePosition(level, pos)
                    || level.getBiome(pos).is(Biomes.MUSHROOM_FIELDS)
                    || isTooCloseToAnyPlayer(group, pos)
                    || BannerClaimEvents.blocksSurfaceHostileSpawn(level, pos)) {
                continue;
            }

            return pos;
        }

        return null;
    }

    private static boolean isValidSurfacePosition(
            ServerLevel level,
            BlockPos pos
    ) {
        if (!level.getWorldBorder().isWithinBounds(pos)
                || !level.getFluidState(pos).isEmpty()
                || !level.getFluidState(pos.below()).isEmpty()
                || !level.getBlockState(pos).isAir()
                || !level.getBlockState(pos.above()).isAir()) {
            return false;
        }

        return level.getBlockState(pos.below()).isFaceSturdy(
                level,
                pos.below(),
                Direction.UP
        );
    }

    private static boolean isTooCloseToAnyPlayer(
            List<ServerPlayer> players,
            BlockPos pos
    ) {
        double x = pos.getX() + 0.5;
        double y = pos.getY();
        double z = pos.getZ() + 0.5;

        for (ServerPlayer player : players) {
            if (player.distanceToSqr(x, y, z)
                    < ABSOLUTE_MIN_PLAYER_DISTANCE_SQR) {
                return true;
            }
        }

        return false;
    }

    private static List<ServerPlayer> getEligiblePlayers(ServerLevel level) {
        List<ServerPlayer> result = new ArrayList<>();

        for (ServerPlayer player : level.players()) {
            if (player.isAlive() && !player.isSpectator()) {
                result.add(player);
            }
        }

        return result;
    }

    private static List<ServerPlayer> collectPlayerGroup(
            ServerPlayer start,
            List<ServerPlayer> players,
            Set<UUID> processed
    ) {
        List<ServerPlayer> group = new ArrayList<>();
        ArrayDeque<ServerPlayer> open = new ArrayDeque<>();

        open.add(start);
        processed.add(start.getUUID());

        while (!open.isEmpty()) {
            ServerPlayer current = open.removeFirst();
            group.add(current);

            for (ServerPlayer candidate : players) {
                if (processed.contains(candidate.getUUID())) {
                    continue;
                }

                if (current.distanceToSqr(candidate) <= PLAYER_GROUP_RADIUS_SQR) {
                    processed.add(candidate.getUUID());
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
        long readyAt = Long.MIN_VALUE;

        for (ServerPlayer player : group) {
            long playerReady = NEXT_ATTEMPT.computeIfAbsent(
                    player.getUUID(),
                    uuid -> now + randomCooldown(random)
            );

            readyAt = Math.max(readyAt, playerReady);
        }

        return readyAt;
    }

    private static void scheduleGroup(
            List<ServerPlayer> group,
            long nextAttempt
    ) {
        for (ServerPlayer player : group) {
            NEXT_ATTEMPT.put(player.getUUID(), nextAttempt);
        }
    }

    private static long randomCooldown(RandomSource random) {
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

        long bound = max - min + 1;

        if (bound <= Integer.MAX_VALUE) {
            return min + random.nextInt((int) bound);
        }

        return min + Math.floorMod(random.nextLong(), bound);
    }

    private static boolean isDaytime(MinecraftServer server) {
        long totalTicks = server.clockManager()
                .getInstance(
                        server.registryAccess().getOrThrow(
                                net.minecraft.world.clock.WorldClocks.OVERWORLD
                        )
                )
                .totalTicks();

        long timeOfDay = Math.floorMod(totalTicks, DAY_TICKS);

        return timeOfDay < NIGHT_START || timeOfDay >= NIGHT_END;
    }

    private static void removeOfflineCooldowns(List<ServerPlayer> players) {
        Set<UUID> active = new HashSet<>();

        for (ServerPlayer player : players) {
            active.add(player.getUUID());
        }

        NEXT_ATTEMPT.keySet().removeIf(uuid -> !active.contains(uuid));
    }
}
