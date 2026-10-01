package com.example.racecore.race;

import com.example.racecore.RaceCore;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.entity.living.LivingEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;

@EventBusSubscriber(modid = RaceCore.MODID)
public final class RaceEvents {
    private RaceEvents() {
    }

    @SubscribeEvent
    public static void onRegisterCommands(RegisterCommandsEvent event) {
        RaceCommands.register(event.getDispatcher());
    }

    @SubscribeEvent
    public static void onPlayerLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            RaceManager.applyCurrentRace(player);
        }
    }

    @SubscribeEvent
    public static void onPlayerRespawn(PlayerEvent.PlayerRespawnEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            RaceManager.applyCurrentRace(player);
        }
    }

    @SubscribeEvent
    public static void onPlayerChangedDimension(
            PlayerEvent.PlayerChangedDimensionEvent event
    ) {
        if (event.getEntity() instanceof ServerPlayer player) {
            RaceManager.applyCurrentRace(player);
        }
    }

    @SubscribeEvent
    public static void onPlayerJump(LivingEvent.LivingJumpEvent event) {
        if (!(event.getEntity() instanceof Player player)) {
            return;
        }

        RaceDefinition race = RaceManager.getRace(player);
        Vec3 motion = player.getDeltaMovement();

        if (motion.y <= 0.0) {
            return;
        }

        double desiredBaseVelocity =
                RaceJumpPhysics.initialVelocityForHeight(race.jumpHeight());

        /*
         * Add only the difference from vanilla's normal player jump.
         *
         * This is important: if another system later adds Jump Boost or another
         * vertical bonus, we do not want RaceCore to wipe that bonus out.
         */
        double raceCorrection =
                desiredBaseVelocity - RaceJumpPhysics.VANILLA_PLAYER_JUMP_VELOCITY;

        player.setDeltaMovement(
                motion.x,
                Math.max(0.0, motion.y + raceCorrection),
                motion.z
        );
    }
}
