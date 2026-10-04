package com.example.racecore.death;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.Containers;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BarrelBlock;
import net.minecraft.world.level.block.entity.BarrelBlockEntity;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;

public final class DeathBarrelBlock extends BarrelBlock {
    public DeathBarrelBlock(BlockBehaviour.Properties properties) {
        super(properties);
    }

    @Override
    public boolean isFlammable(
            BlockState state,
            BlockGetter level,
            BlockPos pos,
            Direction direction
    ) {
        return false;
    }

    @Override
    public boolean ignitedByLava(
            BlockState state,
            BlockGetter level,
            BlockPos pos,
            Direction direction
    ) {
        return false;
    }

    @Override
    public BlockState playerWillDestroy(
            Level level,
            BlockPos pos,
            BlockState state,
            Player player
    ) {
        if (!level.isClientSide()
                && level.getBlockEntity(pos) instanceof BarrelBlockEntity barrel) {
            Containers.dropContents(level, pos, barrel);
            barrel.clearContent();
            barrel.setChanged();
        }

        return super.playerWillDestroy(level, pos, state, player);
    }
}
