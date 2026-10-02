package com.example.racecore.armor;

import com.example.racecore.RaceCore;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.common.damagesource.DamageContainer;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;

/**
 * Replaces Minecraft's normal armor/toughness mitigation with RaceCore armor math.
 *
 * This applies to every LivingEntity wearing at least one armor piece that
 * RaceCore recognizes in armor.json.
 *
 * Damage sources that bypass vanilla armor also bypass this system naturally,
 * because Minecraft never invokes the ARMOR reduction stage for them.
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

        /*
         * No mapped RaceCore armor:
         *
         * Leave vanilla reduction untouched.
         *
         * This preserves natural armor on entities such as mobs and avoids
         * unexpectedly breaking unknown/modded armor before it has a RaceCore
         * definition.
         */
        if (stats.mappedPieces() <= 0) {
            return;
        }

        double reduction =
                ArmorFormula.damageReduction(
                        stats.totalArmor()
                );

        /*
         * Replace vanilla ARMOR/toughness reduction.
         *
         * The callback receives vanilla's calculated reduction amount, but we
         * intentionally ignore it and return RaceCore's reduction amount
         * instead.
         *
         * container.getNewDamage() is the damage amount reaching the armor
         * stage before armor reduction is subtracted.
         */
        event.addReductionModifier(
                DamageContainer.Reduction.ARMOR,
                (container, vanillaReduction) ->
                        (float) (
                                container.getNewDamage()
                                        * reduction
                        )
        );

        /*
         * Step 3E will implement RaceCore Protection.
         *
         * Until then, suppress vanilla armor-enchantment damage reduction for
         * entities using RaceCore armor so Protection does not stack on top of
         * our new formula behind the scenes.
         *
         * This affects damage-reduction enchantments only. It does not disable
         * things such as Unbreaking, Mending or Thorns behavior.
         */
        event.addReductionModifier(
                DamageContainer.Reduction.ENCHANTMENTS,
                (container, vanillaReduction) -> 0.0F
        );
    }
}
