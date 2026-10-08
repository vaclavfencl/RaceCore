package com.example.racecore.world;

import com.example.racecore.RaceCore;
import com.example.racecore.world.encounter.EncounterManager;
import com.example.racecore.world.encounter.EncounterPoolEntry;
import com.example.racecore.world.encounter.EncounterPoolRegistry;
import com.example.racecore.world.encounter.PlayerHuntEncounter;
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
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.level.levelgen.Heightmap;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.server.ServerStoppingEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

/**
 * Phase 12.5: roaming player-hunt encounters.
 *
 * Hunts are independent from Phase 12.2's ordinary daytime hostile packs.
 * They can begin during either day or night:
 * - day uses encounter_pools/day.json;
 * - night uses encounter_pools/night.json.
 *
 * The scheduler aims for roughly one or two hunt attempts per full Minecraft
 * day/night cycle for a nearby player group. Once spawned, PlayerHuntEncounter
 * owns pursuit, last-known-position searching and cleanup.
 */
@EventBusSubscriber(modid = RaceCore.MODID)
public final class HuntEncounterEvents {
    private static final long DAY_TICKS = 24_000L;
    private static final long NIGHT_START = 13_000L;
    private static final long NIGHT_END = 23_000L;

    private static final int TICK_INTERVAL = 20;

    private static final long MIN_COOLDOWN_TICKS = 8L * 60L * 20L;
    private static final long MAX_COOLDOWN_TICKS = 14L * 60L * 20L;
    private static final long FAILED_RETRY_MIN_TICKS = 60L * 20L;
    private static final long FAILED_RETRY_MAX_TICKS = 2L * 60L * 20L;

    private static final double PLAYER_GROUP_RADIUS = 96.0;
    private static final double PLAYER_GROUP_RADIUS_SQR =
            PLAYER_GROUP_RADIUS * PLAYER_GROUP_RADIUS;

    private static final double SPAWN_MIN_DISTANCE = 36.0;
    private static final double SPAWN_MAX_DISTANCE = 52.0;
    private static final double ABSOLUTE_MIN_PLAYER_DISTANCE = 26.0;
    private static final double ABSOLUTE_MIN_PLAYER_DISTANCE_SQR =
            ABSOLUTE_MIN_PLAYER_DISTANCE * ABSOLUTE_MIN_PLAYER_DISTANCE;

    private static final int MAX_GROUP_SEARCH_ATTEMPTS = 24;
    private static final int MAX_MEMBER_SEARCH_ATTEMPTS = 12;
    private static final int EXTRA_HUNTERS_PER_ADDITIONAL_PLAYER = 1;
    private static final int MAX_EXTRA_HUNTERS = 4;

    private static final Map<UUID, Long> NEXT_ATTEMPT = new HashMap<>();
    private static int tickCounter;

    private HuntEncounterEvents() {
    }

    @SubscribeEvent
    public static void onRegisterCommands(RegisterCommandsEvent event) {
        HuntCommands.register(event.getDispatcher());
    }

    /**
     * Starts a hunt immediately using the same validation, player grouping,
     * pool selection and spawn path as the automatic scheduler.
     *
     * The manual command intentionally does not modify NEXT_ATTEMPT, so testing
     * a hunt does not postpone the next naturally scheduled hunt.
     */
    public static HuntStartResult startHuntNow(ServerPlayer target) {
        if (target == null || !target.isAlive() || target.isSpectator()) {
            return HuntStartResult.INVALID_TARGET;
        }

        if (!(target.level() instanceof ServerLevel level)
                || level.dimension() != Level.OVERWORLD) {
            return HuntStartResult.WRONG_DIMENSION;
        }

        if (level.getDifficulty() == Difficulty.PEACEFUL) {
            return HuntStartResult.PEACEFUL;
        }

        List<ServerPlayer> players = getEligiblePlayers(level);
        if (!players.contains(target)) {
            return HuntStartResult.INVALID_TARGET;
        }

        List<ServerPlayer> group = collectPlayerGroup(
                target,
                players,
                new HashSet<>()
        );

        if (hasActiveHunt(group)) {
            return HuntStartResult.ACTIVE_HUNT;
        }

        if (BannerClaimEvents.blocksSurfaceHostileSpawn(
                level,
                target.blockPosition()
        )) {
            return HuntStartResult.PROTECTED;
        }

        boolean started = tryStartHunt(
                level.getServer(),
                level,
                group,
                target,
                level.getRandom()
        );

        return started
                ? HuntStartResult.STARTED
                : HuntStartResult.NO_VALID_SPAWN;
    }

    public enum HuntStartResult {
        STARTED,
        INVALID_TARGET,
        WRONG_DIMENSION,
        PEACEFUL,
        ACTIVE_HUNT,
        PROTECTED,
        NO_VALID_SPAWN
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

        if (level.getDifficulty() == Difficulty.PEACEFUL) {
            for (ServerPlayer player : players) {
                NEXT_ATTEMPT.compute(
                        player.getUUID(),
                        (uuid, existing) -> existing == null || existing <= now
                                ? now + randomCooldown(random)
                                : existing
                );
            }
            return;
        }

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

            long readyAt = initializeAndGetGroupReadyTime(group, now, random);
            if (now < readyAt) {
                continue;
            }

            if (hasActiveHunt(group)) {
                scheduleGroup(
                        group,
                        now + randomBetween(
                                random,
                                FAILED_RETRY_MIN_TICKS,
                                FAILED_RETRY_MAX_TICKS
                        )
                );
                continue;
            }

            ServerPlayer target = group.get(random.nextInt(group.size()));

            if (BannerClaimEvents.blocksSurfaceHostileSpawn(
                    level,
                    target.blockPosition()
            )) {
                scheduleGroup(
                        group,
                        now + randomBetween(
                                random,
                                FAILED_RETRY_MIN_TICKS,
                                FAILED_RETRY_MAX_TICKS
                        )
                );
                continue;
            }

            boolean started = tryStartHunt(
                    server,
                    level,
                    group,
                    target,
                    random
            );

            long delay = started
                    ? randomCooldown(random)
                    : randomBetween(
                            random,
                            FAILED_RETRY_MIN_TICKS,
                            FAILED_RETRY_MAX_TICKS
                    );

            scheduleGroup(group, now + delay);
        }
    }

    @SubscribeEvent
    public static void onServerStopping(ServerStoppingEvent event) {
        NEXT_ATTEMPT.clear();
        tickCounter = 0;
    }

    private static boolean tryStartHunt(
            MinecraftServer server,
            ServerLevel level,
            List<ServerPlayer> playerGroup,
            ServerPlayer target,
            RandomSource random
    ) {
        BlockPos center = findGroupCenter(
                level,
                playerGroup,
                target,
                random
        );

        if (center == null) {
            return false;
        }

        String poolId = isDaytime(server)
                ? EncounterPoolRegistry.DAY
                : EncounterPoolRegistry.NIGHT;

        EncounterPoolEntry entry = EncounterPoolRegistry.pick(
                poolId,
                level,
                center,
                random
        );

        if (entry == null) {
            return false;
        }

        int requested = randomBetweenInt(random, entry.min(), entry.max());
        requested += Math.min(
                MAX_EXTRA_HUNTERS,
                Math.max(0, playerGroup.size() - 1)
                        * EXTRA_HUNTERS_PER_ADDITIONAL_PLAYER
        );

        PlayerHuntEncounter encounter = new PlayerHuntEncounter(
                level,
                target.getUUID(),
                target.blockPosition()
        );

        int spawned = 0;

        for (int i = 0; i < requested; i++) {
            Mob mob = trySpawnHunter(
                    level,
                    playerGroup,
                    target,
                    center,
                    entry,
                    random
            );

            if (mob != null) {
                encounter.addParticipant(mob);
                level.addFreshEntityWithPassengers(mob);
                spawned++;
            }
        }

        if (spawned < 2) {
            encounter.finish(true);
            return false;
        }

        encounter.activate();
        EncounterManager.register(encounter);
        return true;
    }

    private static Mob trySpawnHunter(
            ServerLevel level,
            List<ServerPlayer> playerGroup,
            ServerPlayer target,
            BlockPos center,
            EncounterPoolEntry entry,
            RandomSource random
    ) {
        EntityType<?> type = entry.resolveEntityType().orElse(null);
        if (type == null) {
            RaceCore.LOGGER.warn(
                    "Encounter pool entry '{}' is not a registered entity type.",
                    entry.entityId()
            );
            return null;
        }

        for (int attempt = 0; attempt < MAX_MEMBER_SEARCH_ATTEMPTS; attempt++) {
            int x = center.getX() + random.nextInt(11) - 5;
            int z = center.getZ() + random.nextInt(11) - 5;
            int y = level.getHeight(
                    Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                    x,
                    z
            );

            BlockPos pos = new BlockPos(x, y, z);

            if (Math.abs(y - center.getY()) > 5
                    || !isValidSurfacePosition(level, pos)
                    || level.getBiome(pos).is(Biomes.MUSHROOM_FIELDS)
                    || isTooCloseToAnyPlayer(playerGroup, pos)
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
            List<ServerPlayer> playerGroup,
            ServerPlayer target,
            RandomSource random
    ) {
        for (int attempt = 0; attempt < MAX_GROUP_SEARCH_ATTEMPTS; attempt++) {
            double angle = random.nextDouble() * Math.PI * 2.0;
            double distance = SPAWN_MIN_DISTANCE
                    + random.nextDouble() * (SPAWN_MAX_DISTANCE - SPAWN_MIN_DISTANCE);

            int x = (int) Math.floor(target.getX() + Math.cos(angle) * distance);
            int z = (int) Math.floor(target.getZ() + Math.sin(angle) * distance);
            int y = level.getHeight(
                    Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                    x,
                    z
            );

            BlockPos pos = new BlockPos(x, y, z);

            if (Math.abs(y - target.getY()) > 12.0
                    || !isValidSurfacePosition(level, pos)
                    || level.getBiome(pos).is(Biomes.MUSHROOM_FIELDS)
                    || isTooCloseToAnyPlayer(playerGroup, pos)
                    || BannerClaimEvents.blocksSurfaceHostileSpawn(level, pos)) {
                continue;
            }

            return pos;
        }

        return null;
    }

    private static boolean isValidSurfacePosition(ServerLevel level, BlockPos pos) {
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
            if (player.distanceToSqr(x, y, z) < ABSOLUTE_MIN_PLAYER_DISTANCE_SQR) {
                return true;
            }
        }

        return false;
    }

    private static boolean hasActiveHunt(List<ServerPlayer> group) {
        for (ServerPlayer player : group) {
            if (EncounterManager.hasActiveHuntForTarget(player.getUUID())) {
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

    private static int randomBetweenInt(RandomSource random, int min, int max) {
        if (max <= min) {
            return min;
        }

        return min + random.nextInt(max - min + 1);
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
