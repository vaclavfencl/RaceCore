package com.example.racecore.material;

import com.example.racecore.RaceCore;
import com.example.racecore.race.RaceManager;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BiomeTags;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.levelgen.Heightmap;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.level.BlockDropsEvent;
import net.neoforged.neoforge.event.level.ChunkEvent;
import net.neoforged.neoforge.event.tick.LevelTickEvent;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@EventBusSubscriber(modid = RaceCore.MODID)
public final class MithrilEvents {
    private static final int MIN_Y = -59;
    private static final int MAX_Y = -57;
    private static final int GENERATION_CHANCE_DENOMINATOR = 4;
    private static final int THIRD_ORE_CHANCE_DENOMINATOR = 10;
    private static final int FORTUNE_EXTRA_CHANCE_DENOMINATOR = 5;
    private static final int REQUIRED_EFFECTIVE_FORTUNE = 4;

    private static final Map<ResourceKey<Level>, Set<Long>> PENDING_CHUNKS =
            new ConcurrentHashMap<>();

    private MithrilEvents() {
    }

    @SubscribeEvent
    public static void onChunkLoad(ChunkEvent.Load event) {
        if (!event.isNewChunk()) {
            return;
        }

        LevelChunk chunk = event.getChunk();

        if (!(chunk.getLevel() instanceof ServerLevel level)) {
            return;
        }

        if (level.dimension() != Level.OVERWORLD) {
            return;
        }

        PENDING_CHUNKS
                .computeIfAbsent(
                        level.dimension(),
                        ignored -> ConcurrentHashMap.newKeySet()
                )
                .add(chunk.getPos().pack());
    }

    @SubscribeEvent
    public static void onLevelTick(LevelTickEvent.Post event) {
        if (!(event.getLevel() instanceof ServerLevel level)) {
            return;
        }

        Set<Long> pending = PENDING_CHUNKS.get(level.dimension());

        if (pending == null || pending.isEmpty()) {
            return;
        }

        for (long packed : new ArrayList<>(pending)) {
            if (!pending.remove(packed)) {
                continue;
            }

            ChunkPos chunkPos = ChunkPos.unpack(packed);

            if (level.getChunkSource()
                    .getChunkNow(chunkPos.x(), chunkPos.z()) == null) {
                continue;
            }

            generateMithril(level, chunkPos);
        }

        if (pending.isEmpty()) {
            PENDING_CHUNKS.remove(level.dimension(), pending);
        }
    }

    private static void generateMithril(ServerLevel level, ChunkPos chunkPos) {
        RandomSource random =
                RandomSource.create(mithrilSeed(level.getSeed(), chunkPos));

        List<Column> mountainColumns = findMountainColumns(level, chunkPos);

        if (mountainColumns.isEmpty()) {
            return;
        }

        if (random.nextInt(GENERATION_CHANCE_DENOMINATOR) != 0) {
            return;
        }

        List<BlockPos> replaceable = new ArrayList<>();

        for (Column column : mountainColumns) {
            for (int y = MIN_Y; y <= MAX_Y; y++) {
                BlockPos pos = new BlockPos(column.x(), y, column.z());

                if (level.getBlockState(pos)
                        .is(BlockTags.DEEPSLATE_ORE_REPLACEABLES)) {
                    replaceable.add(pos);
                }
            }
        }

        if (replaceable.size() < 2) {
            return;
        }

        Set<BlockPos> replaceableSet = new HashSet<>(replaceable);
        VeinPair pair = findConnectedPair(replaceable, replaceableSet, random);

        if (pair == null) {
            return;
        }

        level.setBlock(
                pair.first(),
                RaceCoreMaterials.MITHRIL_ORE.get().defaultBlockState(),
                3
        );

        level.setBlock(
                pair.second(),
                RaceCoreMaterials.MITHRIL_ORE.get().defaultBlockState(),
                3
        );

        if (random.nextInt(THIRD_ORE_CHANCE_DENOMINATOR) == 0) {
            BlockPos third = findThirdPosition(pair, replaceableSet, random);

            if (third != null) {
                level.setBlock(
                        third,
                        RaceCoreMaterials.MITHRIL_ORE.get().defaultBlockState(),
                        3
                );
            }
        }
    }

    private static List<Column> findMountainColumns(
            ServerLevel level,
            ChunkPos chunkPos
    ) {
        List<Column> result = new ArrayList<>();

        for (int localX = 0; localX < 16; localX++) {
            int x = chunkPos.getBlockX(localX);

            for (int localZ = 0; localZ < 16; localZ++) {
                int z = chunkPos.getBlockZ(localZ);

                int surfaceY =
                        level.getHeight(
                                Heightmap.Types.WORLD_SURFACE,
                                x,
                                z
                        );

                int biomeY =
                        Math.max(
                                level.getMinY(),
                                surfaceY - 1
                        );

                if (level.getBiome(new BlockPos(x, biomeY, z))
                        .is(BiomeTags.IS_MOUNTAIN)) {
                    result.add(new Column(x, z));
                }
            }
        }

        return result;
    }

    private static VeinPair findConnectedPair(
            List<BlockPos> candidates,
            Set<BlockPos> candidateSet,
            RandomSource random
    ) {
        int start = random.nextInt(candidates.size());

        for (int i = 0; i < candidates.size(); i++) {
            BlockPos first =
                    candidates.get((start + i) % candidates.size());

            List<BlockPos> neighbors =
                    connectedCandidates(
                            first,
                            candidateSet,
                            null,
                            null
                    );

            if (neighbors.isEmpty()) {
                continue;
            }

            BlockPos second =
                    neighbors.get(random.nextInt(neighbors.size()));

            return new VeinPair(first, second);
        }

        return null;
    }

    private static BlockPos findThirdPosition(
            VeinPair pair,
            Set<BlockPos> candidateSet,
            RandomSource random
    ) {
        List<BlockPos> candidates = new ArrayList<>();

        addUnique(
                candidates,
                connectedCandidates(
                        pair.first(),
                        candidateSet,
                        pair.first(),
                        pair.second()
                )
        );

        addUnique(
                candidates,
                connectedCandidates(
                        pair.second(),
                        candidateSet,
                        pair.first(),
                        pair.second()
                )
        );

        if (candidates.isEmpty()) {
            return null;
        }

        return candidates.get(random.nextInt(candidates.size()));
    }

    private static List<BlockPos> connectedCandidates(
            BlockPos origin,
            Set<BlockPos> candidateSet,
            BlockPos excludedA,
            BlockPos excludedB
    ) {
        List<BlockPos> result = new ArrayList<>();

        for (Direction direction : Direction.values()) {
            BlockPos candidate = origin.relative(direction);

            if (candidate.equals(excludedA)
                    || candidate.equals(excludedB)) {
                continue;
            }

            if (candidateSet.contains(candidate)) {
                result.add(candidate);
            }
        }

        return result;
    }

    private static void addUnique(
            List<BlockPos> target,
            List<BlockPos> additions
    ) {
        for (BlockPos pos : additions) {
            if (!target.contains(pos)) {
                target.add(pos);
            }
        }
    }

    private static long mithrilSeed(long worldSeed, ChunkPos chunkPos) {
        long seed = worldSeed;

        seed ^= (long) chunkPos.x() * 341873128712L;
        seed ^= (long) chunkPos.z() * 132897987541L;
        seed ^= 0x4D49544852494C4CL;

        return seed;
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onMithrilDrops(BlockDropsEvent event) {
        if (!event.getState().is(RaceCoreMaterials.MITHRIL_ORE.get())) {
            return;
        }

        ItemStack tool = event.getTool();

        if (tool.isEmpty()) {
            return;
        }

        Holder.Reference<Enchantment> fortune =
                event.getLevel()
                        .registryAccess()
                        .lookupOrThrow(Registries.ENCHANTMENT)
                        .getOrThrow(Enchantments.FORTUNE);

        int effectiveFortune = tool.getEnchantmentLevel(fortune);

        if (event.getBreaker() instanceof Player player) {
            effectiveFortune +=
                    RaceManager.getRace(player).fortuneBonus();
        }

        if (effectiveFortune < REQUIRED_EFFECTIVE_FORTUNE) {
            return;
        }

        if (event.getLevel()
                .getRandom()
                .nextInt(FORTUNE_EXTRA_CHANCE_DENOMINATOR) != 0) {
            return;
        }

        for (ItemEntity drop : event.getDrops()) {
            ItemStack stack = drop.getItem();

            if (stack.is(RaceCoreMaterials.MITHRIL_ORE_ITEM.get())) {
                stack.grow(1);
                return;
            }
        }
    }

    private record Column(int x, int z) {
    }

    private record VeinPair(BlockPos first, BlockPos second) {
    }
}
