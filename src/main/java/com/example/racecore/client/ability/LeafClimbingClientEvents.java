package com.example.racecore.client.ability;

import com.example.racecore.RaceCore;
import com.example.racecore.race.RaceDefinition;
import com.example.racecore.race.RaceManager;
import com.example.racecore.race.ability.LeafClimbingHelper;
import com.example.racecore.race.ability.network.LeafClimbPayload;
import net.minecraft.client.player.LocalPlayer;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.MovementInputUpdateEvent;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;

@EventBusSubscriber(
        modid = RaceCore.MODID,
        value = Dist.CLIENT
)
public final class LeafClimbingClientEvents {
    private LeafClimbingClientEvents() {
    }

    @SubscribeEvent
    public static void onMovementInput(
            MovementInputUpdateEvent event
    ) {
        if (!(event.getEntity()
                instanceof LocalPlayer player)) {
            return;
        }

        RaceDefinition race =
                RaceManager.getRace(player);

        if (!race.leafClimbing()) {
            return;
        }

        if (!LeafClimbingHelper
                .isTouchingLeaves(player)) {
            return;
        }

        var keys =
                event.getInput().keyPresses;

        byte action;

        if (keys.jump()) {
            action =
                    LeafClimbingHelper.ACTION_CLIMB;
        } else if (keys.shift()) {
            action =
                    LeafClimbingHelper.ACTION_HOLD;
        } else {
            // No special input: ordinary Minecraft gravity applies.
            return;
        }

        // Immediate local response.
        LeafClimbingHelper.apply(
                player,
                race,
                action
        );

        // Server-authoritative mirror for dedicated-server play.
        ClientPacketDistributor.sendToServer(
                new LeafClimbPayload(action)
        );
    }
}
