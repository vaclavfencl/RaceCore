package com.example.racecore.mob;

import com.example.racecore.RaceCore;
import com.example.racecore.mob.entity.KoboldEntity;
import com.example.racecore.mob.entity.LesserStoneSentinelEntity;
import com.example.racecore.mob.entity.MarauderEntity;
import com.example.racecore.mob.entity.RiverDrakeEntity;
import com.example.racecore.mob.entity.RockProjectileEntity;
import com.example.racecore.mob.entity.StoneSentinelEntity;
import net.minecraft.core.Direction;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.SpawnPlacementTypes;
import net.minecraft.world.level.levelgen.Heightmap;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.EntityAttributeCreationEvent;
import net.neoforged.neoforge.event.entity.RegisterSpawnPlacementsEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

@EventBusSubscriber(modid = RaceCore.MODID)
public final class RaceCoreMobs {
    public static final DeferredRegister.Entities ENTITY_TYPES =
            DeferredRegister.createEntities(RaceCore.MODID);

    public static final DeferredHolder<EntityType<?>, EntityType<KoboldEntity>> KOBOLD =
            ENTITY_TYPES.registerEntityType("kobold", KoboldEntity::new, MobCategory.MONSTER,
                    builder -> builder.sized(0.60F, 1.0F).clientTrackingRange(8).notInPeaceful());

    public static final DeferredHolder<EntityType<?>, EntityType<RiverDrakeEntity>> RIVER_DRAKE =
            ENTITY_TYPES.registerEntityType("river_drake", RiverDrakeEntity::new, MobCategory.MONSTER,
                    builder -> builder.sized(0.60F, 0.85F).clientTrackingRange(8).notInPeaceful());

    public static final DeferredHolder<EntityType<?>, EntityType<MarauderEntity>> MARAUDER =
            ENTITY_TYPES.registerEntityType("marauder", MarauderEntity::new, MobCategory.MONSTER,
                    builder -> builder.sized(0.60F, 1.95F).clientTrackingRange(8).notInPeaceful());

    public static final DeferredHolder<EntityType<?>, EntityType<StoneSentinelEntity>> STONE_SENTINEL =
            ENTITY_TYPES.registerEntityType("stone_sentinel", StoneSentinelEntity::new, MobCategory.MONSTER,
                    builder -> builder.sized(1.40F, 2.70F).clientTrackingRange(10).notInPeaceful());

    public static final DeferredHolder<EntityType<?>, EntityType<LesserStoneSentinelEntity>> LESSER_STONE_SENTINEL =
            ENTITY_TYPES.registerEntityType("lesser_stone_sentinel", LesserStoneSentinelEntity::new, MobCategory.MONSTER,
                    builder -> builder.sized(1.40F, 2.70F).clientTrackingRange(8).notInPeaceful());

    public static final DeferredHolder<EntityType<?>, EntityType<RockProjectileEntity>> ROCK_PROJECTILE =
            ENTITY_TYPES.registerEntityType("rock_projectile", RockProjectileEntity::new, MobCategory.MISC,
                    builder -> builder.sized(0.25F, 0.25F).clientTrackingRange(6));

    private RaceCoreMobs() {}

    public static void register(IEventBus modBus) {
        ENTITY_TYPES.register(modBus);
    }

    @SubscribeEvent
    public static void onRegisterSpawnPlacements(RegisterSpawnPlacementsEvent event) {
        event.register(
                RIVER_DRAKE.get(), SpawnPlacementTypes.IN_WATER,
                Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                (type, level, spawnReason, pos, random) ->
                        level.getFluidState(pos).is(FluidTags.WATER),
                RegisterSpawnPlacementsEvent.Operation.REPLACE
        );

        event.register(
                STONE_SENTINEL.get(), SpawnPlacementTypes.ON_GROUND,
                Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                (type, level, spawnReason, pos, random) ->
                        level.getFluidState(pos).isEmpty()
                                && level.getBlockState(pos.below()).isFaceSturdy(
                                        level, pos.below(), Direction.UP),
                RegisterSpawnPlacementsEvent.Operation.REPLACE
        );
    }

    @SubscribeEvent
    public static void onEntityAttributes(EntityAttributeCreationEvent event) {
        event.put(KOBOLD.get(), KoboldEntity.createAttributes().build());
        event.put(RIVER_DRAKE.get(), RiverDrakeEntity.createAttributes().build());
        event.put(MARAUDER.get(), MarauderEntity.createAttributes().build());
        event.put(STONE_SENTINEL.get(), StoneSentinelEntity.createAttributes().build());
        event.put(LESSER_STONE_SENTINEL.get(), LesserStoneSentinelEntity.createAttributes().build());
    }
}
