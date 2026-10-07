package com.example.racecore.death.semihardcore;

import com.example.racecore.RaceCore;
import net.minecraft.network.protocol.game.ServerboundClientCommandPacket;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.GameType;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.entity.player.PlayerNegotiationEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.neoforged.neoforge.server.ServerLifecycleHooks;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * Phase 10 semi-hardcore multiplayer death.
 *
 * Active only for Hardcore worlds.
 *
 * Important implementation detail:
 * We MUST NOT call PlayerList.respawn(...) directly while the player's active
 * ServerGamePacketListenerImpl still points at the old dead ServerPlayer.
 * Doing that creates a replacement ServerPlayer in PlayerList while the
 * connection listener still owns the old one. Disconnect cleanup then removes
 * the wrong instance and leaves a "ghost" player behind. The next login waits
 * for that duplicate to disappear and eventually times out after 600 ticks.
 *
 * Instead, we make the player's own connection process the normal
 * PERFORM_RESPAWN command. Vanilla then updates the connection listener's
 * player reference correctly. We immediately replace Hardcore spectator mode
 * with Survival and, for a real penalized death, disconnect the new player.
 */
@EventBusSubscriber(modid = RaceCore.MODID)
public final class SemiHardcoreEvents {
    private static final Map<UUID, Boolean> PENDING_RESPAWNS =
            new HashMap<>();

    private SemiHardcoreEvents() {
    }

    @SubscribeEvent
    public static void onRegisterCommands(
            RegisterCommandsEvent event
    ) {
        SemiHardcoreCommands.register(
                event.getDispatcher()
        );
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onPlayerDeath(
            LivingDeathEvent event
    ) {
        if (!(event.getEntity()
                instanceof ServerPlayer player)) {

            return;
        }

        MinecraftServer server =
                player.level().getServer();

        if (!SemiHardcoreSystem.isEnabled(
                server
        )) {
            return;
        }

        UUID playerId =
                player.getUUID();

        if (PENDING_RESPAWNS.containsKey(
                playerId
        )) {
            return;
        }

        boolean adminKill =
                event.getSource()
                        .is(
                                DamageTypes.GENERIC_KILL
                        );

        boolean explicitBypass =
                SemiHardcoreSystem.consumeBypass(
                        playerId
                );

        boolean penalize =
                !adminKill
                        && !explicitBypass;

        if (penalize) {
            long now =
                    System.currentTimeMillis();

            SemiHardcoreSavedData data =
                    SemiHardcoreSavedData.get(
                            server
                    );

            SemiHardcoreState current =
                    data.getNormalized(
                            playerId,
                            now
                    );

            SemiHardcoreState updated =
                    SemiHardcoreSystem.recordRealDeath(
                            current,
                            now
                    );

            data.put(
                    playerId,
                    updated
            );

            RaceCore.LOGGER.info(
                    "Semi-hardcore death for {}: death #{}; lockout {}.",
                    player.getGameProfile()
                            .name(),
                    updated.deathsToday(),
                    SemiHardcoreSystem.formatDuration(
                            SemiHardcoreSystem
                                    .getPenaltyMillis(
                                            updated.deathsToday()
                                    )
                    )
            );
        } else {
            RaceCore.LOGGER.info(
                    "Semi-hardcore penalty bypassed for {} (admin/test/minigame death).",
                    player.getGameProfile()
                            .name()
            );
        }

        /*
         * Defer one tick so the rest of RaceCore's LivingDeathEvent handlers
         * can finish capturing the death inventory/barrel state first.
         */
        PENDING_RESPAWNS.put(
                playerId,
                penalize
        );
    }

    @SubscribeEvent
    public static void onServerTickPost(
            ServerTickEvent.Post event
    ) {
        MinecraftServer server =
                event.getServer();

        if (!SemiHardcoreSystem.isEnabled(
                server
        )
                || PENDING_RESPAWNS.isEmpty()) {

            return;
        }

        Map<UUID, Boolean> pending =
                Map.copyOf(
                        PENDING_RESPAWNS
                );

        PENDING_RESPAWNS.clear();

        for (Map.Entry<UUID, Boolean> entry
                : pending.entrySet()) {

            UUID playerId =
                    entry.getKey();

            boolean penalize =
                    entry.getValue();

            ServerPlayer deadPlayer =
                    server.getPlayerList()
                            .getPlayer(
                                    playerId
                            );

            if (deadPlayer == null
                    || !deadPlayer.isDeadOrDying()) {

                continue;
            }

            /*
             * IMPORTANT:
             * Route the respawn through ServerGamePacketListenerImpl itself.
             * This updates connection.player to the replacement ServerPlayer,
             * which PlayerList.respawn(...) alone does NOT do.
             */
            deadPlayer.connection.handleClientCommand(
                    new ServerboundClientCommandPacket(
                            ServerboundClientCommandPacket.Action.PERFORM_RESPAWN
                    )
            );

            ServerPlayer respawned =
                    server.getPlayerList()
                            .getPlayer(
                                    playerId
                            );

            if (respawned == null) {
                RaceCore.LOGGER.error(
                        "Semi-hardcore respawn failed to produce a replacement player for {}.",
                        playerId
                );
                continue;
            }

            /*
             * Vanilla Hardcore changes the newly respawned player to spectator.
             * RaceCore replaces that permanent consequence with the temporary
             * lockout, so restore normal Survival immediately.
             */
            respawned.setGameMode(
                    GameType.SURVIVAL
            );

            if (!penalize) {
                continue;
            }

            long now =
                    System.currentTimeMillis();

            SemiHardcoreState state =
                    SemiHardcoreSavedData
                            .get(server)
                            .getNormalized(
                                    playerId,
                                    now
                            );

            if (!state.isLocked(now)) {
                continue;
            }

            respawned.connection.disconnect(
                    SemiHardcoreSystem
                            .lockoutMessage(
                                    state,
                                    now
                            )
            );
        }
    }

    /**
     * Reject an active lockout before the player is placed into the world.
     *
     * SavedData access is performed on the server thread. The negotiation
     * handshake waits for this future, so a locked player receives the denial
     * during connection setup instead of entering the world and then being
     * kicked from PlayerLoggedInEvent.
     */
    @SubscribeEvent
    public static void onPlayerNegotiation(
            PlayerNegotiationEvent event
    ) {
        MinecraftServer server =
                ServerLifecycleHooks
                        .getCurrentServer();

        if (!SemiHardcoreSystem.isEnabled(
                server
        )) {
            return;
        }

        CompletableFuture<Void> checkFinished =
                new CompletableFuture<>();

        event.enqueueWork(
                checkFinished
        );

        server.execute(() -> {
            try {
                long now =
                        System.currentTimeMillis();

                SemiHardcoreState state =
                        SemiHardcoreSavedData
                                .get(server)
                                .getNormalized(
                                        event.getProfile()
                                                .id(),
                                        now
                                );

                if (state.isLocked(now)) {
                    event.getConnection()
                            .disconnect(
                                    SemiHardcoreSystem
                                            .lockoutMessage(
                                                    state,
                                                    now
                                            )
                            );
                }
            } finally {
                checkFinished.complete(
                        null
                );
            }
        });
    }

    /**
     * Safety/fallback check. Normal lockout attempts should already have been
     * rejected by PlayerNegotiationEvent before this point.
     */
    @SubscribeEvent
    public static void onPlayerLogin(
            PlayerEvent.PlayerLoggedInEvent event
    ) {
        if (!(event.getEntity()
                instanceof ServerPlayer player)) {

            return;
        }

        MinecraftServer server =
                player.level().getServer();

        if (!SemiHardcoreSystem.isEnabled(
                server
        )) {
            return;
        }

        long now =
                System.currentTimeMillis();

        SemiHardcoreSavedData data =
                SemiHardcoreSavedData.get(
                        server
                );

        SemiHardcoreState state =
                data.getNormalized(
                        player.getUUID(),
                        now
                );

        if (state.isLocked(now)) {
            player.connection.disconnect(
                    SemiHardcoreSystem
                            .lockoutMessage(
                                    state,
                                    now
                            )
            );
            return;
        }

        /*
         * Migration/safety for a player left as a vanilla Hardcore spectator
         * by an older build or interrupted test.
         */
        if (state.unlockAtEpochMillis() > 0L
                && player.gameMode
                .getGameModeForPlayer()
                == GameType.SPECTATOR) {

            player.setGameMode(
                    GameType.SURVIVAL
            );
        }

        if (state.unlockAtEpochMillis() > 0L) {
            data.put(
                    player.getUUID(),
                    state.withUnlockAt(
                            0L
                    )
            );
        }
    }
}
