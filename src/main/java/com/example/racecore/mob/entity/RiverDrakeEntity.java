package com.example.racecore.mob.entity;

import com.example.racecore.mob.ai.AdaptiveMeleeAttackGoal;
import java.util.List;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.animal.wolf.Wolf;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

/**
 * River Drake.
 *
 * Placeholder visual: vanilla Wolf.
 *
 * The Drake is territorial, not a normal always-hostile surface spawn:
 * outside a Hunt it becomes aggressive after being attacked. Hunt logic may
 * explicitly assign a player target.
 *
 * Bite: always applies one 5-second Poison application.
 * Spin: 1 second telegraph, then deterministic 360 degree damage/knockback.
 */
public final class RiverDrakeEntity extends Wolf {
    private static final int BITE_INTERVAL_TICKS = 24;

    private static final int SPIN_WINDUP_TICKS = 20;
    private static final int SPIN_COOLDOWN_TICKS = 8 * 20;

    private static final double SPIN_TRIGGER_DISTANCE_SQR =
            3.25 * 3.25;

    private static final double SPIN_RADIUS =
            2.75;

    private static final float SPIN_DAMAGE =
            10.0F;

    private static final double SPIN_KNOCKBACK =
            0.90;

    private int spinCooldown =
            3 * 20;

    private int spinWindup;

    public RiverDrakeEntity(
            EntityType<? extends RiverDrakeEntity> type,
            Level level
    ) {
        super(type, level);
        this.xpReward = 8;
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(
                0,
                new FloatGoal(this)
        );

        this.goalSelector.addGoal(
                2,
                new AdaptiveMeleeAttackGoal(
                        this,
                        () -> 1.0,
                        () -> BITE_INTERVAL_TICKS,
                        this::isSpinCharging
                )
        );

        this.goalSelector.addGoal(
                7,
                new WaterAvoidingRandomStrollGoal(
                        this,
                        0.9
                )
        );

        this.goalSelector.addGoal(
                8,
                new LookAtPlayerGoal(
                        this,
                        Player.class,
                        10.0F
                )
        );

        this.goalSelector.addGoal(
                8,
                new RandomLookAroundGoal(this)
        );

        /*
         * Intentionally no NearestAttackableTargetGoal<Player>.
         * Normal Drakes are territorial. Hunts explicitly set a target.
         */
        this.targetSelector.addGoal(
                1,
                new HurtByTargetGoal(this)
        );
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Wolf.createAttributes()
                .add(Attributes.MAX_HEALTH, 90.0)
                .add(Attributes.ATTACK_DAMAGE, 12.0)
                .add(Attributes.MOVEMENT_SPEED, 0.30)
                .add(Attributes.FOLLOW_RANGE, 28.0);
    }

    public boolean isSpinCharging() {
        return spinWindup > 0;
    }

    @Override
    protected void customServerAiStep(
            ServerLevel level
    ) {
        super.customServerAiStep(level);

        if (spinCooldown > 0) {
            spinCooldown--;
        }

        if (spinWindup > 0) {
            this.getNavigation().stop();
            spinWindup--;

            if (spinWindup % 4 == 0) {
                level.sendParticles(
                        ParticleTypes.CLOUD,
                        this.getX(),
                        this.getY() + 0.35,
                        this.getZ(),
                        8,
                        0.55,
                        0.15,
                        0.55,
                        0.01
                );
            }

            if (spinWindup == 0) {
                performSpin(level);
                spinCooldown = SPIN_COOLDOWN_TICKS;
            }

            return;
        }

        LivingEntity target =
                this.getTarget();

        if (target != null
                && target.isAlive()
                && spinCooldown <= 0
                && this.distanceToSqr(target)
                <= SPIN_TRIGGER_DISTANCE_SQR) {
            spinWindup = SPIN_WINDUP_TICKS;
            this.getNavigation().stop();
        }
    }

    @Override
    public boolean doHurtTarget(
            ServerLevel level,
            Entity target
    ) {
        if (isSpinCharging()) {
            return false;
        }

        boolean hit =
                super.doHurtTarget(
                        level,
                        target
                );

        if (hit
                && target instanceof LivingEntity living) {
            /*
             * When RaceCore's potion rebalance is present, re-applications
             * naturally become Poison I -> II -> III... and reset to 5 sec.
             */
            living.addEffect(
                    new MobEffectInstance(
                            MobEffects.POISON,
                            5 * 20,
                            0
                    ),
                    this
            );
        }

        return hit;
    }

    private void performSpin(
            ServerLevel level
    ) {
        level.sendParticles(
                ParticleTypes.CLOUD,
                this.getX(),
                this.getY() + 0.35,
                this.getZ(),
                28,
                1.25,
                0.25,
                1.25,
                0.04
        );

        List<Player> players =
                level.getEntitiesOfClass(
                        Player.class,
                        this.getBoundingBox()
                                .inflate(SPIN_RADIUS),
                        player -> player.isAlive()
                                && !player.isCreative()
                                && !player.isSpectator()
                );

        for (Player player : players) {
            double dx =
                    player.getX() - this.getX();

            double dz =
                    player.getZ() - this.getZ();

            double length =
                    Math.sqrt(
                            dx * dx + dz * dz
                    );

            if (length < 0.001) {
                dx = 1.0;
                dz = 0.0;
                length = 1.0;
            }

            boolean hit =
                    player.hurtServer(
                            level,
                            this.damageSources()
                                    .mobAttack(this),
                            SPIN_DAMAGE
                    );

            if (hit) {
                player.push(
                        dx / length
                                * SPIN_KNOCKBACK,
                        0.18,
                        dz / length
                                * SPIN_KNOCKBACK
                );
            }
        }
    }

    /**
     * Placeholder Wolf must not expose wolf taming/breeding interactions.
     */
    @Override
    public InteractionResult mobInteract(
            Player player,
            InteractionHand hand
    ) {
        return InteractionResult.PASS;
    }
}
