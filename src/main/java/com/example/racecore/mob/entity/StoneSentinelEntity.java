package com.example.racecore.mob.entity;

import com.example.racecore.mob.RaceCoreMobs;
import com.example.racecore.mob.ai.AdaptiveMeleeAttackGoal;
import com.example.racecore.world.encounter.EncounterManager;
import java.util.List;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.animal.golem.IronGolem;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/**
 * Stone Sentinel.
 *
 * Placeholder visual: vanilla Iron Golem.
 *
 * Adult:
 * - deterministic heavy melee,
 * - telegraphed rock throw,
 * - telegraphed summon of one Lesser Stone Sentinel,
 * - at most two lesser sentinels nearby.
 *
 * Lesser:
 * - melee + slower rock throws,
 * - never summons another sentinel.
 *
 * Both are immune to Poison and Wither.
 */
public class StoneSentinelEntity extends IronGolem {
    private static final int MELEE_INTERVAL_TICKS =
            28;

    private static final int ROCK_WINDUP_TICKS =
            15;

    private static final int ADULT_ROCK_COOLDOWN_TICKS =
            5 * 20;

    private static final int LESSER_ROCK_COOLDOWN_TICKS =
            7 * 20;

    private static final double ROCK_MIN_RANGE_SQR =
            4.0 * 4.0;

    private static final double ROCK_MAX_RANGE_SQR =
            24.0 * 24.0;

    private static final int SUMMON_WINDUP_TICKS =
            2 * 20;

    private static final int SUMMON_COOLDOWN_TICKS =
            22 * 20;

    private static final int MAX_LESSERS_NEARBY =
            2;

    private static final double LESSER_COUNT_RADIUS =
            28.0;

    private int rockCooldown =
            3 * 20;

    private int rockWindup;

    private int summonCooldown =
            12 * 20;

    private int summonWindup;

    public StoneSentinelEntity(
            EntityType<? extends StoneSentinelEntity> type,
            Level level
    ) {
        super(type, level);
        this.xpReward = 16;
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(
                0,
                new FloatGoal(this)
        );

        this.goalSelector.addGoal(
                3,
                new AdaptiveMeleeAttackGoal(
                        this,
                        () -> 0.85,
                        () -> MELEE_INTERVAL_TICKS,
                        this::isAbilityCharging
                )
        );

        this.goalSelector.addGoal(
                7,
                new WaterAvoidingRandomStrollGoal(
                        this,
                        0.55
                )
        );

        this.goalSelector.addGoal(
                8,
                new LookAtPlayerGoal(
                        this,
                        Player.class,
                        16.0F
                )
        );

        this.goalSelector.addGoal(
                8,
                new RandomLookAroundGoal(this)
        );

        this.targetSelector.addGoal(
                1,
                new HurtByTargetGoal(this)
        );

        this.targetSelector.addGoal(
                2,
                new NearestAttackableTargetGoal<>(
                        this,
                        Player.class,
                        true
                )
        );
    }

    public static AttributeSupplier.Builder createAttributes() {
        return IronGolem.createAttributes()
                .add(Attributes.MAX_HEALTH, 240.0)
                .add(Attributes.ATTACK_DAMAGE, 22.0)
                .add(Attributes.MOVEMENT_SPEED, 0.20)
                .add(Attributes.FOLLOW_RANGE, 36.0)
                .add(Attributes.ARMOR, 12.0)
                .add(
                        Attributes.KNOCKBACK_RESISTANCE,
                        0.80
                );
    }

    protected boolean canSummonLessers() {
        return true;
    }

    protected boolean isLesserSentinel() {
        return false;
    }

    public boolean isAbilityCharging() {
        return rockWindup > 0
                || summonWindup > 0;
    }

    @Override
    protected void customServerAiStep(
            ServerLevel level
    ) {
        super.customServerAiStep(level);

        if (rockCooldown > 0) {
            rockCooldown--;
        }

        if (canSummonLessers()
                && summonCooldown > 0) {
            summonCooldown--;
        }

        if (summonWindup > 0) {
            this.getNavigation().stop();
            summonWindup--;

            if (summonWindup % 5 == 0) {
                level.sendParticles(
                        ParticleTypes.POOF,
                        this.getX(),
                        this.getY() + 0.25,
                        this.getZ(),
                        7,
                        0.85,
                        0.15,
                        0.85,
                        0.02
                );
            }

            if (summonWindup == 0) {
                summonLesser(level);
                summonCooldown =
                        SUMMON_COOLDOWN_TICKS;
            }

            return;
        }

        if (rockWindup > 0) {
            this.getNavigation().stop();
            rockWindup--;

            if (rockWindup % 5 == 0) {
                level.sendParticles(
                        ParticleTypes.POOF,
                        this.getX(),
                        this.getY()
                                + this.getBbHeight()
                                * 0.75,
                        this.getZ(),
                        4,
                        0.35,
                        0.35,
                        0.35,
                        0.01
                );
            }

            if (rockWindup == 0) {
                throwRock(level);
                rockCooldown =
                        isLesserSentinel()
                                ? LESSER_ROCK_COOLDOWN_TICKS
                                : ADULT_ROCK_COOLDOWN_TICKS;
            }

            return;
        }

        LivingEntity target =
                this.getTarget();

        if (target == null
                || !target.isAlive()) {
            return;
        }

        if (canSummonLessers()
                && summonCooldown <= 0
                && countNearbyLessers(level)
                < MAX_LESSERS_NEARBY) {
            summonWindup =
                    SUMMON_WINDUP_TICKS;

            this.getNavigation().stop();
            return;
        }

        double distanceSqr =
                this.distanceToSqr(target);

        if (rockCooldown <= 0
                && distanceSqr >= ROCK_MIN_RANGE_SQR
                && distanceSqr <= ROCK_MAX_RANGE_SQR
                && this.getSensing()
                        .hasLineOfSight(target)) {
            rockWindup =
                    ROCK_WINDUP_TICKS;

            this.getNavigation().stop();
        }
    }

    /**
     * Iron Golem's vanilla attack adds random damage.
     * RaceCore Sentinel melee is fixed and therefore readable.
     */
    @Override
    public boolean doHurtTarget(
            ServerLevel level,
            Entity target
    ) {
        if (isAbilityCharging()) {
            return false;
        }

        this.level()
                .broadcastEntityEvent(
                        this,
                        (byte) 4
                );

        float damage =
                (float) this.getAttributeValue(
                        Attributes.ATTACK_DAMAGE
                );

        boolean hurt =
                target.hurtServer(
                        level,
                        this.damageSources()
                                .mobAttack(this),
                        damage
                );

        if (hurt
                && target instanceof LivingEntity living) {
            Vec3 delta =
                    living.position()
                            .subtract(
                                    this.position()
                            );

            double horizontal =
                    Math.max(
                            0.001,
                            Math.sqrt(
                                    delta.x * delta.x
                                            + delta.z * delta.z
                            )
                    );

            living.push(
                    delta.x / horizontal
                            * 0.35,
                    0.35,
                    delta.z / horizontal
                            * 0.35
            );
        }

        this.playSound(
                SoundEvents.IRON_GOLEM_ATTACK,
                1.0F,
                1.0F
        );

        return hurt;
    }

    @Override
    public boolean canBeAffected(
            MobEffectInstance effect
    ) {
        if (effect.is(MobEffects.POISON)
                || effect.is(MobEffects.WITHER)) {
            return false;
        }

        return super.canBeAffected(effect);
    }

    @Override
    protected InteractionResult mobInteract(
            Player player,
            InteractionHand hand
    ) {
        /*
         * Do not inherit the normal Iron Golem iron-ingot repair interaction.
         * This is a hostile construct.
         */
        return InteractionResult.PASS;
    }

    private void throwRock(
            ServerLevel level
    ) {
        LivingEntity target =
                this.getTarget();

        if (target == null
                || !target.isAlive()) {
            return;
        }

        RockProjectileEntity rock =
                new RockProjectileEntity(
                        level,
                        this
                );

        double dx =
                target.getX() - rock.getX();

        double dz =
                target.getZ() - rock.getZ();

        double horizontal =
                Math.sqrt(
                        dx * dx + dz * dz
                );

        double dy =
                target.getEyeY()
                        - rock.getY()
                        + horizontal * 0.08;

        rock.shoot(
                dx,
                dy,
                dz,
                1.25F,
                0.0F
        );

        level.addFreshEntity(rock);
    }

    private int countNearbyLessers(
            ServerLevel level
    ) {
        return level.getEntitiesOfClass(
                LesserStoneSentinelEntity.class,
                this.getBoundingBox()
                        .inflate(
                                LESSER_COUNT_RADIUS
                        ),
                Entity::isAlive
        ).size();
    }

    private void summonLesser(
            ServerLevel level
    ) {
        if (!canSummonLessers()
                || countNearbyLessers(level)
                >= MAX_LESSERS_NEARBY) {
            return;
        }

        double[][] offsets = {
                {1.8, 0.0},
                {-1.8, 0.0},
                {0.0, 1.8},
                {0.0, -1.8}
        };

        for (double[] offset : offsets) {
            LesserStoneSentinelEntity lesser =
                    RaceCoreMobs.LESSER_STONE_SENTINEL
                            .get()
                            .create(
                                    level,
                                    EntitySpawnReason.EVENT
                            );

            if (lesser == null) {
                return;
            }

            lesser.snapTo(
                    this.getX() + offset[0],
                    this.getY(),
                    this.getZ() + offset[1],
                    this.getYRot(),
                    0.0F
            );

            if (!level.noCollision(lesser)
                    || level.containsAnyLiquid(
                    lesser.getBoundingBox()
            )) {
                lesser.discard();
                continue;
            }

            lesser.finalizeSpawn(
                    level,
                    level.getCurrentDifficultyAt(
                            lesser.blockPosition()
                    ),
                    EntitySpawnReason.EVENT,
                    null
            );

            lesser.setTarget(
                    this.getTarget()
            );

            level.addFreshEntityWithPassengers(
                    lesser
            );

            EncounterManager.addSummonedParticipant(
                    this,
                    lesser
            );

            level.sendParticles(
                    ParticleTypes.POOF,
                    lesser.getX(),
                    lesser.getY() + 0.5,
                    lesser.getZ(),
                    18,
                    0.45,
                    0.45,
                    0.45,
                    0.03
            );

            return;
        }
    }
}
