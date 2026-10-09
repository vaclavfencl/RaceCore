package com.example.racecore.mixin;

import net.minecraft.world.item.BundleItem;
import net.minecraft.world.item.component.BundleContents;
import org.apache.commons.lang3.math.Fraction;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(BundleItem.class)
public abstract class BundleItemMixin {
    private static final Fraction RACECORE_BUNDLE_CAPACITY =
            Fraction.getFraction(4, 1);

    @Inject(
            method = "getWeightSafe",
            at = @At("RETURN"),
            cancellable = true
    )
    private static void racecore$normalizeBundleBarWeight(
            BundleContents contents,
            CallbackInfoReturnable<Fraction> cir
    ) {
        cir.setReturnValue(
                cir.getReturnValue().divideBy(RACECORE_BUNDLE_CAPACITY)
        );
    }
}
