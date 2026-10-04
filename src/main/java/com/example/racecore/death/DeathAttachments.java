package com.example.racecore.death;

import com.example.racecore.RaceCore;
import net.minecraft.core.UUIDUtil;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

import java.util.UUID;
import java.util.function.Supplier;

public final class DeathAttachments {
    private static final UUID NO_OWNER = new UUID(0L, 0L);

    public static final DeferredRegister<AttachmentType<?>> ATTACHMENT_TYPES =
            DeferredRegister.create(NeoForgeRegistries.ATTACHMENT_TYPES, RaceCore.MODID);

    public static final Supplier<AttachmentType<DeathInventorySnapshot>> DEATH_INVENTORY =
            ATTACHMENT_TYPES.register(
                    "death_inventory",
                    () -> AttachmentType.builder(DeathInventorySnapshot::empty)
                            .serialize(
                                    DeathInventorySnapshot.CODEC.fieldOf("snapshot"),
                                    snapshot -> !snapshot.isEmpty()
                            )
                            .copyOnDeath()
                            .build()
            );

    public static final Supplier<AttachmentType<LastDeathLocation>> LAST_DEATH =
            ATTACHMENT_TYPES.register(
                    "last_death",
                    () -> AttachmentType.builder(LastDeathLocation::empty)
                            .serialize(
                                    LastDeathLocation.CODEC.fieldOf("location"),
                                    LastDeathLocation::exists
                            )
                            .copyOnDeath()
                            .build()
            );

    public static final Supplier<AttachmentType<UUID>> DEATH_BARREL_OWNER =
            ATTACHMENT_TYPES.register(
                    "death_barrel_owner",
                    () -> AttachmentType.builder(() -> NO_OWNER)
                            .serialize(
                                    UUIDUtil.CODEC.fieldOf("owner"),
                                    owner -> !NO_OWNER.equals(owner)
                            )
                            .build()
            );

    private DeathAttachments() {}
}
