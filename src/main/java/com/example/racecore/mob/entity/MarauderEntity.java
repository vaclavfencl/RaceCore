package com.example.racecore.mob.entity;

import com.example.racecore.RaceCore;
import com.example.racecore.mob.ai.AdaptiveMeleeAttackGoal;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.RandomSource;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.illager.Pillager;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;

/**
 * Marauder.
 *
 * Placeholder visual: vanilla Pillager, but this first version is a melee
 * raider.
 *
 * Every combat cycle it performs a clearly telegraphed 2 second power-up.
 * Hitting it DURING that power-up interrupts the ability.
 *
 * Once Frenzy actually begins, damage no longer cancels it. That is deliberate:
 * the player's interruption window is only the telegraphed charge.
 */
public final class MarauderEntity extends Pillager {
    private static final Identifier FRENZY_SPEED_ID =
            Identifier.fromNamespaceAndPath(
                    RaceCore.MODID,
                    "marauder_frenzy_speed"
            );

    private static final AttributeModifier FRENZY_SPEED =
            new AttributeModifier(
                    FRENZY_SPEED_ID,
                    0.35,
                    AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL
            );

    private static final int POWER_UP_TICKS =
            2 * 20;

    private static final int FRENZY_DURATION_TICKS =
            6 * 20;

    private static final int FRENZY_COOLDOWN_TICKS =
            12 * 20;

    private static final int INTERRUPTED_COOLDOWN_TICKS =
            8 * 20;

    private static final int NORMAL_ATTACK_INTERVAL =
            20;

    private static final int FRENZY_ATTACK_INTERVAL =
            10;

    private static final double POWER_UP_MAX_RANGE_SQR =
            16.0 * 16.0;

    private int powerUpTicks;

    private int frenzyTicks;

    private int frenzyCooldown =
            5 * 20;

    public MarauderEntity(
            EntityType<? extends MarauderEntity> type,
            Level level
    ) {
        super(type, level);
        this.xpReward = 10;
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
                        () -> isFrenzied()
                                ? 1.15
                                : 1.0,
                        () -> isFrenzied()
                                ? FRENZY_ATTACK_INTERVAL
                                : NORMAL_ATTACK_INTERVAL,
                        this::isPoweringUp
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
                        12.0F
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
        return Pillager.createAttributes()
                .add(Attributes.MAX_HEALTH, 100.0)
                .add(Attributes.ATTACK_DAMAGE, 10.0)
                .add(Attributes.MOVEMENT_SPEED, 0.29)
                .add(Attributes.FOLLOW_RANGE, 36.0);
    }

    public boolean isPoweringUp() {
        return powerUpTicks > 0;
    }

    public boolean isFrenzied() {
        return frenzyTicks > 0;
    }

    @Override
    protected void customServerAiStep(
            ServerLevel level
    ) {
        super.customServerAiStep(level);

        if (isPoweringUp()) {
            powerUpTicks--;

            if (powerUpTicks % 5 == 0) {
                level.sendParticles(
                        ParticleTypes.ANGRY_VILLAGER,
                        this.getX(),
                        this.getY()
                                + this.getBbHeight()
                                * 0.65,
                        this.getZ(),
                        4,
                        0.35,
                        0.35,
                        0.35,
                        0.0
                );
            }

            if (powerUpTicks == 0) {
                beginFrenzy(level);
            }

            return;
        }

        if (isFrenzied()) {
            frenzyTicks--;

            if (frenzyTicks % 10 == 0) {
                level.sendParticles(
                        ParticleTypes.ANGRY_VILLAGER,
                        this.getX(),
                        this.getY()
                                + this.getBbHeight()
                                * 0.75,
                        this.getZ(),
                        2,
                        0.25,
                        0.25,
                        0.25,
                        0.0
                );
            }

            if (frenzyTicks == 0) {
                endFrenzy();
                frenzyCooldown =
                        FRENZY_COOLDOWN_TICKS;
            }

            return;
        }

        if (frenzyCooldown > 0) {
            frenzyCooldown--;
        }

        if (frenzyCooldown <= 0
                && this.getTarget() != null
                && this.getTarget().isAlive()
                && this.distanceToSqr(
                        this.getTarget()
                ) <= POWER_UP_MAX_RANGE_SQR) {
            beginPowerUp();
        }
    }

    /**
     * The only interruption window.
     *
     * Damage while Frenzy is already active deliberately does NOT remove the
     * buff.
     */
    @Override
    public boolean hurtServer(
            ServerLevel level,
            DamageSource source,
            float damage
    ) {
        boolean wasHurt =
                super.hurtServer(
                        level,
                        source,
                        damage
                );

        if (wasHurt && isPoweringUp()) {
            interruptPowerUp();
        }

        return wasHurt;
    }

    private void beginPowerUp() {
        powerUpTicks =
                POWER_UP_TICKS;

        this.playSound(
                SoundEvents.PILLAGER_CELEBRATE,
                1.0F,
                0.8F
        );
    }

    private void interruptPowerUp() {
        powerUpTicks = 0;
        frenzyCooldown =
                INTERRUPTED_COOLDOWN_TICKS;
    }

    private void beginFrenzy(
            ServerLevel level
    ) {
        frenzyTicks =
                FRENZY_DURATION_TICKS;

        AttributeInstance speed =
                this.getAttribute(
                        Attributes.MOVEMENT_SPEED
                );

        if (speed != null) {
            speed.removeModifier(
                    FRENZY_SPEED_ID
            );

            speed.addTransientModifier(
                    FRENZY_SPEED
            );
        }

        level.sendParticles(
                ParticleTypes.ANGRY_VILLAGER,
                this.getX(),
                this.getY()
                        + this.getBbHeight()
                        * 0.75,
                this.getZ(),
                18,
                0.45,
                0.55,
                0.45,
                0.03
        );
    }

    private void endFrenzy() {
        AttributeInstance speed =
                this.getAttribute(
                        Attributes.MOVEMENT_SPEED
                );

        if (speed != null) {
            speed.removeModifier(
                    FRENZY_SPEED_ID
            );
        }
    }

    @Override
    protected void populateDefaultEquipmentSlots(
            RandomSource random,
            DifficultyInstance difficulty
    ) {
        this.setItemSlot(
                EquipmentSlot.MAINHAND,
                new ItemStack(Items.IRON_AXE)
        );

        this.setDropChance(
                EquipmentSlot.MAINHAND,
                0.0F
        );
    }

    @Override
    protected void populateDefaultEquipmentEnchantments(
            ServerLevelAccessor level,
            RandomSource random,
            DifficultyInstance localDifficulty
    ) {
        // No random spawned-weapon enchantments.
    }
}
