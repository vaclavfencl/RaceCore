package com.example.racecore.death;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;

public record LastDeathLocation(
        String dimension,
        int deathX,
        int deathY,
        int deathZ,
        boolean hasBarrel,
        int barrelX,
        int barrelY,
        int barrelZ
) {
    public static final Codec<LastDeathLocation> CODEC = RecordCodecBuilder.create(
            instance -> instance.group(
                    Codec.STRING.fieldOf("dimension").forGetter(LastDeathLocation::dimension),
                    Codec.INT.fieldOf("deathX").forGetter(LastDeathLocation::deathX),
                    Codec.INT.fieldOf("deathY").forGetter(LastDeathLocation::deathY),
                    Codec.INT.fieldOf("deathZ").forGetter(LastDeathLocation::deathZ),
                    Codec.BOOL.fieldOf("hasBarrel").forGetter(LastDeathLocation::hasBarrel),
                    Codec.INT.fieldOf("barrelX").forGetter(LastDeathLocation::barrelX),
                    Codec.INT.fieldOf("barrelY").forGetter(LastDeathLocation::barrelY),
                    Codec.INT.fieldOf("barrelZ").forGetter(LastDeathLocation::barrelZ)
            ).apply(instance, LastDeathLocation::new)
    );

    public static LastDeathLocation empty() {
        return new LastDeathLocation("", 0, 0, 0, false, 0, 0, 0);
    }

    public static LastDeathLocation create(ServerLevel level, BlockPos deathPos, BlockPos barrelPos) {
        return new LastDeathLocation(
                level.dimension().identifier().toString(),
                deathPos.getX(), deathPos.getY(), deathPos.getZ(),
                barrelPos != null,
                barrelPos == null ? 0 : barrelPos.getX(),
                barrelPos == null ? 0 : barrelPos.getY(),
                barrelPos == null ? 0 : barrelPos.getZ()
        );
    }

    public boolean exists() {
        return !dimension.isBlank();
    }

    public String prettyDimension() {
        return switch (dimension) {
            case "minecraft:overworld" -> "Overworld";
            case "minecraft:the_nether" -> "Nether";
            case "minecraft:the_end" -> "The End";
            default -> dimension;
        };
    }
}
