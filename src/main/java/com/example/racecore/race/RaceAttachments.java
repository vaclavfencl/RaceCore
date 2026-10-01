package com.example.racecore.race;

import com.example.racecore.RaceCore;
import com.mojang.serialization.Codec;
import net.minecraft.network.codec.ByteBufCodecs;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

import java.util.function.Supplier;

public final class RaceAttachments {
    public static final DeferredRegister<AttachmentType<?>> ATTACHMENT_TYPES =
            DeferredRegister.create(
                    NeoForgeRegistries.ATTACHMENT_TYPES,
                    RaceCore.MODID
            );

    public static final Supplier<AttachmentType<String>> RACE =
            ATTACHMENT_TYPES.register(
                    "race",
                    () -> AttachmentType.builder(() -> "human")
                            .serialize(Codec.STRING.fieldOf("race"))
                            .sync(ByteBufCodecs.STRING_UTF8)
                            .copyOnDeath()
                            .build()
            );

    private RaceAttachments() {
    }
}
