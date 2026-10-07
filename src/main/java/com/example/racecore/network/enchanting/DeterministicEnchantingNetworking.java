package com.example.racecore.network.enchanting;

import com.example.racecore.RaceCore;
import com.example.racecore.enchanting.DeterministicEnchantingManager;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.stats.Stats;
import net.minecraft.world.inventory.EnchantmentMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantment;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Server-authoritative application path for the deterministic enchanting UI.
 */
@EventBusSubscriber(modid = RaceCore.MODID)
public final class DeterministicEnchantingNetworking {
    private static final int MAX_PLAN_LENGTH = 4096;
    private static final int MAX_PLAN_ENTRIES = 64;

    private DeterministicEnchantingNetworking() {
    }

    @SubscribeEvent
    public static void registerPayloads(
            RegisterPayloadHandlersEvent event
    ) {
        var registrar =
                event.registrar(
                        "deterministic-enchanting-1"
                );

        registrar.playToServer(
                ApplyEnchantPlanPayload.TYPE,
                ApplyEnchantPlanPayload.STREAM_CODEC,
                DeterministicEnchantingNetworking
                        ::handleApply
        );
    }

    private static void handleApply(
            ApplyEnchantPlanPayload payload,
            IPayloadContext context
    ) {
        context.enqueueWork(() -> {
            if (!(context.player()
                    instanceof ServerPlayer player)) {

                return;
            }

            if (!(player.containerMenu
                    instanceof EnchantmentMenu menu)) {

                reject(
                        player,
                        "message.racecore.enchanting.invalid"
                );

                return;
            }

            if (!menu.stillValid(player)) {
                reject(
                        player,
                        "message.racecore.enchanting.invalid"
                );

                return;
            }

            String encoded =
                    payload.plan();

            if (encoded == null
                    || encoded.isBlank()
                    || encoded.length()
                    > MAX_PLAN_LENGTH) {

                reject(
                        player,
                        "message.racecore.enchanting.invalid"
                );

                return;
            }

            Slot itemSlot =
                    menu.getSlot(0);

            Slot lapisSlot =
                    menu.getSlot(1);

            ItemStack original =
                    itemSlot.getItem();

            /*
             * Normal books intentionally retain the vanilla random system.
             */
            if (original.isEmpty()
                    || original.is(Items.BOOK)) {

                reject(
                        player,
                        "message.racecore.enchanting.invalid"
                );

                return;
            }

            HolderLookup.RegistryLookup<Enchantment> registry =
                    player.level()
                            .registryAccess()
                            .lookupOrThrow(
                                    Registries.ENCHANTMENT
                            );

            Map<Holder<Enchantment>, Integer> targets =
                    parseTargets(
                            encoded,
                            registry
                    );

            if (targets == null
                    || targets.isEmpty()
                    || targets.size()
                    > MAX_PLAN_ENTRIES) {

                reject(
                        player,
                        "message.racecore.enchanting.invalid"
                );

                return;
            }

            for (Map.Entry<Holder<Enchantment>, Integer> entry
                    : targets.entrySet()) {

                if (!DeterministicEnchantingManager
                        .isTableEligible(
                                original,
                                entry.getKey()
                        )) {

                    reject(
                            player,
                            "message.racecore.enchanting.invalid"
                    );

                    return;
                }
            }

            if (!DeterministicEnchantingManager
                    .hasOnlyLevelIncreases(
                            original,
                            targets
                    )) {

                reject(
                        player,
                        "message.racecore.enchanting.invalid"
                );

                return;
            }

            ItemStack output =
                    DeterministicEnchantingManager
                            .applyTargetsCopy(
                                    original,
                                    targets
                            );

            if (!DeterministicEnchantingManager
                    .areEnchantmentsCompatible(
                            output
                    )) {

                reject(
                        player,
                        "message.racecore.enchanting.incompatible"
                );

                return;
            }

            if (!DeterministicEnchantingManager
                    .isWithinCapacity(
                            player,
                            output
                    )) {

                reject(
                        player,
                        "message.racecore.enchanting.capacity"
                );

                return;
            }

            int cost =
                    DeterministicEnchantingManager
                            .getAddedLevelCost(
                                    original,
                                    targets
                            );

            if (cost <= 0
                    || cost > 100) {

                reject(
                        player,
                        "message.racecore.enchanting.invalid"
                );

                return;
            }

            boolean free =
                    player.hasInfiniteMaterials();

            if (!free) {
                ItemStack lapis =
                        lapisSlot.getItem();

                if (!lapis.is(
                        Items.LAPIS_LAZULI
                )
                        || lapis.getCount()
                        < cost) {

                    reject(
                            player,
                            "message.racecore.enchanting.not_enough_lapis"
                    );

                    return;
                }

                if (player.experienceLevel
                        < cost) {

                    reject(
                            player,
                            "message.racecore.enchanting.not_enough_levels"
                    );

                    return;
                }
            }

            /*
             * onEnchantmentPerformed handles the XP-level payment and refreshes
             * the player's vanilla enchantment seed. Infinite-material players
             * use a zero XP cost to preserve creative-mode behavior.
             */
            player.onEnchantmentPerformed(
                    output,
                    free
                            ? 0
                            : cost
            );

            if (!free) {
                lapisSlot.getItem()
                        .shrink(cost);

                lapisSlot.setChanged();
            }

            itemSlot.set(output);
            itemSlot.setChanged();

            player.awardStat(
                    Stats.ENCHANT_ITEM
            );

            menu.broadcastChanges();
        });
    }

    private static Map<Holder<Enchantment>, Integer> parseTargets(
            String encoded,
            HolderLookup.RegistryLookup<Enchantment> registry
    ) {
        String[] entries =
                encoded.split(";");

        if (entries.length == 0
                || entries.length
                > MAX_PLAN_ENTRIES) {

            return null;
        }

        Map<Holder<Enchantment>, Integer> result =
                new LinkedHashMap<>();

        for (String entry : entries) {
            String[] parts =
                    entry.split(
                            "=",
                            2
                    );

            if (parts.length != 2
                    || parts[0].isBlank()
                    || parts[1].isBlank()) {

                return null;
            }

            final Identifier id;

            try {
                id =
                        Identifier.parse(
                                parts[0]
                        );
            } catch (RuntimeException exception) {
                return null;
            }

            final int level;

            try {
                level =
                        Integer.parseInt(
                                parts[1]
                        );
            } catch (NumberFormatException exception) {
                return null;
            }

            ResourceKey<Enchantment> key =
                    ResourceKey.create(
                            Registries.ENCHANTMENT,
                            id
                    );

            Holder.Reference<Enchantment> holder =
                    registry.get(key)
                            .orElse(null);

            if (holder == null
                    || result.containsKey(
                    holder
            )) {

                return null;
            }

            result.put(
                    holder,
                    level
            );
        }

        return result;
    }

    private static void reject(
            ServerPlayer player,
            String translationKey
    ) {
        player.sendOverlayMessage(
                Component.translatable(
                        translationKey
                )
        );
    }
}
