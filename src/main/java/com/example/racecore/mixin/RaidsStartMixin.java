package com.example.racecore.mixin;

import com.example.racecore.raid.RaidStartRequirements;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.raid.Raid;
import net.minecraft.world.entity.raid.Raids;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Deny starting a new vanilla raid unless it is a real RaceCore village. */
@Mixin(Raids.class)
public abstract class RaidsStartMixin {
    @Inject(method = "createOrExtendRaid", at = @At("HEAD"), cancellable = true)
    private void racecore$requireRealVillage(ServerPlayer player, BlockPos raidPosition,
                                             CallbackInfoReturnable<Raid> callback) {
        ServerLevel level = player.level();
        Raids raids = (Raids) (Object) this;
        // Extending an already-running raid must NOT recheck villagers/beds:
        // they may have died or been destroyed after the battle began.
        Raid nearby = raids.getNearbyRaid(raidPosition, 96 * 96);
        if (nearby != null && !nearby.isOver() && !nearby.isStopped()) return;

        if (!RaidStartRequirements.allowNewRaid(level, raidPosition)) {
            callback.setReturnValue(null);
        }
    }
}
