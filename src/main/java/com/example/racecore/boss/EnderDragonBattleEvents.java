package com.example.racecore.boss;

import com.example.racecore.RaceCore;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.boss.enderdragon.EndCrystal;
import net.minecraft.world.entity.boss.enderdragon.EnderDragon;
import net.minecraft.world.entity.boss.enderdragon.phases.EnderDragonPhase;
import net.minecraft.world.entity.monster.Phantom;
import net.minecraft.world.entity.projectile.hurtingprojectile.DragonFireball;
import net.minecraft.world.entity.projectile.hurtingprojectile.LargeFireball;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.EntityLeaveLevelEvent;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.server.ServerStoppingEvent;
import net.neoforged.neoforge.event.tick.EntityTickEvent;

/**
 * RaceCore Ender Dragon encounter, Minecraft 26.3 / NeoForge 26.3.
 * Uses existing vanilla dragon phases, models, dragon breath and ghast fireballs.
 * It deliberately does not interact with RaceCore player hunts.
 */
@EventBusSubscriber(modid = RaceCore.MODID)
public final class EnderDragonBattleEvents {
    private static final String PHANTOM_TAG = "racecore_dragon_phantom";
    private static final String BALL_TAG = "racecore_dragon_reflectable";
    private static final String INTERRUPT_TAG = "racecore_dragon_66_assault_started";
    private static final String INTERRUPT_COMPLETED_TAG = "racecore_dragon_66_assault_completed";
    private static final int MAX_PHANTOMS = 8;
    private static final int ASSAULT_TICKS = 15 * 20;
    private static final int REST_TICKS = 8 * 20;
    private static final int PILLAR_TICKS = 13 * 20;
    private static final int AIR_TICKS = 10 * 20;
    private static final int STUN_TICKS = 2 * 20;
    private static final float SECOND_THRESHOLD = 0.66F;
    private static final float THIRD_THRESHOLD = 0.33F;
    // Intentionally NOT multiplied by 5 here: VanillaStatScaling processes damage.
    private static final float REFLECT_DAMAGE = 32.0F;
    private static final Map<UUID, Battle> BATTLES = new HashMap<>();

    private EnderDragonBattleEvents() {}

    private enum Stage { CRYSTALS, FURY, FINAL }
    private enum Move { ASSAULT, CENTER_APPROACH, CENTER_REST, PILLAR_TRAVEL, PILLAR_FIRE, STUN }

    private static final class Battle {
        Stage stage;
        Move move;
        int ticks;
        int shots;
        int pillarCount;
        int chargeCooldown;
        Vec3 selectedPillar;
        final ArrayDeque<Vec3> waitingPhantoms = new ArrayDeque<>();
        final List<Vec3> pillarPositions = new ArrayList<>();
        boolean forcedAssaultCompleted;

        Battle(Stage stage, EnderDragon dragon) {
            this.stage = stage;
            this.move = Move.ASSAULT;
            this.forcedAssaultCompleted = dragon.entityTags().contains(INTERRUPT_COMPLETED_TAG);
        }
    }

    @SubscribeEvent
    public static void onDragonTick(EntityTickEvent.Post event) {
        if (!(event.getEntity() instanceof EnderDragon dragon)
                || !(dragon.level() instanceof ServerLevel level)
                || level.dimension() != Level.END) {
            return;
        }
        // Leave the vanilla death sequence alone.
        if (!dragon.isAlive() || dragon.dragonDeathTime > 0
                || dragon.getPhaseManager().getCurrentPhase().getPhase() == EnderDragonPhase.DYING) {
            return;
        }

        boolean crystalsRemain = !findCrystals(level, dragon).isEmpty();
        Battle battle = BATTLES.computeIfAbsent(dragon.getUUID(), id ->
                new Battle(crystalsRemain ? Stage.CRYSTALS
                        : (dragon.getHealth() <= dragon.getMaxHealth() * THIRD_THRESHOLD
                        ? Stage.FINAL : Stage.FURY), dragon));

        releaseQueuedPhantoms(level, dragon, battle);
        if (battle.stage == Stage.CRYSTALS) {
            if (crystalsRemain) {
                phaseOne(level, dragon, battle);
                return;
            }
            battle.stage = Stage.FURY;
            beginAssault(level, dragon, battle);
        }
        if (battle.stage == Stage.FURY) {
            // Also handle a dragon that was damaged to 66% while crystals remained.
            if (!dragon.entityTags().contains(INTERRUPT_TAG)
                    && dragon.getHealth() <= dragon.getMaxHealth() * SECOND_THRESHOLD) {
                dragon.addTag(INTERRUPT_TAG);
                battle.forcedAssaultCompleted = false;
                beginAssault(level, dragon, battle);
            }
            if (dragon.getHealth() <= dragon.getMaxHealth() * THIRD_THRESHOLD
                    && battle.forcedAssaultCompleted) {
                battle.stage = Stage.FINAL;
                beginPillarTravel(level, dragon, battle);
            } else {
                phaseTwo(level, dragon, battle);
            }
        } else if (battle.stage == Stage.FINAL) {
            phaseThree(level, dragon, battle);
        }
    }

    private static void phaseOne(ServerLevel level, EnderDragon dragon, Battle battle) {
        ++battle.ticks;
        var phase = dragon.getPhaseManager().getCurrentPhase().getPhase();
        // Neither natural landings nor perching are allowed with active crystals.
        if (phase != EnderDragonPhase.HOLDING_PATTERN && phase != EnderDragonPhase.STRAFE_PLAYER
                && phase != EnderDragonPhase.CHARGING_PLAYER) {
            dragon.getPhaseManager().setPhase(EnderDragonPhase.HOLDING_PATTERN);
        }
        if (battle.ticks % 100 == 0) {
            ServerPlayer player = target(level, dragon);
            if (player != null) shootBreath(level, dragon, player);
        }
    }

    private static void phaseTwo(ServerLevel level, EnderDragon dragon, Battle battle) {
        switch (battle.move) {
            case ASSAULT -> {
                ++battle.ticks;
                // Drive repeated fast attacks; vanilla charging provides flyby collisions.
                if (battle.ticks == 1 || battle.ticks % 65 == 0) charge(level, dragon);
                if (battle.ticks % 90 == 0) {
                    ServerPlayer player = target(level, dragon);
                    if (player != null) shootBreath(level, dragon, player);
                }
                if (battle.ticks >= ASSAULT_TICKS) beginCenterApproach(dragon, battle);
            }
            case CENTER_APPROACH -> centerApproach(dragon, battle, REST_TICKS, false);
            case CENTER_REST -> centerRest(level, dragon, battle, REST_TICKS, false);
            default -> beginAssault(level, dragon, battle);
        }
    }

    private static void phaseThree(ServerLevel level, EnderDragon dragon, Battle battle) {
        switch (battle.move) {
            case PILLAR_TRAVEL -> {
                ++battle.ticks;
                if (battle.selectedPillar == null) {
                    beginPillarTravel(level, dragon, battle);
                    return;
                }
                if (dragon.position().distanceToSqr(battle.selectedPillar) < 9.0 * 9.0
                        || battle.ticks >= 180) {
                    battle.move = Move.PILLAR_FIRE;
                    battle.ticks = 0;
                    dragon.getPhaseManager().setPhase(EnderDragonPhase.HOVERING);
                } else if (battle.ticks % 35 == 0) {
                    aimChargeAt(dragon, battle.selectedPillar);
                }
            }
            case PILLAR_FIRE -> {
                ++battle.ticks;
                if (dragon.getPhaseManager().getCurrentPhase().getPhase() != EnderDragonPhase.HOVERING)
                    dragon.getPhaseManager().setPhase(EnderDragonPhase.HOVERING);
                if (battle.ticks % 30 == 10) {
                    ServerPlayer player = target(level, dragon);
                    if (player != null) fireReflectable(level, dragon, player);
                }
                if (battle.ticks >= PILLAR_TICKS) beginAssault(level, dragon, battle);
            }
            case ASSAULT -> {
                ++battle.ticks;
                if (battle.ticks == 1 || battle.ticks % 65 == 0) charge(level, dragon);
                if (battle.ticks % 90 == 0) {
                    ServerPlayer player = target(level, dragon);
                    if (player != null) shootBreath(level, dragon, player);
                }
                if (battle.ticks >= AIR_TICKS) {
                    if (battle.pillarCount % 2 == 0) beginCenterApproach(dragon, battle);
                    else beginPillarTravel(level, dragon, battle);
                }
            }
            case CENTER_APPROACH -> centerApproach(dragon, battle, 6 * 20, true);
            case CENTER_REST -> centerRest(level, dragon, battle, 6 * 20, true);
            case STUN -> {
                ++battle.ticks;
                if (dragon.getPhaseManager().getCurrentPhase().getPhase() != EnderDragonPhase.HOVERING)
                    dragon.getPhaseManager().setPhase(EnderDragonPhase.HOVERING);
                if (battle.ticks >= STUN_TICKS) beginPillarTravel(level, dragon, battle);
            }
        }
    }

    private static void beginAssault(ServerLevel level, EnderDragon dragon, Battle battle) {
        battle.move = Move.ASSAULT;
        battle.ticks = 0;
        charge(level, dragon);
    }

    private static void charge(ServerLevel level, EnderDragon dragon) {
        ServerPlayer player = target(level, dragon);
        if (player != null) aimChargeAt(dragon, player.position().add(0, 1.2, 0));
        else dragon.getPhaseManager().setPhase(EnderDragonPhase.HOLDING_PATTERN);
    }

    private static void aimChargeAt(EnderDragon dragon, Vec3 pos) {
        dragon.getPhaseManager().setPhase(EnderDragonPhase.CHARGING_PLAYER);
        dragon.getPhaseManager().getPhase(EnderDragonPhase.CHARGING_PLAYER).setTarget(pos);
    }

    private static void beginCenterApproach(EnderDragon dragon, Battle battle) {
        battle.move = Move.CENTER_APPROACH;
        battle.ticks = 0;
        dragon.getPhaseManager().setPhase(EnderDragonPhase.LANDING_APPROACH);
    }

    private static void centerApproach(EnderDragon dragon, Battle battle, int restTicks, boolean finalStage) {
        ++battle.ticks;
        if (dragon.getPhaseManager().getCurrentPhase().isSitting()) {
            battle.move = Move.CENTER_REST;
            battle.ticks = 0;
            dragon.getPhaseManager().setPhase(EnderDragonPhase.SITTING_SCANNING);
        } else if (battle.ticks > 200) {
            // Try the vanilla landing phase if navigation to the portal gets stuck.
            dragon.getPhaseManager().setPhase(EnderDragonPhase.LANDING);
            battle.ticks = 0;
        }
    }

    private static void centerRest(ServerLevel level, EnderDragon dragon,
                                   Battle battle, int restTicks, boolean finalStage) {
        ++battle.ticks;
        // Force a peaceful landing: vanilla sitting scan would otherwise start a breath attack.
        if (dragon.getPhaseManager().getCurrentPhase().getPhase() != EnderDragonPhase.SITTING_SCANNING)
            dragon.getPhaseManager().setPhase(EnderDragonPhase.SITTING_SCANNING);
        if (battle.ticks < restTicks) return;
        if (finalStage) beginPillarTravel(level, dragon, battle);
        else {
            if (dragon.entityTags().contains(INTERRUPT_TAG)) {
                battle.forcedAssaultCompleted = true;
                dragon.addTag(INTERRUPT_COMPLETED_TAG);
            }
            beginAssault(level, dragon, battle);
        }
    }

    private static void beginPillarTravel(ServerLevel level, EnderDragon dragon, Battle battle) {
        battle.move = Move.PILLAR_TRAVEL;
        battle.ticks = 0;
        battle.pillarCount++;
        battle.selectedPillar = selectPillar(level, dragon, battle);
        aimChargeAt(dragon, battle.selectedPillar);
    }

    private static Vec3 selectPillar(ServerLevel level, EnderDragon dragon, Battle battle) {
        List<Vec3> sites = battle.pillarPositions;
        if (!sites.isEmpty()) {
            int n = level.getRandom().nextInt(sites.size());
            Vec3 p = sites.get(n);
            return p.add(0, 4.5, 0);
        }
        // Fallback when the fight was loaded after crystals were destroyed.
        int index = level.getRandom().nextInt(10);
        BlockPos origin = dragon.getFightOrigin();
        double a = 2.0 * Math.PI * index / 10.0;
        int x = origin.getX() + (int) Math.round(42.0 * Math.cos(a));
        int z = origin.getZ() + (int) Math.round(42.0 * Math.sin(a));
        int y = level.getHeight(Heightmap.Types.MOTION_BLOCKING, x, z);
        return new Vec3(x + 0.5, y + 6.0, z + 0.5);
    }

    private static void shootBreath(ServerLevel level, EnderDragon dragon, ServerPlayer player) {
        Vec3 from = dragon.position().add(0.0, 2.0, 0.0);
        Vec3 dir = player.position().add(0, 1.0, 0).subtract(from).normalize();
        DragonFireball ball = new DragonFireball(level, dragon, dir);
        ball.setPos(from);
        level.addFreshEntity(ball);
    }

    private static void fireReflectable(ServerLevel level, EnderDragon dragon, ServerPlayer player) {
        // The dragon is very large and often hovers close to obsidian towers.
        // Spawning a fireball at its body center made shots collide with a pillar
        // immediately and sometimes damage the dragon itself.
        Vec3 muzzle = dragon.position().add(0.0, 4.5, 0.0);
        Vec3 targetPos = player.getEyePosition();
        Vec3 towardPlayer = targetPos.subtract(muzzle);
        if (towardPlayer.lengthSqr() < 1.0) return;
        Vec3 direction = towardPlayer.normalize();
        Vec3 from = muzzle.add(direction.scale(9.0));
        Vec3 newDirection = targetPos.subtract(from);
        if (newDirection.lengthSqr() < 1.0) return;
        newDirection = newDirection.normalize();

        // Do not launch at players who are behind towers or through obsidian.
        // Try again on the next shot interval rather than explode on the pillar.
        if (level.clip(new ClipContext(from, targetPos, ClipContext.Block.COLLIDER,
                ClipContext.Fluid.NONE, dragon)).getType() != HitResult.Type.MISS) return;

        LargeFireball ball = new LargeFireball(level, dragon, newDirection, 1);
        ball.setPos(from);
        ball.addTag(BALL_TAG);
        level.addFreshEntity(ball);
    }

    @SubscribeEvent
    public static void onFireballTick(EntityTickEvent.Post event) {
        if (!(event.getEntity() instanceof LargeFireball ball)
                || !ball.entityTags().contains(BALL_TAG)
                || !(ball.level() instanceof ServerLevel level)
                || ball.tickCount <= 7) return;
        // A returned Ghast fireball reverses direction, now moving toward the dragon.
        for (EnderDragon dragon : level.getEntitiesOfClass(EnderDragon.class,
                ball.getBoundingBox().inflate(18), Entity::isAlive)) {
            Vec3 towardsDragon = dragon.position().add(0, 2, 0).subtract(ball.position());
            if (towardsDragon.lengthSqr() > 11.0 * 11.0) continue;
            if (ball.getDeltaMovement().dot(towardsDragon) <= 0.25) continue;
            // Direction alone is not proof of a return shot. A projectile that
            // flies around a pillar can briefly point toward the dragon.
            // Minecraft changes a Ghast fireball's owner on player deflection.
            if (!(ball.getOwner() instanceof ServerPlayer reflectingPlayer)) continue;
            Battle battle = BATTLES.get(dragon.getUUID());
            if (battle == null || battle.stage != Stage.FINAL) continue;
            dragon.hurtServer(level,
                    level.damageSources().playerAttack(reflectingPlayer), REFLECT_DAMAGE);
            ball.discard();
            battle.move = Move.STUN;
            battle.ticks = 0;
            dragon.getPhaseManager().setPhase(EnderDragonPhase.HOVERING);
            return;
        }
    }

    @SubscribeEvent
    public static void onCrystalRemoved(EntityLeaveLevelEvent event) {
        if (!(event.getEntity() instanceof EndCrystal crystal)
                || !(event.getLevel() instanceof ServerLevel level)
                || level.dimension() != Level.END) return;
        // Don't count unloaded chunks or the four respawn ritual crystals.
        if (crystal.getRemovalReason() == Entity.RemovalReason.UNLOADED_TO_CHUNK
                || crystal.getRemovalReason() == Entity.RemovalReason.CHANGED_DIMENSION) return;
        if (crystal.distanceToSqr(Vec3.atCenterOf(BlockPos.ZERO)) > 120 * 120) return;
        for (EnderDragon dragon : level.getEntitiesOfClass(EnderDragon.class,
                crystal.getBoundingBox().inflate(220), Entity::isAlive)) {
            if (crystal.position().distanceToSqr(Vec3.atCenterOf(dragon.getFightOrigin())) > 105 * 105)
                continue;
            Battle battle = BATTLES.computeIfAbsent(dragon.getUUID(), id -> new Battle(Stage.CRYSTALS, dragon));
            if (battle.stage != Stage.CRYSTALS) return;
            battle.pillarPositions.add(crystal.position());
            battle.waitingPhantoms.add(crystal.position());
            battle.waitingPhantoms.add(crystal.position());
            return;
        }
    }

    private static void releaseQueuedPhantoms(ServerLevel level, EnderDragon dragon, Battle battle) {
        if (battle.waitingPhantoms.isEmpty()) return;
        int active = level.getEntitiesOfClass(Phantom.class,
                dragon.getBoundingBox().inflate(160), p -> p.entityTags().contains(PHANTOM_TAG)).size();
        while (active < MAX_PHANTOMS && !battle.waitingPhantoms.isEmpty()) {
            Vec3 p = battle.waitingPhantoms.remove();
            Phantom phantom = EntityTypes.PHANTOM.create(level, EntitySpawnReason.EVENT);
            if (phantom == null) continue;
            phantom.setPos(p.x, p.y + 2.0, p.z);
            phantom.addTag(PHANTOM_TAG);
            ServerPlayer player = target(level, phantom);
            if (player != null) phantom.setTarget(player);
            level.addFreshEntity(phantom);
            active++;
        }
    }

    private static List<EndCrystal> findCrystals(ServerLevel level, EnderDragon dragon) {
        BlockPos origin = dragon.getFightOrigin();
        AABB arena = new AABB(origin).inflate(110.0, 190.0, 110.0);
        return level.getEntitiesOfClass(EndCrystal.class, arena);
    }

    private static ServerPlayer target(ServerLevel level, Entity entity) {
        ServerPlayer best = null;
        double dist = Double.POSITIVE_INFINITY;
        for (ServerPlayer player : level.players()) {
            if (player.isSpectator() || !player.isAlive() || player.isCreative()) continue;
            double d = player.distanceToSqr(entity);
            if (d < dist && d <= 180 * 180) { dist = d; best = player; }
        }
        return best;
    }

    /**
     * Low priority deliberately runs after RaceCore VanillaStatScaling's x5
     * damage listener. Prevents bypassing 66% assault or 33% final phase.
     */
    /**
     * The boss must not kill itself by launching its own tagged fireballs into
     * an obsidian tower. A truly reflected projectile has a player owner, so
     * player return-fire and its explosion remain eligible to damage her.
     */
    @SubscribeEvent
    public static void onDragonSelfFireballDamage(LivingIncomingDamageEvent event) {
        if (!(event.getEntity() instanceof EnderDragon dragon)) return;
        if (event.getSource().getEntity() != dragon) return;
        if (event.getSource().getDirectEntity() instanceof LargeFireball fireball
                && fireball.entityTags().contains(BALL_TAG)) {
            event.setCanceled(true);
            return;
        }
        // Explosion damage can have no direct projectile entity in its source.
        if (event.getSource().is(DamageTypes.EXPLOSION)
                || event.getSource().is(DamageTypes.FIREBALL)) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onDragonDamaged(LivingDamageEvent.Pre event) {
        if (!(event.getEntity() instanceof EnderDragon dragon)
                || !(dragon.level() instanceof ServerLevel level)
                || level.dimension() != Level.END) return;
        Battle battle = BATTLES.get(dragon.getUUID());
        if (battle == null) return;
        float damage = Math.max(0.0F, event.getNewDamage());
        boolean trigger66 = battle.stage == Stage.FURY
                && !dragon.entityTags().contains(INTERRUPT_TAG)
                && dragon.getHealth() > dragon.getMaxHealth() * SECOND_THRESHOLD
                && dragon.getHealth() - damage <= dragon.getMaxHealth() * SECOND_THRESHOLD;
        // An oversized hit must NOT skip the interrupt or the last phase.
        float floor = battle.stage == Stage.CRYSTALS || trigger66
                ? dragon.getMaxHealth() * SECOND_THRESHOLD
                : (battle.stage == Stage.FURY ? dragon.getMaxHealth() * THIRD_THRESHOLD : 0.0F);
        if (floor > 0.0F) {
            event.setNewDamage(Math.min(damage, Math.max(0.0F, dragon.getHealth() - floor)));
        }
        if (trigger66) {
            dragon.addTag(INTERRUPT_TAG);
            battle.forcedAssaultCompleted = false;
            beginAssault(level, dragon, battle);
        }
    }

    @SubscribeEvent
    public static void onEntityLeave(EntityLeaveLevelEvent event) {
        if (!(event.getEntity() instanceof EnderDragon dragon)) return;
        Battle battle = BATTLES.remove(dragon.getUUID());
        if (battle == null || !(event.getLevel() instanceof ServerLevel level)) return;
        // Encounter-only phantoms should never linger after the dragon dies.
        for (Phantom phantom : level.getEntitiesOfClass(Phantom.class,
                dragon.getBoundingBox().inflate(200), p -> p.entityTags().contains(PHANTOM_TAG))) {
            phantom.discard();
        }
    }

    @SubscribeEvent
    public static void onServerStopping(ServerStoppingEvent event) {
        BATTLES.clear();
    }
}
