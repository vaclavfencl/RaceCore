package com.example.racecore.creative;

import com.example.racecore.RaceCore;
import com.example.racecore.material.RaceCoreMaterials;
import com.example.racecore.weapon.RaceCoreWeapons;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.RegisterEvent;

public final class RaceCoreCreativeTabs {
    private static final ResourceKey<CreativeModeTab> RACECORE_TAB =
            ResourceKey.create(
                    Registries.CREATIVE_MODE_TAB,
                    Identifier.fromNamespaceAndPath(
                            RaceCore.MODID,
                            "racecore"
                    )
            );

    private RaceCoreCreativeTabs() {
    }

    public static void register(
            IEventBus modBus
    ) {
        modBus.addListener(
                RaceCoreCreativeTabs::onRegister
        );
    }

    private static void onRegister(
            RegisterEvent event
    ) {
        event.register(
                Registries.CREATIVE_MODE_TAB,
                helper ->
                        helper.register(
                                RACECORE_TAB,
                                CreativeModeTab.builder()
                                        .title(
                                                Component.translatable(
                                                        "itemGroup.racecore"
                                                )
                                        )
                                        .icon(
                                                () ->
                                                        new ItemStack(
                                                                RaceCoreWeapons
                                                                        .LONGSWORD
                                                                        .get()
                                                        )
                                        )
                                        .displayItems(
                                                (parameters, output) -> {
                                                    RaceCoreMaterials.ITEMS
                                                            .getEntries()
                                                            .forEach(
                                                                    entry -> output.accept(
                                                                            entry.get()
                                                                    )
                                                            );

                                                    RaceCoreWeapons.ITEMS
                                                            .getEntries()
                                                            .forEach(
                                                                    entry -> output.accept(
                                                                            entry.get()
                                                                    )
                                                            );
                                                }
                                        )
                                        .build()
                        )
        );
    }
}
