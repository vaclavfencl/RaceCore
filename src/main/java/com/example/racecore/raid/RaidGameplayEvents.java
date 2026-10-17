package com.example.racecore.raid;

import com.example.racecore.RaceCore;
import com.example.racecore.death.DeathAttachments;
import com.example.racecore.death.DeathContent;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.raid.Raid;
import net.minecraft.world.entity.raid.Raider;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BarrelBlockEntity;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.AABB;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.UUID;

/**
 * Server-side raid UX and victory rewards. Invoked by the Raid.tick mixin.
 * The entire eligible roster is snapped on the first tick, before wave spawning.
 */
@EventBusSubscriber(modid = RaceCore.MODID)
public final class RaidGameplayEvents {
    private static final int PARTICIPATION_RADIUS = 96;
    private static final double PARTICIPATION_RADIUS_SQR =
            (double) PARTICIPATION_RADIUS * PARTICIPATION_RADIUS;
    private static final int REWARD_SEARCH_RADIUS = 10;
    private static final int BELL_CUBE_HALF_EXTENT = 2;
    private static final int BELL_SEARCH_RADIUS = 32;

    private RaidGameplayEvents() {}

    /** Executed before the vanilla Raid.tick body, including before first wave spawning. */
    public static void beforeRaidTick(ServerLevel level, Raid raid) {
        int raidId = level.getRaids().getId(raid).orElse(-1);
        if (raidId < 0) return;
        RaidRewardSavedData data = RaidRewardSavedData.get(level);
        if (data.getRecord(raidId) != null) return;

        // If RaceCore was installed mid-raid, do NOT let latecomers claim rewards.
        if (raid.isOver() || raid.getGroupsSpawned() > 0) return;

        BlockPos center = raid.getCenter();
        Set<UUID> initialPlayers = new HashSet<>();
        for (ServerPlayer player : level.getPlayers(p -> !p.isSpectator()
                && p.isAlive()
                && p.distanceToSqr(center.getX() + 0.5,
                        center.getY() + 0.5, center.getZ() + 0.5)
                   <= PARTICIPATION_RADIUS_SQR)) {
            initialPlayers.add(player.getUUID());
        }

        data.rememberRaid(raidId, center, initialPlayers);
        RaceCore.LOGGER.info("Raid {} began at {} with {} eligible reward players.",
                raidId, center, initialPlayers.size());
    }

    /** Executed after the vanilla Raid.tick body, to observe victory promptly. */
    public static void afterRaidTick(ServerLevel level, Raid raid) {
        int raidId = level.getRaids().getId(raid).orElse(-1);
        if (raidId < 0) return;
        RaidRewardSavedData data = RaidRewardSavedData.get(level);

        if (raid.isVictory()) {
            RaidRewardSavedData.RaidRecord record = data.getRecord(raidId);
            if (record != null && !record.victory) {
                data.markVictory(raidId);
                dispensePending(level, raidId, data);
            }
        } else if (raid.isLoss() || raid.isStopped()) {
            data.forgetDefeatedRaid(raidId);
        }

        // Short-lived Glowing is continuously refreshed while a nearby raid
        // participant holds a spyglass. It fades naturally after removal.
        if (!raid.isOver() && level.getGameTime() % 10 == 0
                && nearbyPlayerHasSpyglass(level, raid.getCenter())) {
            for (Raider raider : raid.getAllRaiders()) {
                if (raider.isAlive() && !raider.isRemoved()) {
                    raider.addEffect(new MobEffectInstance(
                            MobEffects.GLOWING, 25, 0, false, false, true));
                }
            }
        }
    }

    /** Retry if the bell area was obstructed or the game restarted after victory. */
    @SubscribeEvent
    public static void onServerTick(ServerTickEvent.Post event) {
        if (event.getServer().overworld().getGameTime() % 20 != 0) return;
        for (ServerLevel level : event.getServer().getAllLevels()) {
            RaidRewardSavedData data = RaidRewardSavedData.get(level);
            for (Integer raidId : Set.copyOf(data.records().keySet())) {
                RaidRewardSavedData.RaidRecord record = data.getRecord(raidId);
                if (record != null && record.victory
                        && record.paid.size() < record.eligible.size()) {
                    dispensePending(level, raidId, data);
                }
            }
        }
    }

    private static boolean nearbyPlayerHasSpyglass(ServerLevel level, BlockPos center) {
        for (ServerPlayer player : level.getPlayers(p -> !p.isSpectator()
                && p.distanceToSqr(center.getX() + 0.5,
                        center.getY() + 0.5, center.getZ() + 0.5)
                        <= PARTICIPATION_RADIUS_SQR)) {
            for (int slot = 0; slot < player.getInventory().getContainerSize(); slot++) {
                if (player.getInventory().getItem(slot).is(Items.SPYGLASS)) return true;
            }
        }
        return false;
    }

    private static void dispensePending(ServerLevel level, int raidId,
                                        RaidRewardSavedData data) {
        RaidRewardSavedData.RaidRecord record = data.getRecord(raidId);
        if (record == null || !record.victory) return;

        BlockPos origin = findBell(level, record.center);
        if (origin == null) origin = record.center; // Bell may have been destroyed in raid.

        for (UUID recipient : Set.copyOf(record.eligible)) {
            if (record.paid.contains(recipient)) continue;
            BlockPos placement = findSafePlace(level, origin);
            if (placement == null) {
                // Do not consume the entitlement: retry on the next server tick interval.
                return;
            }
            if (!level.setBlock(placement,
                    DeathContent.DEATH_BARREL.get().defaultBlockState(), 3)) return;

            if (!(level.getBlockEntity(placement) instanceof BarrelBlockEntity barrel)) {
                level.removeBlock(placement, false);
                return;
            }

            for (int slot = 0; slot < 27; slot++) {
                barrel.setItem(slot, new ItemStack(
                        slot == 13 ? Items.TOTEM_OF_UNDYING : Items.EMERALD));
            }
            barrel.setData(DeathAttachments.DEATH_BARREL_OWNER, recipient);
            barrel.setChanged();
            data.markPaid(raidId, recipient);

            ServerPlayer player = level.getServer().getPlayerList().getPlayer(recipient);
            if (player != null) {
                player.sendSystemMessage(Component.literal(
                        "Raid reward barrel for you: "
                                + placement.getX() + " "
                                + placement.getY() + " "
                                + placement.getZ()));
            }
            RaceCore.LOGGER.info("Placed raid {} victory reward for {} at {}.",
                    raidId, recipient, placement);
        }
    }

    /** Scan for a placed village bell, not an inventory item. Only used at victory. */
    private static BlockPos findBell(ServerLevel level, BlockPos center) {
        BlockPos best = null;
        double bestDistance = Double.MAX_VALUE;
        for (int x = -BELL_SEARCH_RADIUS; x <= BELL_SEARCH_RADIUS; x++) {
            for (int z = -BELL_SEARCH_RADIUS; z <= BELL_SEARCH_RADIUS; z++) {
                for (int y = -8; y <= 8; y++) {
                    BlockPos pos = center.offset(x, y, z);
                    if (!level.isInWorldBounds(pos) || !level.hasChunkAt(pos)) continue;
                    if (!level.getBlockState(pos).is(Blocks.BELL)) continue;
                    double dist = pos.distSqr(center);
                    if (dist < bestDistance) {
                        best = pos.immutable();
                        bestDistance = dist;
                    }
                }
            }
        }
        return best;
    }

    /**
     * Choose a random usable position in the 5x5x5 bell-centered cube first.
     * Prefer aboveground, supported floor locations; indoors is an allowed
     * fallback. If that small cube is full, expand the search ring-by-ring.
     * No existing blocks are overwritten.
     */
    private static BlockPos findSafePlace(ServerLevel level, BlockPos origin) {
        List<BlockPos> inner = new ArrayList<>();
        for (int dy = -BELL_CUBE_HALF_EXTENT; dy <= BELL_CUBE_HALF_EXTENT; dy++) {
            for (int dx = -BELL_CUBE_HALF_EXTENT; dx <= BELL_CUBE_HALF_EXTENT; dx++) {
                for (int dz = -BELL_CUBE_HALF_EXTENT; dz <= BELL_CUBE_HALF_EXTENT; dz++) {
                    BlockPos pos = origin.offset(dx, dy, dz);
                    if (isSafe(level, pos)) inner.add(pos.immutable());
                }
            }
        }
        BlockPos nearby = randomlyChooseGroundFirst(level, inner);
        if (nearby != null) return nearby;

        // Bell may be boxed in, or more than one reward barrel is due.
        // Expand gradually without touching existing terrain or containers.
        for (int radius = 3; radius <= REWARD_SEARCH_RADIUS; radius++) {
            List<BlockPos> ring = new ArrayList<>();
            for (int dy = -3; dy <= 3; dy++) {
                for (int dx = -radius; dx <= radius; dx++) {
                    for (int dz = -radius; dz <= radius; dz++) {
                        if (Math.max(Math.abs(dx), Math.abs(dz)) != radius) continue;
                        BlockPos pos = origin.offset(dx, dy, dz);
                        if (isSafe(level, pos)) ring.add(pos.immutable());
                    }
                }
            }
            BlockPos farther = randomlyChooseGroundFirst(level, ring);
            if (farther != null) return farther;
        }
        return null; // Retry later: the eligible player's reward is never consumed.
    }

    private static BlockPos randomlyChooseGroundFirst(ServerLevel level,
                                                     List<BlockPos> candidates) {
        if (candidates.isEmpty()) return null;
        // Shuffle first so position isn't biased toward negative X/Z or low Y.
        Collections.shuffle(candidates, new Random(level.getRandom().nextLong()));
        for (BlockPos candidate : candidates) {
            if (level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                    candidate.getX(), candidate.getZ()) == candidate.getY()) {
                return candidate; // Exposed ground, not a rooftop gap or indoor shelf.
            }
        }
        return candidates.getFirst(); // A supported indoor floor is fine too.
    }

    private static boolean isSafe(ServerLevel level, BlockPos pos) {
        if (!level.isInWorldBounds(pos) || !level.isInWorldBounds(pos.above())
                || !level.hasChunkAt(pos) || !level.hasChunkAt(pos.above())) return false;
        if (!level.isEmptyBlock(pos) || !level.isEmptyBlock(pos.above())
                || !level.getFluidState(pos).isEmpty()) return false;
        BlockPos ground = pos.below();
        if (!level.isInWorldBounds(ground) || !level.hasChunkAt(ground)
                || !level.getBlockState(ground).isFaceSturdy(level, ground, Direction.UP)) {
            return false;
        }
        // Do not trap villagers, players, or animals inside a newly placed barrel.
        return level.getEntities(null, new AABB(pos)).isEmpty();
    }
}
