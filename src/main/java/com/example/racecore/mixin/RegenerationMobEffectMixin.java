package com.example.racecore.mixin;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Regeneration keeps vanilla tick frequency by tier, but each successful tick
 * heals 1% of maximum health instead of a flat 1 HP.
 */
@Mixin(targets = "net.minecraft.world.effect.RegenerationMobEffect")
public abstract class RegenerationMobEffectMixin {
    @Inject(
            method = "applyEffectTick",
            at = @At("HEAD"),
            cancellable = true
    )
    private void racecore$percentageRegeneration(
            ServerLevel level,
            LivingEntity mob,
            int amplification,
            CallbackInfoReturnable<Boolean> cir
    ) {
        if (mob.getHealth() < mob.getMaxHealth()) {
            mob.heal(
                    mob.getMaxHealth() * 0.01F
            );
        }

        cir.setReturnValue(true);
    }
}
