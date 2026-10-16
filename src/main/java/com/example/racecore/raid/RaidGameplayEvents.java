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
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

import java.util.HashSet;
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

    private static BlockPos findSafePlace(ServerLevel level, BlockPos origin) {
        // Keep at least one block gap from bell. Search usable air without
        // replacing villagers, beds, furniture, or player-made constructions.
        for (int radius = 1; radius <= REWARD_SEARCH_RADIUS; radius++) {
            for (int yOffset = 0; yOffset <= 3; yOffset++) {
                for (int side = 0; side < 2; side++) {
                    int dy = side == 0 ? yOffset : -yOffset;
                    if (yOffset == 0 && side == 1) continue;
                    for (int dx = -radius; dx <= radius; dx++) {
                        for (int dz = -radius; dz <= radius; dz++) {
                            if (Math.max(Math.abs(dx), Math.abs(dz)) != radius) continue;
                            BlockPos pos = origin.offset(dx, dy, dz);
                            if (isSafe(level, pos)) return pos.immutable();
                        }
                    }
                }
            }
        }
        return null;
    }

    private static boolean isSafe(ServerLevel level, BlockPos pos) {
        if (!level.isInWorldBounds(pos) || !level.isInWorldBounds(pos.above())
                || !level.hasChunkAt(pos) || !level.hasChunkAt(pos.above())) return false;
        if (!level.isEmptyBlock(pos) || !level.isEmptyBlock(pos.above())
                || !level.getFluidState(pos).isEmpty()) return false;
        BlockPos ground = pos.below();
        return level.isInWorldBounds(ground) && level.hasChunkAt(ground)
                && level.getBlockState(ground).isFaceSturdy(level, ground, Direction.UP);
    }
}
