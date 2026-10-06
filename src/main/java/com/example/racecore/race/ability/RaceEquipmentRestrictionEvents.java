package com.example.racecore.race.ability;

import com.example.racecore.RaceCore;
import com.example.racecore.armor.ArmorDefinition;
import com.example.racecore.armor.ArmorManager;
import com.example.racecore.armor.ArmorWeight;
import com.example.racecore.enchanting.UtilityEnchantingManager;
import com.example.racecore.race.RaceDefinition;
import com.example.racecore.race.RaceManager;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingEquipmentChangeEvent;

@EventBusSubscriber(modid = RaceCore.MODID)
public final class RaceEquipmentRestrictionEvents {
    private RaceEquipmentRestrictionEvents() {}

    @SubscribeEvent
    public static void onEquipmentChanged(
            LivingEquipmentChangeEvent event
    ) {
        if (!(event.getEntity() instanceof Player player)) {
            return;
        }

        ItemStack equipped = event.getTo();

        if (equipped.isEmpty()) {
            return;
        }

        RaceDefinition race =
                RaceManager.getRace(player);

        EquipmentSlot slot =
                event.getSlot();

        Component reason =
                getDisallowedReason(
                        player,
                        race,
                        slot,
                        equipped
                );

        if (reason == null) {
            return;
        }

        ItemStack rejected =
                equipped.copy();

        player.setItemSlot(
                slot,
                ItemStack.EMPTY
        );

        boolean fullyReturned =
                player.getInventory()
                        .add(rejected);

        if (!fullyReturned
                && !rejected.isEmpty()) {

            player.level()
                    .addFreshEntity(
                            new ItemEntity(
                                    player.level(),
                                    player.getX(),
                                    player.getY(),
                                    player.getZ(),
                                    rejected
                            )
                    );
        }

        player.sendOverlayMessage(reason);
    }

    private static Component getDisallowedReason(
            Player player,
            RaceDefinition race,
            EquipmentSlot slot,
            ItemStack stack
    ) {
        if (slot == EquipmentSlot.FEET
                && !race.canWearBoots()) {

            return Component.translatable(
                    "message.racecore.equipment.no_boots"
            );
        }

        ArmorDefinition armor =
                ArmorManager.getDefinition(stack);

        if (armor == null) {
            return null;
        }

        if (UtilityEnchantingManager
                .isOverCapacity(
                        player,
                        stack
                )) {

            return Component.translatable(
                    "message.racecore.equipment.utility_over_cap",
                    UtilityEnchantingManager
                            .getUsedCapacity(stack),
                    UtilityEnchantingManager
                            .getCapacity(
                                    player,
                                    stack
                            )
            );
        }

        ArmorWeight weight =
                armor.weight();

        boolean allowed =
                switch (weight) {
                    case LIGHT ->
                            race.canWearLightArmor();

                    case MEDIUM ->
                            race.canWearMediumArmor();

                    case HEAVY, ULTRA_HEAVY ->
                            race.canWearHeavyArmor();
                };

        if (allowed) {
            return null;
        }

        return Component.translatable(
                "message.racecore.equipment.weight_blocked",
                Component.translatable(
                        switch (weight) {
                            case LIGHT ->
                                    "tooltip.racecore.weight.light";
                            case MEDIUM ->
                                    "tooltip.racecore.weight.medium";
                            case HEAVY ->
                                    "tooltip.racecore.weight.heavy";
                            case ULTRA_HEAVY ->
                                    "tooltip.racecore.weight.ultra_heavy";
                        }
                )
        );
    }
}
