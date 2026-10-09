package com.example.racecore.mixin.client;

import com.mojang.serialization.DataResult;
import net.minecraft.client.gui.screens.inventory.tooltip.ClientBundleTooltip;
import net.minecraft.world.item.component.BundleContents;
import org.apache.commons.lang3.math.Fraction;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(ClientBundleTooltip.class)
public abstract class ClientBundleTooltipMixin {
    private static final Fraction RACECORE_BUNDLE_CAPACITY =
            Fraction.getFraction(4, 1);

    @Redirect(
            method = "extractImage",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/item/component/BundleContents;weight()Lcom/mojang/serialization/DataResult;"
            )
    )
    private DataResult<Fraction> racecore$normalizeTooltipWeight(
            BundleContents contents
    ) {
        return contents.weight()
                .map(weight -> weight.divideBy(RACECORE_BUNDLE_CAPACITY));
    }
}
