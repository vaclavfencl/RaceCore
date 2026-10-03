package com.example.racecore.race.ability.network;

import com.example.racecore.RaceCore;
import com.example.racecore.race.RaceDefinition;
import com.example.racecore.race.RaceManager;
import com.example.racecore.race.ability.LeafClimbingHelper;
import com.example.racecore.race.ability.GargoyleMovementHelper;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@EventBusSubscriber(modid = RaceCore.MODID)
public final class RaceAbilityNetworking {
    private static final Map<UUID, Long> GARGOYLE_CHARGE_START =
            new HashMap<>();

    private RaceAbilityNetworking() {
    }

    @SubscribeEvent
    public static void registerPayloads(
            RegisterPayloadHandlersEvent event
    ) {
        var registrar =
                event.registrar("race-selection-1");

        registrar.playToServer(
                LeafClimbPayload.TYPE,
                LeafClimbPayload.STREAM_CODEC,
                RaceAbilityNetworking::handleLeafClimb
        );

        registrar.playToServer(
                GargoyleMovementPayload.TYPE,
                GargoyleMovementPayload.STREAM_CODEC,
                RaceAbilityNetworking::handleGargoyleMovement
        );
    }

    private static void handleLeafClimb(
            LeafClimbPayload payload,
            IPayloadContext context
    ) {
        context.enqueueWork(() -> {
            if (!(context.player()
                    instanceof ServerPlayer player)) {
                return;
            }

            byte action =
                    payload.action();

            if (action != LeafClimbingHelper.ACTION_HOLD
                    && action != LeafClimbingHelper.ACTION_CLIMB) {
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

            LeafClimbingHelper.apply(
                    player,
                    race,
                    action
            );
        });
    }

    private static void handleGargoyleMovement(
            GargoyleMovementPayload payload,
            IPayloadContext context
    ) {
        context.enqueueWork(() -> {
            if (!(context.player()
                    instanceof ServerPlayer player)) {
                return;
            }

            RaceDefinition race =
                    RaceManager.getRace(player);

            byte action =
                    payload.action();

            if (action
                    == GargoyleMovementPayload.START_CHARGE) {

                if (!race.chargedJumpEnabled()
                        || !player.onGround()) {
                    return;
                }

                GARGOYLE_CHARGE_START.put(
                        player.getUUID(),
                        player.level().getGameTime()
                );

                return;
            }

            if (action
                    == GargoyleMovementPayload.CANCEL_CHARGE) {

                GARGOYLE_CHARGE_START.remove(
                        player.getUUID()
                );

                return;
            }

            if (action
                    == GargoyleMovementPayload.RELEASE_CHARGE) {

                if (!race.chargedJumpEnabled()) {

                    GARGOYLE_CHARGE_START.remove(
                            player.getUUID()
                    );

                    return;
                }

                Long started =
                        GARGOYLE_CHARGE_START.remove(
                                player.getUUID()
                        );

                if (started == null) {
                    return;
                }

                long elapsed =
                        player.level().getGameTime()
                                - started;

                int chargeTicks =
                        (int) Math.max(
                                0,
                                Math.min(
                                        race.chargedJumpMaxChargeTicks(),
                                        elapsed
                                )
                        );

                GargoyleMovementHelper.launch(
                        player,
                        race,
                        chargeTicks
                );

                return;
            }

            if (action
                    == GargoyleMovementPayload.GLIDE) {

                if (!race.glideEnabled()
                        || player.onGround()) {
                    return;
                }

                GargoyleMovementHelper.applyGlide(
                        player,
                        race
                );
            }
        });
    }

}
