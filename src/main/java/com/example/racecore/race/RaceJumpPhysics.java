package com.example.racecore.race;

/**
 * Converts an intended jump apex in blocks into the initial vertical velocity
 * used by vanilla-like player gravity/drag.
 *
 * This keeps races.json readable in world-space units ("jumpHeight": 1.485)
 * instead of exposing internal motion values.
 */
public final class RaceJumpPhysics {
    private static final double VANILLA_GRAVITY = 0.08;
    private static final double VANILLA_VERTICAL_DRAG = 0.98;

    // A normal vanilla player starts a ground jump at roughly this Y velocity.
    public static final double VANILLA_PLAYER_JUMP_VELOCITY = 0.42;

    private RaceJumpPhysics() {
    }

    public static double initialVelocityForHeight(double targetHeight) {
        if (targetHeight <= 0.0) {
            return 0.0;
        }

        double low = 0.0;
        double high = 2.0;

        while (simulatedApex(high) < targetHeight) {
            high *= 2.0;
        }

        // Plenty of precision for Minecraft movement without doing expensive math.
        for (int i = 0; i < 40; i++) {
            double mid = (low + high) * 0.5;

            if (simulatedApex(mid) < targetHeight) {
                low = mid;
            } else {
                high = mid;
            }
        }

        return (low + high) * 0.5;
    }

    private static double simulatedApex(double initialVelocity) {
        double height = 0.0;
        double velocity = initialVelocity;

        for (int tick = 0; tick < 100 && velocity > 0.0; tick++) {
            height += velocity;
            velocity = (velocity - VANILLA_GRAVITY) * VANILLA_VERTICAL_DRAG;
        }

        return height;
    }
}
