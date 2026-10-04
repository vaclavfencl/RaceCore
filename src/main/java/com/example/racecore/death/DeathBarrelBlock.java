package com.example.racecore.death;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.Containers;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BarrelBlock;
import net.minecraft.world.level.block.entity.BarrelBlockEntity;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;

public final class DeathBarrelBlock extends BarrelBlock {
    /**
     * 20 ticks/sec * 60 sec = roughly one minute.
     */
    private static final int EMPTY_CHECK_INTERVAL_TICKS =
            20 * 60;

    public DeathBarrelBlock(
            BlockBehaviour.Properties properties
    ) {
        super(properties);
    }

    @Override
    protected void onPlace(
            BlockState state,
            Level level,
            BlockPos pos,
            BlockState oldState,
            boolean movedByPiston
    ) {
        super.onPlace(
                state,
                level,
                pos,
                oldState,
                movedByPiston
        );

        if (!level.isClientSide()
                && level instanceof ServerLevel serverLevel) {

            serverLevel.scheduleTick(
                    pos,
                    this,
                    EMPTY_CHECK_INTERVAL_TICKS
            );
        }
    }

    /**
     * Every ~60 seconds:
     * - empty -> delete the Death Barrel;
     * - non-empty -> schedule another check.
     *
     * This uses Minecraft's scheduled block ticks, so RaceCore does not scan
     * the world or all loaded block entities.
     */
    @Override
    protected void tick(
            BlockState state,
            ServerLevel level,
            BlockPos pos,
            RandomSource random
    ) {
        if (!(level.getBlockEntity(pos)
                instanceof BarrelBlockEntity barrel)) {

            return;
        }

        if (barrel.isEmpty()) {
            level.removeBlock(
                    pos,
                    false
            );
            return;
        }

        level.scheduleTick(
                pos,
                this,
                EMPTY_CHECK_INTERVAL_TICKS
        );
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

    /**
     * The block has no loot table, so the Death Barrel itself never drops.
     * Its inventory is explicitly dropped here before the block entity is
     * removed.
     */
    @Override
    public BlockState playerWillDestroy(
            Level level,
            BlockPos pos,
            BlockState state,
            Player player
    ) {
        if (!level.isClientSide()
                && level.getBlockEntity(pos)
                instanceof BarrelBlockEntity barrel) {

            Containers.dropContents(
                    level,
                    pos,
                    barrel
            );

            barrel.clearContent();
            barrel.setChanged();
        }

        return super.playerWillDestroy(
                level,
                pos,
                state,
                player
        );
    }
}
