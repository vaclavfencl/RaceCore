package com.example.racecore.network.raceselection;

import com.example.racecore.RaceCore;
import com.example.racecore.race.RaceManager;
import com.example.racecore.race.RaceRegistry;
import com.example.racecore.race.selection.RaceSelectionState;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/**
 * Common/server networking for race selection.
 *
 * The client only requests a race. The server validates and applies it.
 */
@EventBusSubscriber(modid = RaceCore.MODID)

public final class RaceSelectionNetworking {
    private RaceSelectionNetworking() {
    }

    @SubscribeEvent
    public static void registerPayloads(
            RegisterPayloadHandlersEvent event
    ) {
        var registrar =
                event.registrar("race-selection-1");

        registrar.playToClient(
                OpenRaceSelectionPayload.TYPE,
                OpenRaceSelectionPayload.STREAM_CODEC
        );

        registrar.playToServer(
                SelectRacePayload.TYPE,
                SelectRacePayload.STREAM_CODEC,
                RaceSelectionNetworking::handleSelection
        );
    }

    private static void handleSelection(
            SelectRacePayload payload,
            IPayloadContext context
    ) {
        if (!(context.player() instanceof ServerPlayer player)) {
            return;
        }

        /*
         * Normal first-time selection is one-time only.
         * A future respec item can deliberately reset/authorize this state
         * before reopening the same UI.
         */
        if (RaceSelectionState.hasSelectedRace(player)) {
            return;
        }

        String raceId =
                RaceRegistry.normalizeId(payload.raceId());

        if (!RaceRegistry.contains(raceId)) {
            return;
        }

        if (!RaceManager.setRace(player, raceId)) {
            return;
        }

        RaceSelectionState.markSelected(player);

        RaceCore.LOGGER.info(
                "{} confirmed RaceCore race '{}'.",
                player.getGameProfile().name(),
                raceId
        );
    }
}
