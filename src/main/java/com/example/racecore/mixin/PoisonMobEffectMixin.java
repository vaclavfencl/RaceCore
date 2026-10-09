package com.example.racecore.mixin;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.PoisonMobEffect;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Disable vanilla Poison's own ticking damage.
 * PotionRebalanceEvents applies RaceCore's stack-based non-lethal damage.
 */
@Mixin(PoisonMobEffect.class)
public abstract class PoisonMobEffectMixin {
    @Inject(
            method = "applyEffectTick",
            at = @At("HEAD"),
            cancellable = true
    )
    private void racecore$disableVanillaPoisonDamage(
            ServerLevel level,
            LivingEntity mob,
            int amplification,
            CallbackInfoReturnable<Boolean> cir
    ) {
        cir.setReturnValue(true);
    }
}
