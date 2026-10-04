package com.example.racecore.death;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;

public record DeathInventorySnapshot(List<ItemStack> restoreStacks) {
    public static final Codec<DeathInventorySnapshot> CODEC = RecordCodecBuilder.create(
            instance -> instance.group(
                    ItemStack.OPTIONAL_CODEC.listOf().fieldOf("restoreStacks").forGetter(DeathInventorySnapshot::restoreStacks)
            ).apply(instance, DeathInventorySnapshot::new)
    );

    public DeathInventorySnapshot {
        List<ItemStack> copied = new ArrayList<>(restoreStacks.size());
        for (ItemStack stack : restoreStacks) copied.add(stack.copy());
        restoreStacks = List.copyOf(copied);
    }

    public static DeathInventorySnapshot empty() {
        return new DeathInventorySnapshot(List.of());
    }

    public boolean isEmpty() {
        return restoreStacks.isEmpty();
    }

    public List<ItemStack> copyStacks() {
        List<ItemStack> copied = new ArrayList<>(restoreStacks.size());
        for (ItemStack stack : restoreStacks) copied.add(stack.copy());
        return copied;
    }
}
