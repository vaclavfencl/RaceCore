package com.example.racecore.network.enchanting;

import com.example.racecore.RaceCore;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

/**
 * Client request containing only enchantment target levels.
 *
 * The server resolves every enchantment id and re-validates the complete
 * operation. The client never decides whether an enchant is legal.
 */
public record ApplyEnchantPlanPayload(
        String plan
) implements CustomPacketPayload {

    public static final Type<ApplyEnchantPlanPayload> TYPE =
            new Type<>(
                    Identifier.fromNamespaceAndPath(
                            RaceCore.MODID,
                            "apply_enchant_plan"
                    )
            );

    public static final StreamCodec<
            RegistryFriendlyByteBuf,
            ApplyEnchantPlanPayload> STREAM_CODEC =
            StreamCodec.composite(
                    ByteBufCodecs.STRING_UTF8,
                    ApplyEnchantPlanPayload::plan,
                    ApplyEnchantPlanPayload::new
            );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
