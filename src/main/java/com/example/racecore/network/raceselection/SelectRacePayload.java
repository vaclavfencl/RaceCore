package com.example.racecore.network.raceselection;

import com.example.racecore.RaceCore;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

public record SelectRacePayload(String raceId)
        implements CustomPacketPayload {

    public static final Type<SelectRacePayload> TYPE =
            new Type<>(
                    Identifier.fromNamespaceAndPath(
                            RaceCore.MODID,
                            "select_race"
                    )
            );

    public static final StreamCodec<
            RegistryFriendlyByteBuf,
            SelectRacePayload> STREAM_CODEC =
            StreamCodec.composite(
                    ByteBufCodecs.STRING_UTF8,
                    SelectRacePayload::raceId,
                    SelectRacePayload::new
            );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
