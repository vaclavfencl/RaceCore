package com.example.racecore.race.ability;

import com.example.racecore.race.RaceDefinition;
import net.minecraft.core.BlockPos;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/**
 * Elf-style ladder movement on leaves.
 *
 * Leaves keep normal vanilla collision.
 *
 * While the player is touching a leaf block:
 * - Space -> climb upward.
 * - Shift -> hold vertical position.
 * - No key -> vanilla gravity/movement.
 */
public final class LeafClimbingHelper {
    private static final double TOUCH_MARGIN = 0.10;

    public static final byte ACTION_HOLD = 0;
    public static final byte ACTION_CLIMB = 1;

    private LeafClimbingHelper() {
    }

    public static boolean isTouchingLeaves(
            Player player
    ) {
        AABB box =
                player.getBoundingBox()
                        .inflate(TOUCH_MARGIN);

        int minX = Mth.floor(box.minX);
        int minY = Mth.floor(box.minY);
        int minZ = Mth.floor(box.minZ);

        int maxX = Mth.floor(box.maxX);
        int maxY = Mth.floor(box.maxY);
        int maxZ = Mth.floor(box.maxZ);

        for (BlockPos pos : BlockPos.betweenClosed(
                minX,
                minY,
                minZ,
                maxX,
                maxY,
                maxZ
        )) {
            if (player.level()
                    .getBlockState(pos)
                    .is(BlockTags.LEAVES)) {
                return true;
            }
        }

        return false;
    }

    public static void apply(
            Player player,
            RaceDefinition race,
            byte action
    ) {
        Vec3 movement =
                player.getDeltaMovement();

        double vertical;

        if (action == ACTION_CLIMB) {
            vertical =
                    Math.max(
                            movement.y,
                            race.leafClimbSpeed()
                    );
        } else {
            // Shift = grab/hold onto the leaves.
            vertical = 0.0;
        }

        player.setDeltaMovement(
                movement.x,
                vertical,
                movement.z
        );

        player.resetFallDistance();
    }
}
