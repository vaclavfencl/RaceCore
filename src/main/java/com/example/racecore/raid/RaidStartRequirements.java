package com.example.racecore.raid;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.ai.village.poi.PoiManager;
import net.minecraft.world.entity.ai.village.poi.PoiRecord;
import net.minecraft.world.entity.ai.village.poi.PoiTypes;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.BedBlock;
import net.minecraft.world.phys.AABB;

import java.util.Comparator;

/**
 * A raid can START only when one 64 x 64 x 64 cube contains:
 *  - at least three living villagers,
 *  - three occupied (claimed) beds,
 *  - a placed bell.
 *
 * The cube is centered on the nearest bell within a 64-block cube of the
 * vanilla raid activation position. Each axis extends [-32, +31] blocks.
 * No roof or iron-golem requirement.
 */
public final class RaidStartRequirements {
    private static final int HALF = 32;
    // Maximum diagonal distance from cube center to corner is ~55.4 blocks.
    private static final int SEARCH_RADIUS = 56;

    private RaidStartRequirements() {}

    public static boolean allowNewRaid(ServerLevel level, BlockPos activation) {
        BlockPos bell = level.getPoiManager()
                .getInRange(type -> type.is(PoiTypes.MEETING),
                        activation, SEARCH_RADIUS, PoiManager.Occupancy.ANY)
                .map(PoiRecord::getPos)
                .filter(pos -> withinCube(activation, pos))
                .filter(level::hasChunkAt)
                .filter(pos -> level.getBlockState(pos).is(Blocks.BELL))
                .min(Comparator.comparingDouble(pos -> pos.distSqr(activation)))
                .orElse(null);
        if (bell == null) return false;

        // Villagers can be elsewhere in the same cube; a roof is not needed.
        AABB box = new AABB(
                bell.getX() - HALF, bell.getY() - HALF, bell.getZ() - HALF,
                bell.getX() + HALF, bell.getY() + HALF, bell.getZ() + HALF
        );
        if (level.getEntitiesOfClass(Villager.class, box, Villager::isAlive).size() < 3) {
            return false;
        }

        // HOME POIs with occupied tickets are *claimed* beds, not merely
        // blocks that happen to look like beds. Count one POI per bed head.
        long claimedBeds = level.getPoiManager()
                .getInRange(type -> type.is(PoiTypes.HOME),
                        bell, SEARCH_RADIUS, PoiManager.Occupancy.IS_OCCUPIED)
                .map(PoiRecord::getPos)
                .filter(pos -> withinCube(bell, pos))
                .filter(level::hasChunkAt)
                .filter(pos -> level.getBlockState(pos).getBlock() instanceof BedBlock)
                .limit(3)
                .count();
        return claimedBeds >= 3;
    }

    private static boolean withinCube(BlockPos origin, BlockPos pos) {
        return inRange(pos.getX() - origin.getX())
                && inRange(pos.getY() - origin.getY())
                && inRange(pos.getZ() - origin.getZ());
    }

    private static boolean inRange(int delta) {
        return delta >= -HALF && delta < HALF;
    }
}
