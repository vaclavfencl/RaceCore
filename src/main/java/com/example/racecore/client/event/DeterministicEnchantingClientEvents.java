package com.example.racecore.client.event;

import com.example.racecore.RaceCore;
import com.example.racecore.client.screen.DeterministicEnchantmentScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.EnchantmentScreen;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ScreenEvent;

/**
 * Replaces only the visual screen. The vanilla EnchantmentMenu remains open,
 * so item/lapis inventory handling and table validity stay vanilla.
 */
@EventBusSubscriber(
        modid = RaceCore.MODID,
        value = Dist.CLIENT
)
public final class DeterministicEnchantingClientEvents {
    private DeterministicEnchantingClientEvents() {
    }

    @SubscribeEvent
    public static void onScreenOpening(
            ScreenEvent.Opening event
    ) {
        if (!(event.getNewScreen()
                instanceof EnchantmentScreen vanilla)) {

            return;
        }

        if (vanilla
                instanceof DeterministicEnchantmentScreen) {

            return;
        }

        Minecraft minecraft =
                Minecraft.getInstance();

        if (minecraft.player == null) {
            return;
        }

        event.setNewScreen(
                new DeterministicEnchantmentScreen(
                        vanilla.getMenu(),
                        minecraft.player
                                .getInventory(),
                        vanilla.getTitle()
                )
        );
    }
    @SubscribeEvent
    public static void onMouseButtonPressed(
            ScreenEvent.MouseButtonPressed.Pre event
    ) {
        if (!(event.getScreen()
                instanceof DeterministicEnchantmentScreen screen)) {

            return;
        }

        if (screen.handleOverlayClick(
                event.getMouseX(),
                event.getMouseY(),
                event.getButton()
        )) {
            /*
             * Consume the click before vanilla EnchantmentScreen processes it.
             * This is especially important in compact mode, where our +/-
             * controls sit directly over vanilla's hidden three offer rows.
             */
            event.setCanceled(true);
        }
    }

}
