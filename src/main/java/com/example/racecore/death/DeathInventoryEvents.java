package com.example.racecore.death;

import com.example.racecore.RaceCore;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.List;

@EventBusSubscriber(modid = RaceCore.MODID)
public final class DeathInventoryEvents {
    private static final int HOTBAR_SIZE = 9;
    private DeathInventoryEvents() {
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onPlayerDeath(
            LivingDeathEvent event
    ) {
        if (!(event.getEntity()
                instanceof ServerPlayer player)) {
            return;
        }

        DeathInventorySettings settings =
                DeathInventoryRegistry.settings();

        if (!settings.enabled()) {
            return;
        }

        Inventory inventory =
                player.getInventory();

        int inventorySize =
                inventory.getContainerSize();

        List<ItemStack> original =
                copyInventory(inventory);

        List<ItemStack> chestContents =
                copyChestSection(original);

        boolean hasChestContents =
                containsAnyItem(chestContents);

        boolean chestPlaced =
                !hasChestContents
                        || placeDeathChest(
                                player,
                                chestContents,
                                settings
                        );

        List<ItemStack> restore =
                new ArrayList<>(
                        inventorySize
                );

        for (int slot = 0;
             slot < inventorySize;
             slot++) {

            ItemStack originalStack =
                    original.get(slot);

            boolean keptSlot =
                    isKeptSlot(slot);

            if (keptSlot || !chestPlaced) {
                restore.add(
                        originalStack.copy()
                );
            } else {
                restore.add(
                        ItemStack.EMPTY
                );
            }
        }

        player.setData(
                DeathAttachments.DEATH_INVENTORY,
                new DeathInventorySnapshot(
                        restore
                )
        );

        clearInventory(inventory);

        if (!chestPlaced) {
            RaceCore.LOGGER.warn(
                    "Could not place a safe death chest for {} at {}. "
                            + "Full inventory will be restored on respawn.",
                    player.getGameProfile().name(),
                    player.blockPosition()
            );
        }
    }

    @SubscribeEvent
    public static void onPlayerRespawn(
            PlayerEvent.PlayerRespawnEvent event
    ) {
        if (!(event.getEntity()
                instanceof ServerPlayer player)) {
            return;
        }

        DeathInventorySnapshot snapshot =
                player.getData(
                        DeathAttachments.DEATH_INVENTORY
                );

        if (snapshot.isEmpty()) {
            return;
        }

        Inventory inventory =
                player.getInventory();

        List<ItemStack> restore =
                snapshot.copyStacks();

        int max =
                Math.min(
                        restore.size(),
                        inventory.getContainerSize()
                );

        for (int slot = 0;
             slot < max;
             slot++) {

            ItemStack stack =
                    restore.get(slot);

            if (!stack.isEmpty()) {
                inventory.setItem(
                        slot,
                        stack.copy()
                );
            }
        }

        player.setData(
                DeathAttachments.DEATH_INVENTORY,
                DeathInventorySnapshot.empty()
        );

        player.containerMenu.broadcastChanges();
    }

    private static List<ItemStack> copyInventory(
            Inventory inventory
    ) {
        List<ItemStack> copied =
                new ArrayList<>(
                        inventory.getContainerSize()
                );

        for (int slot = 0;
             slot < inventory.getContainerSize();
             slot++) {

            copied.add(
                    inventory.getItem(slot).copy()
            );
        }

        return copied;
    }

    private static List<ItemStack> copyChestSection(
            List<ItemStack> original
    ) {
        List<ItemStack> chest =
                new ArrayList<>(27);

        for (int slot = HOTBAR_SIZE;
             slot < Inventory.INVENTORY_SIZE;
             slot++) {

            chest.add(
                    original.get(slot).copy()
            );
        }

        return chest;
    }

    private static boolean isKeptSlot(
            int slot
    ) {
        return slot < HOTBAR_SIZE
                || slot >= Inventory.INVENTORY_SIZE;
    }

    private static void clearInventory(
            Inventory inventory
    ) {
        for (int slot = 0;
             slot < inventory.getContainerSize();
             slot++) {

            inventory.setItem(
                    slot,
                    ItemStack.EMPTY
            );
        }

        inventory.setChanged();
    }

    private static boolean containsAnyItem(
            List<ItemStack> stacks
    ) {
        for (ItemStack stack : stacks) {
            if (!stack.isEmpty()) {
                return true;
            }
        }

        return false;
    }

    private static boolean placeDeathChest(
            ServerPlayer player,
            List<ItemStack> chestContents,
            DeathInventorySettings settings
    ) {
        if (!(player.level()
                instanceof ServerLevel level)) {
            return false;
        }

        BlockPos chestPos =
                findSafeChestPosition(
                        level,
                        player.blockPosition(),
                        settings
                );

        if (chestPos == null) {
            return false;
        }

        if (!level.setBlock(
                chestPos,
                Blocks.CHEST.defaultBlockState(),
                3
        )) {
            return false;
        }

        if (!(level.getBlockEntity(chestPos)
                instanceof ChestBlockEntity chest)) {

            level.removeBlock(
                    chestPos,
                    false
            );

            return false;
        }

        for (int slot = 0;
             slot < chestContents.size();
             slot++) {

            ItemStack stack =
                    chestContents.get(slot);

            if (!stack.isEmpty()) {
                chest.setItem(
                        slot,
                        stack.copy()
                );
            }
        }

        chest.setChanged();

        RaceCore.LOGGER.info(
                "Placed death chest for {} at {} in {}.",
                player.getGameProfile().name(),
                chestPos,
                level.dimension().identifier()
        );

        return true;
    }

    private static BlockPos findSafeChestPosition(
            ServerLevel level,
            BlockPos origin,
            DeathInventorySettings settings
    ) {
        int horizontal =
                settings.horizontalSearchRadius();

        int vertical =
                settings.verticalSearchRadius();

        for (int verticalDistance = 0;
             verticalDistance <= vertical;
             verticalDistance++) {

            if (verticalDistance == 0) {
                BlockPos result =
                        searchHorizontalLayer(
                                level,
                                origin,
                                0,
                                horizontal,
                                settings
                        );

                if (result != null) {
                    return result;
                }

                continue;
            }

            BlockPos above =
                    searchHorizontalLayer(
                            level,
                            origin,
                            verticalDistance,
                            horizontal,
                            settings
                    );

            if (above != null) {
                return above;
            }

            BlockPos below =
                    searchHorizontalLayer(
                            level,
                            origin,
                            -verticalDistance,
                            horizontal,
                            settings
                    );

            if (below != null) {
                return below;
            }
        }

        return null;
    }

    private static BlockPos searchHorizontalLayer(
            ServerLevel level,
            BlockPos origin,
            int yOffset,
            int maxRadius,
            DeathInventorySettings settings
    ) {
        for (int radius = 0;
             radius <= maxRadius;
             radius++) {

            for (int dx = -radius;
                 dx <= radius;
                 dx++) {

                for (int dz = -radius;
                     dz <= radius;
                     dz++) {

                    if (radius > 0
                            && Math.abs(dx) != radius
                            && Math.abs(dz) != radius) {
                        continue;
                    }

                    BlockPos candidate =
                            origin.offset(
                                    dx,
                                    yOffset,
                                    dz
                            );

                    if (isSafeChestPosition(
                            level,
                            candidate,
                            settings
                    )) {
                        return candidate.immutable();
                    }
                }
            }
        }

        return null;
    }

    private static boolean isSafeChestPosition(
            ServerLevel level,
            BlockPos pos,
            DeathInventorySettings settings
    ) {
        if (!level.isInWorldBounds(pos)
                || !level.hasChunkAt(pos)) {
            return false;
        }

        if (!level.isEmptyBlock(pos)) {
            return false;
        }

        if (settings.requireAirAbove()) {
            BlockPos above =
                    pos.above();

            if (!level.isInWorldBounds(above)
                    || !level.isEmptyBlock(above)) {
                return false;
            }
        }

        if (settings.requireSupportBelow()) {
            BlockPos below =
                    pos.below();

            if (!level.isInWorldBounds(below)
                    || level.isEmptyBlock(below)) {
                return false;
            }
        }

        return true;
    }
}
