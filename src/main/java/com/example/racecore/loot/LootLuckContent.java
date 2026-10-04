package com.example.racecore.loot;

import com.example.racecore.RaceCore;
import com.mojang.serialization.MapCodec;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.loot.IGlobalLootModifier;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

public final class LootLuckContent {
    private static final DeferredRegister<
            MapCodec<? extends IGlobalLootModifier>
            > GLOBAL_LOOT_MODIFIERS =
            DeferredRegister.create(
                    NeoForgeRegistries.Keys
                            .GLOBAL_LOOT_MODIFIER_SERIALIZERS,
                    RaceCore.MODID
            );

    public static final DeferredHolder<
            MapCodec<? extends IGlobalLootModifier>,
            MapCodec<LuckQualityLootModifier>
            > LUCK_QUALITY =
            GLOBAL_LOOT_MODIFIERS.register(
                    "luck_quality",
                    LuckQualityLootModifier.CODEC
            );

    private LootLuckContent() {
    }

    public static void register(
            IEventBus modBus
    ) {
        GLOBAL_LOOT_MODIFIERS.register(
                modBus
        );

        /*
         * Fail early if the Phase 7 tuning file is malformed.
         */
        LootLuckRegistry.load();
    }
}
