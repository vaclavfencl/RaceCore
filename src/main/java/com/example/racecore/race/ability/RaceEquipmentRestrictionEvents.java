package com.example.racecore.race.ability;

import com.example.racecore.RaceCore;
import com.example.racecore.armor.ArmorDefinition;
import com.example.racecore.armor.ArmorManager;
import com.example.racecore.armor.ArmorWeight;
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
    public static void onEquipmentChanged(LivingEquipmentChangeEvent event) {
        if (!(event.getEntity() instanceof Player player)) return;
        ItemStack equipped = event.getTo();
        if (equipped.isEmpty()) return;
        RaceDefinition race = RaceManager.getRace(player);
        EquipmentSlot slot = event.getSlot();
        String reason = getDisallowedReason(race, slot, equipped);
        if (reason == null) return;
        ItemStack rejected = equipped.copy();
        player.setItemSlot(slot, ItemStack.EMPTY);
        boolean fullyReturned = player.getInventory().add(rejected);
        if (!fullyReturned && !rejected.isEmpty()) {
            player.level().addFreshEntity(new ItemEntity(player.level(), player.getX(), player.getY(), player.getZ(), rejected));
        }
        player.sendOverlayMessage(Component.literal(reason));
    }

    private static String getDisallowedReason(RaceDefinition race, EquipmentSlot slot, ItemStack stack) {
        if (slot == EquipmentSlot.FEET && !race.canWearBoots()) return "Your race cannot wear boots.";
        ArmorDefinition armor = ArmorManager.getDefinition(stack);
        if (armor == null) return null;
        ArmorWeight weight = armor.weight();
        boolean allowed = switch (weight) {
            case LIGHT -> race.canWearLightArmor();
            case MEDIUM -> race.canWearMediumArmor();
            case HEAVY, ULTRA_HEAVY -> race.canWearHeavyArmor();
        };
        if (allowed) return null;
        return "Your race cannot wear " + weight.name().toLowerCase().replace('_', ' ') + " armor.";
    }
}
