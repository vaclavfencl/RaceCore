package com.example.racecore.mixin;

import net.minecraft.util.RandomSource;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Adds a single RaceCore vault template to vanilla mansion small-room draws (not guaranteed). */
@Mixin(targets = "net.minecraft.world.level.levelgen.structure.structures.WoodlandMansionPieces$SecondFloorRoomCollection")
public abstract class MansionTreasuryUpperRoomMixin {
    private static final int TREASURY_CHANCE_PERCENT = 25;
    
    @Inject(method = "get1x1", at = @At("RETURN"), cancellable = true)
    private void racecore$selectTreasury(RandomSource random, CallbackInfoReturnable<String> cir) {
        if (random.nextInt(100) < TREASURY_CHANCE_PERCENT) {
            cir.setReturnValue("1x1_a91");
        }
    }
}
