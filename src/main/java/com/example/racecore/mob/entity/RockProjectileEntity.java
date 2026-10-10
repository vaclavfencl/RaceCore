package com.example.racecore.mob.entity;

import com.example.racecore.mob.RaceCoreMobs;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.throwableitemprojectile.Snowball;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.EntityHitResult;

/**
 * Visible cobblestone projectile thrown by Stone Sentinels.
 */
public final class RockProjectileEntity extends Snowball {
    private static final float ADULT_DAMAGE =
            14.0F;

    private static final float LESSER_DAMAGE =
            7.0F;

    public RockProjectileEntity(
            EntityType<? extends RockProjectileEntity> type,
            Level level
    ) {
        super(type, level);
    }

    public RockProjectileEntity(
            ServerLevel level,
            LivingEntity owner
    ) {
        this(
                RaceCoreMobs.ROCK_PROJECTILE.get(),
                level
        );

        this.setOwner(owner);

        this.setPos(
                owner.getX(),
                owner.getEyeY() - 0.10,
                owner.getZ()
        );

        this.setItem(
                new ItemStack(
                        Items.COBBLESTONE
                )
        );
    }

    @Override
    protected Item getDefaultItem() {
        return Items.COBBLESTONE;
    }

    @Override
    protected void onHitEntity(
            EntityHitResult hitResult
    ) {
        Entity target =
                hitResult.getEntity();

        if (!(this.level()
                instanceof ServerLevel level)) {
            return;
        }

        Entity owner =
                this.getOwner();

        if (target == owner) {
            return;
        }

        float damage =
                owner instanceof LesserStoneSentinelEntity
                        ? LESSER_DAMAGE
                        : ADULT_DAMAGE;

        target.hurtServer(
                level,
                this.damageSources()
                        .thrown(
                                this,
                                owner
                        ),
                damage
        );
    }
}
