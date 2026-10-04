package com.example.racecore.loot;

import com.google.common.base.Suppliers;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;
import net.neoforged.neoforge.common.loot.IGlobalLootModifier;
import net.neoforged.neoforge.common.loot.LootModifier;

import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.function.Supplier;

/**
 * RaceCore Phase 7 global loot modifier.
 *
 * Base loot is generated FIRST and never removed.
 *
 * Luck then:
 * 1) increases quantity of common/low-quality stackable loot;
 * 2) performs extra raw rolls of the same loot table;
 * 3) only accepts items from those extra rolls according to their contextual
 *    quality;
 * 4) never adds a bonus copy of an item type already present in the base loot.
 *
 * This makes Luck additive rather than substitutive:
 * more Luck cannot turn a normal chest into a worse chest.
 */
public final class LuckQualityLootModifier
        extends LootModifier {

    public static final Supplier<MapCodec<LuckQualityLootModifier>>
            CODEC =
            Suppliers.memoize(
                    () ->
                            RecordCodecBuilder.mapCodec(
                                    instance ->
                                            codecStart(instance)
                                                    .apply(
                                                            instance,
                                                            LuckQualityLootModifier::new
                                                    )
                            )
            );

    public LuckQualityLootModifier(
            Optional<Holder<LootItemCondition>> condition,
            int priority
    ) {
        super(
                condition,
                priority
        );
    }

    @Override
    protected ObjectArrayList<ItemStack> doApply(
            ObjectArrayList<ItemStack> generatedLoot,
            LootContext context
    ) {
        LootLuckSettings settings =
                LootLuckRegistry.settings();

        if (!settings.enabled()) {
            return generatedLoot;
        }

        float luck =
                Math.max(
                        0.0F,
                        context.getLuck()
                );

        if (luck <= 0.0F) {
            return generatedLoot;
        }

        Identifier tableId =
                context.getQueriedLootTableId();

        if (tableId == null
                || !LootLuckRegistry.enabledForPath(
                        tableId.getPath()
                )) {

            return generatedLoot;
        }

        applyQuantityBonus(
                generatedLoot,
                context,
                tableId,
                luck,
                settings
        );

        applyDiscoveryBonus(
                generatedLoot,
                context,
                tableId,
                luck,
                settings
        );

        return generatedLoot;
    }

    private static void applyQuantityBonus(
            ObjectArrayList<ItemStack> generatedLoot,
            LootContext context,
            Identifier tableId,
            float luck,
            LootLuckSettings settings
    ) {
        /*
         * Snapshot because extra split stacks can be appended while iterating.
         */
        ObjectArrayList<ItemStack> original =
                new ObjectArrayList<>(
                        generatedLoot
                );

        for (ItemStack stack
                : original) {

            if (stack.isEmpty()
                    || stack.getMaxStackSize() <= 1) {
                continue;
            }

            Identifier itemId =
                    BuiltInRegistries.ITEM
                            .getKeyOrNull(
                                    stack.getItem()
                            );

            if (itemId == null) {
                continue;
            }

            Optional<LootQualityRegistry.ItemProfile> maybeProfile =
                    LootQualityRegistry.resolve(
                            tableId,
                            itemId
                    );

            if (maybeProfile.isEmpty()
                    || !maybeProfile.get()
                    .quantityEligible()) {

                continue;
            }

            double quality =
                    maybeProfile.get()
                            .quality();

            double multiplier =
                    1.0
                            + settings
                            .quantityBonusPerLuck()
                            * luck
                            * (1.0 - quality);

            multiplier =
                    Math.min(
                            settings.maxQuantityMultiplier(),
                            multiplier
                    );

            int originalCount =
                    stack.getCount();

            double exactTarget =
                    originalCount
                            * multiplier;

            int target =
                    (int) Math.floor(
                            exactTarget
                    );

            double fractional =
                    exactTarget - target;

            if (fractional > 0.0
                    && context.getRandom()
                    .nextDouble()
                    < fractional) {

                target++;
            }

            int extra =
                    Math.max(
                            0,
                            target - originalCount
                    );

            if (extra <= 0) {
                continue;
            }

            addExtraCount(
                    generatedLoot,
                    stack,
                    extra
            );
        }
    }

    private static void addExtraCount(
            ObjectArrayList<ItemStack> generatedLoot,
            ItemStack base,
            int extra
    ) {
        int maxStack =
                base.getMaxStackSize();

        int room =
                Math.max(
                        0,
                        maxStack
                                - base.getCount()
                );

        int intoBase =
                Math.min(
                        room,
                        extra
                );

        if (intoBase > 0) {
            base.grow(
                    intoBase
            );
            extra -= intoBase;
        }

        while (extra > 0) {
            int amount =
                    Math.min(
                            maxStack,
                            extra
                    );

            generatedLoot.add(
                    base.copyWithCount(
                            amount
                    )
            );

            extra -= amount;
        }
    }

    @SuppressWarnings("deprecation")
    private static void applyDiscoveryBonus(
            ObjectArrayList<ItemStack> generatedLoot,
            LootContext context,
            Identifier tableId,
            float luck,
            LootLuckSettings settings
    ) {
        int extraRolls =
                stochasticRollCount(
                        context,
                        luck
                                * settings
                                .bonusDiscoveryRollsPerLuck()
                );

        extraRolls =
                Math.min(
                        settings.maxBonusDiscoveryRolls(),
                        extraRolls
                );

        if (extraRolls <= 0) {
            return;
        }

        ResourceKey<LootTable> tableKey =
                ResourceKey.create(
                        Registries.LOOT_TABLE,
                        tableId
                );

        LootTable table =
                context
                        .getLevel()
                        .getServer()
                        .reloadableRegistries()
                        .getLootTable(
                                tableKey
                        );

        Set<Identifier> alreadyPresent =
                new HashSet<>();

        for (ItemStack stack
                : generatedLoot) {

            Identifier itemId =
                    BuiltInRegistries.ITEM
                            .getKeyOrNull(
                                    stack.getItem()
                            );

            if (itemId != null) {
                alreadyPresent.add(
                        itemId
                );
            }
        }

        for (int roll = 0;
             roll < extraRolls;
             roll++) {

            ObjectArrayList<ItemStack> candidates =
                    new ObjectArrayList<>();

            /*
             * RAW is intentional:
             * this rerolls the real table including its conditions/functions,
             * but bypasses Global Loot Modifiers so RaceCore cannot recurse
             * into itself.
             */
            table.getRandomItemsRaw(
                    context,
                    candidates::add
            );

            for (ItemStack candidate
                    : candidates) {

                if (candidate.isEmpty()) {
                    continue;
                }

                Identifier itemId =
                        BuiltInRegistries.ITEM
                                .getKeyOrNull(
                                        candidate.getItem()
                                );

                if (itemId == null
                        || alreadyPresent.contains(
                                itemId
                        )) {

                    continue;
                }

                Optional<LootQualityRegistry.ItemProfile> maybeProfile =
                        LootQualityRegistry.resolve(
                                tableId,
                                itemId
                        );

                if (maybeProfile.isEmpty()) {
                    continue;
                }

                double quality =
                        maybeProfile.get()
                                .quality();

                if (quality <= 0.0) {
                    continue;
                }

                if (context.getRandom()
                        .nextDouble()
                        >= quality) {

                    continue;
                }

                /*
                 * At most one bonus stack of each item type per loot
                 * generation. Luck improves discovery, not duplication.
                 */
                generatedLoot.add(
                        candidate.copy()
                );

                alreadyPresent.add(
                        itemId
                );
            }
        }
    }

    private static int stochasticRollCount(
            LootContext context,
            double exact
    ) {
        if (!Double.isFinite(exact)
                || exact <= 0.0) {
            return 0;
        }

        int whole =
                (int) Math.floor(exact);

        double fractional =
                exact - whole;

        if (fractional > 0.0
                && context.getRandom()
                .nextDouble()
                < fractional) {

            whole++;
        }

        return whole;
    }

    @Override
    public MapCodec<? extends IGlobalLootModifier> codec() {
        return CODEC.get();
    }
}
