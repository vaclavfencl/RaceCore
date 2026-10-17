package com.example.racecore.mixin;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.raid.Raid;
import net.minecraft.world.level.levelgen.Heightmap;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Every wave retains its vanilla species composition but gains +50% members
 * (stochastic rounding for odd counts). Bonus raiders scale as well.
 * Wave spawn locations are filtered to suitable ground, within +/-12 Y
 * of the raid center, so raids do not spawn on cliffs or underground.
 */
@Mixin(Raid.class)
public abstract class RaidWaveBalanceMixin {
    @Shadow public abstract BlockPos getCenter();

    @Inject(method = "getDefaultNumSpawns", at = @At("RETURN"), cancellable = true)
    private void racecore$scaleDefaultSpawns(CallbackInfoReturnable<Integer> callback) {
        callback.setReturnValue(extraHalf(callback.getReturnValue()));
    }

    @Inject(method = "getPotentialBonusSpawns", at = @At("RETURN"), cancellable = true)
    private void racecore$scaleBonusSpawns(CallbackInfoReturnable<Integer> callback) {
        callback.setReturnValue(extraHalf(callback.getReturnValue()));
    }

    private int extraHalf(int original) {
        if (original <= 0) return original;
        // n = 1 => 1 or 2 (50/50). n = 2 => 3, etc.
        return original + original / 2 + ((original % 2 != 0 && ThreadLocalRandom.current().nextBoolean()) ? 1 : 0);
    }

    @Inject(method = "findRandomSpawnPos", at = @At("RETURN"), cancellable = true)
    private void racecore$clampWaveSpawnElevation(ServerLevel level, int maxTries,
                                                  CallbackInfoReturnable<BlockPos> callback) {
        BlockPos original = callback.getReturnValue();
        if (original == null) return; // Preserve vanilla's failure/retry behavior.

        BlockPos center = getCenter();
        BlockPos sameColumn = validSurface(level, center, original.getX(), original.getZ());
        if (sameColumn != null) {
            callback.setReturnValue(sameColumn);
            return;
        }

        // Find another spot along the normal raid outer ring (not in the village).
        // Never force a spawn at an extreme altitude just to complete a wave.
        for (int attempt = 0; attempt < Math.min(64, Math.max(32, maxTries * 8)); attempt++) {
            double angle = ThreadLocalRandom.current().nextDouble() * Math.PI * 2.0;
            double range = 36.0 + ThreadLocalRandom.current().nextDouble() * 40.0;
            int x = center.getX() + (int) Math.round(Math.cos(angle) * range);
            int z = center.getZ() + (int) Math.round(Math.sin(angle) * range);
            BlockPos candidate = validSurface(level, center, x, z);
            if (candidate != null) {
                callback.setReturnValue(candidate);
                return;
            }
        }
        // Fail safely: vanilla can retry spawn-position search on later ticks.
        callback.setReturnValue(null);
    }

    private static BlockPos validSurface(ServerLevel level, BlockPos center, int x, int z) {
        BlockPos atCenterY = new BlockPos(x, center.getY(), z);
        if (!level.hasChunkAt(atCenterY)) return null;
        int y = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z);
        if (Math.abs(y - center.getY()) > 12) return null;
        BlockPos pos = new BlockPos(x, y, z);
        if (!level.isInWorldBounds(pos) || !level.isInWorldBounds(pos.above())
                || !level.hasChunkAt(pos) || !level.getWorldBorder().isWithinBounds(pos)
                || !level.isEmptyBlock(pos) || !level.isEmptyBlock(pos.above())
                || !level.getFluidState(pos).isEmpty()
                || !level.getFluidState(pos.below()).isEmpty()) return null;
        if (!level.getBlockState(pos.below()).isFaceSturdy(level, pos.below(), Direction.UP)) {
            return null;
        }
        return pos;
    }
}
