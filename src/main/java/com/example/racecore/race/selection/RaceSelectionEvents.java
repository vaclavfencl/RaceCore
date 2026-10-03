package com.example.racecore.race.selection;

import com.example.racecore.RaceCore;
import com.example.racecore.network.raceselection.OpenRaceSelectionPayload;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.network.PacketDistributor;

/**
 * Opens the selection UI on first login and preserves the confirmed state
 * when Minecraft clones the player entity.
 */
@EventBusSubscriber(modid = RaceCore.MODID)
public final class RaceSelectionEvents {
    private RaceSelectionEvents() {
    }

    @SubscribeEvent
    public static void onPlayerLogin(
            PlayerEvent.PlayerLoggedInEvent event
    ) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }

        if (RaceSelectionState.hasSelectedRace(player)) {
            return;
        }

        PacketDistributor.sendToPlayer(
                player,
                OpenRaceSelectionPayload.INSTANCE
        );
    }

    @SubscribeEvent
    public static void onPlayerClone(
            PlayerEvent.Clone event
    ) {
        RaceSelectionState.copy(
                event.getOriginal(),
                event.getEntity()
        );
    }
}
