package com.example.racecore.mixin;

import com.example.racecore.world.structure.MansionTreasuryClueProcessor;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.levelgen.structure.TemplateStructurePiece;
import net.minecraft.world.level.levelgen.structure.structures.WoodlandMansionPieces;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Attach the processor before the actual structure blocks are placed. */
@Mixin(TemplateStructurePiece.class)
public abstract class MansionTreasuryProcessorMixin {
    @Shadow @Final protected String templateName;
    @Shadow @Final protected StructurePlaceSettings placeSettings;

    @Inject(method = "postProcess", at = @At("HEAD"))
    private void racecore$randomizeTreasuryPanels(
            WorldGenLevel level, StructureManager manager, ChunkGenerator generator,
            RandomSource random, BoundingBox chunkBB, ChunkPos chunkPos,
            BlockPos referencePos, CallbackInfo ci) {
        if (!((Object) this instanceof WoodlandMansionPieces.WoodlandMansionPiece)) return;
        if (!"1x1_a91".equals(templateName)) return;
        // Piece is sometimes processed once for each intersecting chunk.
        // Avoid adding duplicate processors to a persistent placement setting.
        if (placeSettings.getProcessors().stream().noneMatch(
                processor -> processor instanceof MansionTreasuryClueProcessor)) {
            placeSettings.addProcessor(MansionTreasuryClueProcessor.INSTANCE);
        }
    }
}
