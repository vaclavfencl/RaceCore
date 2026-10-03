package com.example.racecore.race.ability.network;

import com.example.racecore.RaceCore;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

public record GargoyleMovementPayload(byte action)
        implements CustomPacketPayload {

    public static final byte START_CHARGE = 0;
    public static final byte RELEASE_CHARGE = 1;
    public static final byte CANCEL_CHARGE = 2;
    public static final byte GLIDE = 3;

    public static final Type<GargoyleMovementPayload> TYPE =
            new Type<>(
                    Identifier.fromNamespaceAndPath(
                            RaceCore.MODID,
                            "gargoyle_movement"
                    )
            );

    public static final StreamCodec<
            RegistryFriendlyByteBuf,
            GargoyleMovementPayload> STREAM_CODEC =
            StreamCodec.composite(
                    ByteBufCodecs.BYTE,
                    GargoyleMovementPayload::action,
                    GargoyleMovementPayload::new
            );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
