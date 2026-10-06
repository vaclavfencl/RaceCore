package com.example.racecore.client.tooltip;

import com.example.racecore.RaceCore;
import com.example.racecore.crafting.CraftedEquipmentManager;
import com.example.racecore.race.RaceDefinition;
import com.example.racecore.race.RaceRegistry;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.ItemTooltipEvent;

import java.util.List;

@EventBusSubscriber(
        modid = RaceCore.MODID,
        value = Dist.CLIENT
)
public final class CraftedEquipmentTooltipEvents {
    private CraftedEquipmentTooltipEvents() {
    }

    @SubscribeEvent
    public static void onItemTooltip(
            ItemTooltipEvent event
    ) {
        String raceId =
                CraftedEquipmentManager
                        .getCraftedRaceId(
                                event.getItemStack()
                        );

        if (raceId.isBlank()) {
            return;
        }

        List<Component> tooltip =
                event.getToolTip();

        if (tooltip.isEmpty()) {
            return;
        }

        RaceDefinition race =
                RaceRegistry.getOrHuman(
                        raceId
                );

        Component original =
                tooltip.getFirst();

        MutableComponent prefixed =
                Component.literal(
                                race.displayName()
                                        + " "
                        )
                        .withStyle(
                                original.getStyle()
                        )
                        .append(
                                original.copy()
                        );

        tooltip.set(
                0,
                prefixed
        );
    }
}
