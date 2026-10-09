package com.example.racecore.mixin;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.WitherMobEffect;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Keep vanilla Wither duration and tick schedule, but multiply each status
 * effect damage tick from 1 to 5.
 */
@Mixin(WitherMobEffect.class)
public abstract class WitherMobEffectMixin {
    @Inject(
            method = "applyEffectTick",
            at = @At("HEAD"),
            cancellable = true
    )
    private void racecore$fiveTimesWitherDamage(
            ServerLevel level,
            LivingEntity mob,
            int amplification,
            CallbackInfoReturnable<Boolean> cir
    ) {
        mob.hurtServer(
                level,
                mob.damageSources().wither(),
                5.0F
        );

        cir.setReturnValue(true);
    }
}
