package com.example.racecore.enchanting;

import com.example.racecore.RaceCore;
import com.example.racecore.armor.ArmorManager;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.Enchantments;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.AnvilUpdateEvent;

import java.util.List;

/**
 * Extends all four vanilla armor Protection enchantments to level V via anvil:
 *
 * - Protection IV + Protection IV -> Protection V
 * - Projectile Protection IV + IV -> V
 * - Fire Protection IV + IV -> V
 * - Blast Protection IV + IV -> V
 *
 * Works for both enchanted books and RaceCore-mapped armor.
 *
 * The actual level 5 enchantment is stored on the resulting ItemStack, so the
 * normal vanilla tooltip renders "Protection V", etc.
 */
@EventBusSubscriber(modid = RaceCore.MODID)
public final class ProtectionVAnvilEvents {
    private static final int LEVEL_IV = 4;
    private static final int LEVEL_V = 5;

    private static final List<ResourceKey<Enchantment>> SUPPORTED = List.of(
            Enchantments.PROTECTION,
            Enchantments.PROJECTILE_PROTECTION,
            Enchantments.FIRE_PROTECTION,
            Enchantments.BLAST_PROTECTION
    );

    private ProtectionVAnvilEvents() {
    }

    @SubscribeEvent
    public static void onAnvilUpdate(AnvilUpdateEvent event) {
        ItemStack left = event.getLeft();
        ItemStack right = event.getRight();

        if (left.isEmpty() || right.isEmpty()) {
            return;
        }

        HolderLookup.RegistryLookup<Enchantment> enchantments =
                event.getPlayer()
                        .level()
                        .registryAccess()
                        .lookupOrThrow(Registries.ENCHANTMENT);

        boolean leftIsBook = left.is(Items.ENCHANTED_BOOK);
        boolean rightIsBook = right.is(Items.ENCHANTED_BOOK);

        for (ResourceKey<Enchantment> enchantmentKey : SUPPORTED) {
            Holder.Reference<Enchantment> enchantment =
                    enchantments.getOrThrow(enchantmentKey);

            int leftLevel =
                    EnchantmentHelper
                            .getEnchantmentsForCrafting(left)
                            .getLevel(enchantment);

            int rightLevel =
                    EnchantmentHelper
                            .getEnchantmentsForCrafting(right)
                            .getLevel(enchantment);

            /*
             * Book IV + same Book IV -> Book V
             */
            if (leftIsBook
                    && rightIsBook
                    && leftLevel == LEVEL_IV
                    && rightLevel == LEVEL_IV) {

                ItemStack output =
                        chooseOutput(event, left);

                setLevelV(output, enchantment);

                finishOperation(event, output);
                return;
            }

            /*
             * RaceCore armor IV + same Book IV/V -> Armor V
             *
             * Requiring the armor to already contain level IV avoids bypassing
             * vanilla compatibility rules by applying a completely new
             * protection type through this custom event.
             */
            if (ArmorManager.getDefinition(left) != null
                    && rightIsBook
                    && leftLevel == LEVEL_IV
                    && (rightLevel == LEVEL_IV
                    || rightLevel == LEVEL_V)) {

                ItemStack output =
                        chooseOutput(event, left);

                setLevelV(output, enchantment);

                finishOperation(event, output);
                return;
            }
        }
    }

    private static ItemStack chooseOutput(
            AnvilUpdateEvent event,
            ItemStack fallback
    ) {
        ItemStack existing =
                event.getOutput();

        if (!existing.isEmpty()) {
            return existing.copy();
        }

        return fallback.copy();
    }

    private static void setLevelV(
            ItemStack stack,
            Holder<Enchantment> enchantment
    ) {
        EnchantmentHelper.updateEnchantments(
                stack,
                mutable ->
                        mutable.set(
                                enchantment,
                                LEVEL_V
                        )
        );
    }

    private static void finishOperation(
            AnvilUpdateEvent event,
            ItemStack output
    ) {
        event.setOutput(output);

        /*
         * Preserve a more expensive vanilla cost if one was already calculated.
         */
        event.setXpCost(
                Math.max(
                        5,
                        event.getXpCost()
                )
        );

        event.setMaterialCost(1);
    }
}
