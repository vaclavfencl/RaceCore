package com.example.racecore.race.ability;

import com.example.racecore.RaceCore;
import com.example.racecore.race.RaceDefinition;
import com.example.racecore.race.RaceManager;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.tags.ItemTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.projectile.FireworkRocketEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.CrossbowItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.ProjectileWeaponItem;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biome;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import net.neoforged.neoforge.event.entity.living.LivingEntityUseItemEvent;
import net.neoforged.neoforge.event.entity.living.LivingKnockBackEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.entity.player.PlayerXpEvent;
import net.neoforged.neoforge.event.level.BlockDropsEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

import java.util.List;

@EventBusSubscriber(modid = RaceCore.MODID)
public final class RaceAbilityEvents {
    private static final String XP_CARRY_KEY =
            "racecore:xp_bonus_carry";

    private static final String CRACKED_TICKS_KEY =
            "racecore:cracked_ticks";

    private static final TagKey<EntityType<?>> DRAGONS =
            TagKey.create(
                    Registries.ENTITY_TYPE,
                    Identifier.fromNamespaceAndPath(
                            RaceCore.MODID,
                            "dragons"
                    )
            );

    private static final TagKey<net.minecraft.world.level.block.Block> FORTUNE_CROPS =
            TagKey.create(
                    Registries.BLOCK,
                    Identifier.fromNamespaceAndPath(
                            RaceCore.MODID,
                            "fortune_crops"
                    )
            );

    private static final TagKey<Biome> FOREST_BIOMES =
            TagKey.create(
                    Registries.BIOME,
                    Identifier.fromNamespaceAndPath(
                            RaceCore.MODID,
                            "forest_biomes"
                    )
            );

    private RaceAbilityEvents() {
    }

    @SubscribeEvent
    public static void onXpChange(
            PlayerXpEvent.XpChange event
    ) {
        if (event.getAmount() <= 0) {
            return;
        }

        Player player =
                event.getEntity();

        RaceDefinition race =
                RaceManager.getRace(player);

        double bonusRate =
                race.xpBonus();

        if (bonusRate <= 0.0) {
            return;
        }

        double oldCarry =
                player.getPersistentData()
                        .getDoubleOr(
                                XP_CARRY_KEY,
                                0.0
                        );

        double exactBonus =
                event.getAmount()
                        * bonusRate
                        + oldCarry;

        int wholeBonus =
                (int) Math.floor(exactBonus);

        double newCarry =
                exactBonus - wholeBonus;

        player.getPersistentData()
                .putDouble(
                        XP_CARRY_KEY,
                        newCarry
                );

        if (wholeBonus > 0) {
            event.setAmount(
                    event.getAmount()
                            + wholeBonus
            );
        }
    }

    @SubscribeEvent
    public static void onBreakSpeed(
            PlayerEvent.BreakSpeed event
    ) {
        RaceDefinition race =
                RaceManager.getRace(
                        event.getEntity()
                );

        double multiplier =
                race.miningSpeedMultiplier();

        if (Math.abs(multiplier - 1.0)
                <= 0.000001) {

            return;
        }

        event.setNewSpeed(
                (float) (
                        event.getNewSpeed()
                                * multiplier
                )
        );
    }

    @SubscribeEvent
    public static void onKnockback(
            LivingKnockBackEvent event
    ) {
        if (!(event.getEntity()
                instanceof Player player)) {

            return;
        }

        RaceDefinition race =
                RaceManager.getRace(player);

        double resistance =
                race.knockbackResistanceBonus();

        if (resistance <= 0.0) {
            return;
        }

        event.setStrength(
                (float) (
                        event.getStrength()
                                * (1.0 - resistance)
                )
        );
    }

    @SubscribeEvent
    public static void onLivingDamage(
            LivingDamageEvent.Pre event
    ) {
        float damage = event.getNewDamage();

        if (damage <= 0.0F) {
            return;
        }

        if (event.getSource().getEntity()
                instanceof Player attacker) {

            RaceDefinition attackerRace =
                    RaceManager.getRace(attacker);

            damage *= (float)
                    attackerRace
                            .outgoingDamageMultiplier();

            if (attacker.level().dimension()
                    == Level.NETHER) {

                damage *= (float)
                        attackerRace
                                .netherDamageMultiplier();
            }

            int light =
                    attacker.level()
                            .getMaxLocalRawBrightness(
                                    attacker.blockPosition()
                            );

            if (light
                    <= attackerRace.darkLightThreshold()) {

                damage *= (float)
                        attackerRace
                                .darkDamageMultiplier();
            } else {
                damage *= (float)
                        attackerRace
                                .brightDamageMultiplier();
            }

            if (event.getEntity().is(DRAGONS)) {
                damage *= (float)
                        attackerRace
                                .dragonDamageMultiplier();
            }
        }

        if (event.getEntity()
                instanceof Player victim) {

            RaceDefinition victimRace =
                    RaceManager.getRace(victim);

            if (event.getSource().getEntity()
                    instanceof LivingEntity attacker
                    && attacker.getMainHandItem()
                    .is(ItemTags.PICKAXES)
                    && victimRace.crackedDurationTicks() > 0) {

                victim.getPersistentData().putInt(
                        CRACKED_TICKS_KEY,
                        victimRace.crackedDurationTicks()
                );

                victim.sendOverlayMessage(
                        Component.translatable(
                                "message.racecore.ability.cracked"
                        )
                );
            }

            damage *= (float)
                    victimRace
                            .incomingDamageMultiplier();

            if (victim.level().dimension()
                    != Level.NETHER) {

                damage *= (float)
                        victimRace
                                .nonNetherIncomingDamageMultiplier();
            }

            if (event.getSource()
                    .is(DamageTypeTags.IS_FIRE)) {

                damage *= (float)
                        victimRace
                                .fireDamageMultiplier();
            }

            if (event.getSource()
                    .getDirectEntity()
                    instanceof FireworkRocketEntity) {

                damage *= (float)
                        victimRace
                                .fireworkDamageMultiplier();
            }

            boolean poisonTick =
                    event.getSource()
                            .is(DamageTypes.MAGIC)
                            && event.getSource()
                            .getEntity() == null
                            && event.getSource()
                            .getDirectEntity() == null
                            && victim.hasEffect(
                                    MobEffects.POISON
                            );

            if (poisonTick) {
                damage *= (float)
                        victimRace
                                .poisonDamageMultiplier();
            }

            if (victim.getPersistentData()
                    .getIntOr(
                            CRACKED_TICKS_KEY,
                            0
                    ) > 0) {

                damage *= (float)
                        victimRace
                                .crackedIncomingDamageMultiplier();
            }
        }

        event.setNewDamage(
                Math.max(0.0F, damage)
        );
    }

    @SubscribeEvent
    public static void onPlayerTick(
            PlayerTickEvent.Post event
    ) {
        Player player = event.getEntity();

        if (player.level().isClientSide()) {
            return;
        }

        RaceDefinition race =
                RaceManager.getRace(player);

        int crackedTicks =
                player.getPersistentData()
                        .getIntOr(
                                CRACKED_TICKS_KEY,
                                0
                        );

        if (crackedTicks > 0) {
            player.getPersistentData().putInt(
                    CRACKED_TICKS_KEY,
                    crackedTicks - 1
            );
        }

        if (player.tickCount % 10 == 0) {
            updateEnvironmentMovement(
                    player,
                    race
            );
        }

        if (player.tickCount % 20 == 0
                && race.passiveRegenerationPerSecond() > 0.0
                && player.getHealth() < player.getMaxHealth()) {

            player.setHealth(
                    (float) Math.min(
                            player.getMaxHealth(),
                            player.getHealth()
                                    + race.passiveRegenerationPerSecond()
                    )
            );
        }

        if (race.permanentNightVision()
                && !player.hasEffect(
                        MobEffects.NIGHT_VISION
                )) {

            RaceManager.ensurePermanentNightVision(
                    player,
                    race
            );
        }
    }

    private static final Identifier ENVIRONMENT_MOVEMENT_ID =
            Identifier.fromNamespaceAndPath(
                    RaceCore.MODID,
                    "environment_movement"
            );

    private static void updateEnvironmentMovement(
            Player player,
            RaceDefinition race
    ) {
        var movement =
                player.getAttribute(
                        net.minecraft.world.entity.ai.attributes.Attributes.MOVEMENT_SPEED
                );

        if (movement == null) {
            return;
        }

        double multiplier = 1.0;

        int light =
                player.level()
                        .getMaxLocalRawBrightness(
                                player.blockPosition()
                        );

        if (light <= race.darkLightThreshold()) {
            multiplier *=
                    race.darkMovementMultiplier();
        }

        if (player.level()
                .getBiome(player.blockPosition())
                .is(FOREST_BIOMES)) {

            multiplier *=
                    race.forestMovementMultiplier();
        }

        if (player.level().dimension()
                == Level.NETHER) {

            multiplier *=
                    race.netherMovementMultiplier();
        }

        if (Math.abs(multiplier - 1.0)
                <= 0.000001) {

            movement.removeModifier(
                    ENVIRONMENT_MOVEMENT_ID
            );

            return;
        }

        movement.addOrUpdateTransientModifier(
                new net.minecraft.world.entity.ai.attributes.AttributeModifier(
                        ENVIRONMENT_MOVEMENT_ID,
                        multiplier - 1.0,
                        net.minecraft.world.entity.ai.attributes.AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL
                )
        );
    }

    @SubscribeEvent
    public static void onRightClickItem(
            PlayerInteractEvent.RightClickItem event
    ) {
        Player player = event.getEntity();
        ItemStack stack = player.getItemInHand(event.getHand());
        RaceDefinition race = RaceManager.getRace(player);

        if (!race.rocketFlightBoostAllowed()
                && stack.is(Items.FIREWORK_ROCKET)
                && player.isFallFlying()) {

            event.setCanceled(true);
            event.setCancellationResult(
                    InteractionResult.FAIL
            );

            if (!player.level().isClientSide()) {
                player.sendOverlayMessage(
                        Component.translatable(
                                "message.racecore.ability.no_rocket_boost"
                        )
                );
            }

            return;
        }

        if (!race.rangedAllowedInBrightLight()
                && stack.getItem()
                instanceof ProjectileWeaponItem) {

            int light =
                    player.level()
                            .getMaxLocalRawBrightness(
                                    player.blockPosition()
                            );

            if (light > race.darkLightThreshold()) {
                event.setCanceled(true);
                event.setCancellationResult(
                        InteractionResult.FAIL
                );

                if (!player.level().isClientSide()) {
                    player.sendOverlayMessage(
                            Component.translatable(
                                    "message.racecore.ability.too_bright_ranged"
                            )
                    );
                }
                return;
            }
        }

        if (stack.getItem() instanceof CrossbowItem
                && !race.canLoadFireworkRockets()) {

            ItemStack projectile = player.getProjectile(stack);

            if (projectile.is(Items.FIREWORK_ROCKET)) {
                event.setCanceled(true);
                event.setCancellationResult(InteractionResult.FAIL);

                if (!player.level().isClientSide()) {
                    player.sendOverlayMessage(
                            Component.translatable(
                                    "message.racecore.ability.no_firework_crossbow"
                            )
                    );
                }
                return;
            }
        }

        if (race.canEatWhenFull()
                && stack.has(DataComponents.FOOD)
                && stack.has(DataComponents.CONSUMABLE)
                && !player.canEat(false)) {

            player.startUsingItem(event.getHand());
            event.setCanceled(true);
            event.setCancellationResult(InteractionResult.CONSUME);
        }
    }

    @SubscribeEvent
    public static void onFoodFinished(
            LivingEntityUseItemEvent.Finish event
    ) {
        if (!(event.getEntity() instanceof Player player)
                || player.level().isClientSide()) {
            return;
        }

        FoodProperties food =
                event.getItem().get(DataComponents.FOOD);

        if (food == null) {
            return;
        }

        RaceDefinition race = RaceManager.getRace(player);

        double multiplier = race.foodHungerMultiplier();
        if (multiplier > 1.0) {
            int extraNutrition = (int) Math.round(
                    food.nutrition() * (multiplier - 1.0)
            );

            if (extraNutrition > 0) {
                player.getFoodData().setFoodLevel(
                        Math.min(
                                20,
                                player.getFoodData().getFoodLevel()
                                        + extraNutrition
                        )
                );
            }
        }

        double healing = race.foodHealing();
        if (healing > 0.0
                && player.getHealth() < player.getMaxHealth()) {

            player.setHealth(
                    (float) Math.min(
                            player.getMaxHealth(),
                            player.getHealth() + healing
                    )
            );
        }
    }

    @SubscribeEvent
    public static void onBlockDrops(
            BlockDropsEvent event
    ) {
        if (!(event.getBreaker()
                instanceof Player player)) {

            return;
        }

        RaceDefinition race =
                RaceManager.getRace(player);

        int fortuneBonus =
                race.fortuneBonus();

        if (event.getState().is(FORTUNE_CROPS)) {
            fortuneBonus += race.cropFortuneBonus();
        }

        if (fortuneBonus <= 0) {
            return;
        }

        ItemStack originalTool =
                event.getTool();

        if (originalTool.isEmpty()) {
            return;
        }

        var lookup =
                event.getLevel()
                        .registryAccess()
                        .lookupOrThrow(
                                Registries.ENCHANTMENT
                        );

        Holder.Reference<Enchantment> fortune =
                lookup.getOrThrow(
                        Enchantments.FORTUNE
                );

        ItemStack effectiveTool =
                originalTool.copy();

        int originalFortune =
                effectiveTool
                        .getEnchantmentLevel(
                                fortune
                        );

        final int effectiveFortuneBonus =
                fortuneBonus;

        EnchantmentHelper.updateEnchantments(
                effectiveTool,
                mutable -> mutable.set(
                        fortune,
                        originalFortune
                                + effectiveFortuneBonus
                )
        );

        List<ItemStack> rerolled =
                Block.getDrops(
                        event.getState(),
                        event.getLevel(),
                        event.getPos(),
                        event.getBlockEntity(),
                        player,
                        effectiveTool
                );

        event.getDrops().clear();

        double x =
                event.getPos().getX() + 0.5;

        double y =
                event.getPos().getY() + 0.5;

        double z =
                event.getPos().getZ() + 0.5;

        for (ItemStack stack : rerolled) {
            if (stack.isEmpty()) {
                continue;
            }

            event.getDrops().add(
                    new ItemEntity(
                            event.getLevel(),
                            x,
                            y,
                            z,
                            stack
                    )
            );
        }
    }
}
