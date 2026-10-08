package com.example.racecore.world.encounter;

import com.example.racecore.world.BannerClaimEvents;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.phys.Vec3;

/**
 * A coordinated pack that actively hunts one player.
 *
 * Behaviour:
 * - while any hunter can see the target within ~50 blocks, all hunters stay
 *   focused on that target and refresh the last-known position/direction;
 * - after losing sight, the pack goes to the last-known position, then roughly
 *   50 blocks farther in the target's last observed direction;
 * - if the player is not reacquired, the remaining hunters are cleaned up;
 * - entering a claimed surface chunk ends the hunt rather than letting the
 *   encounter invade a protected settlement.
 */
public final class PlayerHuntEncounter extends Encounter {
    public static final String HUNT_MEMBER_TAG = "racecore_hunt";

    private static final int LOGIC_INTERVAL_TICKS = 10;
    private static final int MAX_LOST_TICKS = 45 * 20;
    private static final double REACQUIRE_RANGE = 50.0;
    private static final double REACQUIRE_RANGE_SQR = REACQUIRE_RANGE * REACQUIRE_RANGE;
    private static final double SEARCH_DISTANCE = 50.0;
    private static final double NAVIGATION_SPEED = 1.15;
    private static final double REACHED_POSITION_SQR = 4.0 * 4.0;

    private final UUID targetId;

    private BlockPos lastKnownPosition;
    private BlockPos previousSeenPosition;
    private BlockPos searchDestination;
    private double lastDirectionX;
    private double lastDirectionZ;
    private boolean reachedLastKnownPosition;
    private int lostTicks;
    private int logicTickCounter;

    public PlayerHuntEncounter(
            ServerLevel level,
            UUID targetId,
            BlockPos initialTargetPosition
    ) {
        super(level);
        this.targetId = targetId;
        this.lastKnownPosition = initialTargetPosition.immutable();
        this.previousSeenPosition = initialTargetPosition.immutable();
        setState(EncounterState.SPAWNING);
    }

    public UUID targetId() {
        return targetId;
    }

    @Override
    public void addParticipant(Entity entity) {
        super.addParticipant(entity);
        entity.addTag(HUNT_MEMBER_TAG);
    }

    public void activate() {
        if (!isFinished()) {
            setState(EncounterState.ACTIVE);
        }
    }

    @Override
    protected void tickEncounter() {
        logicTickCounter++;
        if (logicTickCounter < LOGIC_INTERVAL_TICKS) {
            return;
        }
        logicTickCounter = 0;

        ServerPlayer target = level().getServer()
                .getPlayerList()
                .getPlayer(targetId);

        if (target == null
                || target.level() != level()
                || !target.isAlive()
                || target.isSpectator()) {
            finish(true);
            return;
        }

        if (BannerClaimEvents.blocksSurfaceHostileSpawn(
                level(),
                target.blockPosition()
        )) {
            finish(true);
            return;
        }

        List<Mob> hunters = liveHunters();
        if (hunters.isEmpty()) {
            finish(false);
            return;
        }

        boolean targetSeen = false;

        for (Mob hunter : hunters) {
            if (hunter.distanceToSqr(target) <= REACQUIRE_RANGE_SQR
                    && hunter.getSensing().hasLineOfSight(target)) {
                targetSeen = true;
                break;
            }
        }

        if (targetSeen) {
            refreshLastKnownTarget(target);
            lostTicks = 0;
            reachedLastKnownPosition = false;
            searchDestination = null;
            setState(EncounterState.ACTIVE);

            for (Mob hunter : hunters) {
                hunter.setTarget(target);
                hunter.getNavigation().moveTo(target, NAVIGATION_SPEED);
            }

            return;
        }

        lostTicks += LOGIC_INTERVAL_TICKS;
        setState(EncounterState.SEARCHING);

        for (Mob hunter : hunters) {
            hunter.setTarget(null);
        }

        if (!reachedLastKnownPosition) {
            for (Mob hunter : hunters) {
                hunter.getNavigation().moveTo(
                        lastKnownPosition.getX() + 0.5,
                        lastKnownPosition.getY(),
                        lastKnownPosition.getZ() + 0.5,
                        NAVIGATION_SPEED
                );

                if (hunter.distanceToSqr(
                        lastKnownPosition.getX() + 0.5,
                        lastKnownPosition.getY(),
                        lastKnownPosition.getZ() + 0.5
                ) <= REACHED_POSITION_SQR) {
                    reachedLastKnownPosition = true;
                }
            }
        }

        if (reachedLastKnownPosition) {
            if (searchDestination == null) {
                searchDestination = buildSearchDestination(target);
            }

            boolean reachedEnd = false;

            for (Mob hunter : hunters) {
                hunter.getNavigation().moveTo(
                        searchDestination.getX() + 0.5,
                        searchDestination.getY(),
                        searchDestination.getZ() + 0.5,
                        NAVIGATION_SPEED
                );

                if (hunter.distanceToSqr(
                        searchDestination.getX() + 0.5,
                        searchDestination.getY(),
                        searchDestination.getZ() + 0.5
                ) <= REACHED_POSITION_SQR) {
                    reachedEnd = true;
                }
            }

            if (reachedEnd) {
                finish(true);
                return;
            }
        }

        if (lostTicks >= MAX_LOST_TICKS) {
            finish(true);
        }
    }

    private List<Mob> liveHunters() {
        List<Mob> result = new ArrayList<>();

        for (UUID participantId : Set.copyOf(participants())) {
            Entity entity = level().getEntity(participantId);

            if (entity instanceof Mob mob && mob.isAlive()) {
                if (BannerClaimEvents.blocksSurfaceHostileSpawn(
                        level(),
                        mob.blockPosition()
                )) {
                    mob.discard();
                    removeParticipant(participantId);
                    continue;
                }

                result.add(mob);
            } else {
                removeParticipant(participantId);
            }
        }

        return result;
    }

    private void refreshLastKnownTarget(ServerPlayer target) {
        BlockPos current = target.blockPosition().immutable();

        double dx = current.getX() - previousSeenPosition.getX();
        double dz = current.getZ() - previousSeenPosition.getZ();
        double length = Math.sqrt(dx * dx + dz * dz);

        if (length > 0.01) {
            lastDirectionX = dx / length;
            lastDirectionZ = dz / length;
        } else if (Math.abs(lastDirectionX) < 0.01
                && Math.abs(lastDirectionZ) < 0.01) {
            Vec3 look = target.getLookAngle();
            double horizontal = Math.sqrt(look.x * look.x + look.z * look.z);

            if (horizontal > 0.01) {
                lastDirectionX = look.x / horizontal;
                lastDirectionZ = look.z / horizontal;
            }
        }

        previousSeenPosition = current;
        lastKnownPosition = current;
    }

    private BlockPos buildSearchDestination(ServerPlayer target) {
        double directionX = lastDirectionX;
        double directionZ = lastDirectionZ;

        if (Math.abs(directionX) < 0.01
                && Math.abs(directionZ) < 0.01) {
            Vec3 look = target.getLookAngle();
            double horizontal = Math.sqrt(look.x * look.x + look.z * look.z);

            if (horizontal > 0.01) {
                directionX = look.x / horizontal;
                directionZ = look.z / horizontal;
            } else {
                directionZ = 1.0;
            }
        }

        int x = (int) Math.floor(
                lastKnownPosition.getX() + directionX * SEARCH_DISTANCE
        );
        int z = (int) Math.floor(
                lastKnownPosition.getZ() + directionZ * SEARCH_DISTANCE
        );

        return new BlockPos(x, lastKnownPosition.getY(), z);
    }
}
