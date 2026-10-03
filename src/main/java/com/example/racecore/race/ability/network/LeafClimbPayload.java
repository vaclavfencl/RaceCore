package com.example.racecore.race.ability.network;

import com.example.racecore.RaceCore;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

public record LeafClimbPayload(byte action)
        implements CustomPacketPayload {

    public static final Type<LeafClimbPayload> TYPE =
            new Type<>(
                    Identifier.fromNamespaceAndPath(
                            RaceCore.MODID,
                            "leaf_climb"
                    )
            );

    public static final StreamCodec<
            RegistryFriendlyByteBuf,
            LeafClimbPayload> STREAM_CODEC =
            StreamCodec.composite(
                    ByteBufCodecs.BYTE,
                    LeafClimbPayload::action,
                    LeafClimbPayload::new
            );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
