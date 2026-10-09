package com.example.racecore.mixin;

import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Converts the potion effects which still use flat or mismatched attribute
 * values into RaceCore percentage scaling.
 *
 * Strength: +10% attack damage / tier
 * Weakness: -10% attack damage / tier, capped at -80%
 * Slowness: -20% movement speed / tier, capped at -80%
 *
 * Speed (+20%/tier) and Haste attack speed (+10%/tier) are already
 * percentage-based in vanilla and are intentionally left alone.
 */
@Mixin(targets = "net.minecraft.world.effect.MobEffect$AttributeTemplate")
public abstract class MobEffectAttributeTemplateMixin {
    private static final Identifier STRENGTH_ID =
            Identifier.withDefaultNamespace(
                    "effect.strength"
            );

    private static final Identifier WEAKNESS_ID =
            Identifier.withDefaultNamespace(
                    "effect.weakness"
            );

    private static final Identifier SLOWNESS_ID =
            Identifier.withDefaultNamespace(
                    "effect.slowness"
            );

    @Shadow
    @Final
    private Identifier id;

    @Inject(
            method = "create",
            at = @At("RETURN"),
            cancellable = true
    )
    private void racecore$percentagePotionAttributes(
            int amplifier,
            CallbackInfoReturnable<AttributeModifier> cir
    ) {
        int tier = amplifier + 1;

        if (this.id.equals(STRENGTH_ID)) {
            cir.setReturnValue(
                    new AttributeModifier(
                            this.id,
                            0.10 * tier,
                            AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL
                    )
            );
            return;
        }

        if (this.id.equals(WEAKNESS_ID)) {
            cir.setReturnValue(
                    new AttributeModifier(
                            this.id,
                            Math.max(
                                    -0.80,
                                    -0.10 * tier
                            ),
                            AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL
                    )
            );
            return;
        }

        if (this.id.equals(SLOWNESS_ID)) {
            cir.setReturnValue(
                    new AttributeModifier(
                            this.id,
                            Math.max(
                                    -0.80,
                                    -0.20 * tier
                            ),
                            AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL
                    )
            );
        }
    }
}
