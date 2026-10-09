package com.example.racecore.potion;

import com.example.racecore.RaceCore;
import com.example.racecore.mixin.MobEffectInstanceAccessor;
import java.util.Optional;
import java.util.UUID;
import net.minecraft.core.component.DataComponents;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.UseCooldown;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.common.damagesource.DamageContainer;
import net.neoforged.neoforge.event.ModifyDefaultComponentsEvent;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.entity.living.MobEffectEvent;
import net.neoforged.neoforge.event.tick.EntityTickEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

/**
 * RaceCore potion rebalance.
 *
 * Stack sizes:
 * - drinkable potions: 16
 * - splash potions: 4
 * - lingering potions: 4
 *
 * All three share one 3 second cooldown group.
 *
 * Effect changes handled here:
 * - Resistance: 5% damage reduction per tier.
 * - Poison: stacks on every application, resets to 5 seconds, deals
 *   1 damage/second per stack, and cannot kill.
 *
 * Other percentage conversions are implemented by the mixins in
 * com.example.racecore.mixin.
 */
@EventBusSubscriber(modid = RaceCore.MODID)
public final class PotionRebalanceEvents {
    private static final float POTION_COOLDOWN_SECONDS = 3.0F;

    private static final Identifier POTION_COOLDOWN_GROUP =
            Identifier.fromNamespaceAndPath(
                    RaceCore.MODID,
                    "potions"
            );

    private static final String POISON_STACKS_KEY =
            "racecore:poison_stacks";

    private static final String POISON_NEXT_DAMAGE_KEY =
            "racecore:poison_next_damage";

    private static final int POISON_DURATION_TICKS = 5 * 20;
    private static final int POISON_DAMAGE_INTERVAL_TICKS = 20;

    /**
     * Used only while RaceCore is applying its own poison tick.
     * LivingDamageEvent.Pre uses this to guarantee that Poison can never
     * reduce the target below 1 HP, even if another modifier increases magic
     * damage later in the damage pipeline.
     */
    private static final ThreadLocal<UUID> ACTIVE_POISON_DAMAGE_TARGET =
            new ThreadLocal<>();

    private PotionRebalanceEvents() {
    }

    /**
     * 26.3 default-item-component patching.
     *
     * The shared cooldown group means drinking or throwing any potion locks
     * all three potion forms for 3 seconds.
     *
     * For drinkables the cooldown begins when drinking completes because
     * USE_COOLDOWN is applied by ItemStack's post-use component processing.
     * Splash/lingering potions apply it immediately after the successful throw.
     */
    @SubscribeEvent
    public static void onModifyDefaultComponents(
            ModifyDefaultComponentsEvent event
    ) {
        UseCooldown sharedCooldown =
                new UseCooldown(
                        POTION_COOLDOWN_SECONDS,
                        Optional.of(POTION_COOLDOWN_GROUP)
                );

        event.modify(
                Items.POTION,
                (components, context, item) -> components
                        .set(DataComponents.MAX_STACK_SIZE, 16)
                        .set(DataComponents.USE_COOLDOWN, sharedCooldown)
        );

        event.modify(
                Items.SPLASH_POTION,
                (components, context, item) -> components
                        .set(DataComponents.MAX_STACK_SIZE, 4)
                        .set(DataComponents.USE_COOLDOWN, sharedCooldown)
        );

        event.modify(
                Items.LINGERING_POTION,
                (components, context, item) -> components
                        .set(DataComponents.MAX_STACK_SIZE, 4)
                        .set(DataComponents.USE_COOLDOWN, sharedCooldown)
        );
    }

    /**
     * Vanilla Resistance is 20% per level. Replace the MOB_EFFECTS reduction
     * with RaceCore's 5% per tier.
     *
     * The reduction is multiplicative with armor because this stage executes
     * after armor. Damage sources which bypass Resistance never trigger the
     * vanilla MOB_EFFECTS reduction, so this modifier also naturally respects
     * the bypass tag.
     */
    @SubscribeEvent
    public static void onIncomingDamage(
            LivingIncomingDamageEvent event
    ) {
        MobEffectInstance resistance =
                event.getEntity()
                        .getEffect(MobEffects.RESISTANCE);

        if (resistance == null) {
            return;
        }

        int tier = resistance.getAmplifier() + 1;

        double reductionFraction =
                Math.min(
                        0.95,
                        tier * 0.05
                );

        event.addReductionModifier(
                DamageContainer.Reduction.MOB_EFFECTS,
                (container, vanillaReduction) ->
                        (float) (
                                container.getNewDamage()
                                        * reductionFraction
                        )
        );
    }

    /**
     * Every fresh Poison application adds exactly one stack, regardless of the
     * incoming potion/effect amplifier, and resets the visible effect to 5s.
     *
     * Actual stack count is stored as an int in persistent entity data so
     * damage can continue beyond Minecraft's 0..255 visible amplifier limit.
     * The HUD icon is capped at Poison 256 because the vanilla effect protocol
     * cannot represent a larger amplifier.
     */
    @SubscribeEvent
    public static void onPoisonAdded(
            MobEffectEvent.Added event
    ) {
        MobEffectInstance newEffect =
                event.getEffectInstance();

        if (!newEffect.is(MobEffects.POISON)
                || event.getEntity()
                        .level()
                        .isClientSide()) {
            return;
        }

        LivingEntity entity =
                event.getEntity();

        MobEffectInstance oldEffect =
                event.getOldEffectInstance();

        var data =
                entity.getPersistentData();

        int storedStacks =
                data.getIntOr(
                        POISON_STACKS_KEY,
                        0
                );

        int baselineStacks =
                storedStacks > 0
                        ? storedStacks
                        : oldEffect == null
                        ? 0
                        : oldEffect.getAmplifier() + 1;

        int newStacks =
                baselineStacks == Integer.MAX_VALUE
                        ? Integer.MAX_VALUE
                        : baselineStacks + 1;

        data.putInt(
                POISON_STACKS_KEY,
                newStacks
        );

        if (oldEffect == null
                || storedStacks <= 0) {
            data.putLong(
                    POISON_NEXT_DAMAGE_KEY,
                    entity.level()
                            .getGameTime()
                            + POISON_DAMAGE_INTERVAL_TICKS
            );
        }

        MobEffectInstanceAccessor accessor =
                (MobEffectInstanceAccessor) (Object) newEffect;

        accessor.racecore$setDuration(
                POISON_DURATION_TICKS
        );

        accessor.racecore$setAmplifier(
                Math.min(
                        255,
                        newStacks - 1
                )
        );
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onPoisonRemoved(
            MobEffectEvent.Remove event
    ) {
        if (event.getEffect()
                .equals(MobEffects.POISON)
                && !event.isCanceled()) {
            clearPoisonState(
                    event.getEntity()
            );
        }
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onPoisonExpired(
            MobEffectEvent.Expired event
    ) {
        MobEffectInstance effect =
                event.getEffectInstance();

        if (effect != null
                && effect.is(MobEffects.POISON)
                && !event.isCanceled()) {
            clearPoisonState(
                    event.getEntity()
            );
        }
    }

    /**
     * Non-player living entities tick here.
     */
    @SubscribeEvent
    public static void onEntityTick(
            EntityTickEvent.Post event
    ) {
        if (event.getEntity() instanceof Player) {
            return;
        }

        if (event.getEntity()
                instanceof LivingEntity living) {
            tickPoison(living);
        }
    }

    /**
     * Server players have their own tick event in NeoForge 26.3.
     */
    @SubscribeEvent
    public static void onPlayerTick(
            PlayerTickEvent.Post event
    ) {
        tickPoison(
                event.getEntity()
        );
    }

    /**
     * Absolute non-lethal guarantee for RaceCore poison.
     *
     * This runs after armor/effects reductions and caps only the custom poison
     * damage sequence to currentHealth - 1.
     */
    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onDamagePre(
            LivingDamageEvent.Pre event
    ) {
        UUID poisonTarget =
                ACTIVE_POISON_DAMAGE_TARGET.get();

        if (poisonTarget == null
                || !poisonTarget.equals(
                        event.getEntity()
                                .getUUID()
                )) {
            return;
        }

        float maximumNonLethalDamage =
                Math.max(
                        0.0F,
                        event.getEntity()
                                .getHealth()
                                - 1.0F
                );

        event.setNewDamage(
                Math.min(
                        event.getNewDamage(),
                        maximumNonLethalDamage
                )
        );
    }

    private static void tickPoison(
            LivingEntity entity
    ) {
        if (!(entity.level()
                instanceof ServerLevel level)) {
            return;
        }

        var data =
                entity.getPersistentData();

        MobEffectInstance poison =
                entity.getEffect(
                        MobEffects.POISON
                );

        if (poison == null) {
            if (data.getIntOr(
                    POISON_STACKS_KEY,
                    0
            ) > 0) {
                clearPoisonState(entity);
            }

            return;
        }

        int stacks =
                data.getIntOr(
                        POISON_STACKS_KEY,
                        0
                );

        /*
         * Compatibility path for an entity loaded with an already-active
         * Poison effect but no RaceCore poison metadata.
         */
        if (stacks <= 0) {
            stacks =
                    poison.getAmplifier() + 1;

            data.putInt(
                    POISON_STACKS_KEY,
                    stacks
            );

            data.putLong(
                    POISON_NEXT_DAMAGE_KEY,
                    level.getGameTime()
                            + POISON_DAMAGE_INTERVAL_TICKS
            );
        }

        long now =
                level.getGameTime();

        long nextDamage =
                data.getLongOr(
                        POISON_NEXT_DAMAGE_KEY,
                        now + POISON_DAMAGE_INTERVAL_TICKS
                );

        if (now < nextDamage) {
            return;
        }

        data.putLong(
                POISON_NEXT_DAMAGE_KEY,
                now + POISON_DAMAGE_INTERVAL_TICKS
        );

        float health =
                entity.getHealth();

        if (health <= 1.0F) {
            return;
        }

        float requestedDamage =
                Math.min(
                        (float) stacks,
                        health - 1.0F
                );

        if (requestedDamage <= 0.0F) {
            return;
        }

        ACTIVE_POISON_DAMAGE_TARGET.set(
                entity.getUUID()
        );

        try {
            entity.hurtServer(
                    level,
                    entity.damageSources()
                            .magic(),
                    requestedDamage
            );
        } finally {
            ACTIVE_POISON_DAMAGE_TARGET.remove();
        }
    }

    private static void clearPoisonState(
            LivingEntity entity
    ) {
        var data =
                entity.getPersistentData();

        data.remove(
                POISON_STACKS_KEY
        );

        data.remove(
                POISON_NEXT_DAMAGE_KEY
        );
    }
}
