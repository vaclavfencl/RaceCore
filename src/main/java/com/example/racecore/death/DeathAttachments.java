package com.example.racecore.death;

import com.example.racecore.RaceCore;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

import java.util.function.Supplier;

public final class DeathAttachments {
    public static final DeferredRegister<AttachmentType<?>>
            ATTACHMENT_TYPES =
            DeferredRegister.create(
                    NeoForgeRegistries.ATTACHMENT_TYPES,
                    RaceCore.MODID
            );

    public static final Supplier<AttachmentType<DeathInventorySnapshot>>
            DEATH_INVENTORY =
            ATTACHMENT_TYPES.register(
                    "death_inventory",
                    () -> AttachmentType
                            .builder(
                                    DeathInventorySnapshot::empty
                            )
                            .serialize(
                                    DeathInventorySnapshot.CODEC
                                            .fieldOf("snapshot"),
                                    snapshot ->
                                            !snapshot.isEmpty()
                            )
                            .copyOnDeath()
                            .build()
            );

    private DeathAttachments() {
    }
}
