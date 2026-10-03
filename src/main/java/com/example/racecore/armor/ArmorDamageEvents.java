package com.example.racecore.armor;

import com.example.racecore.RaceCore;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.common.damagesource.DamageContainer;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;

/**
 * Replaces Minecraft's normal armor/toughness mitigation with RaceCore armor math
 * and RaceCore Protection.
 */
@EventBusSubscriber(modid = RaceCore.MODID)
public final class ArmorDamageEvents {
    private ArmorDamageEvents() {
    }

    @SubscribeEvent
    public static void onIncomingDamage(
            LivingIncomingDamageEvent event
    ) {
        ArmorStats stats =
                ArmorManager.getStats(event.getEntity());

        // No mapped RaceCore armor: leave vanilla behavior untouched.
        if (stats.mappedPieces() <= 0) {
            return;
        }

        ProtectionType protectionType =
                ProtectionTypeResolver.resolve(event.getSource());

        double protection =
                ProtectionManager.getTotalProtection(
                        event.getEntity(),
                        protectionType
                );

        double reduction =
                ArmorFormula.damageReduction(
                        stats.totalArmor(),
                        protection
                );

        event.addReductionModifier(
                DamageContainer.Reduction.ARMOR,
                (container, vanillaReduction) ->
                        (float) (
                                container.getNewDamage()
                                        * reduction
                        )
        );

        // Suppress vanilla damage-reduction enchantments.
        event.addReductionModifier(
                DamageContainer.Reduction.ENCHANTMENTS,
                (container, vanillaReduction) -> 0.0F
        );
    }
}
