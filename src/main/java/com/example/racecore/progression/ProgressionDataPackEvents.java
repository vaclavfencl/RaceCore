package com.example.racecore.progression;

import com.example.racecore.RaceCore;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.repository.Pack;
import net.minecraft.server.packs.repository.PackSource;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.AddPackFindersEvent;

/**
 * Registers RaceCore's Phase 8 vanilla-data overrides as an always-enabled
 * high-priority built-in server datapack.
 *
 * This is intentionally separate from the normal mod data pack because
 * recipe collisions with vanilla are resolved by pack priority.
 */
@EventBusSubscriber(modid = RaceCore.MODID)
public final class ProgressionDataPackEvents {
    private ProgressionDataPackEvents() {
    }

    @SubscribeEvent
    public static void addPackFinders(AddPackFindersEvent event) {
        event.addPackFinders(
                Identifier.fromNamespaceAndPath(RaceCore.MODID, "progression_overrides"),
                PackType.SERVER_DATA,
                Component.literal("RaceCore Progression Overrides"),
                PackSource.BUILT_IN,
                true,
                Pack.Position.TOP
        );
    }
}
