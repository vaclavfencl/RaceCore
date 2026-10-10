package com.example.racecore.mob.ai;

import java.util.EnumSet;
import java.util.function.BooleanSupplier;
import java.util.function.DoubleSupplier;
import java.util.function.IntSupplier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.player.Player;

/**
 * Small deterministic melee goal used by the first RaceCore custom mobs.
 *
 * Vanilla MeleeAttackGoal has a fixed 20-tick attack interval. Marauders need
 * that interval to become 10 ticks during Frenzy, and ability wind-ups need a
 * clean way to temporarily disable melee without random behaviour.
 */
public final class AdaptiveMeleeAttackGoal extends Goal {
    private final Mob mob;
    private final DoubleSupplier movementSpeed;
    private final IntSupplier attackIntervalTicks;
    private final BooleanSupplier meleeBlocked;

    private int attackCooldown;

    public AdaptiveMeleeAttackGoal(
            Mob mob,
            DoubleSupplier movementSpeed,
            IntSupplier attackIntervalTicks,
            BooleanSupplier meleeBlocked
    ) {
        this.mob = mob;
        this.movementSpeed = movementSpeed;
        this.attackIntervalTicks = attackIntervalTicks;
        this.meleeBlocked = meleeBlocked;

        this.setFlags(
                EnumSet.of(
                        Goal.Flag.MOVE,
                        Goal.Flag.LOOK
                )
        );
    }

    @Override
    public boolean canUse() {
        return hasValidTarget()
                && !meleeBlocked.getAsBoolean();
    }

    @Override
    public boolean canContinueToUse() {
        return hasValidTarget()
                && !meleeBlocked.getAsBoolean();
    }

    @Override
    public void start() {
        attackCooldown = 0;
        mob.setAggressive(true);
    }

    @Override
    public void stop() {
        mob.getNavigation().stop();
        mob.setAggressive(false);
        attackCooldown = 0;
    }

    @Override
    public void tick() {
        LivingEntity target = mob.getTarget();
        if (target == null) {
            return;
        }

        mob.getLookControl().setLookAt(
                target,
                30.0F,
                30.0F
        );

        mob.getNavigation().moveTo(
                target,
                movementSpeed.getAsDouble()
        );

        if (attackCooldown > 0) {
            attackCooldown--;
        }

        if (attackCooldown <= 0
                && mob.isWithinMeleeAttackRange(target)
                && mob.getSensing().hasLineOfSight(target)
                && mob.level() instanceof ServerLevel serverLevel) {
            mob.swing(
                    net.minecraft.world.InteractionHand.MAIN_HAND,
                    net.minecraft.world.item.component.SwingAnimation.DEFAULT
            );

            mob.doHurtTarget(
                    serverLevel,
                    target
            );

            attackCooldown =
                    Math.max(
                            1,
                            attackIntervalTicks.getAsInt()
                    );
        }
    }

    private boolean hasValidTarget() {
        LivingEntity target =
                mob.getTarget();

        if (target == null
                || !target.isAlive()) {
            return false;
        }

        if (target instanceof Player player
                && (player.isCreative()
                || player.isSpectator())) {
            return false;
        }

        return true;
    }
}
