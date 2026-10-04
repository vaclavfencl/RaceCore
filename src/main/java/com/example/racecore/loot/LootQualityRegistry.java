package com.example.racecore.loot;

import com.example.racecore.RaceCore;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.minecraft.resources.Identifier;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Automatically derives contextual item quality from loaded loot tables.
 *
 * Primary signal:
 *   relative entry weight inside the same sibling group.
 *
 * Secondary signal:
 *   simple minecraft:random_chance conditions.
 *
 * Vanilla explicit quality:
 *   positive -> semantic high-quality hint
 *   negative -> suppresses RaceCore quality for that branch
 *
 * No global item list is maintained. "Diamond" can therefore be rare in one
 * table and ordinary in another.
 */
public final class LootQualityRegistry {
    private static final Map<Identifier, TableProfile> TABLES =
            new ConcurrentHashMap<>();

    private LootQualityRegistry() {
    }

    public static void put(
            Identifier tableId,
            JsonObject json
    ) {
        if (!LootLuckRegistry.enabledForPath(
                tableId.getPath()
        )) {
            TABLES.remove(tableId);
            return;
        }

        TABLES.put(
                tableId,
                analyze(
                        tableId,
                        json
                )
        );
    }

    public static Optional<ItemProfile> resolve(
            Identifier tableId,
            Identifier itemId
    ) {
        return resolve(
                tableId,
                itemId,
                new HashSet<>()
        );
    }

    private static Optional<ItemProfile> resolve(
            Identifier tableId,
            Identifier itemId,
            Set<Identifier> visited
    ) {
        if (!visited.add(tableId)) {
            return Optional.empty();
        }

        TableProfile table =
                TABLES.get(tableId);

        if (table == null) {
            return Optional.empty();
        }

        ItemProfile best =
                table.direct().get(itemId);

        for (TableReference reference
                : table.references()) {

            if (reference.suppressChildren()) {
                continue;
            }

            Optional<ItemProfile> child =
                    resolve(
                            reference.tableId(),
                            itemId,
                            visited
                    );

            if (child.isEmpty()) {
                continue;
            }

            ItemProfile combined =
                    new ItemProfile(
                            combineQuality(
                                    reference.quality(),
                                    child.get().quality()
                            ),
                            child.get().quantityEligible()
                    );

            best = betterOf(
                    best,
                    combined
            );
        }

        Double override =
                qualityOverride(
                        tableId,
                        itemId
                );

        if (override != null) {
            if (best == null) {
                best = new ItemProfile(
                        override,
                        false
                );
            } else {
                best = new ItemProfile(
                        override,
                        best.quantityEligible()
                );
            }
        }

        return Optional.ofNullable(best);
    }

    private static @Nullable Double qualityOverride(
            Identifier tableId,
            Identifier itemId
    ) {
        Map<String, Map<String, Double>> overrides =
                LootLuckRegistry
                        .settings()
                        .qualityOverrides();

        Map<String, Double> table =
                overrides.get(
                        tableId.toString()
                );

        if (table == null) {
            return null;
        }

        return table.get(
                itemId.toString()
        );
    }

    private static ItemProfile betterOf(
            @Nullable ItemProfile first,
            ItemProfile second
    ) {
        if (first == null) {
            return second;
        }

        if (second.quality()
                > first.quality()) {

            return new ItemProfile(
                    second.quality(),
                    first.quantityEligible()
                            || second.quantityEligible()
            );
        }

        return new ItemProfile(
                first.quality(),
                first.quantityEligible()
                        || second.quantityEligible()
        );
    }

    private static TableProfile analyze(
            Identifier tableId,
            JsonObject root
    ) {
        Map<Identifier, ItemProfile> direct =
                new HashMap<>();

        List<TableReference> references =
                new ArrayList<>();

        if (!root.has("pools")
                || !root.get("pools")
                .isJsonArray()) {

            return new TableProfile(
                    Map.of(),
                    List.of()
            );
        }

        JsonArray pools =
                root.getAsJsonArray(
                        "pools"
                );

        for (JsonElement poolElement
                : pools) {

            if (!poolElement.isJsonObject()) {
                continue;
            }

            JsonObject pool =
                    poolElement.getAsJsonObject();

            if (!pool.has("entries")
                    || !pool.get("entries")
                    .isJsonArray()) {
                continue;
            }

            analyzeSiblingEntries(
                    pool.getAsJsonArray(
                            "entries"
                    ),
                    0.0,
                    false,
                    direct,
                    references
            );
        }

        return new TableProfile(
                Map.copyOf(direct),
                List.copyOf(references)
        );
    }

    private static void analyzeSiblingEntries(
            JsonArray entries,
            double inheritedQuality,
            boolean inheritedSuppression,
            Map<Identifier, ItemProfile> direct,
            List<TableReference> references
    ) {
        int maxWeight = 1;

        for (JsonElement element
                : entries) {

            if (!element.isJsonObject()) {
                continue;
            }

            maxWeight =
                    Math.max(
                            maxWeight,
                            readWeight(
                                    element.getAsJsonObject()
                            )
                    );
        }

        int siblingCount =
                entries.size();

        for (JsonElement element
                : entries) {

            if (!element.isJsonObject()) {
                continue;
            }

            JsonObject entry =
                    element.getAsJsonObject();

            int weight =
                    readWeight(entry);

            QualitySignal signal =
                    qualitySignal(
                            entry,
                            maxWeight,
                            weight
                    );

            boolean suppressed =
                    inheritedSuppression
                            || signal.suppressChildren();

            double quality =
                    suppressed
                            ? 0.0
                            : combineQuality(
                                    inheritedQuality,
                                    signal.quality()
                            );

            scanEntry(
                    entry,
                    quality,
                    suppressed,
                    siblingCount > 1,
                    direct,
                    references
            );
        }
    }

    private static void scanEntry(
            JsonObject entry,
            double quality,
            boolean suppressChildren,
            boolean hasSiblings,
            Map<Identifier, ItemProfile> direct,
            List<TableReference> references
    ) {
        String type =
                readString(
                        entry,
                        "type"
                );

        if ("minecraft:item".equals(type)) {
            Identifier item =
                    parseIdentifier(
                            readString(
                                    entry,
                                    "name"
                            )
                    );

            if (item == null) {
                return;
            }

            /*
             * Quantity Luck is intentionally disabled for guaranteed
             * one-entry pools. This prevents things such as a guaranteed
             * unique reward from silently becoming 2 copies just because its
             * derived quality is 0.
             */
            boolean quantityEligible =
                    hasSiblings;

            direct.merge(
                    item,
                    new ItemProfile(
                            quality,
                            quantityEligible
                    ),
                    LootQualityRegistry::betterOf
            );

            return;
        }

        if ("minecraft:loot_table".equals(type)) {
            for (Identifier reference
                    : readLootTableReferences(entry)) {

                references.add(
                        new TableReference(
                                reference,
                                quality,
                                suppressChildren
                        )
                );
            }

            return;
        }

        /*
         * Composite entries: alternatives / sequence / group.
         */
        if (entry.has("children")
                && entry.get("children")
                .isJsonArray()) {

            analyzeSiblingEntries(
                    entry.getAsJsonArray(
                            "children"
                    ),
                    quality,
                    suppressChildren,
                    direct,
                    references
            );
        }
    }

    private static QualitySignal qualitySignal(
            JsonObject entry,
            int maxWeight,
            int weight
    ) {
        /*
         * Mojang already uses explicit quality semantically in fishing.
         * Respect it instead of interpreting low-weight junk as premium loot.
         */
        if (entry.has("quality")
                && entry.get("quality")
                .isJsonPrimitive()
                && entry.getAsJsonPrimitive("quality")
                .isNumber()) {

            int explicit =
                    entry.get("quality")
                            .getAsInt();

            if (explicit < 0) {
                return new QualitySignal(
                        0.0,
                        true
                );
            }

            if (explicit > 0) {
                return new QualitySignal(
                        Math.min(
                                1.0,
                                explicit / 2.0
                        ),
                        false
                );
            }
        }

        double weightQuality =
                qualityFromRatio(
                        (double) maxWeight
                                / Math.max(
                                        1,
                                        weight
                                )
                );

        double chanceQuality =
                conditionQuality(entry);

        return new QualitySignal(
                combineQuality(
                        weightQuality,
                        chanceQuality
                ),
                false
        );
    }

    private static double conditionQuality(
            JsonObject entry
    ) {
        if (entry.has("condition")) {
            return scanConditionForRarity(
                    entry.get("condition")
            );
        }

        /*
         * Compatibility with tables/datapacks authored during earlier 26.3
         * snapshots or older formats.
         */
        if (entry.has("conditions")) {
            return scanConditionForRarity(
                    entry.get("conditions")
            );
        }

        return 0.0;
    }

    private static double scanConditionForRarity(
            JsonElement element
    ) {
        if (element == null
                || element.isJsonNull()) {
            return 0.0;
        }

        if (element.isJsonArray()) {
            double best = 0.0;

            for (JsonElement child
                    : element.getAsJsonArray()) {

                best = Math.max(
                        best,
                        scanConditionForRarity(
                                child
                        )
                );
            }

            return best;
        }

        if (!element.isJsonObject()) {
            return 0.0;
        }

        JsonObject object =
                element.getAsJsonObject();

        String type =
                readString(
                        object,
                        "type"
                );

        if (type == null) {
            type =
                    readString(
                            object,
                            "condition"
                    );
        }

        if ("minecraft:random_chance".equals(type)
                && object.has("chance")
                && object.get("chance")
                .isJsonPrimitive()
                && object.getAsJsonPrimitive("chance")
                .isNumber()) {

            double chance =
                    object.get("chance")
                            .getAsDouble();

            if (chance > 0.0
                    && chance < 1.0) {

                return qualityFromRatio(
                        1.0 / chance
                );
            }
        }

        double best = 0.0;

        for (Map.Entry<String, JsonElement> child
                : object.entrySet()) {

            if ("chance".equals(child.getKey())) {
                continue;
            }

            best = Math.max(
                    best,
                    scanConditionForRarity(
                            child.getValue()
                    )
            );
        }

        return best;
    }

    private static double qualityFromRatio(
            double ratio
    ) {
        if (!Double.isFinite(ratio)
                || ratio <= 1.0) {
            return 0.0;
        }

        double maximumRatio =
                LootLuckRegistry
                        .settings()
                        .maxQualityWeightRatio();

        double quality =
                Math.log(ratio)
                        / Math.log(
                                maximumRatio
                        );

        return clamp01(quality);
    }

    private static double combineQuality(
            double a,
            double b
    ) {
        a = clamp01(a);
        b = clamp01(b);

        /*
         * Probabilistic union:
         * 0.5 parent + 0.5 child = 0.75, not 1.0.
         */
        return clamp01(
                1.0
                        - (1.0 - a)
                        * (1.0 - b)
        );
    }

    private static double clamp01(
            double value
    ) {
        return Math.max(
                0.0,
                Math.min(
                        1.0,
                        value
                )
        );
    }

    private static int readWeight(
            JsonObject entry
    ) {
        if (!entry.has("weight")
                || !entry.get("weight")
                .isJsonPrimitive()
                || !entry.getAsJsonPrimitive("weight")
                .isNumber()) {

            return 1;
        }

        return Math.max(
                1,
                entry.get("weight")
                        .getAsInt()
        );
    }

    private static @Nullable String readString(
            JsonObject object,
            String key
    ) {
        if (!object.has(key)
                || !object.get(key)
                .isJsonPrimitive()
                || !object.getAsJsonPrimitive(key)
                .isString()) {

            return null;
        }

        return object.get(key)
                .getAsString();
    }

    private static @Nullable Identifier parseIdentifier(
            @Nullable String raw
    ) {
        return raw == null
                ? null
                : Identifier.tryParse(raw);
    }

    private static List<Identifier> readLootTableReferences(
            JsonObject entry
    ) {
        List<Identifier> result =
                new ArrayList<>();

        JsonElement value =
                entry.get("value");

        if (value == null) {
            /*
             * Backwards compatibility with old loot-table entry syntax.
             */
            value = entry.get("name");
        }

        readReferenceElement(
                value,
                result
        );

        return result;
    }

    private static void readReferenceElement(
            @Nullable JsonElement value,
            List<Identifier> result
    ) {
        if (value == null
                || value.isJsonNull()) {
            return;
        }

        if (value.isJsonPrimitive()
                && value.getAsJsonPrimitive()
                .isString()) {

            Identifier id =
                    Identifier.tryParse(
                            value.getAsString()
                    );

            if (id != null) {
                result.add(id);
            }

            return;
        }

        if (value.isJsonArray()) {
            for (JsonElement child
                    : value.getAsJsonArray()) {

                readReferenceElement(
                        child,
                        result
                );
            }
        }
    }

    public record ItemProfile(
            double quality,
            boolean quantityEligible
    ) {
    }

    private record TableProfile(
            Map<Identifier, ItemProfile> direct,
            List<TableReference> references
    ) {
    }

    private record TableReference(
            Identifier tableId,
            double quality,
            boolean suppressChildren
    ) {
    }

    private record QualitySignal(
            double quality,
            boolean suppressChildren
    ) {
    }
}
