package com.example.racecore.enchanting;

import com.example.racecore.RaceCore;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.EnchantmentInstance;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.AnvilUpdateEvent;
import net.neoforged.neoforge.event.entity.player.AttackEntityEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEnchantItemEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;

import java.util.List;

@EventBusSubscriber(modid = RaceCore.MODID)
public final class WeaponToolEnchantingEvents {
    private WeaponToolEnchantingEvents() {
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onAnvilUpdate(
            AnvilUpdateEvent event
    ) {
        ItemStack output =
                event.getOutput();

        if (!WeaponToolEnchantingManager
                .isWeaponOrTool(output)) {
            return;
        }

        if (WeaponToolEnchantingManager
                .isOverCapacity(
                        event.getPlayer(),
                        output
                )) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void onPlayerEnchantItem(
            PlayerEnchantItemEvent event
    ) {
        ItemStack stack =
                event.getEnchantedItem();

        if (!WeaponToolEnchantingManager
                .isWeaponOrTool(stack)) {
            return;
        }

        trimPool(
                event,
                stack,
                true
        );

        trimPool(
                event,
                stack,
                false
        );
    }

    private static void trimPool(
            PlayerEnchantItemEvent event,
            ItemStack stack,
            boolean effective
    ) {
        int capacity =
                effective
                        ? WeaponToolEnchantingManager
                                .getEffectiveCapacity(
                                        event.getEntity(),
                                        stack
                                )
                        : WeaponToolEnchantingManager
                                .getUtilityCapacity(
                                        event.getEntity(),
                                        stack
                                );

        int used =
                effective
                        ? WeaponToolEnchantingManager
                                .getUsedEffectiveCapacity(
                                        stack
                                )
                        : WeaponToolEnchantingManager
                                .getUsedUtilityCapacity(
                                        stack
                                );

        int over =
                used - capacity;

        if (over <= 0) {
            return;
        }

        List<EnchantmentInstance> applied =
                event.getEnchantments();

        for (int i = applied.size() - 1;
             i >= 0 && over > 0;
             i--) {

            EnchantmentInstance instance =
                    applied.get(i);

            boolean belongsToPool =
                    effective
                            ? WeaponToolEnchantingManager
                                    .isEffectiveEnchantment(
                                            instance.enchantment()
                                    )
                            : WeaponToolEnchantingManager
                                    .isUtilityEnchantment(
                                            instance.enchantment()
                                    );

            if (!belongsToPool) {
                continue;
            }

            int currentLevel =
                    EnchantmentHelper
                            .getEnchantmentsForCrafting(
                                    stack
                            )
                            .getLevel(
                                    instance.enchantment()
                            );

            if (currentLevel <= 0) {
                continue;
            }

            int reduction =
                    Math.min(
                            over,
                            currentLevel
                    );

            int newLevel =
                    currentLevel - reduction;

            EnchantmentHelper.updateEnchantments(
                    stack,
                    mutable -> {
                        if (newLevel <= 0) {
                            mutable.removeIf(
                                    holder ->
                                            holder.equals(
                                                    instance.enchantment()
                                            )
                            );
                        } else {
                            mutable.set(
                                    instance.enchantment(),
                                    newLevel
                            );
                        }
                    }
            );

            over -= reduction;
        }
    }

    @SubscribeEvent
    public static void onBreakSpeed(
            PlayerEvent.BreakSpeed event
    ) {
        if (isHeldItemOverCap(
                event.getEntity()
        )) {
            event.setNewSpeed(0.0F);
        }
    }

    @SubscribeEvent
    public static void onAttackEntity(
            AttackEntityEvent event
    ) {
        if (isHeldItemOverCap(
                event.getEntity()
        )) {
            event.setCanceled(true);
            warn(event.getEntity());
        }
    }

    @SubscribeEvent
    public static void onLeftClickBlock(
            PlayerInteractEvent.LeftClickBlock event
    ) {
        if (WeaponToolEnchantingManager
                .isOverCapacity(
                        event.getEntity(),
                        event.getItemStack()
                )) {

            event.setCanceled(true);
            warn(event.getEntity());
        }
    }

    @SubscribeEvent
    public static void onRightClickItem(
            PlayerInteractEvent.RightClickItem event
    ) {
        if (WeaponToolEnchantingManager
                .isOverCapacity(
                        event.getEntity(),
                        event.getItemStack()
                )) {

            event.setCanceled(true);
            event.setCancellationResult(
                    InteractionResult.FAIL
            );
            warn(event.getEntity());
        }
    }

    @SubscribeEvent
    public static void onRightClickBlock(
            PlayerInteractEvent.RightClickBlock event
    ) {
        if (WeaponToolEnchantingManager
                .isOverCapacity(
                        event.getEntity(),
                        event.getItemStack()
                )) {

            event.setCanceled(true);
            event.setCancellationResult(
                    InteractionResult.FAIL
            );
            warn(event.getEntity());
        }
    }

    @SubscribeEvent
    public static void onEntityInteract(
            PlayerInteractEvent.EntityInteract event
    ) {
        if (WeaponToolEnchantingManager
                .isOverCapacity(
                        event.getEntity(),
                        event.getItemStack()
                )) {

            event.setCanceled(true);
            event.setCancellationResult(
                    InteractionResult.FAIL
            );
            warn(event.getEntity());
        }
    }

    private static boolean isHeldItemOverCap(
            Player player
    ) {
        return WeaponToolEnchantingManager
                .isOverCapacity(
                        player,
                        player.getMainHandItem()
                );
    }

    private static void warn(
            Player player
    ) {
        if (player.level().isClientSide()) {
            return;
        }

        ItemStack stack =
                player.getMainHandItem();

        player.sendOverlayMessage(
                Component.translatable(
                        "message.racecore.enchant.weapon_over_cap",
                        WeaponToolEnchantingManager
                                .getUsedEffectiveCapacity(
                                        stack
                                ),
                        WeaponToolEnchantingManager
                                .getEffectiveCapacity(
                                        player,
                                        stack
                                ),
                        WeaponToolEnchantingManager
                                .getUsedUtilityCapacity(
                                        stack
                                ),
                        WeaponToolEnchantingManager
                                .getUtilityCapacity(
                                        player,
                                        stack
                                )
                )
        );
    }
}
