package com.example.racecore.mixin;

import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Vanilla Mining Fatigue uses extremely steep non-linear multipliers.
 * RaceCore replaces the mining-speed part with -20% per tier, capped so the
 * player always retains at least 20% mining speed.
 *
 * The vanilla -10% attack-speed attribute modifier is already percentage-based
 * and remains unchanged.
 */
@Mixin(Player.class)
public abstract class PlayerPotionMiningMixin {
    @Inject(
            method = "getDestroySpeed",
            at = @At("RETURN"),
            cancellable = true
    )
    private void racecore$percentageMiningFatigue(
            BlockState state,
            CallbackInfoReturnable<Float> cir
    ) {
        Player self =
                (Player) (Object) this;

        MobEffectInstance fatigue =
                self.getEffect(
                        MobEffects.MINING_FATIGUE
                );

        if (fatigue == null) {
            return;
        }

        int amplifier =
                fatigue.getAmplifier();

        float vanillaScale =
                switch (amplifier) {
                    case 0 -> 0.3F;
                    case 1 -> 0.09F;
                    case 2 -> 0.0027F;
                    default -> 8.1E-4F;
                };

        float customScale =
                Math.max(
                        0.20F,
                        1.0F
                                - 0.20F
                                * (amplifier + 1)
                );

        float vanillaResult =
                cir.getReturnValue();

        cir.setReturnValue(
                vanillaResult
                        / vanillaScale
                        * customScale
        );
    }
}
