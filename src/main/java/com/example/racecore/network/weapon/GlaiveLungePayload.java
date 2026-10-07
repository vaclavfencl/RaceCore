package com.example.racecore.network.weapon;

import com.example.racecore.RaceCore;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

public final class GlaiveLungePayload
        implements CustomPacketPayload {

    public static final GlaiveLungePayload INSTANCE =
            new GlaiveLungePayload();

    public static final Type<GlaiveLungePayload> TYPE =
            new Type<>(
                    Identifier.fromNamespaceAndPath(
                            RaceCore.MODID,
                            "glaive_lunge"
                    )
            );

    public static final StreamCodec<
            RegistryFriendlyByteBuf,
            GlaiveLungePayload> STREAM_CODEC =
            StreamCodec.unit(INSTANCE);

    private GlaiveLungePayload() {
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
