package com.example.racecore.loot;

import com.google.common.base.Suppliers;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.minecraft.resources.Identifier;
import net.minecraft.core.Holder;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;
import net.neoforged.neoforge.common.loot.LootModifier;
import net.neoforged.neoforge.common.loot.IGlobalLootModifier;

import java.util.Iterator;
import java.util.Optional;
import java.util.function.Supplier;

/**
 * Enforce a total of at most two Emerald items per buried treasure chest,
 * even for high-Luck Goblins and after other RaceCore loot modifiers.
 * Runs after the priority-1000 Luck modifier (priority 0 in our JSON).
 */
public final class BuriedTreasureEmeraldCapModifier extends LootModifier {
    private static final Identifier BURIED_TREASURE = Identifier.fromNamespaceAndPath(
            "minecraft", "chests/buried_treasure");

    public static final Supplier<MapCodec<BuriedTreasureEmeraldCapModifier>> CODEC =
            Suppliers.memoize(() -> RecordCodecBuilder.mapCodec(instance ->
                    codecStart(instance).apply(instance, BuriedTreasureEmeraldCapModifier::new)));

    public BuriedTreasureEmeraldCapModifier(Optional<Holder<LootItemCondition>> condition,
                                           int priority) {
        super(condition, priority);
    }

    @Override
    protected ObjectArrayList<ItemStack> doApply(ObjectArrayList<ItemStack> loot, LootContext context) {
        if (!BURIED_TREASURE.equals(context.getQueriedLootTableId())) {
            return loot;
        }

        int remaining = 2;
        Iterator<ItemStack> iterator = loot.iterator();
        while (iterator.hasNext()) {
            ItemStack stack = iterator.next();
            if (!stack.is(Items.EMERALD)) continue;
            if (remaining <= 0) {
                iterator.remove();
                continue;
            }
            int keep = Math.min(remaining, stack.getCount());
            stack.setCount(keep);
            remaining -= keep;
        }
        return loot;
    }
    @Override
    public MapCodec<? extends IGlobalLootModifier> codec() {
        return CODEC.get();
    }
}
