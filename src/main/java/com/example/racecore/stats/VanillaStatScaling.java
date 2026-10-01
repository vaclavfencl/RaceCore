package com.example.racecore.stats;

import com.example.racecore.RaceCore;
import net.minecraft.resources.Identifier;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import net.neoforged.neoforge.event.entity.living.LivingHealEvent;
import net.neoforged.neoforge.event.entity.living.MobEffectEvent;

/**
 * Converts vanilla Minecraft's 20-HP combat scale to RaceCore's 100-HP scale.
 *
 * Core rule:
 * vanilla-equivalent health/damage/healing values are multiplied by 5.
 *
 * Things that are ratios, timings, distances or durability are intentionally NOT
 * multiplied here.
 */
@EventBusSubscriber(modid = RaceCore.MODID)
public final class VanillaStatScaling {
    public static final double SCALE = 5.0;
    public static final float SCALE_F = 5.0F;

    private static final Identifier MAX_HEALTH_SCALE_ID =
            Identifier.fromNamespaceAndPath(RaceCore.MODID, "vanilla_x5_max_health");

    private static final Identifier MAX_ABSORPTION_SCALE_ID =
            Identifier.fromNamespaceAndPath(RaceCore.MODID, "vanilla_x5_max_absorption");

    private VanillaStatScaling() {
    }

    /**
     * Applies the permanent x5 capacity modifiers when an entity enters a level.
     *
     * Permanent modifiers are used so partially injured mobs keep sensible health
     * when worlds are saved and loaded. Re-adding by the same ID is safe.
     */
    @SubscribeEvent
    public static void onEntityJoinLevel(EntityJoinLevelEvent event) {
        if (event.getLevel().isClientSide()) {
            return;
        }

        if (!(event.getEntity() instanceof LivingEntity living)) {
            return;
        }

        applyHealthCapacityScale(living);
        applyAbsorptionCapacityScale(living);
    }

    private static void applyHealthCapacityScale(LivingEntity living) {
        AttributeInstance maxHealth = living.getAttribute(Attributes.MAX_HEALTH);

        if (maxHealth == null) {
            return;
        }

        double oldMax = living.getMaxHealth();
        float oldHealth = living.getHealth();

        maxHealth.addOrReplacePermanentModifier(
                new AttributeModifier(
                        MAX_HEALTH_SCALE_ID,
                        SCALE - 1.0,
                        AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL
                )
        );

        // Preserve health percentage when migrating an already-existing entity.
        if (oldMax > 0.0) {
            double ratio = oldHealth / oldMax;
            living.setHealth((float) Math.max(
                    0.0,
                    Math.min(living.getMaxHealth(), living.getMaxHealth() * ratio)
            ));
        }
    }

    private static void applyAbsorptionCapacityScale(LivingEntity living) {
        AttributeInstance maxAbsorption = living.getAttribute(Attributes.MAX_ABSORPTION);

        if (maxAbsorption == null) {
            return;
        }

        maxAbsorption.addOrReplacePermanentModifier(
                new AttributeModifier(
                        MAX_ABSORPTION_SCALE_ID,
                        SCALE - 1.0,
                        AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL
                )
        );
    }

    /**
     * Vanilla armor, Protection, Resistance, shields, etc. have already done their
     * normal calculations when this event fires.
     *
     * Multiplying here therefore preserves their vanilla-relative effectiveness
     * while converting the resulting health loss to the 100-HP scale.
     */
    @SubscribeEvent
    public static void onLivingDamage(LivingDamageEvent.Pre event) {
        if (event.getEntity().level().isClientSide()) {
            return;
        }

        event.setNewDamage(event.getNewDamage() * SCALE_F);
    }

    /**
     * Captures vanilla heal(), including regeneration, natural regeneration,
     * Instant Health and normal entity healing.
     */
    @SubscribeEvent
    public static void onLivingHeal(LivingHealEvent event) {
        if (event.getEntity().level().isClientSide()) {
            return;
        }

        event.setAmount(event.getAmount() * SCALE_F);
    }

    /**
     * Vanilla Absorption grants 4 HP per level. The max-absorption attribute is
     * already scaled above; this scales the actual yellow-health amount granted.
     */
    @SubscribeEvent
    public static void onMobEffectAdded(MobEffectEvent.Added event) {
        if (event.getEntity().level().isClientSide()) {
            return;
        }

        if (!event.getEffectInstance().getEffect().equals(MobEffects.ABSORPTION)) {
            return;
        }

        int amplifier = event.getEffectInstance().getAmplifier();

        // Vanilla target is 4 * (amplifier + 1).
        float scaledTarget = (float) (4.0 * (amplifier + 1) * SCALE);

        if (event.getEntity().getAbsorptionAmount() < scaledTarget) {
            event.getEntity().setAbsorptionAmount(scaledTarget);
        }
    }
}
