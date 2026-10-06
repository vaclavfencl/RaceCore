package com.example.racecore.durability;

import com.example.racecore.RaceCore;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingEquipmentChangeEvent;
import net.neoforged.neoforge.event.entity.player.AttackEntityEvent;
import net.neoforged.neoforge.event.entity.player.ItemTooltipEvent;
import net.neoforged.neoforge.event.entity.player.PlayerDestroyItemEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

@EventBusSubscriber(modid = RaceCore.MODID)
public final class BrokenItemEvents {
    private BrokenItemEvents() {
    }

    @SubscribeEvent
    public static void onPlayerTick(
            PlayerTickEvent.Post event
    ) {
        Player player =
                event.getEntity();

        if (player.level().isClientSide()) {
            return;
        }

        for (int slot = 0;
             slot < player.getInventory()
                     .getContainerSize();
             slot++) {

            markIfNeeded(
                    player.getInventory()
                            .getItem(slot)
            );
        }

        markIfNeeded(
                player.getMainHandItem()
        );
        markIfNeeded(
                player.getOffhandItem()
        );

        markIfNeeded(
                player.getItemBySlot(
                        EquipmentSlot.HEAD
                )
        );
        markIfNeeded(
                player.getItemBySlot(
                        EquipmentSlot.CHEST
                )
        );
        markIfNeeded(
                player.getItemBySlot(
                        EquipmentSlot.LEGS
                )
        );
        markIfNeeded(
                player.getItemBySlot(
                        EquipmentSlot.FEET
                )
        );
    }

    private static void markIfNeeded(
            ItemStack stack
    ) {
        if (BrokenItemManager
                .shouldBecomeBroken(stack)) {

            BrokenItemManager
                    .markBroken(stack);
        }
    }

    @SubscribeEvent
    public static void onDestroyedItem(
            PlayerDestroyItemEvent event
    ) {
        ItemStack original =
                event.getOriginal();

        if (!BrokenItemManager
                .isSupported(original)
                || BrokenItemManager
                .isBroken(original)) {
            return;
        }

        ItemStack broken =
                BrokenItemManager
                        .brokenCopy(original);

        if (event.getHand() != null
                && event.getEntity()
                .getItemInHand(
                        event.getHand()
                )
                .isEmpty()) {

            event.getEntity()
                    .setItemInHand(
                            event.getHand(),
                            broken
                    );

            return;
        }

        returnToPlayer(
                event.getEntity(),
                broken
        );
    }

    @SubscribeEvent
    public static void onEquipmentChanged(
            LivingEquipmentChangeEvent event
    ) {
        if (!(event.getEntity()
                instanceof Player player)) {
            return;
        }

        if (!event.getTo().isEmpty()) {
            return;
        }

        ItemStack previous =
                event.getFrom();

        if (!BrokenItemManager
                .isSupported(previous)
                || BrokenItemManager
                .isBroken(previous)
                || previous.getMaxDamage() <= 0
                || previous.getDamageValue()
                < previous.getMaxDamage() - 1) {
            return;
        }

        returnToPlayer(
                player,
                BrokenItemManager
                        .brokenCopy(previous)
        );
    }

    private static void returnToPlayer(
            Player player,
            ItemStack stack
    ) {
        ItemStack returned =
                stack.copy();

        boolean inserted =
                player.getInventory()
                        .add(returned);

        if (!inserted
                && !returned.isEmpty()) {

            player.level()
                    .addFreshEntity(
                            new ItemEntity(
                                    player.level(),
                                    player.getX(),
                                    player.getY(),
                                    player.getZ(),
                                    returned
                            )
                    );
        }
    }

    @SubscribeEvent
    public static void onBreakSpeed(
            PlayerEvent.BreakSpeed event
    ) {
        if (BrokenItemManager.isBroken(
                event.getEntity()
                        .getMainHandItem()
        )) {
            event.setNewSpeed(0.0F);
        }
    }

    @SubscribeEvent
    public static void onAttackEntity(
            AttackEntityEvent event
    ) {
        if (BrokenItemManager.isBroken(
                event.getEntity()
                        .getMainHandItem()
        )) {

            event.setCanceled(true);
            warnBroken(event.getEntity());
        }
    }

    @SubscribeEvent
    public static void onLeftClickBlock(
            PlayerInteractEvent.LeftClickBlock event
    ) {
        if (BrokenItemManager.isBroken(
                event.getItemStack()
        )) {
            event.setCanceled(true);
            warnBroken(event.getEntity());
        }
    }

    @SubscribeEvent
    public static void onRightClickItem(
            PlayerInteractEvent.RightClickItem event
    ) {
        if (BrokenItemManager.isBroken(
                event.getItemStack()
        )) {

            event.setCanceled(true);
            event.setCancellationResult(
                    InteractionResult.FAIL
            );
            warnBroken(event.getEntity());
        }
    }

    @SubscribeEvent
    public static void onRightClickBlock(
            PlayerInteractEvent.RightClickBlock event
    ) {
        if (BrokenItemManager.isBroken(
                event.getItemStack()
        )) {

            event.setCanceled(true);
            event.setCancellationResult(
                    InteractionResult.FAIL
            );
            warnBroken(event.getEntity());
        }
    }

    @SubscribeEvent
    public static void onEntityInteract(
            PlayerInteractEvent.EntityInteract event
    ) {
        if (BrokenItemManager.isBroken(
                event.getItemStack()
        )) {

            event.setCanceled(true);
            event.setCancellationResult(
                    InteractionResult.FAIL
            );
            warnBroken(event.getEntity());
        }
    }

    private static void warnBroken(
            Player player
    ) {
        if (!player.level().isClientSide()) {
            player.sendOverlayMessage(
                    Component.translatable(
                            "message.racecore.item.broken"
                    )
            );
        }
    }

    @EventBusSubscriber(
            modid = RaceCore.MODID,
            value = Dist.CLIENT
    )
    public static final class Client {
        private Client() {
        }

        @SubscribeEvent
        public static void onTooltip(
                ItemTooltipEvent event
        ) {
            if (!BrokenItemManager.isBroken(
                    event.getItemStack()
            )) {
                return;
            }

            event.getToolTip().add(
                    Component.translatable(
                                    "tooltip.racecore.item.broken"
                            )
                            .withStyle(
                                    ChatFormatting.RED
                            )
            );
        }
    }
}
