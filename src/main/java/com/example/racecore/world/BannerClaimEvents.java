package com.example.racecore.world;

import com.example.racecore.RaceCore;
import java.util.HashMap;
import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.AbstractBannerBlock;
import net.minecraft.world.level.block.entity.BannerBlockEntity;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.levelgen.Heightmap;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.entity.living.MobSpawnEvent;
import net.neoforged.neoforge.event.level.BlockEvent;
import net.neoforged.neoforge.event.level.ChunkEvent;
import net.neoforged.neoforge.event.server.ServerStoppingEvent;

/**
 * Phase 12.4: banner-based surface spawn protection.
 *
 * Any banner in an Overworld chunk claims that chunk against:
 * - vanilla NATURAL hostile surface spawning;
 * - RaceCore Phase 12.2 daytime groups.
 *
 * It intentionally does NOT affect underground natural spawning, spawners,
 * trial spawners, raids, structure/event spawns, or mobs already present.
 */
@EventBusSubscriber(modid = RaceCore.MODID)
public final class BannerClaimEvents {
    private static final String DAY_GROUP_TAG =
            DaytimeGroupSpawnEvents.DAY_GROUP_TAG;

    private static final long CACHE_VALIDATION_INTERVAL =
            20L;

    private static final Map<Long, ClaimCacheEntry> CLAIM_CACHE =
            new HashMap<>();

    private BannerClaimEvents() {
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

        ServerLevel level =
                event.getLevel()
                        .getLevel();

        if (level.dimension()
                != Level.OVERWORLD) {
            return;
        }

        if (blocksSurfaceHostileSpawn(
                level,
                event.getPos()
        )) {
            event.setResult(
                    MobSpawnEvent
                            .SpawnPlacementCheck
                            .Result
                            .FAIL
            );
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onEntityJoinLevel(
            EntityJoinLevelEvent event
    ) {
        if (!(event.getLevel()
                instanceof ServerLevel level)) {
            return;
        }

        if (level.dimension()
                != Level.OVERWORLD) {
            return;
        }

        Entity entity =
                event.getEntity();

        if (!(entity instanceof Mob mob)
                || !mob.entityTags()
                .contains(
                        DAY_GROUP_TAG
                )) {
            return;
        }

        if (blocksSurfaceHostileSpawn(
                level,
                mob.blockPosition()
        )) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void onChunkLoad(
            ChunkEvent.Load event
    ) {
        if (!(event.getLevel()
                instanceof ServerLevel level)
                || level.dimension()
                != Level.OVERWORLD
                || !(event.getChunk()
                instanceof LevelChunk chunk)) {
            return;
        }

        updateCacheFromChunk(
                level,
                chunk
        );
    }

    @SubscribeEvent
    public static void onChunkUnload(
            ChunkEvent.Unload event
    ) {
        if (!(event.getLevel()
                instanceof ServerLevel level)
                || level.dimension()
                != Level.OVERWORLD) {
            return;
        }

        CLAIM_CACHE.remove(
                event.getChunk()
                        .getPos()
                        .pack()
        );
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onBannerPlaced(
            BlockEvent.EntityPlaceEvent event
    ) {
        if (event.isCanceled()
                || !(event.getLevel()
                instanceof ServerLevel level)
                || level.dimension()
                != Level.OVERWORLD
                || !(event.getPlacedBlock()
                .getBlock()
                instanceof AbstractBannerBlock)) {
            return;
        }

        long chunkKey =
                ChunkPos.pack(
                        event.getPos()
                                .getX() >> 4,
                        event.getPos()
                                .getZ() >> 4
                );

        CLAIM_CACHE.put(
                chunkKey,
                new ClaimCacheEntry(
                        true,
                        level.getGameTime()
                )
        );
    }

    @SubscribeEvent
    public static void onServerStopping(
            ServerStoppingEvent event
    ) {
        CLAIM_CACHE.clear();
    }

    public static boolean blocksSurfaceHostileSpawn(
            ServerLevel level,
            BlockPos pos
    ) {
        if (level.dimension()
                != Level.OVERWORLD) {
            return false;
        }

        if (!isClaimed(
                level,
                pos
        )) {
            return false;
        }

        int surfaceY =
                level.getHeight(
                        Heightmap.Types
                                .MOTION_BLOCKING_NO_LEAVES,
                        pos.getX(),
                        pos.getZ()
                );

        return pos.getY()
                >= surfaceY;
    }

    private static boolean isClaimed(
            ServerLevel level,
            BlockPos pos
    ) {
        int chunkX =
                pos.getX() >> 4;

        int chunkZ =
                pos.getZ() >> 4;

        long chunkKey =
                ChunkPos.pack(
                        chunkX,
                        chunkZ
                );

        long now =
                level.getGameTime();

        ClaimCacheEntry cached =
                CLAIM_CACHE.get(
                        chunkKey
                );

        if (cached != null
                && now - cached.lastValidatedTick()
                < CACHE_VALIDATION_INTERVAL) {
            return cached.claimed();
        }

        LevelChunk chunk =
                level.getChunkSource()
                        .getChunkNow(
                                chunkX,
                                chunkZ
                        );

        if (chunk == null) {
            CLAIM_CACHE.remove(
                    chunkKey
            );
            return false;
        }

        return updateCacheFromChunk(
                level,
                chunk
        );
    }

    private static boolean updateCacheFromChunk(
            ServerLevel level,
            LevelChunk chunk
    ) {
        boolean claimed =
                chunk.getBlockEntities()
                        .values()
                        .stream()
                        .anyMatch(
                                blockEntity ->
                                        blockEntity
                                                instanceof BannerBlockEntity
                        );

        CLAIM_CACHE.put(
                chunk.getPos()
                        .pack(),
                new ClaimCacheEntry(
                        claimed,
                        level.getGameTime()
                )
        );

        return claimed;
    }

    private record ClaimCacheEntry(
            boolean claimed,
            long lastValidatedTick
    ) {
    }
}
