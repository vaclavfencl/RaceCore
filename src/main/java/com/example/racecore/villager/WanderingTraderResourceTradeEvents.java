package com.example.racecore.villager;

import com.example.racecore.RaceCore;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.npc.villager.AbstractVillager;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.MerchantMenu;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.trading.ItemCost;
import net.minecraft.world.item.trading.MerchantOffer;
import net.minecraft.world.item.trading.MerchantOffers;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerContainerEvent;

/**
 * Gives every Wandering Trader exactly one RaceCore "player sells a rare
 * resource for Emeralds" offer in addition to all of the trader's normal
 * vanilla trades.
 *
 * The chosen offer is persisted on the trader, so reopening the menu or
 * reloading the world does not reroll it.
 */
@EventBusSubscriber(modid = RaceCore.MODID)
public final class WanderingTraderResourceTradeEvents {
    private static final double SEARCH_RADIUS = 6.0D;

    private static final String RESOURCE_TRADE_INDEX_KEY =
            "racecore:wandering_resource_trade_index";

    private static final ResourceTrade[] RESOURCE_TRADES = {
            // Original example.
            new ResourceTrade(
                    Items.IRON_INGOT,
                    10,
                    2,
                    2
            ),

            new ResourceTrade(
                    Items.GHAST_TEAR,
                    2,
                    3,
                    2
            ),

            new ResourceTrade(
                    Items.NAUTILUS_SHELL,
                    1,
                    2,
                    2
            ),

            new ResourceTrade(
                    Items.ECHO_SHARD,
                    2,
                    4,
                    2
            ),

            new ResourceTrade(
                    Items.BLAZE_ROD,
                    4,
                    2,
                    2
            ),

            new ResourceTrade(
                    Items.WITHER_SKELETON_SKULL,
                    1,
                    6,
                    1
            ),

            new ResourceTrade(
                    Items.RABBIT_FOOT,
                    4,
                    2,
                    2
            ),

            new ResourceTrade(
                    Items.PHANTOM_MEMBRANE,
                    6,
                    3,
                    2
            ),

            new ResourceTrade(
                    Items.SHULKER_SHELL,
                    2,
                    5,
                    2
            ),

            new ResourceTrade(
                    Items.HEART_OF_THE_SEA,
                    1,
                    8,
                    1
            ),

            new ResourceTrade(
                    Items.SNIFFER_EGG,
                    1,
                    6,
                    1
            )
    };

    private WanderingTraderResourceTradeEvents() {
    }

    @SubscribeEvent
    public static void onMerchantMenuOpen(
            PlayerContainerEvent.Open event
    ) {
        if (!(event.getContainer()
                instanceof MerchantMenu)) {

            return;
        }

        Player player =
                event.getEntity();

        if (!(player.level()
                instanceof ServerLevel level)) {

            return;
        }

        AbstractVillager trader =
                level.getEntitiesOfClass(
                                AbstractVillager.class,
                                player.getBoundingBox()
                                        .inflate(
                                                SEARCH_RADIUS
                                        ),
                                candidate ->
                                        Identifier.fromNamespaceAndPath(
                                                "minecraft",
                                                "wandering_trader"
                                        ).equals(
                                                BuiltInRegistries.ENTITY_TYPE
                                                        .getKey(candidate.getType())
                                        )
                                                && candidate.getTradingPlayer()
                                                == player
                        )
                        .stream()
                        .findFirst()
                        .orElse(null);

        if (trader == null) {
            return;
        }

        ensureResourceTrade(
                trader
        );
    }

    private static void ensureResourceTrade(
            AbstractVillager trader
    ) {
        int selected =
                trader.getPersistentData()
                        .getIntOr(
                                RESOURCE_TRADE_INDEX_KEY,
                                -1
                        );

        if (selected < 0
                || selected
                >= RESOURCE_TRADES.length) {

            selected =
                    trader.getRandom()
                            .nextInt(
                                    RESOURCE_TRADES.length
                            );

            trader.getPersistentData()
                    .putInt(
                            RESOURCE_TRADE_INDEX_KEY,
                            selected
                    );
        }

        ResourceTrade trade =
                RESOURCE_TRADES[selected];

        MerchantOffers offers =
                trader.getOffers();

        if (containsTrade(
                offers,
                trade
        )) {

            return;
        }

        offers.add(
                new MerchantOffer(
                        new ItemCost(
                                trade.input(),
                                trade.inputCount()
                        ),
                        new ItemStack(
                                Items.EMERALD,
                                trade.emeraldCount()
                        ),
                        trade.maxUses(),
                        0,
                        0.0F
                )
        );
    }

    private static boolean containsTrade(
            MerchantOffers offers,
            ResourceTrade trade
    ) {
        for (MerchantOffer offer
                : offers) {

            ItemStack input =
                    offer.getBaseCostA();

            ItemStack output =
                    offer.getResult();

            if (input.is(trade.input())
                    && input.getCount()
                    == trade.inputCount()
                    && output.is(Items.EMERALD)
                    && output.getCount()
                    == trade.emeraldCount()
                    && offer.getMaxUses()
                    == trade.maxUses()) {

                return true;
            }
        }

        return false;
    }

    private record ResourceTrade(
            Item input,
            int inputCount,
            int emeraldCount,
            int maxUses
    ) {
    }
}
