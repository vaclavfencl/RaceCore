package com.example.racecore.villager;

import com.example.racecore.RaceCore;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.MerchantMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.ItemEnchantments;
import net.minecraft.world.item.trading.ItemCost;
import net.minecraft.world.item.trading.MerchantOffer;
import net.minecraft.world.item.trading.MerchantOffers;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerContainerEvent;

import java.util.ListIterator;

/**
 * RaceCore villager economy enforcement.
 *
 * Trade inventories themselves are data-driven under data/racecore/villager_trade
 * plus vanilla trade-set/tag overrides under data/minecraft.
 *
 * This version intentionally leaves vanilla villager trade persistence,
 * restocking, profession locking and workstation behavior alone.
 */
@EventBusSubscriber(modid = RaceCore.MODID)
public final class VillagerEconomyEvents {
    private static final double VILLAGER_SEARCH_RADIUS = 6.0D;

    private VillagerEconomyEvents() {
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onMerchantMenuOpen(PlayerContainerEvent.Open event) {
        if (!(event.getContainer() instanceof MerchantMenu)) {
            return;
        }

        Player player = event.getEntity();
        if (!(player.level() instanceof ServerLevel level)) {
            return;
        }

        Villager villager = level.getEntitiesOfClass(
                        Villager.class,
                        player.getBoundingBox().inflate(VILLAGER_SEARCH_RADIUS),
                        candidate -> candidate.getTradingPlayer() == player
                )
                .stream()
                .findFirst()
                .orElse(null);

        if (villager == null) {
            return;
        }

        sanitizeOffers(villager.getOffers());
    }

    private static void sanitizeOffers(MerchantOffers offers) {
        ListIterator<MerchantOffer> iterator = offers.listIterator();

        while (iterator.hasNext()) {
            MerchantOffer offer = iterator.next();

            if (!isEmeraldOnlyPurchase(offer)
                    || isForbiddenProgressionResult(offer.getResult())) {
                iterator.remove();
                continue;
            }

            MerchantOffer fixedPrice = new MerchantOffer(
                    offer.getItemCostA(),
                    offer.getItemCostB(),
                    offer.getResult().copy(),
                    offer.getUses(),
                    offer.getMaxUses(),
                    offer.getXp(),
                    0.0F,
                    0
            );

            fixedPrice.resetSpecialPriceDiff();
            iterator.set(fixedPrice);
        }
    }

    private static boolean isEmeraldOnlyPurchase(MerchantOffer offer) {
        if (!isEmeraldCost(offer.getItemCostA())) {
            return false;
        }

        if (offer.getResult().is(Items.EMERALD)) {
            return false;
        }

        return offer.getItemCostB()
                .map(VillagerEconomyEvents::isEmeraldCost)
                .orElse(true);
    }

    private static boolean isEmeraldCost(ItemCost cost) {
        return cost.itemStack().is(Items.EMERALD);
    }

    private static boolean isForbiddenProgressionResult(ItemStack result) {
        if (result.is(Items.ENCHANTED_BOOK)) {
            return true;
        }

        ItemEnchantments enchantments = result.getOrDefault(
                DataComponents.ENCHANTMENTS,
                ItemEnchantments.EMPTY
        );

        if (!enchantments.isEmpty()) {
            return true;
        }

        Identifier id = BuiltInRegistries.ITEM.getKey(result.getItem());
        String path = id.getPath();

        return path.equals("diamond")
                || path.equals("netherite_ingot")
                || path.startsWith("diamond_")
                || path.startsWith("netherite_")
                || path.startsWith("steel_")
                || path.startsWith("mithril_");
    }
}
