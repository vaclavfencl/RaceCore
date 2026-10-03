package com.example.racecore.armor;

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

/**
 * Computes RaceCore protection totals from equipped armor enchantments.
 */
public final class ProtectionManager {
    private static final EquipmentSlot[] ARMOR_SLOTS = {
            EquipmentSlot.HEAD,
            EquipmentSlot.CHEST,
            EquipmentSlot.LEGS,
            EquipmentSlot.FEET
    };

    private ProtectionManager() {
    }

    /**
     * Players use their race-specific capacity from races.json.
     *
     * Non-player living entities use the general default from protection.json.
     *
     * A race value <= 0 falls back to the general default, which keeps the
     * system safe while editing/migrating race JSON.
     */
    public static int getCapacityPerPiece(LivingEntity entity) {
        int defaultCapacity =
                ProtectionRegistry.settings().defaultCapacityPerPiece();

        if (entity instanceof Player player) {
            RaceDefinition race =
                    RaceManager.getRace(player);

            if (race.enchantmentCapacity() > 0) {
                return race.enchantmentCapacity();
            }
        }

        return defaultCapacity;
    }

    public static double getTotalProtection(
            LivingEntity entity,
            ProtectionType damageType
    ) {
        if (entity == null || damageType == null) {
            return 0.0;
        }

        int capacityPerPiece =
                getCapacityPerPiece(entity);

        double total = 0.0;

        for (EquipmentSlot slot : ARMOR_SLOTS) {
            ItemStack stack = entity.getItemBySlot(slot);

            if (stack.isEmpty()) {
                continue;
            }

            total += Math.min(
                    capacityPerPiece,
                    getProtectionForSinglePiece(stack, damageType)
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

            /*
             * Minecraft/NeoForge 26.2:
             * Enchantments are dynamic-registry entries, so use the Holder's
             * ResourceKey directly. ResourceKey uses identifier() in 26.2.
             */
            Identifier enchantmentId =
                    entry.getKey()
                            .unwrapKey()
                            .map(key -> key.identifier())
                            .orElse(null);

            if (enchantmentId == null) {
                continue;
            }

            ProtectionEnchantmentDefinition definition =
                    ProtectionRegistry.get(enchantmentId.toString());

            if (definition == null) {
                continue;
            }

            double effectiveness;

            if (definition.type() == ProtectionType.UNIVERSAL) {
                /*
                 * Generic Protection is fully effective against melee, but
                 * only partially effective against typed damage.
                 */
                if (damageType == ProtectionType.MELEE) {
                    effectiveness = 1.0;
                } else {
                    effectiveness =
                            ProtectionRegistry.settings()
                                    .universalTypedEffectiveness();
                }

            } else if (definition.type() == damageType) {
                // Specialized Protection is fully effective.
                effectiveness = 1.0;

            } else {
                // This enchantment does not apply to this damage category.
                continue;
            }

            total += definition.pointsPerLevel()
                    * entry.getIntValue()
                    * effectiveness;
        }

        return total;
    }
}
