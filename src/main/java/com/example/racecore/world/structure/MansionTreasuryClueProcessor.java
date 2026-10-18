package com.example.racecore.world.structure;

import com.mojang.serialization.MapCodec;
import java.util.Random;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureProcessor;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;

/**
 * One 9x9x9 treasury template, four 3x5 spruce placeholder panels.
 * For each wall, keep exactly 3..6 spruce planks; turn every other placeholder
 * into dark oak. The template origin and wall number seed a deterministic
 * shuffle, so patterns vary between treasuries but remain consistent when
 * Minecraft places one room in multiple chunk-generation passes.
 */
public final class MansionTreasuryClueProcessor implements StructureProcessor {
    public static final MansionTreasuryClueProcessor INSTANCE = new MansionTreasuryClueProcessor();
    private static final MapCodec<MansionTreasuryClueProcessor> CODEC = MapCodec.unit(INSTANCE);

    private MansionTreasuryClueProcessor() {}

    @Override
    public MapCodec<MansionTreasuryClueProcessor> codec() {
        return CODEC;
    }

    @Override
    public StructureTemplate.StructureBlockInfo process(
            LevelReader level,
            BlockPos templateOrigin,
            BlockPos referencePos,
            StructureTemplate.StructureBlockInfo original,
            StructureTemplate.StructureBlockInfo placed,
            StructurePlaceSettings settings,
            StructureTemplate template) {
        if (!original.state().is(Blocks.SPRUCE_PLANKS)) return placed;

        BlockPos p = original.pos(); // unrotated position from the saved NBT
        int x = p.getX(), y = p.getY(), z = p.getZ();
        if (y < 1 || y > 5) return placed;

        int side, lateral;
        if (x == 0 && z >= 3 && z <= 5) {
            side = 0; lateral = z - 3;
        } else if (x == 8 && z >= 3 && z <= 5) {
            side = 1; lateral = z - 3;
        } else if (z == 0 && x >= 3 && x <= 5) {
            side = 2; lateral = x - 3;
        } else if (z == 8 && x >= 3 && x <= 5) {
            side = 3; lateral = x - 3;
        } else {
            return placed;
        }

        int slot = (y - 1) * 3 + lateral; // 0..14
        long seed = 0x6A09E667F3BCC909L;
        seed ^= Long.rotateLeft(templateOrigin.asLong(), 23);
        seed ^= (long) (side + 1) * 0x9E3779B97F4A7C15L;
        seed ^= (long) settings.getRotation().ordinal() * 0xBF58476D1CE4E5B9L;
        seed ^= (long) settings.getMirror().ordinal() * 0x94D049BB133111EBL;

        Random rng = new Random(seed);
        int spruceCount = 3 + rng.nextInt(4); // inclusive 3..6
        int[] order = new int[15];
        for (int i = 0; i < 15; i++) order[i] = i;
        for (int i = 14; i > 0; i--) {
            int j = rng.nextInt(i + 1);
            int temp = order[i]; order[i] = order[j]; order[j] = temp;
        }
        for (int i = 0; i < spruceCount; i++) {
            if (order[i] == slot) return placed;
        }

        return new StructureTemplate.StructureBlockInfo(
                placed.pos(), Blocks.DARK_OAK_PLANKS.defaultBlockState(), placed.nbt());
    }
}
