package com.example.racecore.mob.entity;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.animal.golem.IronGolem;
import net.minecraft.world.level.Level;

/**
 * Smaller Stone Sentinel summoned by the adult.
 *
 * It keeps melee + rock throwing, but can never summon another Sentinel.
 */
public final class LesserStoneSentinelEntity extends StoneSentinelEntity {
    public LesserStoneSentinelEntity(
            EntityType<? extends LesserStoneSentinelEntity> type,
            Level level
    ) {
        super(type, level);
        this.xpReward = 7;
    }

    public static AttributeSupplier.Builder createAttributes() {
        return IronGolem.createAttributes()
                .add(Attributes.MAX_HEALTH, 80.0)
                .add(Attributes.ATTACK_DAMAGE, 11.0)
                .add(Attributes.MOVEMENT_SPEED, 0.25)
                .add(Attributes.FOLLOW_RANGE, 32.0)
                .add(Attributes.ARMOR, 6.0)
                .add(
                        Attributes.KNOCKBACK_RESISTANCE,
                        0.45
                )
                .add(
                        Attributes.SCALE,
                        0.60
                );
    }

    @Override
    protected boolean canSummonLessers() {
        return false;
    }

    @Override
    protected boolean isLesserSentinel() {
        return true;
    }
}
