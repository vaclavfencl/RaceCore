package com.example.racecore.client.ability;

import com.example.racecore.RaceCore;
import com.example.racecore.race.RaceDefinition;
import com.example.racecore.race.RaceManager;
import com.example.racecore.race.ability.GargoyleMovementHelper;
import com.example.racecore.race.ability.network.GargoyleMovementPayload;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.player.Input;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.MovementInputUpdateEvent;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;

/**
 * Gargoyle controls.
 *
 * Ground:
 * - quick Space tap -> ordinary vanilla jump
 * - hold Space past chargedJumpStartDelayTicks -> begin charging
 * - release after charging -> charged jump
 *
 * Air:
 * - hold Space -> glide
 */
@EventBusSubscriber(
        modid = RaceCore.MODID,
        value = Dist.CLIENT
)
public final class GargoyleMovementClientEvents {
    private static boolean jumpHeld = false;
    private static boolean pendingNormalJump = false;
    private static boolean charging = false;

    private static int heldTicks = 0;
    private static int chargeTicks = 0;

    private GargoyleMovementClientEvents() {
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

        var clientInput =
                event.getInput();

        Input keys =
                clientInput.keyPresses;

        boolean jumpDown =
                keys.jump();

        if (!race.chargedJumpEnabled()
                && !race.glideEnabled()) {

            resetAndCancelServerCharge();
            return;
        }

        /*
         * Ground charged-jump handling.
         *
         * We temporarily consume vanilla's jump input while deciding whether
         * this is a quick tap or a charge. A quick release calls the normal
         * vanilla jump method; holding long enough transitions into charge.
         */
        if (race.chargedJumpEnabled()
                && player.onGround()) {

            if (jumpDown) {
                if (!jumpHeld) {
                    jumpHeld = true;
                    pendingNormalJump = true;
                    charging = false;
                    heldTicks = 0;
                    chargeTicks = 0;
                }

                heldTicks++;

                if (pendingNormalJump
                        && heldTicks
                        > race.chargedJumpStartDelayTicks()) {

                    pendingNormalJump = false;
                    charging = true;
                    chargeTicks = 0;

                    ClientPacketDistributor.sendToServer(
                            new GargoyleMovementPayload(
                                    GargoyleMovementPayload.START_CHARGE
                            )
                    );
                }

                if (charging) {
                    chargeTicks =
                            Math.min(
                                    race.chargedJumpMaxChargeTicks(),
                                    chargeTicks + 1
                            );
                }

                suppressVanillaJump(
                        clientInput,
                        keys
                );

                return;
            }

            if (jumpHeld) {
                jumpHeld = false;

                if (pendingNormalJump) {
                    pendingNormalJump = false;
                    heldTicks = 0;
                    chargeTicks = 0;

                    // Genuine vanilla jump: normal jump boost/effects still work.
                    player.jumpFromGround();
                    return;
                }

                if (charging) {
                    int localCharge =
                            chargeTicks;

                    charging = false;
                    heldTicks = 0;
                    chargeTicks = 0;

                    GargoyleMovementHelper.launch(
                            player,
                            race,
                            localCharge
                    );

                    ClientPacketDistributor.sendToServer(
                            new GargoyleMovementPayload(
                                    GargoyleMovementPayload.RELEASE_CHARGE
                            )
                    );

                    return;
                }
            }
        }

        /*
         * If we left the ground by some external cause while still in the
         * tap/charge state, discard the pending ground action.
         */
        if (!player.onGround()
                && (pendingNormalJump || charging)) {

            boolean serverChargeWasStarted =
                    charging;

            jumpHeld = false;
            pendingNormalJump = false;
            charging = false;
            heldTicks = 0;
            chargeTicks = 0;

            if (serverChargeWasStarted) {
                ClientPacketDistributor.sendToServer(
                        new GargoyleMovementPayload(
                                GargoyleMovementPayload.CANCEL_CHARGE
                        )
                );
            }
        }

        if (race.glideEnabled()
                && !player.onGround()
                && jumpDown) {

            GargoyleMovementHelper.applyGlide(
                    player,
                    race
            );

            ClientPacketDistributor.sendToServer(
                    new GargoyleMovementPayload(
                            GargoyleMovementPayload.GLIDE
                    )
            );
        }

        if (!jumpDown) {
            jumpHeld = false;
        }
    }

    private static void suppressVanillaJump(
            net.minecraft.client.player.ClientInput clientInput,
            Input keys
    ) {
        clientInput.keyPresses =
                new Input(
                        keys.forward(),
                        keys.backward(),
                        keys.left(),
                        keys.right(),
                        false,
                        keys.shift(),
                        keys.sprint()
                );
    }

    private static void resetAndCancelServerCharge() {
        if (charging) {
            ClientPacketDistributor.sendToServer(
                    new GargoyleMovementPayload(
                            GargoyleMovementPayload.CANCEL_CHARGE
                    )
            );
        }

        jumpHeld = false;
        pendingNormalJump = false;
        charging = false;
        heldTicks = 0;
        chargeTicks = 0;
    }

    /**
     * Used by the HUD layer.
     */
    public static boolean isCharging() {
        return charging;
    }

    public static float getChargeProgress(
            RaceDefinition race
    ) {
        if (!charging) {
            return 0.0F;
        }

        int max =
                Math.max(
                        1,
                        race.chargedJumpMaxChargeTicks()
                );

        return Math.min(
                1.0F,
                chargeTicks / (float) max
        );
    }
}
