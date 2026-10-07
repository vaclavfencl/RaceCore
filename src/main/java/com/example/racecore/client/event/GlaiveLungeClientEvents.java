package com.example.racecore.client.event;

import com.example.racecore.RaceCore;
import com.example.racecore.network.weapon.GlaiveLungePayload;
import com.example.racecore.weapon.RaceCoreWeapons;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import net.minecraft.client.Minecraft;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.resources.Identifier;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.ItemEnchantments;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.InputEvent;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;

/**
 * Right-click ability for RaceCore glaives enchanted with Lunge.
 */
@EventBusSubscriber(
        modid = RaceCore.MODID,
        value = Dist.CLIENT
)
public final class GlaiveLungeClientEvents {
    private static final String LUNGE_ID =
            "minecraft:lunge";

    private GlaiveLungeClientEvents() {
    }

    @SubscribeEvent
    public static void onUseInput(
            InputEvent.InteractionKeyMappingTriggered event
    ) {
        if (!event.isUseItem()
                || event.getHand()
                != InteractionHand.MAIN_HAND) {

            return;
        }

        Minecraft minecraft =
                Minecraft.getInstance();

        if (minecraft.player == null) {
            return;
        }

        ItemStack stack =
                minecraft.player
                        .getMainHandItem();

        if (!RaceCoreWeapons.isGlaive(stack)
                || !hasLunge(stack)) {

            return;
        }

        /*
         * Lunge owns right-click while a Lunge glaive is held.
         * Do not also activate blocks/entities/items with the same click.
         */
        event.setCanceled(true);
        event.setSwingHand(false);

        if (minecraft.player
                .getCooldowns()
                .isOnCooldown(stack)) {

            return;
        }

        ClientPacketDistributor.sendToServer(
                GlaiveLungePayload.INSTANCE
        );
    }

    private static boolean hasLunge(
            ItemStack stack
    ) {
        ItemEnchantments enchantments =
                stack.getOrDefault(
                        DataComponents.ENCHANTMENTS,
                        ItemEnchantments.EMPTY
                );

        for (Object2IntMap.Entry<
                Holder<Enchantment>> entry
                : enchantments.entrySet()) {

            if (entry.getIntValue() <= 0) {
                continue;
            }

            Identifier id =
                    entry.getKey()
                            .unwrapKey()
                            .map(key ->
                                    key.identifier())
                            .orElse(null);

            if (id != null
                    && LUNGE_ID.equals(
                    id.toString()
            )) {

                return true;
            }
        }

        return false;
    }
}
