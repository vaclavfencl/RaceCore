package com.example.racecore.network.raceselection;

import com.example.racecore.RaceCore;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

public record OpenRaceSelectionPayload()
        implements CustomPacketPayload {

    public static final OpenRaceSelectionPayload INSTANCE =
            new OpenRaceSelectionPayload();

    public static final Type<OpenRaceSelectionPayload> TYPE =
            new Type<>(
                    Identifier.fromNamespaceAndPath(
                            RaceCore.MODID,
                            "open_race_selection"
                    )
            );

    public static final StreamCodec<
            RegistryFriendlyByteBuf,
            OpenRaceSelectionPayload> STREAM_CODEC =
            StreamCodec.unit(INSTANCE);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
