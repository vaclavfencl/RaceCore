package com.example.racecore.mob.entity;

import com.example.racecore.mob.ai.AdaptiveMeleeAttackGoal;
import com.example.racecore.world.encounter.PlayerHuntEncounter;
import java.util.List;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.AnimationState;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.spider.Spider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

/**
 * Territorial River Drake: bites with poison, hunts Spiders outside hunts,
 * and charges a 360-degree spin that knocks nearby creatures away.
 * Client events play Blockbench bite and tail-swing animations.
 */
public final class RiverDrakeEntity extends PathfinderMob {
    // The SCALE attribute keeps the visual model and collision box in sync.
    public static final float DEFAULT_SCALE = 3.5F;
    public static final float BASE_HITBOX_WIDTH = 0.625F;
    public static final float BASE_HITBOX_HEIGHT = 0.5F;
    private static final String STATS_VERSION_KEY = "racecore_drake_stats_version";
    private static final int STATS_VERSION = 1;

    private static final int BITE_INTERVAL_TICKS = 24;
    private static final int SPIN_WINDUP_TICKS = 20;
    private static final int SPIN_COOLDOWN_TICKS = 8 * 20;
    private static final double SPIN_TRIGGER_DISTANCE_SQR = 3.25 * 3.25;
    private static final double SPIN_RADIUS = 2.75;
    private static final double SPIN_VERTICAL_REACH = 2.0;
    // Base damage is scaled by VanillaStatScaling, just like the bite.
    private static final float SPIN_DAMAGE = 36.0F;
    private static final double SPIN_KNOCKBACK = 0.90;

    // Custom events are sent from the server whenever an ability starts.
    private static final byte BITE_ANIMATION_EVENT = 61;
    private static final byte SPIN_ANIMATION_EVENT = 62;
    public final AnimationState biteAnimationState = new AnimationState();
    public final AnimationState spinAnimationState = new AnimationState();
    private int biteAnimationEndTick;
    private int spinAnimationEndTick;

    private int spinCooldown = 3 * 20;
    private int spinWindup;

    public RiverDrakeEntity(EntityType<? extends RiverDrakeEntity> type, Level level) {
        super(type, level);
        this.xpReward = 8;
        // Entity's constructor ran before our scale attribute was available.
        this.refreshDimensions();
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this));
        this.goalSelector.addGoal(2, new AdaptiveMeleeAttackGoal(
                this, () -> 1.0, () -> BITE_INTERVAL_TICKS, this::isSpinCharging));
        this.goalSelector.addGoal(7, new WaterAvoidingRandomStrollGoal(this, 0.9));
        this.goalSelector.addGoal(8, new LookAtPlayerGoal(this, Player.class, 10.0F));
        this.goalSelector.addGoal(8, new RandomLookAroundGoal(this));

        // No general player-target goal: only revenge or player hunts.
        this.targetSelector.addGoal(1, new HurtByTargetGoal(this));
        this.targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(
                this, Spider.class, true) {
            @Override
            public boolean canUse() {
                return !RiverDrakeEntity.this.entityTags().contains(PlayerHuntEncounter.HUNT_MEMBER_TAG)
                        && super.canUse();
            }

            @Override
            public boolean canContinueToUse() {
                return !RiverDrakeEntity.this.entityTags().contains(PlayerHuntEncounter.HUNT_MEMBER_TAG)
                        && super.canContinueToUse();
            }
        });
    }

    public static AttributeSupplier.Builder createAttributes() {
        return PathfinderMob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 50.0) // Half of a ravager's base health.
                .add(Attributes.ATTACK_DAMAGE, 12.0)
                .add(Attributes.SCALE, DEFAULT_SCALE)
                .add(Attributes.MOVEMENT_SPEED, 0.30)
                .add(Attributes.FOLLOW_RANGE, 28.0);
    }

    @Override
    protected void addAdditionalSaveData(ValueOutput output) {
        super.addAdditionalSaveData(output);
        output.putInt(STATS_VERSION_KEY, STATS_VERSION);
    }

    @Override
    protected void readAdditionalSaveData(ValueInput input) {
        super.readAdditionalSaveData(input);
        if (input.getIntOr(STATS_VERSION_KEY, 0) < STATS_VERSION) {
            // Migrate wolf-era health and renderer-only scale without healing injuries.
            float healthRatio = this.getHealth() / this.getMaxHealth();
            this.getAttribute(Attributes.MAX_HEALTH).setBaseValue(50.0);
            this.getAttribute(Attributes.SCALE).setBaseValue(DEFAULT_SCALE);
            this.setHealth(this.getMaxHealth() * healthRatio);
        }
        this.refreshDimensions();
    }

    public boolean isSpinCharging() {
        return spinWindup > 0;
    }

    @Override
    public void handleEntityEvent(byte eventId) {
        if (eventId == BITE_ANIMATION_EVENT) {
            biteAnimationState.start(this.tickCount);
            biteAnimationEndTick = this.tickCount + 5; // Authored bite is 0.25s.
        } else if (eventId == SPIN_ANIMATION_EVENT) {
            biteAnimationState.stop();
            spinAnimationState.start(this.tickCount);
            spinAnimationEndTick = this.tickCount + 22; // Authored 1.1667s scaled to ~22 ticks.
        } else {
            super.handleEntityEvent(eventId);
        }
    }

    @Override
    public void tick() {
        super.tick();
        if (!this.level().isClientSide()) {
            return;
        }
        if (biteAnimationState.isStarted() && this.tickCount >= biteAnimationEndTick) {
            biteAnimationState.stop();
        }
        if (spinAnimationState.isStarted() && this.tickCount >= spinAnimationEndTick) {
            spinAnimationState.stop();
        }
    }

    @Override
    protected void customServerAiStep(ServerLevel level) {
        super.customServerAiStep(level);

        if (spinCooldown > 0) spinCooldown--;

        if (spinWindup > 0) {
            this.getNavigation().stop();
            spinWindup--;
            if (spinWindup == 0) {
                performSpin(level);
                spinCooldown = SPIN_COOLDOWN_TICKS;
            }
            return;
        }

        LivingEntity target = this.getTarget();
        if (target != null && target.isAlive() && spinCooldown <= 0
                && this.distanceToSqr(target) <= SPIN_TRIGGER_DISTANCE_SQR) {
            spinWindup = SPIN_WINDUP_TICKS;
            this.getNavigation().stop();
            this.level().broadcastEntityEvent(this, SPIN_ANIMATION_EVENT);
        }
    }

    @Override
    public boolean doHurtTarget(ServerLevel level, Entity target) {
        if (isSpinCharging()) return false;

        this.level().broadcastEntityEvent(this, BITE_ANIMATION_EVENT);
        boolean hit = super.doHurtTarget(level, target);
        if (hit && target instanceof LivingEntity living) {
            // RaceCore potion rebalance supports Poison stacking/reapplications.
            living.addEffect(new MobEffectInstance(MobEffects.POISON, 5 * 20, 0), this);
        }
        return hit;
    }

    private void performSpin(ServerLevel level) {
        level.sendParticles(ParticleTypes.CLOUD,
                this.getX(), this.getY() + 0.35, this.getZ(),
                28, 1.25, 0.25, 1.25, 0.04);

        List<LivingEntity> victims = level.getEntitiesOfClass(
                LivingEntity.class,
                this.getBoundingBox().inflate(SPIN_RADIUS),
                entity -> entity != this && entity.isAlive()
                        // Feet-relative range keeps elevated targets outside the swing.
                        && Math.abs(entity.getY() - this.getY()) <= SPIN_VERTICAL_REACH
                        && (!(entity instanceof Player player)
                                || (!player.isCreative() && !player.isSpectator())));
        for (LivingEntity victim : victims) {
            double dx = victim.getX() - this.getX();
            double dz = victim.getZ() - this.getZ();
            double length = Math.sqrt(dx * dx + dz * dz);
            if (length < 0.001) {
                dx = 1.0;
                dz = 0.0;
                length = 1.0;
            }
            boolean hit = victim.hurtServer(
                    level, this.damageSources().mobAttack(this), SPIN_DAMAGE);
            if (hit) {
                victim.push(dx / length * SPIN_KNOCKBACK,
                        0.18, dz / length * SPIN_KNOCKBACK);
            }
        }
    }

    @Override
    public InteractionResult mobInteract(Player player, InteractionHand hand) {
        // Disable Wolf taming, breeding, and feeding interactions.
        return InteractionResult.PASS;
    }
}
