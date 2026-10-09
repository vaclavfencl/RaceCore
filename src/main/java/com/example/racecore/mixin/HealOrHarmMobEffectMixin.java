package com.example.racecore.mixin;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Instant Health / Instant Damage scale from target maximum HP.
 *
 * Tier I:  15% max HP
 * Tier II: 25% max HP
 * Higher custom tiers: +10 percentage points per tier, capped at 100%.
 *
 * Splash-distance scaling is preserved through the vanilla 'scale' parameter.
 * Undead heal/harm inversion is preserved.
 */
@Mixin(targets = "net.minecraft.world.effect.HealOrHarmMobEffect")
public abstract class HealOrHarmMobEffectMixin {
    @Shadow
    @Final
    private boolean isHarm;

    @Inject(
            method = "applyEffectTick",
            at = @At("HEAD"),
            cancellable = true
    )
    private void racecore$percentageDirectHealOrHarm(
            ServerLevel level,
            LivingEntity mob,
            int amplification,
            CallbackInfoReturnable<Boolean> cir
    ) {
        racecore$apply(
                level,
                null,
                null,
                mob,
                amplification,
                1.0
        );

        cir.setReturnValue(true);
    }

    @Inject(
            method = "applyInstantaneousEffect",
            at = @At("HEAD"),
            cancellable = true
    )
    private void racecore$percentageInstantHealOrHarm(
            ServerLevel level,
            @Nullable Entity source,
            @Nullable Entity owner,
            LivingEntity mob,
            int amplification,
            double scale,
            CallbackInfo ci
    ) {
        racecore$apply(
                level,
                source,
                owner,
                mob,
                amplification,
                scale
        );

        ci.cancel();
    }

    private void racecore$apply(
            ServerLevel level,
            @Nullable Entity source,
            @Nullable Entity owner,
            LivingEntity mob,
            int amplification,
            double scale
    ) {
        double fraction =
                Math.min(
                        1.0,
                        0.15 + 0.10 * amplification
                );

        float amount =
                (float) (
                        mob.getMaxHealth()
                                * fraction
                                * Math.max(0.0, scale)
                );

        if (amount <= 0.0F) {
            return;
        }

        if (this.isHarm
                == mob.isInvertedHealAndHarm()) {
            mob.heal(amount);
            return;
        }

        if (source == null) {
            mob.hurtServer(
                    level,
                    mob.damageSources().magic(),
                    amount
            );
        } else {
            mob.hurtServer(
                    level,
                    mob.damageSources()
                            .indirectMagic(
                                    source,
                                    owner
                            ),
                    amount
            );
        }
    }
}
