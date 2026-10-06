package com.example.racecore.client.tooltip;

import com.example.racecore.RaceCore;
import com.example.racecore.enchanting.WeaponToolEnchantingManager;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.ItemTooltipEvent;

@EventBusSubscriber(
        modid = RaceCore.MODID,
        value = Dist.CLIENT
)
public final class WeaponToolTooltipEvents {
    private WeaponToolTooltipEvents() {
    }

    @SubscribeEvent
    public static void onItemTooltip(
            ItemTooltipEvent event
    ) {
        ItemStack stack =
                event.getItemStack();

        if (!WeaponToolEnchantingManager
                .isWeaponOrTool(stack)) {
            return;
        }

        int effectiveCapacity =
                WeaponToolEnchantingManager
                        .getEffectiveCapacity(
                                event.getEntity(),
                                stack
                        );

        int utilityCapacity =
                WeaponToolEnchantingManager
                        .getUtilityCapacity(
                                event.getEntity(),
                                stack
                        );

        event.getToolTip().add(
                Component.empty()
        );

        event.getToolTip().add(
                Component.translatable(
                                "tooltip.racecore.weapon.effective_cap",
                                Component.literal(
                                                Integer.toString(
                                                        effectiveCapacity
                                                )
                                        )
                                        .withStyle(
                                                ChatFormatting.AQUA
                                        )
                        )
                        .withStyle(
                                ChatFormatting.GRAY
                        )
        );

        event.getToolTip().add(
                Component.translatable(
                                "tooltip.racecore.weapon.utility_cap",
                                Component.literal(
                                                Integer.toString(
                                                        utilityCapacity
                                                )
                                        )
                                        .withStyle(
                                                ChatFormatting.AQUA
                                        )
                        )
                        .withStyle(
                                ChatFormatting.GRAY
                        )
        );
    }
}
