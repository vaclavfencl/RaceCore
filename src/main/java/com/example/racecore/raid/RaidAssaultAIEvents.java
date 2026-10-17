package com.example.racecore.raid;

import com.example.racecore.RaceCore;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.monster.Ravager;
import net.minecraft.world.entity.monster.illager.Evoker;
import net.minecraft.world.entity.monster.illager.Vindicator;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.entity.raid.Raider;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.gamerules.GameRules;
import net.minecraft.world.level.pathfinder.Path;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import net.neoforged.neoforge.event.tick.EntityTickEvent;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.Set;
import java.util.UUID;
import java.util.WeakHashMap;

/**
 * Raid-only tactical abilities for Minecraft 26.3 / NeoForge 26.3.
 * No vanilla entity class replacement, no mixins, and no effects outside active raids.
 */
@EventBusSubscriber(modid = RaceCore.MODID)
public final class RaidAssaultAIEvents {
    private static final DustParticleOptions GREEN = new DustParticleOptions(0x39FF70, 1.6F);
    private static final DustParticleOptions DARK_GREEN = new DustParticleOptions(0x0C9242, 1.1F);
    private static final Map<Ravager, RavagerCharge> CHARGES = new WeakHashMap<>();

    private RaidAssaultAIEvents() {}

    @SubscribeEvent
    public static void onJoin(EntityJoinLevelEvent event) {
        if (event.getLevel().isClientSide()) return;
        if (event.getEntity() instanceof Vindicator vindicator) {
            if (vindicator.goalSelector.getAvailableGoals().stream()
                    .noneMatch(g -> g.getGoal() instanceof VindicatorBreachGoal)) {
                vindicator.goalSelector.addGoal(2, new VindicatorBreachGoal(vindicator));
            }
        } else if (event.getEntity() instanceof Evoker evoker) {
            if (evoker.goalSelector.getAvailableGoals().stream()
                    .noneMatch(g -> g.getGoal() instanceof EvokerAbductionGoal)) {
                evoker.goalSelector.addGoal(2, new EvokerAbductionGoal(evoker));
            }
        }
    }

    @SubscribeEvent
    public static void afterMobTick(EntityTickEvent.Post event) {
        if (event.getEntity() instanceof Ravager ravager
                && ravager.level() instanceof ServerLevel level) {
            if (!active(ravager)) {
                CHARGES.remove(ravager);
                return;
            }
            CHARGES.computeIfAbsent(ravager, key -> new RavagerCharge()).tick(level, ravager);
        }
    }

    @SubscribeEvent
    public static void afterDamage(LivingDamageEvent.Post event) {
        if (event.getHealthDamage() <= 0.0F || !(event.getEntity() instanceof Evoker evoker)) return;
        for (var wrapped : evoker.goalSelector.getAvailableGoals()) {
            if (wrapped.getGoal() instanceof EvokerAbductionGoal goal) {
                goal.interrupt();
            }
        }
    }

    private static boolean active(Raider raider) {
        return raider.isAlive() && raider.getCurrentRaid() != null
                && !raider.getCurrentRaid().isOver();
    }

    private static boolean griefAllowed(ServerLevel level) {
        return level.getGameRules().get(GameRules.MOB_GRIEFING);
    }

    /** Identify villagers in range, but require a real failed vanilla path before any special power. */
    private static Villager unreachableVillager(Raider raider, ServerLevel level, double radius) {
        List<Villager> villagers = level.getEntitiesOfClass(Villager.class,
                raider.getBoundingBox().inflate(radius, 8.0, radius), Villager::isAlive);
        villagers.sort(Comparator.comparingDouble(raider::distanceToSqr));
        for (Villager villager : villagers) {
            Path path = raider.getNavigation().createPath(villager, 0);
            if (path == null || !path.canReach()) return villager;
        }
        return null;
    }

    private static Villager randomUnreachableVillager(Evoker evoker, ServerLevel level, double radius) {
        List<Villager> villagers = level.getEntitiesOfClass(Villager.class,
                evoker.getBoundingBox().inflate(radius, 8.0, radius), Villager::isAlive);
        if (villagers.isEmpty()) return null;
        // Do not bias towards the same villager on every cast.
        java.util.Collections.shuffle(villagers, new java.util.Random(evoker.getRandom().nextLong()));
        for (Villager villager : villagers) {
            Path path = evoker.getNavigation().createPath(villager, 0);
            if (path == null || !path.canReach()) return villager;
        }
        return null;
    }

    /**
     * A* corridor search, four cardinal directions, and 1-block step changes.
     * The total cost combines walking distance with hardness-based iron-axe mining time.
     * Every obstacle in the two-high corridor counts towards MAX_BREAKS=4.
     * Stops after a bounded number of node expansions, rather than searching indefinitely.
     */
    private static final class BreachPlanner {
        private static final int MAX_BREAKS = 4;
        private static final int EXPANSION_LIMIT = 3500;
        private static final int MAX_HORIZONTAL = 28;
        private static final int MAX_Y = 5;
        private static final int[] DX = {1, -1, 0, 0};
        private static final int[] DZ = {0, 0, 1, -1};

        private record State(BlockPos pos, int broken, double cost, State parent,
                             List<BlockPos> cellBlocks) {}
        private record Key(BlockPos pos, int broken) {}
        private record Stage(BlockPos staging, List<BlockPos> blocks) {}
        private record Result(List<Stage> stages, int totalBlocks) {}

        private static Result find(ServerLevel level, Vindicator vindicator, Villager villager) {
            BlockPos origin = vindicator.blockPosition();
            BlockPos destination = villager.blockPosition();
            if (Math.abs(origin.getX() - destination.getX()) > MAX_HORIZONTAL
                    || Math.abs(origin.getZ() - destination.getZ()) > MAX_HORIZONTAL
                    || Math.abs(origin.getY() - destination.getY()) > MAX_Y) return null;

            PriorityQueue<State> frontier = new PriorityQueue<>(Comparator.comparingDouble(
                    s -> s.cost() + heuristic(s.pos(), destination)));
            Map<Key, Double> best = new HashMap<>();
            State first = new State(origin, 0, 0.0, null, List.of());
            frontier.add(first);
            best.put(new Key(origin, 0), 0.0);
            int expansions = 0;

            while (!frontier.isEmpty() && ++expansions <= EXPANSION_LIMIT) {
                State state = frontier.poll();
                if (state.cost() > best.getOrDefault(new Key(state.pos(), state.broken()),
                        Double.POSITIVE_INFINITY)) continue;
                if (Math.abs(state.pos().getX() - destination.getX()) <= 1
                        && Math.abs(state.pos().getZ() - destination.getZ()) <= 1
                        && Math.abs(state.pos().getY() - destination.getY()) <= 1) {
                    if (state.broken() > 0) return extract(state);
                    continue;
                }

                for (int direction = 0; direction < 4; direction++) {
                    int x = state.pos().getX() + DX[direction];
                    int z = state.pos().getZ() + DZ[direction];
                    if (Math.abs(x - origin.getX()) > MAX_HORIZONTAL
                            || Math.abs(z - origin.getZ()) > MAX_HORIZONTAL) continue;
                    for (int vertical = -1; vertical <= 1; vertical++) {
                        int y = state.pos().getY() + vertical;
                        if (Math.abs(y - origin.getY()) > MAX_Y) continue;
                        BlockPos foot = new BlockPos(x, y, z);
                        if (!level.isInWorldBounds(foot) || !level.isInWorldBounds(foot.above())
                                || !level.hasChunkAt(foot)) continue;
                        BlockPos support = foot.below();
                        if (!level.getBlockState(support).isFaceSturdy(level, support, Direction.UP)) continue;

                        ArrayList<BlockPos> obstructing = new ArrayList<>(2);
                        double extraCost = 0.0;
                        boolean impossible = false;
                        for (BlockPos pos : List.of(foot, foot.above())) {
                            BlockState block = level.getBlockState(pos);
                            if (block.getCollisionShape(level, pos).isEmpty()) continue;
                            if (!breakable(level, pos, Float.POSITIVE_INFINITY)) {
                                impossible = true;
                                break;
                            }
                            obstructing.add(pos);
                            extraCost += miningTicks(block.getDestroySpeed(level, pos)) / 12.0;
                        }
                        if (impossible) continue;
                        int newBroken = state.broken() + obstructing.size();
                        if (newBroken > MAX_BREAKS) continue;
                        double newCost = state.cost() + 1.0 + Math.abs(vertical) * 1.5 + extraCost;
                        Key nextKey = new Key(foot, newBroken);
                        if (newCost >= best.getOrDefault(nextKey, Double.POSITIVE_INFINITY)) continue;
                        best.put(nextKey, newCost);
                        frontier.add(new State(foot, newBroken, newCost, state, obstructing));
                    }
                }
            }
            return null;
        }

        private static double heuristic(BlockPos a, BlockPos b) {
            return Math.abs(a.getX() - b.getX()) + Math.abs(a.getZ() - b.getZ())
                    + Math.abs(a.getY() - b.getY()) * 1.5;
        }

        private static Result extract(State end) {
            List<State> chain = new ArrayList<>();
            for (State cur = end; cur != null; cur = cur.parent()) chain.add(cur);
            java.util.Collections.reverse(chain);
            List<Stage> stages = new ArrayList<>();
            for (int i = 1; i < chain.size(); i++) {
                if (!chain.get(i).cellBlocks().isEmpty()) {
                    stages.add(new Stage(chain.get(i - 1).pos(),
                            List.copyOf(chain.get(i).cellBlocks())));
                }
            }
            return stages.isEmpty() ? null : new Result(List.copyOf(stages), end.broken());
        }
    }

    private static boolean breakable(ServerLevel level, BlockPos pos, float maxHardness) {
        if (!level.hasChunkAt(pos) || !level.isInWorldBounds(pos)) return false;
        BlockState state = level.getBlockState(pos);
        if (state.isAir()) return false;
        float hardness = state.getDestroySpeed(level, pos);
        // Tile entities (chests, spawners, etc.) are not used as excavation paths.
        return hardness >= 0.0F && hardness <= maxHardness
                && level.getBlockEntity(pos) == null
                && level.getFluidState(pos).isEmpty();
    }

    // No Efficiency/Haste. Pretend an iron axe has mining speed 6 on every solid material;
    // do not require the correct tool type. Vanilla-like hardness scaling, with a startup cost.
    private static int miningTicks(float hardness) {
        return Math.max(10, (int) Math.ceil(30.0F * Math.max(0.1F, hardness) / 6.0F));
    }

    private static final class VindicatorBreachGoal extends Goal {
        private final Vindicator mob;
        private Villager target;
        private BreachPlanner.Result plan;
        private int scanCooldown;
        private int mining;
        private int stageIndex;
        private int blockIndex;
        private boolean reached;
        private BlockPos current;

        private VindicatorBreachGoal(Vindicator mob) {
            this.mob = mob;
            setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
        }

        @Override
        public boolean canUse() {
            if (!(mob.level() instanceof ServerLevel level) || !active(mob)
                    || !griefAllowed(level)) return false;
            if (scanCooldown-- > 0) return false;
            scanCooldown = 55;
            target = unreachableVillager(mob, level, 28);
            plan = target == null ? null : BreachPlanner.find(level, mob, target);
            return plan != null;
        }

        @Override
        public void start() {
            stageIndex = 0;
            blockIndex = 0;
            mining = 0;
            current = null;
            reached = false;
        }

        @Override
        public boolean canContinueToUse() {
            return target != null && target.isAlive() && plan != null
                    && active(mob) && mob.level() instanceof ServerLevel level
                    && griefAllowed(level) && stageIndex < plan.stages().size();
        }

        @Override
        public void tick() {
            if (!(mob.level() instanceof ServerLevel level) || plan == null) return;
            BreachPlanner.Stage stage = plan.stages().get(stageIndex);
            BlockPos staging = stage.staging();
            mob.getLookControl().setLookAt(staging.getX() + 0.5, staging.getY() + 1.0,
                    staging.getZ() + 0.5);
            double distance = mob.distanceToSqr(staging.getX() + 0.5,
                    staging.getY(), staging.getZ() + 0.5);
            if (distance > 3.2 * 3.2) {
                if (mob.tickCount % 16 == 0 || !reached) {
                    mob.getNavigation().moveTo(staging.getX() + 0.5, staging.getY(),
                            staging.getZ() + 0.5, 1.05);
                }
                reached = true;
                return;
            }
            mob.getNavigation().stop();
            reached = true;
            BlockPos candidate = stage.blocks().get(blockIndex);
            if (!breakable(level, candidate, Float.POSITIVE_INFINITY)) {
                if (level.getBlockState(candidate).getCollisionShape(level, candidate).isEmpty()) {
                    advance(level);
                } else {
                    stageIndex = plan.stages().size();
                }
                return;
            }
            if (!candidate.equals(current)) {
                current = candidate;
                mining = 0;
            }
            int required = miningTicks(level.getBlockState(candidate).getDestroySpeed(level, candidate));
            mining++;
            if (mining % 4 == 0) {
                level.destroyBlockProgress(mob.getId(), candidate, Math.min(9, mining * 10 / required));
                level.sendParticles(ParticleTypes.CRIT, candidate.getX() + 0.5,
                        candidate.getY() + 0.5, candidate.getZ() + 0.5,
                        2, 0.25, 0.25, 0.25, 0.02);
            }
            mob.getLookControl().setLookAt(candidate.getX() + 0.5,
                    candidate.getY() + 0.5, candidate.getZ() + 0.5);
            if (mining >= required) {
                // No block drops from raid excavation; avoid renewable loot farms.
                level.destroyBlock(candidate, false);
                advance(level);
            }
        }

        private void advance(ServerLevel level) {
            if (current != null) level.destroyBlockProgress(mob.getId(), current, -1);
            mining = 0;
            current = null;
            blockIndex++;
            if (plan != null && stageIndex < plan.stages().size()
                    && blockIndex >= plan.stages().get(stageIndex).blocks().size()) {
                stageIndex++;
                blockIndex = 0;
                reached = false;
            }
        }

        @Override
        public void stop() {
            if (mob.level() instanceof ServerLevel level && current != null) {
                level.destroyBlockProgress(mob.getId(), current, -1);
            }
            current = null;
            target = null;
            plan = null;
            mining = 0;
            scanCooldown = 15; // Recheck whether the newly opened path works normally.
            mob.getNavigation().stop();
        }
    }

    /** Five-second cancellable telegraph with a blinking green pseudo-beacon at both ends. */
    private static final class EvokerAbductionGoal extends Goal {
        private static final int CAST_TICKS = 100;
        private final Evoker mob;
        private Villager target;
        private int remaining;
        private int cooldown = 80;
        private boolean interrupted;

        private EvokerAbductionGoal(Evoker mob) {
            this.mob = mob;
            setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
        }

        @Override
        public boolean canUse() {
            if (!(mob.level() instanceof ServerLevel level) || !active(mob)) return false;
            if (cooldown-- > 0) return false;
            cooldown = 50;
            target = randomUnreachableVillager(mob, level, 32);
            return target != null && hasLanding(level, mob, target);
        }

        @Override
        public void start() {
            remaining = CAST_TICKS;
            interrupted = false;
            mob.getNavigation().stop();
            if (mob.level() instanceof ServerLevel level) {
                level.playSound(null, mob.blockPosition(), SoundEvents.EVOKER_PREPARE_SUMMON,
                        SoundSource.HOSTILE, 2.5F, 0.75F);
                // Deliberate opening flash visible across a village.
                drawColumn(level, mob.position(), 3.8, 22, true);
                if (target != null) drawColumn(level, target.position(), 3.5, 22, true);
            }
        }

        @Override
        public boolean canContinueToUse() {
            return !interrupted && remaining > 0 && target != null && target.isAlive()
                    && active(mob) && mob.distanceToSqr(target) < 42 * 42;
        }

        @Override
        public void tick() {
            if (!(mob.level() instanceof ServerLevel level) || target == null) return;
            remaining--;
            mob.getNavigation().stop();
            mob.getLookControl().setLookAt(target, 40, 40);
            int elapsed = CAST_TICKS - remaining;
            // Blinking visual pulses accelerate during the last two seconds.
            int period = elapsed < 40 ? 10 : elapsed < 75 ? 6 : 3;
            if (elapsed % period == 0) {
                boolean bright = (elapsed / period) % 2 == 0;
                drawColumn(level, mob.position(), 3.8, bright ? 14 : 8, bright);
                drawColumn(level, target.position(), 3.5, bright ? 14 : 8, bright);
                if (bright && elapsed % 12 == 0) {
                    level.playSound(null, mob.blockPosition(), SoundEvents.BEACON_AMBIENT,
                            SoundSource.HOSTILE, 1.25F, 1.1F + (elapsed / 120.0F));
                }
            }
            if (remaining == 0) {
                BlockPos safe = landing(level, mob, target);
                if (safe != null && target.isAlive()) {
                    Vec3 before = target.position();
                    target.teleportTo(safe.getX() + 0.5, safe.getY(), safe.getZ() + 0.5);
                    level.sendParticles(ParticleTypes.PORTAL, before.x, before.y + 0.9,
                            before.z, 100, 0.6, 1.0, 0.6, 0.3);
                    level.sendParticles(ParticleTypes.PORTAL, target.getX(),
                            target.getY() + 0.9, target.getZ(), 100, 0.6, 1.0, 0.6, 0.3);
                    drawColumn(level, target.position(), 4.4, 32, true);
                    level.playSound(null, mob.blockPosition(), SoundEvents.EVOKER_CAST_SPELL,
                            SoundSource.HOSTILE, 2.5F, 0.8F);
                }
            }
        }

        public void interrupt() {
            if (remaining > 0) interrupted = true;
        }

        @Override
        public void stop() {
            if (interrupted && mob.level() instanceof ServerLevel level) {
                level.playSound(null, mob.blockPosition(), SoundEvents.FIRE_EXTINGUISH,
                        SoundSource.HOSTILE, 1.5F, 0.85F);
                level.sendParticles(ParticleTypes.SMOKE, mob.getX(), mob.getY() + 1.5,
                        mob.getZ(), 25, 0.7, 0.7, 0.7, 0.07);
            }
            remaining = 0;
            target = null;
            cooldown = interrupted ? 160 : 360;
            interrupted = false;
        }

        private static void drawColumn(ServerLevel level, Vec3 center, double height,
                                       int particles, boolean bright) {
            // A green column, double rings and drifting end-cap sparks. No actual
            // beacon block/light placement, so terrain and sky access are untouched.
            DustParticleOptions color = bright ? GREEN : DARK_GREEN;
            for (int i = 0; i < particles; i++) {
                double angle = i * (Math.PI * 2.0 / particles);
                double offsetX = Math.cos(angle) * 0.85;
                double offsetZ = Math.sin(angle) * 0.85;
                for (int ring = 0; ring < 2; ring++) {
                    level.sendParticles(color, center.x + offsetX,
                            center.y + 0.25 + ring * 2.0, center.z + offsetZ,
                            1, 0, 0, 0, 0);
                }
                double columnY = center.y + 0.25 + (i * height / particles);
                level.sendParticles(color, center.x + offsetX * 0.2, columnY,
                        center.z + offsetZ * 0.2, 1, 0, 0, 0, 0);
            }
            level.sendParticles(ParticleTypes.ENCHANT, center.x, center.y + 2.0,
                    center.z, 8, 0.65, 1.7, 0.65, 0.1);
        }

        private static boolean hasLanding(ServerLevel level, Evoker mob, Villager villager) {
            return landing(level, mob, villager) != null;
        }

        private static BlockPos landing(ServerLevel level, Evoker mob, Villager villager) {
            BlockPos base = mob.blockPosition();
            List<BlockPos> choices = new ArrayList<>();
            for (int x = -4; x <= 4; x++) {
                for (int z = -4; z <= 4; z++) {
                    if (Math.abs(x) + Math.abs(z) < 2) continue;
                    for (int y = -1; y <= 1; y++) {
                        BlockPos at = base.offset(x, y, z);
                        if (!level.hasChunkAt(at) || !level.isInWorldBounds(at.above())) continue;
                        if (!level.getBlockState(at).getCollisionShape(level, at).isEmpty()) continue;
                        if (!level.getBlockState(at.above()).getCollisionShape(level, at.above()).isEmpty()) continue;
                        if (!level.getFluidState(at).isEmpty()) continue;
                        BlockPos ground = at.below();
                        if (!level.getBlockState(ground).isFaceSturdy(level, ground, Direction.UP)) continue;
                        if (!level.getEntities((net.minecraft.world.entity.Entity) null,
                                new AABB(at).inflate(0.15),
                                entity -> entity != villager && entity.isAlive()).isEmpty()) continue;
                        choices.add(at.immutable());
                    }
                }
            }
            if (choices.isEmpty()) return null;
            return choices.get(mob.getRandom().nextInt(choices.size()));
        }
    }

    private static final class RavagerCharge {
        private static final int CHARGE_WINDUP = 20;
        private static final int CHARGE_TICKS = 28;
        private static final int COOLDOWN_TICKS = 15 * 20;
        private int cooldown = 60;
        private int windup;
        private int charging;
        private Vec3 direction = Vec3.ZERO;
        private final Set<UUID> hit = new HashSet<>();

        private void tick(ServerLevel level, Ravager ravager) {
            if (windup > 0) {
                windup--;
                ravager.getNavigation().stop();
                ravager.setDeltaMovement(0.0, ravager.getDeltaMovement().y, 0.0);
                if (windup % 5 == 0) {
                    level.sendParticles(ParticleTypes.ANGRY_VILLAGER,
                            ravager.getX(), ravager.getY() + ravager.getBbHeight(),
                            ravager.getZ(), 8, 0.7, 0.5, 0.7, 0.02);
                }
                if (windup == 0) {
                    charging = CHARGE_TICKS;
                    hit.clear();
                    level.playSound(null, ravager.blockPosition(), SoundEvents.RAVAGER_ROAR,
                            SoundSource.HOSTILE, 2.4F, 0.8F);
                }
                return;
            }
            if (charging > 0) {
                charging--;
                ravager.getNavigation().stop();
                // The wall directly ahead stops the charge even if it is too strong to break.
                BlockPos frontFoot = BlockPos.containing(ravager.getX() + direction.x * 1.65,
                        ravager.getY() + 0.25, ravager.getZ() + direction.z * 1.65);
                if (hitsWall(level, frontFoot)) {
                    if (griefAllowed(level)) smashWall(level, ravager, frontFoot, direction);
                    stop(ravager);
                    return;
                }
                ravager.setDeltaMovement(direction.x * 0.72,
                        ravager.getDeltaMovement().y, direction.z * 0.72);
                for (LivingEntity victim : level.getEntitiesOfClass(LivingEntity.class,
                        ravager.getBoundingBox().inflate(0.8),
                        entity -> entity.isAlive() && entity != ravager && !(entity instanceof Raider))) {
                    if (!hit.add(victim.getUUID())) continue;
                    victim.hurtServer(level, level.damageSources().mobAttack(ravager),
                            (float) (ravager.getAttributeValue(Attributes.ATTACK_DAMAGE) * 2.0));
                    victim.knockback(
                            2.0,
                            -direction.x,
                            -direction.z,
                            level.damageSources().mobAttack(ravager),
                            1.0F
                    );
                }
                if (charging == 0) stop(ravager);
                return;
            }
            if (cooldown-- > 0) return;
            LivingEntity target = ravager.getTarget();
            if (target == null || !target.isAlive()) {
                List<Villager> villagers = level.getEntitiesOfClass(Villager.class,
                        ravager.getBoundingBox().inflate(17, 6, 17), Villager::isAlive);
                target = villagers.stream().min(Comparator.comparingDouble(ravager::distanceToSqr))
                        .orElse(null);
            }
            if (target == null || ravager.distanceToSqr(target) < 4.0
                    || ravager.distanceToSqr(target) > 17 * 17) {
                cooldown = 30;
                return;
            }
            Vec3 delta = target.position().subtract(ravager.position());
            Vec3 horizontal = new Vec3(delta.x, 0, delta.z);
            if (horizontal.lengthSqr() < 0.05) return;
            direction = horizontal.normalize();
            windup = CHARGE_WINDUP;
            level.playSound(null, ravager.blockPosition(), SoundEvents.RAVAGER_ROAR,
                    SoundSource.HOSTILE, 1.8F, 0.65F);
        }

        private static boolean hitsWall(ServerLevel level, BlockPos foot) {
            for (BlockPos check : List.of(foot, foot.above())) {
                if (!level.hasChunkAt(check)) return true;
                if (!level.getBlockState(check).getCollisionShape(level, check).isEmpty()) return true;
            }
            return false;
        }

        private static void smashWall(ServerLevel level, Ravager ravager, BlockPos center,
                                      Vec3 direction) {
            // Plane perpendicular to the charge vector; exactly 3 across x 3 tall.
            boolean alongX = Math.abs(direction.x) >= Math.abs(direction.z);
            int destroyed = 0;
            for (int across = -1; across <= 1; across++) {
                for (int up = 0; up <= 2; up++) {
                    BlockPos at = alongX ? center.offset(0, up, across)
                            : center.offset(across, up, 0);
                    if (!breakable(level, at, 1.5F)) continue;
                    level.destroyBlock(at, false);
                    destroyed++;
                }
            }
            if (destroyed > 0) {
                level.sendParticles(ParticleTypes.POOF,
                        center.getX() + 0.5, center.getY() + 0.5, center.getZ() + 0.5,
                        35, 1.15, 1.15, 1.15, 0.13);
                level.playSound(null, center, SoundEvents.RAVAGER_ATTACK,
                        SoundSource.HOSTILE, 2.0F, 0.8F);
            }
        }

        private void stop(Ravager mob) {
            charging = 0;
            windup = 0;
            cooldown = COOLDOWN_TICKS;
            mob.setDeltaMovement(0.0, mob.getDeltaMovement().y, 0.0);
        }
    }
}
