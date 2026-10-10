package com.example.racecore.mob.entity;

import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.util.RandomSource;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.goal.ZombieAttackGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.zombie.Zombie;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import org.jspecify.annotations.Nullable;

/**
 * First-pass Kobold.
 *
 * Placeholder visual: vanilla baby Zombie.
 * Variants are communicated only through held melee equipment.
 * No special combat ability is attached to the Kobold itself.
 */
public final class KoboldEntity extends Zombie {
    public KoboldEntity(
            EntityType<? extends KoboldEntity> type,
            Level level
    ) {
        super(type, level);
        this.setBaby(true);
        this.xpReward = 5;
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(
                0,
                new FloatGoal(this)
        );

        this.goalSelector.addGoal(
                2,
                new ZombieAttackGoal(
                        this,
                        1.0,
                        false
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
                        8.0F
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
        return Zombie.createAttributes()
                .add(Attributes.MAX_HEALTH, 70.0)
                .add(Attributes.ATTACK_DAMAGE, 6.0)
                .add(Attributes.MOVEMENT_SPEED, 0.18)
                .add(Attributes.FOLLOW_RANGE, 32.0)
                .add(
                        Attributes.SPAWN_REINFORCEMENTS_CHANCE,
                        0.0
                );
    }

    @Override
    protected boolean isSunSensitive() {
        return false;
    }

    @Override
    protected boolean convertsInWater() {
        return false;
    }

    /**
     * Remove Zombie's random spawn-stat modifiers/reinforcement roll.
     * Variant randomness is restricted to visible equipment.
     */
    @Override
    protected void handleAttributes(
            float difficultyModifier,
            EntitySpawnReason spawnReason
    ) {
    }

    @Override
    protected void populateDefaultEquipmentSlots(
            RandomSource random,
            DifficultyInstance difficulty
    ) {
        ItemStack weapon =
                switch (random.nextInt(3)) {
                    case 0 -> new ItemStack(Items.DIAMOND_PICKAXE);
                    case 1 -> new ItemStack(Items.IRON_SWORD);
                    default -> new ItemStack(Items.IRON_AXE);
                };

        this.setItemSlot(
                EquipmentSlot.MAINHAND,
                weapon
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
        // Equipment is deterministic once the visible variant is chosen.
    }

    @Override
    public @Nullable SpawnGroupData finalizeSpawn(
            ServerLevelAccessor level,
            DifficultyInstance difficulty,
            EntitySpawnReason spawnReason,
            @Nullable SpawnGroupData groupData
    ) {
        SpawnGroupData result =
                super.finalizeSpawn(
                        level,
                        difficulty,
                        spawnReason,
                        groupData
                );

        this.setBaby(true);
        this.setCanPickUpLoot(false);
        this.setCanBreakDoors(false);

        return result;
    }
}
