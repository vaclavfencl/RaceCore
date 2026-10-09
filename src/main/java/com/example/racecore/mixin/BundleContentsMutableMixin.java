package com.example.racecore.mixin;

import net.minecraft.world.item.component.BundleContents;
import org.apache.commons.lang3.math.Fraction;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(BundleContents.Mutable.class)
public abstract class BundleContentsMutableMixin {
    private static final Fraction RACECORE_BUNDLE_CAPACITY =
            Fraction.getFraction(4, 1);

    @Redirect(
            method = "getMaxAmountToAdd",
            at = @At(
                    value = "INVOKE",
                    target = "Lorg/apache/commons/lang3/math/Fraction;subtract(Lorg/apache/commons/lang3/math/Fraction;)Lorg/apache/commons/lang3/math/Fraction;"
            )
    )
    private Fraction racecore$fourStackBundleCapacity(
            Fraction vanillaCapacity,
            Fraction currentWeight
    ) {
        return RACECORE_BUNDLE_CAPACITY.subtract(currentWeight);
    }
}
