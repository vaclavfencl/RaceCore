package com.example.racecore.armor;

import com.example.racecore.durability.BrokenItemManager;
import com.example.racecore.race.RaceDefinition;
import com.example.racecore.race.RaceManager;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.ItemEnchantments;

public final class ProtectionManager {
    private static final EquipmentSlot[] ARMOR_SLOTS = {
            EquipmentSlot.HEAD,
            EquipmentSlot.CHEST,
            EquipmentSlot.LEGS,
            EquipmentSlot.FEET
    };

    private ProtectionManager() {}

    public static int getCapacityPerPiece(
            LivingEntity entity
    ) {
        int defaultCapacity =
                ProtectionRegistry.settings()
                        .defaultCapacityPerPiece();

        if (entity instanceof Player player) {
            RaceDefinition race =
                    RaceManager.getRace(player);

            if (race.enchantmentCapacity() > 0) {
                return race.enchantmentCapacity();
            }
        }

        return defaultCapacity;
    }

    public static int getCapacityForPiece(
            LivingEntity entity,
            ItemStack stack
    ) {
        return getCapacityPerPiece(entity);
    }

    public static double getTotalProtection(
            LivingEntity entity,
            ProtectionType damageType
    ) {
        if (entity == null
                || damageType == null) {

            return 0.0;
        }

        int capacityPerPiece =
                getCapacityPerPiece(entity);

        double total = 0.0;

        for (EquipmentSlot slot : ARMOR_SLOTS) {
            ItemStack stack =
                    entity.getItemBySlot(slot);

            if (stack.isEmpty()
                    || BrokenItemManager
                    .isBroken(stack)) {

                continue;
            }

            total += Math.min(
                    capacityPerPiece,
                    getProtectionForSinglePiece(
                            stack,
                            damageType
                    )
            );
        }

        return total;
    }

    private static double getProtectionForSinglePiece(
            ItemStack stack,
            ProtectionType damageType
    ) {
        ItemEnchantments enchantments =
                stack.getOrDefault(
                        DataComponents.ENCHANTMENTS,
                        ItemEnchantments.EMPTY
                );

        double total = 0.0;

        for (Object2IntMap.Entry<Holder<Enchantment>> entry
                : enchantments.entrySet()) {

            Identifier enchantmentId =
                    entry.getKey()
                            .unwrapKey()
                            .map(key -> key.identifier())
                            .orElse(null);

            if (enchantmentId == null) {
                continue;
            }

            ProtectionEnchantmentDefinition definition =
                    ProtectionRegistry.get(
                            enchantmentId.toString()
                    );

            if (definition == null
                    || definition.type()
                    != damageType) {

                continue;
            }

            total += definition.pointsPerLevel()
                    * entry.getIntValue();
        }

        return total;
    }
}
