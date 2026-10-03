package com.example.racecore.race.ability;

import com.example.racecore.race.RaceDefinition;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

/**
 * Shared Gargoyle charged-jump + glide physics.
 *
 * This is intentionally NOT Elytra flight.
 */
public final class GargoyleMovementHelper {
    private GargoyleMovementHelper() {
    }

    public static void launch(
            Player player,
            RaceDefinition race,
            int chargeTicks
    ) {
        int maxTicks =
                Math.max(
                        1,
                        race.chargedJumpMaxChargeTicks()
                );

        double charge =
                Mth.clamp(
                        chargeTicks / (double) maxTicks,
                        0.0,
                        1.0
                );

        double minHeight =
                race.chargedJumpMinHeight();

        double maxHeight =
                race.chargedJumpMaxHeight();

        double desiredHeight =
                minHeight
                        + (maxHeight - minHeight)
                        * charge;

        double velocity =
                velocityForHeight(
                        desiredHeight
                );

        Vec3 current =
                player.getDeltaMovement();

        player.setDeltaMovement(
                current.x,
                velocity,
                current.z
        );

        player.resetFallDistance();
    }

    /**
     * Numerically solves vanilla-style vertical motion:
     *
     * position += velocity
     * velocity = (velocity - 0.08) * 0.98
     *
     * so chargedJumpMaxHeight remains an intuitive blocks-high config value.
     */
    private static double velocityForHeight(
            double targetHeight
    ) {
        if (targetHeight <= 0.0) {
            return 0.0;
        }

        double low = 0.0;
        double high = 2.0;

        for (int i = 0; i < 24; i++) {
            double mid =
                    (low + high) * 0.5;

            if (simulatedHeight(mid)
                    < targetHeight) {
                low = mid;
            } else {
                high = mid;
            }
        }

        return high;
    }

    private static double simulatedHeight(
            double initialVelocity
    ) {
        double y = 0.0;
        double velocity =
                initialVelocity;

        for (int i = 0;
                i < 100 && velocity > 0.0;
                i++) {

            y += velocity;

            velocity =
                    (velocity - 0.08)
                            * 0.98;
        }

        return y;
    }

    public static void applyGlide(
            Player player,
            RaceDefinition race
    ) {
        Vec3 movement =
                player.getDeltaMovement();

        Vec3 look =
                player.getLookAngle();

        double x =
                movement.x;

        double z =
                movement.z;

        double horizontalLook =
                Math.sqrt(
                        look.x * look.x
                                + look.z * look.z
                );

        if (horizontalLook > 0.0001) {
            double acceleration =
                    race.glideForwardAcceleration();

            x += look.x
                    / horizontalLook
                    * acceleration;

            z += look.z
                    / horizontalLook
                    * acceleration;
        }

        double horizontalSpeed =
                Math.sqrt(
                        x * x + z * z
                );

        double maxHorizontal =
                race.glideMaxHorizontalSpeed();

        if (maxHorizontal > 0.0
                && horizontalSpeed
                > maxHorizontal) {

            double scale =
                    maxHorizontal
                            / horizontalSpeed;

            x *= scale;
            z *= scale;
        }

        double y =
                Math.max(
                        movement.y,
                        -race.glideFallSpeed()
                );

        player.setDeltaMovement(
                x,
                y,
                z
        );

        player.resetFallDistance();
    }
}
