package com.example.racecore.loot;

import com.example.racecore.RaceCore;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.mojang.serialization.JsonOps;
import com.mojang.logging.LogUtils;
import net.minecraft.core.HolderGetter;
import net.minecraft.core.Registry;
import net.minecraft.resources.RegistryOps;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.storage.loot.LootTable;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.LootTableLoadEvent;
import org.slf4j.Logger;

import java.util.Optional;
import java.util.Set;

/**
 * RaceCore chest economy rules (2026-10-15).
 *
 * Only changes entries in the chosen vanilla chest loot tables, leaving
 * all other pools/entries/structure identity intact. The handler runs before
 * LootLuckEvents so RaceCore's existing luck scaling sees the updated table.
 *
 * Unlike direct data/minecraft table overrides, this works on the actual
 * loaded (possibly datapack-modified) table and preserves unrelated entries.
 */
@EventBusSubscriber(modid = RaceCore.MODID)
public final class ChestLootRebalanceEvents {
    private static final Logger LOGGER = LogUtils.getLogger();

    private static final Set<String> NO_DIAMOND_IRON_EMERALD = Set.of(
            "chests/abandoned_mineshaft",
            "chests/desert_pyramid",
            "chests/shipwreck_treasure"
    );

    private static final Set<String> BASTION_CHESTS = Set.of(
            "chests/bastion_treasure",
            "chests/bastion_bridge",
            "chests/bastion_other",
            "chests/bastion_hoglin_stable"
    );

    private static final Set<String> TARGET_CHESTS = Set.of(
            "chests/simple_dungeon",
            "chests/abandoned_mineshaft",
            "chests/desert_pyramid",
            "chests/shipwreck_treasure",
            "chests/buried_treasure",
            "chests/nether_bridge",
            "chests/bastion_treasure",
            "chests/bastion_bridge",
            "chests/bastion_other",
            "chests/bastion_hoglin_stable",
            "chests/ancient_city"
    );

    private static final Set<String> FINISHED_DIAMOND_GEAR = Set.of(
            "minecraft:diamond_sword",
            "minecraft:diamond_pickaxe",
            "minecraft:diamond_axe",
            "minecraft:diamond_shovel",
            "minecraft:diamond_hoe",
            "minecraft:diamond_spear",
            "minecraft:diamond_helmet",
            "minecraft:diamond_chestplate",
            "minecraft:diamond_leggings",
            "minecraft:diamond_boots",
            "minecraft:diamond_horse_armor",
            "minecraft:diamond_nautilus_armor"
    );

    private ChestLootRebalanceEvents() {
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onTableLoad(LootTableLoadEvent event) {
        if (!"minecraft".equals(event.getName().getNamespace())) {
            return;
        }
        String tablePath = event.getName().getPath();
        if (!TARGET_CHESTS.contains(tablePath)) {
            return;
        }

        RegistryOps<JsonElement> ops = RegistryOps.create(
                JsonOps.INSTANCE,
                registryInfoLookup(event.getRegistries())
        );

        JsonElement serialized = LootTable.DIRECT_CODEC.encodeStart(ops, event.getTable())
                .resultOrPartial(msg -> LOGGER.warn(
                        "RaceCore could not encode chest table {}: {}", tablePath, msg))
                .orElse(null);

        if (!(serialized instanceof JsonObject root)) {
            return;
        }

        // Work on a copy; keep the original table if validation fails.
        JsonObject edited = root.deepCopy();
        int changes = rewriteRecursively(edited, tablePath);
        if (changes == 0) {
            return;
        }

        LootTable.DIRECT_CODEC.parse(ops, edited)
                .resultOrPartial(msg -> LOGGER.warn(
                        "RaceCore chest table {} failed validation; original retained: {}",
                        tablePath, msg))
                .ifPresent(table -> {
                    event.setTable(table);
                    LOGGER.info("RaceCore chest loot changed {} entries in {}", changes, tablePath);
                });
    }

    /**
     * Visit only loot pool entries and nested composite/inline-table entries,
     * not conditions or unrelated JSON subtrees.
     */
    private static int rewriteRecursively(JsonElement element, String chest) {
        if (element instanceof JsonArray array) {
            int changes = 0;
            for (JsonElement child : array) {
                changes += rewriteRecursively(child, chest);
            }
            return changes;
        }
        if (!(element instanceof JsonObject object)) {
            return 0;
        }

        // A direct item entry, distinguished from other objects that also have a "name".
        if ("minecraft:item".equals(string(object, "type"))) {
            String item = string(object, "name");
            if (item != null) {
                if (shouldRemove(chest, item)) {
                    convertToEmptyEntry(object);
                    return 1;
                }
                if ("chests/buried_treasure".equals(chest)
                        && "minecraft:iron_ingot".equals(item)) {
                    object.addProperty("name", "minecraft:iron_nugget");
                    return 1;
                }
                if (isNetherDiamondChest(chest) && "minecraft:diamond".equals(item)) {
                    // A small weight rather than guaranteed loose diamonds.
                    object.addProperty("weight", 1);
                    // Limit the underlying vanilla stack, even with many pool rolls.
                    object.add("modifier", setCountOneToTwo());
                    object.remove("functions");
                    return 1;
                }
            }
        }

        int changes = 0;
        // Standard pools, composite entries and inline loot tables.
        for (String field : new String[]{"pools", "entries", "children", "value"}) {
            JsonElement child = object.get(field);
            if (child != null) {
                changes += rewriteRecursively(child, chest);
            }
        }
        return changes;
    }

    private static boolean shouldRemove(String chest, String item) {
        if (NO_DIAMOND_IRON_EMERALD.contains(chest)) {
            if (Set.of("minecraft:diamond", "minecraft:iron_ingot", "minecraft:emerald")
                    .contains(item)) return true;
        }

        if ("minecraft:enchanted_golden_apple".equals(item)) {
            return "chests/simple_dungeon".equals(chest)
                    || "chests/abandoned_mineshaft".equals(chest);
        }

        if ("chests/buried_treasure".equals(chest)) {
            return "minecraft:diamond".equals(item);
        }

        if ("chests/ancient_city".equals(chest)) {
            return "minecraft:diamond_leggings".equals(item);
        }

        if ("chests/bastion_treasure".equals(chest)
                && "minecraft:netherite_ingot".equals(item)) {
            return true;
        }

        return isNetherDiamondChest(chest) && FINISHED_DIAMOND_GEAR.contains(item);
    }

    private static boolean isNetherDiamondChest(String chest) {
        return "chests/nether_bridge".equals(chest) || BASTION_CHESTS.contains(chest);
    }

    private static JsonObject setCountOneToTwo() {
        JsonObject uniform = new JsonObject();
        uniform.addProperty("type", "minecraft:uniform");
        uniform.addProperty("min", 1);
        uniform.addProperty("max", 2);
        JsonObject modifier = new JsonObject();
        modifier.addProperty("type", "minecraft:set_count");
        modifier.add("count", uniform);
        return modifier;
    }

    /** Preserve the removed entry's weight/quality/conditions as an empty roll.
     * This prevents reweighting other rewards, especially unique templates.
     */
    private static void convertToEmptyEntry(JsonObject object) {
        JsonElement weight = object.get("weight");
        JsonElement quality = object.get("quality");
        JsonElement condition = object.get("condition");
        JsonElement conditions = object.get("conditions");
        JsonObject empty = new JsonObject();
        empty.addProperty("type", "minecraft:empty");
        if (weight != null) empty.add("weight", weight.deepCopy());
        if (quality != null) empty.add("quality", quality.deepCopy());
        if (condition != null) empty.add("condition", condition.deepCopy());
        if (conditions != null) empty.add("conditions", conditions.deepCopy());
        for (String key : Set.copyOf(object.keySet())) object.remove(key);
        for (String key : empty.keySet()) object.add(key, empty.get(key));
    }

    private static String string(JsonObject object, String name) {
        JsonElement element = object.get(name);
        try {
            return element == null || !element.isJsonPrimitive() ? null : element.getAsString();
        } catch (RuntimeException e) {
            return null;
        }
    }

    private static RegistryOps.RegistryInfoLookup registryInfoLookup(HolderGetter.Provider provider) {
        return new RegistryOps.RegistryInfoLookup() {
            @Override
            public <T> Optional<HolderGetter<T>> lookup(
                    ResourceKey<? extends Registry<? extends T>> key) {
                Optional<? extends HolderGetter<T>> result = provider.lookup(key);
                if (result.isEmpty()) return Optional.empty();
                return Optional.of(result.get());
            }
        };
    }
}
