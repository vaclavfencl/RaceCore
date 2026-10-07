package com.example.racecore.weapon;

import com.example.racecore.RaceCore;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.entity.player.CriticalHitEvent;
import net.neoforged.neoforge.event.entity.player.SweepAttackEvent;

/**
 * RaceCore flail combat identity.
 *
 * Attack states:
 *
 * 1) NOT fully charged:
 *    - single target only
 *    - normal Minecraft cooldown scaling still applies first
 *    - final pre-mitigation damage is then multiplied by 1.50
 *    - no critical hits
 *    - Sweeping Edge does NOTHING to this attack
 *
 * 2) FULLY charged:
 *    - no 1.50 single-target multiplier
 *    - the primary hit becomes the center of a custom 180-degree frontal AoE
 *    - every valid target in the front half-circle receives normal full-charge
 *      melee damage for that target
 *    - Sweeping Edge buffs ONLY this AoE:
 *          level 0 = 100%
 *          level I = 110%
 *          level II = 120%
 *          level III = 130%
 *    - no critical hits
 *
 * The AoE resolves normal attack-damage attributes and enchantment damage per
 * target. It does not inspect or guess a "best" enchantment.
 */
@EventBusSubscriber(modid = RaceCore.MODID)
public final class FlailCombatEvents {
    private static final float SINGLE_TARGET_MULTIPLIER = 1.50F;
    private static final float SWEEPING_EDGE_BONUS_PER_LEVEL = 0.10F;

    /*
     * getAttackStrengthScale() clamps at 1.0. Use a very small tolerance so
     * "fully charged" really means the meter has reached full charge, rather
     * than vanilla's broader >90% "full strength" test.
     */
    private static final float FULL_CHARGE_THRESHOLD = 0.999F;

    /*
     * Custom AoE damage re-enters NeoForge's normal damage event pipeline.
     * Guard our own handlers so secondary targets are not multiplied again and
     * do not recursively spawn another half-circle.
     */
    private static final ThreadLocal<Boolean> IN_CUSTOM_SWEEP =
            ThreadLocal.withInitial(() -> false);

    private FlailCombatEvents() {
    }

    @SubscribeEvent
    public static void onCriticalHit(
            CriticalHitEvent event
    ) {
        if (!RaceCoreWeapons.isFlail(
                event.getEntity()
                        .getMainHandItem()
        )) {
            return;
        }

        /*
         * The flail never critically hits. Its burst identity comes from the
         * deliberate 150% not-quite-full attack instead.
         */
        event.setCriticalHit(false);
        event.setDamageMultiplier(1.0F);
    }

    @SubscribeEvent
    public static void onSweepAttack(
            SweepAttackEvent event
    ) {
        if (!RaceCoreWeapons.isFlail(
                event.getEntity()
                        .getMainHandItem()
        )) {
            return;
        }

        /*
         * Disable Minecraft's built-in sword sweep. The flail has its own
         * full-damage 180-degree sweep with its own Sweeping Edge scaling.
         */
        event.setSweeping(false);
    }

    @SubscribeEvent
    public static void onIncomingDamage(
            LivingIncomingDamageEvent event
    ) {
        if (IN_CUSTOM_SWEEP.get()) {
            return;
        }

        if (!(event.getSource()
                .getEntity()
                instanceof ServerPlayer player)) {

            return;
        }

        /*
         * Require the player to be the direct source too. This prevents an
         * arrow/projectile fired earlier from being treated as a flail hit just
         * because the player happens to be holding a flail when it lands.
         */
        if (event.getSource()
                .getDirectEntity()
                != player) {

            return;
        }

        ItemStack flail =
                player.getMainHandItem();

        if (!RaceCoreWeapons.isFlail(flail)) {
            return;
        }

        float charge =
                player.getAttackStrengthScale(
                        0.5F
                );

        if (isFullyCharged(charge)) {
            /*
             * Fully charged = AoE mode.
             *
             * The primary target goes through the normal vanilla attack
             * calculation first. We only apply the explicit Sweeping Edge AoE
             * multiplier here. There is intentionally NO 150% multiplier.
             */
            int sweepingLevel =
                    getSweepingEdgeLevel(
                            (ServerLevel) player.level(),
                            flail
                    );

            event.setAmount(
                    event.getAmount()
                            * getSweepMultiplier(
                            sweepingLevel
                    )
            );
        } else {
            /*
             * Partial charge = heavy single-target mode.
             *
             * event.getAmount() already contains Minecraft's current cooldown
             * scaling plus normal attack-damage attributes/enchantment damage.
             * Multiply that resolved pre-mitigation value by 1.50.
             *
             * Sweeping Edge is intentionally absent from this calculation.
             */
            event.setAmount(
                    event.getAmount()
                            * SINGLE_TARGET_MULTIPLIER
            );
        }
    }

    @SubscribeEvent
    public static void onDamagePost(
            LivingDamageEvent.Post event
    ) {
        if (IN_CUSTOM_SWEEP.get()) {
            return;
        }

        if (!(event.getSource()
                .getEntity()
                instanceof ServerPlayer player)) {

            return;
        }

        if (event.getSource()
                .getDirectEntity()
                != player) {

            return;
        }

        ItemStack flail =
                player.getMainHandItem();

        if (!RaceCoreWeapons.isFlail(flail)) {
            return;
        }

        float charge =
                player.getAttackStrengthScale(
                        0.5F
                );

        if (!isFullyCharged(charge)) {
            return;
        }

        /*
         * The primary target already received the full-charge hit in the
         * vanilla damage pipeline. Now apply the same attack concept to every
         * other valid target in the frontal half-circle.
         */
        performFullChargeSweep(
                player,
                event.getEntity(),
                flail
        );
    }

    private static void performFullChargeSweep(
            ServerPlayer player,
            LivingEntity primaryTarget,
            ItemStack flail
    ) {
        ServerLevel level =
                (ServerLevel) player.level();

        double reach =
                Math.max(
                        0.0,
                        player.getAttributeValue(
                                Attributes.ENTITY_INTERACTION_RANGE
                        )
                );

        if (reach <= 0.0) {
            return;
        }

        Vec3 look =
                player.getLookAngle();

        Vec3 forward =
                new Vec3(
                        look.x,
                        0.0,
                        look.z
                );

        if (forward.lengthSqr()
                <= 0.000001) {

            return;
        }

        forward =
                forward.normalize();

        AABB searchBox =
                player.getBoundingBox()
                        .inflate(
                                reach,
                                2.0,
                                reach
                        );

        DamageSource damageSource =
                player.damageSources()
                        .playerAttack(
                                player
                        );

        float baseDamage =
                (float) player.getAttributeValue(
                        Attributes.ATTACK_DAMAGE
                );

        int sweepingLevel =
                getSweepingEdgeLevel(
                        level,
                        flail
                );

        float sweepMultiplier =
                getSweepMultiplier(
                        sweepingLevel
                );

        IN_CUSTOM_SWEEP.set(true);

        try {
            for (LivingEntity target
                    : level.getEntitiesOfClass(
                            LivingEntity.class,
                            searchBox
                    )) {

                if (target == player
                        || target == primaryTarget
                        || !target.isAlive()
                        || player.isAlliedTo(target)
                        || player.distanceToSqr(target)
                        > reach * reach
                        || !player.hasLineOfSight(target)
                        || !isInFrontHalfCircle(
                                player,
                                target,
                                forward
                        )) {

                    continue;
                }

                /*
                 * Resolve enchantment damage for THIS target. This means
                 * target-specific combat enchants are evaluated normally
                 * rather than copying damage from whichever mob happened to
                 * be clicked first.
                 *
                 * At full charge Minecraft's base cooldown multiplier is 1.0,
                 * so the player's current ATTACK_DAMAGE attribute is the
                 * correct base for this custom secondary hit.
                 */
                float resolvedDamage =
                        EnchantmentHelper.modifyDamage(
                                level,
                                flail,
                                target,
                                damageSource,
                                baseDamage
                        );

                target.hurtServer(
                        level,
                        damageSource,
                        resolvedDamage
                                * sweepMultiplier
                );
            }
        } finally {
            IN_CUSTOM_SWEEP.set(false);
        }
    }

    private static boolean isInFrontHalfCircle(
            ServerPlayer player,
            LivingEntity target,
            Vec3 forward
    ) {
        Vec3 toTarget =
                target.getBoundingBox()
                        .getCenter()
                        .subtract(
                                player.position()
                        );

        Vec3 horizontal =
                new Vec3(
                        toTarget.x,
                        0.0,
                        toTarget.z
                );

        if (horizontal.lengthSqr()
                <= 0.000001) {

            return true;
        }

        /*
         * dot >= 0 is exactly the front 180-degree half-plane:
         * - +1 = directly ahead
         * -  0 = directly to either side
         * - <0 = behind the player
         */
        return forward.dot(
                horizontal.normalize()
        ) >= 0.0;
    }

    private static boolean isFullyCharged(
            float charge
    ) {
        return charge
                >= FULL_CHARGE_THRESHOLD;
    }

    private static float getSweepMultiplier(
            int sweepingLevel
    ) {
        return 1.0F
                + Math.max(
                        0,
                        sweepingLevel
                )
                * SWEEPING_EDGE_BONUS_PER_LEVEL;
    }

    private static int getSweepingEdgeLevel(
            ServerLevel level,
            ItemStack stack
    ) {
        HolderLookup.RegistryLookup<Enchantment> enchantments =
                level.registryAccess()
                        .lookupOrThrow(
                                Registries.ENCHANTMENT
                        );

        Holder.Reference<Enchantment> sweepingEdge =
                enchantments.getOrThrow(
                        Enchantments.SWEEPING_EDGE
                );

        return EnchantmentHelper
                .getEnchantmentsForCrafting(
                        stack
                )
                .getLevel(
                        sweepingEdge
                );
    }
}
