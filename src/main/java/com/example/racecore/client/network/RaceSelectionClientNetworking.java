package com.example.racecore.client.network;

import com.example.racecore.RaceCore;
import com.example.racecore.client.screen.RaceSelectionScreen;
import com.example.racecore.network.raceselection.OpenRaceSelectionPayload;
import net.minecraft.client.Minecraft;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.network.event.RegisterClientPayloadHandlersEvent;

/**
 * Physical-client-only registration for the open-screen payload.
 */
@EventBusSubscriber(
        modid = RaceCore.MODID,
        value = Dist.CLIENT
)
public final class RaceSelectionClientNetworking {
    private RaceSelectionClientNetworking() {
    }

    @SubscribeEvent
    public static void registerClientPayloads(
            RegisterClientPayloadHandlersEvent event
    ) {
        event.register(
                OpenRaceSelectionPayload.TYPE,
                (payload, context) -> {
                    Minecraft minecraft =
                            Minecraft.getInstance();

                    minecraft.gui.setScreen(
                            new RaceSelectionScreen()
                    );
                }
        );
    }
}
