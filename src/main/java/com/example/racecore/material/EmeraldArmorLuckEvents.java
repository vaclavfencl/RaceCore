package com.example.racecore.material;

import com.example.racecore.RaceCore;
import com.example.racecore.durability.BrokenItemManager;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

/**
 * Each intact Emerald armor piece grants +1 generic Minecraft Luck.
 *
 * RaceCore's loot system already reads LootContext luck, so this naturally
 * stacks with Goblin racial Luck and any future Luck sources.
 */
@EventBusSubscriber(modid = RaceCore.MODID)
public final class EmeraldArmorLuckEvents {
    private static final Identifier EMERALD_ARMOR_LUCK_ID =
            Identifier.fromNamespaceAndPath(
                    RaceCore.MODID,
                    "emerald_armor_luck"
            );

    private static final String LAST_COUNT_KEY =
            "racecore:emerald_armor_luck_count";

    private static final EquipmentSlot[] ARMOR_SLOTS = {
            EquipmentSlot.HEAD,
            EquipmentSlot.CHEST,
            EquipmentSlot.LEGS,
            EquipmentSlot.FEET
    };

    private EmeraldArmorLuckEvents() {
    }

    @SubscribeEvent
    public static void onLogin(
            PlayerEvent.PlayerLoggedInEvent event
    ) {
        if (event.getEntity()
                instanceof ServerPlayer player) {

            sync(player, true);
        }
    }

    @SubscribeEvent
    public static void onRespawn(
            PlayerEvent.PlayerRespawnEvent event
    ) {
        if (event.getEntity()
                instanceof ServerPlayer player) {

            sync(player, true);
        }
    }

    @SubscribeEvent
    public static void onChangedDimension(
            PlayerEvent.PlayerChangedDimensionEvent event
    ) {
        if (event.getEntity()
                instanceof ServerPlayer player) {

            sync(player, true);
        }
    }

    @SubscribeEvent
    public static void onPlayerTick(
            PlayerTickEvent.Post event
    ) {
        Player player = event.getEntity();

        if (player.level().isClientSide()
                || player.tickCount % 20 != 0) {

            return;
        }

        sync(player, false);
    }

    private static void sync(
            Player player,
            boolean force
    ) {
        int count = countEmeraldArmor(player);

        int previous =
                player.getPersistentData()
                        .getIntOr(
                                LAST_COUNT_KEY,
                                -1
                        );

        if (!force && previous == count) {
            return;
        }

        AttributeInstance luck =
                player.getAttribute(
                        Attributes.LUCK
                );

        if (luck == null) {
            return;
        }

        luck.removeModifier(
                EMERALD_ARMOR_LUCK_ID
        );

        if (count > 0) {
            luck.addTransientModifier(
                    new AttributeModifier(
                            EMERALD_ARMOR_LUCK_ID,
                            count,
                            AttributeModifier.Operation.ADD_VALUE
                    )
            );
        }

        player.getPersistentData()
                .putInt(
                        LAST_COUNT_KEY,
                        count
                );
    }

    private static int countEmeraldArmor(
            Player player
    ) {
        int count = 0;

        for (EquipmentSlot slot : ARMOR_SLOTS) {
            ItemStack stack =
                    player.getItemBySlot(slot);

            if (isEmeraldArmor(stack)
                    && !BrokenItemManager
                    .isBroken(stack)) {

                count++;
            }
        }

        return count;
    }

    public static boolean isEmeraldArmor(
            ItemStack stack
    ) {
        return stack.is(
                        RaceCoreMaterials
                                .EMERALD_HELMET
                                .get()
                )
                || stack.is(
                        RaceCoreMaterials
                                .EMERALD_CHESTPLATE
                                .get()
                )
                || stack.is(
                        RaceCoreMaterials
                                .EMERALD_LEGGINGS
                                .get()
                )
                || stack.is(
                        RaceCoreMaterials
                                .EMERALD_BOOTS
                                .get()
                );
    }
}
