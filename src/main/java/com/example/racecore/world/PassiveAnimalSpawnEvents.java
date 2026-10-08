package com.example.racecore.world;

import com.example.racecore.RaceCore;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BiomeTags;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.clock.WorldClock;
import net.minecraft.world.clock.WorldClocks;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.block.TurtleEggBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.MobSpawnEvent;

/**
 * Phase 12.3: passive animals may naturally spawn at night in the Overworld,
 * at roughly 40% of the daytime success rate.
 *
 * Vanilla creature spawning cadence, biome selection, mob caps, player
 * distance rules, collision checks and group behavior remain in charge.
 *
 * The only rule RaceCore changes is the nighttime light gate:
 * otherwise-valid passive animal placement checks are allowed to succeed in
 * darkness, then throttled to 40%.
 */
@EventBusSubscriber(modid = RaceCore.MODID)
public final class PassiveAnimalSpawnEvents {
    private static final float NIGHT_PASSIVE_MULTIPLIER = 0.40F;

    private static final long DAY_TICKS = 24_000L;
    private static final long NIGHT_START = 13_000L;
    private static final long NIGHT_END = 23_000L;

    private PassiveAnimalSpawnEvents() {
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onSpawnPlacementCheck(
            MobSpawnEvent.SpawnPlacementCheck event
    ) {
        if (event.getSpawnType() != EntitySpawnReason.NATURAL) {
            return;
        }

        EntityType<?> type = event.getEntityType();

        if (type.getCategory() != MobCategory.CREATURE) {
            return;
        }

        ServerLevelAccessor level = event.getLevel();
        ServerLevel serverLevel = level.getLevel();

        if (serverLevel.dimension() != Level.OVERWORLD) {
            return;
        }

        if (!isRaceCoreNight(serverLevel)) {
            return;
        }

        /* Respect an explicit decision made by another event handler. */
        if (event.getResult()
                != MobSpawnEvent.SpawnPlacementCheck.Result.DEFAULT) {
            return;
        }

        boolean vanillaPassed = event.getDefaultResult();

        /*
         * If vanilla already accepted the spawn, all of its ordinary rules
         * have passed. If it rejected the spawn, only reopen it for vanilla
         * passive-animal predicates whose non-light requirements we can
         * reproduce exactly.
         */
        boolean validWithoutNightLight =
                vanillaPassed
                        || passesKnownNonLightRules(
                                type,
                                level,
                                event.getPos()
                        );

        if (!validWithoutNightLight) {
            return;
        }

        if (event.getRandom().nextFloat() >= NIGHT_PASSIVE_MULTIPLIER) {
            event.setResult(
                    MobSpawnEvent.SpawnPlacementCheck.Result.FAIL
            );
            return;
        }

        /*
         * Vanilla failed only because the known animal predicate requires
         * daylight brightness. Force that otherwise-valid nighttime attempt
         * through. If vanilla already passed, leaving DEFAULT is enough.
         */
        if (!vanillaPassed) {
            event.setResult(
                    MobSpawnEvent.SpawnPlacementCheck.Result.SUCCEED
            );
        }
    }

    private static boolean passesKnownNonLightRules(
            EntityType<?> type,
            ServerLevelAccessor level,
            BlockPos pos
    ) {
        BlockState below = level.getBlockState(pos.below());

        /* Vanilla Animal::checkAnimalSpawnRules: ground tag + brightness. */
        if (type == EntityTypes.CHICKEN
                || type == EntityTypes.COW
                || type == EntityTypes.DONKEY
                || type == EntityTypes.HORSE
                || type == EntityTypes.LLAMA
                || type == EntityTypes.MULE
                || type == EntityTypes.PIG
                || type == EntityTypes.SHEEP
                || type == EntityTypes.CAT
                || type == EntityTypes.PANDA) {

            return below.is(BlockTags.ANIMALS_SPAWNABLE_ON);
        }

        if (type == EntityTypes.ARMADILLO) {
            return below.is(BlockTags.ARMADILLO_SPAWNABLE_ON);
        }

        if (type == EntityTypes.CAMEL) {
            return below.is(BlockTags.CAMELS_SPAWNABLE_ON);
        }

        if (type == EntityTypes.FROG) {
            return below.is(BlockTags.FROGS_SPAWNABLE_ON);
        }

        if (type == EntityTypes.GOAT) {
            return below.is(BlockTags.GOATS_SPAWNABLE_ON);
        }

        if (type == EntityTypes.MOOSHROOM) {
            return below.is(BlockTags.MOOSHROOMS_SPAWNABLE_ON);
        }

        if (type == EntityTypes.PARROT) {
            return below.is(BlockTags.PARROTS_SPAWNABLE_ON);
        }

        if (type == EntityTypes.RABBIT) {
            return below.is(BlockTags.RABBITS_SPAWNABLE_ON);
        }

        if (type == EntityTypes.WOLF) {
            return below.is(BlockTags.WOLVES_SPAWNABLE_ON);
        }

        if (type == EntityTypes.FOX) {
            return below.is(BlockTags.FOXES_SPAWNABLE_ON);
        }

        if (type == EntityTypes.POLAR_BEAR) {
            if (level.getBiome(pos)
                    .is(BiomeTags.POLAR_BEARS_SPAWN_ON_ALTERNATE_BLOCKS)) {

                return below.is(
                        BlockTags.POLAR_BEARS_SPAWNABLE_ON_ALTERNATE
                );
            }

            return below.is(BlockTags.ANIMALS_SPAWNABLE_ON);
        }

        if (type == EntityTypes.TURTLE) {
            return pos.getY() < level.getSeaLevel() + 4
                    && TurtleEggBlock.onSand(level, pos);
        }

        /*
         * Ocelots deliberately are not reopened here: their vanilla
         * placement predicate is a random 2/3 chance rather than a brightness
         * check. If vanilla accepts an ocelot attempt it is still throttled
         * to 40% above, but a failed vanilla ocelot roll remains failed.
         *
         * Unknown/modded CREATURE predicates are also not force-overridden.
         * If their own predicate succeeds naturally at night, they are
         * throttled to 40%; if it fails, RaceCore leaves it alone.
         */
        return false;
    }

    private static boolean isRaceCoreNight(ServerLevel level) {
        MinecraftServer server = level.getServer();

        Holder<WorldClock> overworldClock =
                server.registryAccess()
                        .getOrThrow(WorldClocks.OVERWORLD);

        long totalTicks =
                server.clockManager()
                        .getInstance(overworldClock)
                        .totalTicks();

        long timeOfDay = Math.floorMod(totalTicks, DAY_TICKS);

        return timeOfDay >= NIGHT_START
                && timeOfDay < NIGHT_END;
    }
}
