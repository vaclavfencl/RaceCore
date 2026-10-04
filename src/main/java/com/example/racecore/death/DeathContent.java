package com.example.racecore.death;

import com.example.racecore.RaceCore;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntityTypes;
import net.minecraft.world.level.material.PushReaction;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.event.BlockEntityTypeAddBlocksEvent;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class DeathContent {
    public static final DeferredRegister.Blocks BLOCKS =
            DeferredRegister.createBlocks(RaceCore.MODID);

    public static final DeferredBlock<DeathBarrelBlock> DEATH_BARREL =
            BLOCKS.registerBlock(
                    "death_barrel",
                    DeathBarrelBlock::new,
                    () -> Block.Properties.ofFullCopy(Blocks.BARREL)
                            .strength(2.5F, 3_600_000.0F)
                            .pushReaction(PushReaction.IMMOVEABLE)
                            .noLootTable()
            );

    private DeathContent() {}

    public static void register(IEventBus modBus) {
        BLOCKS.register(modBus);
        modBus.addListener(DeathContent::addBarrelBlockEntitySupport);
    }

    private static void addBarrelBlockEntitySupport(
            BlockEntityTypeAddBlocksEvent event
    ) {
        event.modify(BlockEntityTypes.BARREL, DEATH_BARREL.get());
    }
}
