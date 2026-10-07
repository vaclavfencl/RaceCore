package com.example.racecore.network.weapon;

import com.example.racecore.RaceCore;
import com.example.racecore.weapon.RaceCoreWeapons;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.minecraft.network.protocol.game.ClientboundSetEntityMotionPacket;

/**
 * Server-authoritative right-click Lunge for RaceCore glaives.
 *
 * RaceCore replaces vanilla Lunge's hunger/exhaustion cost with a fixed
 * 4-second cooldown. Dash strength still scales with Lunge level.
 */
@EventBusSubscriber(modid = RaceCore.MODID)
public final class GlaiveLungeNetworking {
    private static final int LUNGE_COOLDOWN_TICKS =
            4 * 20;

    private static final double LUNGE_IMPULSE_PER_LEVEL =
            0.458D;

    private GlaiveLungeNetworking() {
    }

    @SubscribeEvent
    public static void registerPayloads(
            RegisterPayloadHandlersEvent event
    ) {
        var registrar =
                event.registrar(
                        "glaive-lunge-1"
                );

        registrar.playToServer(
                GlaiveLungePayload.TYPE,
                GlaiveLungePayload.STREAM_CODEC,
                GlaiveLungeNetworking::handle
        );
    }

    private static void handle(
            GlaiveLungePayload payload,
            IPayloadContext context
    ) {
        context.enqueueWork(() -> {
            if (!(context.player()
                    instanceof ServerPlayer player)) {

                return;
            }

            ItemStack stack =
                    player.getMainHandItem();

            if (!RaceCoreWeapons.isGlaive(stack)
                    || player.isSpectator()) {

                return;
            }

            int lungeLevel =
                    getLungeLevel(
                            player,
                            stack
                    );

            if (lungeLevel <= 0
                    || player.getCooldowns()
                    .isOnCooldown(stack)) {

                return;
            }

            /*
             * Keep vanilla movement restrictions, but deliberately remove
             * vanilla's food-level requirement and exhaustion cost.
             */
            if (player.isPassenger()
                    || player.isFallFlying()
                    || player.isInWater()) {

                return;
            }

            Vec3 look =
                    player.getLookAngle();

            Vec3 horizontal =
                    new Vec3(
                            look.x,
                            0.0D,
                            look.z
                    );

            if (horizontal.lengthSqr()
                    <= 0.000001D) {

                return;
            }

            /*
             * Apply one shared cooldown to every glaive material so swapping
             * glaives cannot bypass the 4-second ability cooldown.
             */
            addGlaiveCooldowns(
                    player
            );

            double impulse =
                    LUNGE_IMPULSE_PER_LEVEL
                            * lungeLevel;

            Vec3 dash =
                    horizontal.normalize()
                            .scale(impulse);

            player.addDeltaMovement(dash);
            player.needsSync = true;
            player.applyPostImpulseGraceTime(10);

            player.connection.send(
                    new ClientboundSetEntityMotionPacket(player)
            );

            /*
             * Preserve vanilla Lunge's 1 durability cost, but not its hunger
             * / exhaustion cost.
             */
            if (!player.hasInfiniteMaterials()) {
                stack.hurtAndBreak(
                        1,
                        player,
                        EquipmentSlot.MAINHAND
                );
            }

            playLungeSound(
                    player,
                    lungeLevel
            );
        });
    }

    private static int getLungeLevel(
            ServerPlayer player,
            ItemStack stack
    ) {
        HolderLookup.RegistryLookup<Enchantment> enchantments =
                player.level()
                        .registryAccess()
                        .lookupOrThrow(
                                Registries.ENCHANTMENT
                        );

        Holder.Reference<Enchantment> lunge =
                enchantments.getOrThrow(
                        Enchantments.LUNGE
                );

        return EnchantmentHelper
                .getEnchantmentsForCrafting(
                        stack
                )
                .getLevel(
                        lunge
                );
    }

    private static void addGlaiveCooldowns(
            ServerPlayer player
    ) {
        player.getCooldowns()
                .addCooldown(
                        new ItemStack(
                                RaceCoreWeapons
                                        .STONE_GLAIVE
                                        .get()
                        ),
                        LUNGE_COOLDOWN_TICKS
                );

        player.getCooldowns()
                .addCooldown(
                        new ItemStack(
                                RaceCoreWeapons
                                        .COPPER_GLAIVE
                                        .get()
                        ),
                        LUNGE_COOLDOWN_TICKS
                );

        player.getCooldowns()
                .addCooldown(
                        new ItemStack(
                                RaceCoreWeapons
                                        .GLAIVE
                                        .get()
                        ),
                        LUNGE_COOLDOWN_TICKS
                );

        player.getCooldowns()
                .addCooldown(
                        new ItemStack(
                                RaceCoreWeapons
                                        .GOLDEN_GLAIVE
                                        .get()
                        ),
                        LUNGE_COOLDOWN_TICKS
                );

        player.getCooldowns()
                .addCooldown(
                        new ItemStack(
                                RaceCoreWeapons
                                        .DIAMOND_GLAIVE
                                        .get()
                        ),
                        LUNGE_COOLDOWN_TICKS
                );

        player.getCooldowns()
                .addCooldown(
                        new ItemStack(
                                RaceCoreWeapons
                                        .NETHERITE_GLAIVE
                                        .get()
                        ),
                        LUNGE_COOLDOWN_TICKS
                );
    }

    private static void playLungeSound(
            ServerPlayer player,
            int level
    ) {
        Holder<SoundEvent> sound =
                switch (Math.min(
                        3,
                        Math.max(
                                1,
                                level
                        )
                )) {
                    case 1 ->
                            SoundEvents.LUNGE_1;

                    case 2 ->
                            SoundEvents.LUNGE_2;

                    default ->
                            SoundEvents.LUNGE_3;
                };

        player.level()
                .playSound(
                        null,
                        player.getX(),
                        player.getY(),
                        player.getZ(),
                        sound.value(),
                        SoundSource.PLAYERS,
                        1.0F,
                        1.0F
                );
    }
}
