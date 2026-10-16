package com.example.racecore.mixin;

import com.example.racecore.raid.RaidGameplayEvents;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.raid.Raid;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Raid lifecycle callbacks for RaceCore. Runs on the logical server. */
@Mixin(Raid.class)
public abstract class RaidTickMixin {
    @Inject(method = "tick(Lnet/minecraft/server/level/ServerLevel;)V", at = @At("HEAD"))
    private void racecore$raidStart(ServerLevel level, CallbackInfo callback) {
        RaidGameplayEvents.beforeRaidTick(level, (Raid) (Object) this);
    }

    @Inject(method = "tick(Lnet/minecraft/server/level/ServerLevel;)V", at = @At("TAIL"))
    private void racecore$raidEnd(ServerLevel level, CallbackInfo callback) {
        RaidGameplayEvents.afterRaidTick(level, (Raid) (Object) this);
    }
}
