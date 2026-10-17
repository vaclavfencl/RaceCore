package com.example.racecore.loot;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import com.mojang.logging.LogUtils;
import com.mojang.serialization.Codec;
import com.mojang.serialization.JsonOps;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.HolderGetter;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.RegistryOps;
import net.minecraft.resources.ResourceKey;
import net.minecraft.util.context.ContextKey;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.ValidationContext;
import net.minecraft.world.level.storage.loot.functions.LootItemFunction;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.LootTableLoadEvent;
import net.neoforged.neoforge.registries.RegisterEvent;
import org.slf4j.Logger;

import java.util.Optional;
import java.util.Set;

/**
 * RaceCore loot luck rewrite.
 *
 * <p>Design goals:</p>
 * <ul>
 *     <li>Luck 0 stays vanilla.</li>
 *     <li>Luck makes rare entries more likely inside the existing roll instead of creating bonus rolls.</li>
 *     <li>Common loot gains a modest quantity bonus.</li>
 *     <li>Rare loot gains mostly quality/chance, not raw stack size.</li>
 * </ul>
 *
 * <p>Final tested formula:</p>
 * <pre>
 * quality = clamp(log(maxWeight / weight) / log(20), 0, 1)
 * effectiveWeight = vanillaWeight * (1 + quality * luck / 4)
 * quantity = baseCount * (1 + 0.033 * luck * (1 - quality))
 * </pre>
 * Quantity uses stochastic rounding so it scales smoothly instead of jumping at .5 boundaries.
 */
@EventBusSubscriber(modid = LootLuckEvents.MOD_ID)
public final class LootLuckEvents {
    public static final String MOD_ID = "racecore";

    private static final Logger LOGGER = LogUtils.getLogger();
    private static final String QUANTITY_FUNCTION_ID = MOD_ID + ":scaled_luck_quantity";

    /**
     * Minecraft's entry weight/quality values are integers. Scaling all competing weights equally lets
     * us encode RaceCore's multiplicative weight formula through vanilla's weight + quality * luck math
     * while preserving Luck 0 probabilities.
     */
    private static final int WEIGHT_SCALE = 1000;
    private static final double QUALITY_LOG_BASE = Math.log(20.0D);
    private static final double RARITY_LUCK_DIVISOR = 4.0D;
    private static final double QUANTITY_PER_LUCK = 0.033D;

    private LootLuckEvents() {
    }

    /** Register the runtime quantity modifier used by patched loot entries. */
    @SubscribeEvent
    public static void registerLootFunction(RegisterEvent event) {
        event.register(
                Registries.LOOT_FUNCTION_TYPE,
                Identifier.fromNamespaceAndPath(MOD_ID, "scaled_luck_quantity"),
                () -> LuckQuantityFunction.MAP_CODEC
        );
    }

    /**
     * Rewrites loaded loot tables once, at data reload time. Runtime loot generation then uses normal
     * vanilla selection with the player's LootContext luck value.
     */

    @SubscribeEvent
    public static void onLootTableLoad(LootTableLoadEvent event) {
        // Only modify loot tables enabled in RaceCore's configuration.
        if (!LootLuckRegistry.enabledForPath(event.getName().getPath())) {
            return;
        }

        RegistryOps<JsonElement> ops = RegistryOps.create(
                JsonOps.INSTANCE,
                registryInfoLookup(event.getRegistries())
        );

        JsonElement encoded = LootTable.DIRECT_CODEC
                .encodeStart(ops, event.getTable())
                .resultOrPartial(message ->
                        LOGGER.warn("RaceCore could not serialize loot table {}: {}",
                                event.getName(), message))
                .orElse(null);

        if (!(encoded instanceof JsonObject originalRoot)) {
            return;
        }

        if (containsRaceCoreQuantityModifier(originalRoot)) {
            return;
        }

        JsonStyle preferred = detectStyle(originalRoot);
        JsonStyle fallback = preferred == JsonStyle.MODERN
                ? JsonStyle.LEGACY
                : JsonStyle.MODERN;

        // A table with no eligible weighted entries is not a failure.
        JsonObject probe = originalRoot.deepCopy();
        if (patchLootTableObject(probe, preferred) == 0) {
            return;
        }

        StringBuilder errors = new StringBuilder();

        LootTable patched = tryPatchAndDecode(
                originalRoot, ops, preferred, errors
        );

        if (patched == null) {
            patched = tryPatchAndDecode(
                    originalRoot, ops, fallback, errors
            );
        }

        if (patched != null) {
            event.setTable(patched);
        } else {
            LOGGER.warn(
                    "RaceCore loot luck failed to patch {}: {}",
                    event.getName(), errors
            );
        }
    }



    private static LootTable tryPatchAndDecode(
            JsonObject originalRoot,
            RegistryOps<JsonElement> ops,
            JsonStyle style,
            StringBuilder errors
    ) {
        JsonObject candidate = originalRoot.deepCopy();

        int changedEntries = patchLootTableObject(candidate, style);

        if (changedEntries == 0) {
            return null;
        }

        return LootTable.DIRECT_CODEC
                .parse(ops, candidate)
                .resultOrPartial(message -> {
                    if (errors.length() > 0) {
                        errors.append(" | ");
                    }

                    errors.append(style)
                            .append(": ")
                            .append(message);
                })
                .orElse(null);
    }

    /** Patch every direct weighted entry in every pool. */
    private static int patchLootTableObject(JsonObject table, JsonStyle style) {
        JsonElement poolsElement = table.get("pools");
        if (!(poolsElement instanceof JsonArray pools)) {
            return 0;
        }

        int changed = 0;

        for (JsonElement poolElement : pools) {
            if (!(poolElement instanceof JsonObject pool)) {
                continue;
            }

            JsonElement entriesElement = pool.get("entries");
            if (!(entriesElement instanceof JsonArray entries) || entries.size() == 0) {
                continue;
            }

            int maxWeight = 0;
            for (JsonElement entryElement : entries) {
                if (!(entryElement instanceof JsonObject entry) || !isWeightedEntry(entry)) {
                    continue;
                }
                maxWeight = Math.max(maxWeight, readInt(entry, "weight", 1));
            }

            if (maxWeight <= 0) {
                continue;
            }

            for (JsonElement entryElement : entries) {
                if (!(entryElement instanceof JsonObject entry)) {
                    continue;
                }

                // Inline loot tables can contain their own pools. Named tables are loaded and patched separately.
                changed += patchInlineLootTables(entry, style);

                if (!isWeightedEntry(entry)) {
                    continue;
                }

                int originalWeight = Math.max(1, readInt(entry, "weight", 1));
                int originalVanillaQuality = readInt(entry, "quality", 0);

                long scaledWeightLong = (long) originalWeight * WEIGHT_SCALE;
                long scaledQualityLong = (long) originalVanillaQuality * WEIGHT_SCALE;

                boolean itemEntry = isItemEntry(entry);
                double raceCoreQuality = 0.0D;

                if (itemEntry) {
                    raceCoreQuality = contextualQuality(originalWeight, maxWeight);
                    long rarityQuality = Math.round(
                            originalWeight * (double) WEIGHT_SCALE * raceCoreQuality / RARITY_LUCK_DIVISOR
                    );
                    scaledQualityLong += rarityQuality;
                }

                entry.addProperty("weight", clampToPositiveInt(scaledWeightLong));

                int scaledQuality = clampToInt(scaledQualityLong);
                if (scaledQuality != 0) {
                    entry.addProperty("quality", scaledQuality);
                } else {
                    entry.remove("quality");
                }

                if (itemEntry) {
                    appendQuantityModifier(entry, raceCoreQuality, style);
                }

                changed++;
            }
        }

        return changed;
    }

    private static int patchInlineLootTables(JsonObject entry, JsonStyle style) {
        if (!"minecraft:loot_table".equals(readType(entry))) {
            return 0;
        }

        JsonElement value = entry.get("value");
        if (value == null) {
            return 0;
        }

        int changed = 0;
        if (value instanceof JsonObject object && object.has("pools")) {
            changed += patchLootTableObject(object, style);
        } else if (value instanceof JsonArray array) {
            for (JsonElement element : array) {
                if (element instanceof JsonObject object && object.has("pools")) {
                    changed += patchLootTableObject(object, style);
                }
            }
        }
        return changed;
    }

    /**
     * Direct pool entries that use vanilla weight/quality selection. Composite entries are intentionally
     * left alone because their children do not participate as independent siblings in the outer pool.
     */
    private static boolean isWeightedEntry(JsonObject entry) {
        String type = readType(entry);
        return switch (type) {
            case "minecraft:item",
                 "minecraft:empty",
                 "minecraft:loot_table",
                 "minecraft:tag",
                 "minecraft:dynamic",
                 "minecraft:slot" -> true;
            default -> entry.has("weight") || entry.has("quality");
        };
    }

    private static boolean isItemEntry(JsonObject entry) {
        return "minecraft:item".equals(readType(entry));
    }

    private static String readType(JsonObject object) {
        JsonElement type = object.get("type");
        return type instanceof JsonPrimitive primitive && primitive.isString()
                ? primitive.getAsString()
                : "";
    }

    private static int readInt(JsonObject object, String key, int fallback) {
        JsonElement element = object.get(key);
        if (element instanceof JsonPrimitive primitive && primitive.isNumber()) {
            try {
                return primitive.getAsInt();
            } catch (RuntimeException ignored) {
                return fallback;
            }
        }
        return fallback;
    }

    private static double contextualQuality(int weight, int maxWeight) {
        if (weight <= 0) {
            return 1.0D;
        }
        if (maxWeight <= 0 || weight >= maxWeight) {
            return 0.0D;
        }

        double quality = Math.log(maxWeight / (double) weight) / QUALITY_LOG_BASE;
        return clamp(quality, 0.0D, 1.0D);
    }

    private static void appendQuantityModifier(JsonObject entry, double quality, JsonStyle style) {
        JsonObject raceCoreModifier = new JsonObject();
        raceCoreModifier.addProperty(style.discriminator, QUANTITY_FUNCTION_ID);
        raceCoreModifier.addProperty("quality", quality);

        String field = style.entryModifierField;
        JsonElement existing = entry.get(field);

        if (existing == null || existing.isJsonNull()) {
            if (style == JsonStyle.LEGACY) {
                JsonArray functions = new JsonArray();
                functions.add(raceCoreModifier);
                entry.add(field, functions);
            } else {
                entry.add(field, raceCoreModifier);
            }
            return;
        }

        if (existing instanceof JsonArray array) {
            array.add(raceCoreModifier);
            return;
        }

        // In 26.3 a list is the short form of minecraft:sequence, so this preserves an existing
        // modifier (object or namespaced-id reference) and runs ours afterwards.
        JsonArray sequence = new JsonArray();
        sequence.add(existing.deepCopy());
        sequence.add(raceCoreModifier);
        entry.add(field, sequence);
    }

    private static JsonStyle detectStyle(JsonElement root) {
        StyleCounts counts = new StyleCounts();
        countStyleKeys(root, counts);
        return counts.modern >= counts.legacy ? JsonStyle.MODERN : JsonStyle.LEGACY;
    }

    private static void countStyleKeys(JsonElement element, StyleCounts counts) {
        if (element instanceof JsonObject object) {
            if (object.has("modifier")) counts.modern++;
            if (object.has("functions")) counts.legacy++;
            if (object.has("function")) counts.legacy++;

            for (String key : object.keySet()) {
                countStyleKeys(object.get(key), counts);
            }
        } else if (element instanceof JsonArray array) {
            for (JsonElement child : array) {
                countStyleKeys(child, counts);
            }
        }
    }

    private static boolean containsRaceCoreQuantityModifier(JsonElement element) {
        if (element instanceof JsonObject object) {
            for (String key : object.keySet()) {
                if (containsRaceCoreQuantityModifier(object.get(key))) {
                    return true;
                }
            }
            return false;
        }

        if (element instanceof JsonArray array) {
            for (JsonElement child : array) {
                if (containsRaceCoreQuantityModifier(child)) {
                    return true;
                }
            }
            return false;
        }

        return element instanceof JsonPrimitive primitive
                && primitive.isString()
                && QUANTITY_FUNCTION_ID.equals(primitive.getAsString());
    }

    private static int clampToPositiveInt(long value) {
        if (value <= 0L) return 1;
        if (value > Integer.MAX_VALUE) return Integer.MAX_VALUE;
        return (int) value;
    }

    private static int clampToInt(long value) {
        if (value > Integer.MAX_VALUE) return Integer.MAX_VALUE;
        if (value < Integer.MIN_VALUE) return Integer.MIN_VALUE;
        return (int) value;
    }

    private static double clamp(double value, double min, double max) {
        return Math.max(min, Math.min(max, value));
    }

    /**
     * LootTableLoadEvent exposes a HolderGetter.Provider in 26.3 while RegistryOps.create expects a
     * RegistryInfoLookup (or full HolderLookup.Provider). This adapter is sufficient for codec parsing.
     */
    private static RegistryOps.RegistryInfoLookup registryInfoLookup(HolderGetter.Provider provider) {
        return new RegistryOps.RegistryInfoLookup() {
            @Override
            public <T> Optional<HolderGetter<T>> lookup(
                    ResourceKey<? extends Registry<? extends T>> registryKey
            ) {
                Optional<? extends HolderGetter<T>> result = provider.lookup(registryKey);
                if (result.isEmpty()) {
                    return Optional.empty();
                }
                return Optional.of(result.get());
            }
        };
    }

    private enum JsonStyle {
        MODERN("modifier", "type"),
        LEGACY("functions", "function");

        private final String entryModifierField;
        private final String discriminator;

        JsonStyle(String entryModifierField, String discriminator) {
            this.entryModifierField = entryModifierField;
            this.discriminator = discriminator;
        }
    }

    private static final class StyleCounts {
        private int modern;
        private int legacy;
    }

    /**
     * Runtime quantity modifier appended after an entry's existing modifiers.
     *
     * <p>Expected quantity:</p>
     * <pre>baseCount * (1 + 0.033 * luck * (1 - quality))</pre>
     *
     * <p>Fractional expected counts use stochastic rounding, e.g. 1.33 becomes 1 with 67% chance
     * and 2 with 33% chance.</p>
     */
    public record LuckQuantityFunction(double quality) implements LootItemFunction {
        public static final MapCodec<LuckQuantityFunction> MAP_CODEC = RecordCodecBuilder.mapCodec(instance ->
                instance.group(
                        Codec.DOUBLE.fieldOf("quality").forGetter(LuckQuantityFunction::quality)
                ).apply(instance, LuckQuantityFunction::new)
        );

        @Override
        public ItemStack apply(ItemStack stack, LootContext context) {
            if (stack.isEmpty()) {
                return stack;
            }

            double q = clamp(quality, 0.0D, 1.0D);
            double luck = Math.max(0.0D, context.getLuck());

            if (luck <= 0.0D) {
                return stack;
            }

            double multiplier = 1.0D + QUANTITY_PER_LUCK * luck * (1.0D - q);
            double expected = stack.getCount() * multiplier;

            int whole = (int) Math.floor(expected);
            double fraction = expected - whole;

            if (context.getRandom().nextDouble() < fraction) {
                whole++;
            }

            stack.setCount(Math.max(1, whole));
            return stack;
        }

        @Override
        public Set<ContextKey<?>> getReferencedContextParams() {
            return Set.of();
        }

        @Override
        public void validate(ValidationContext context) {
            // No context parameters are required. Luck is a built-in property on LootContext.
        }

        @Override
        public MapCodec<LuckQuantityFunction> codec() {
            return MAP_CODEC;
        }
    }
}
