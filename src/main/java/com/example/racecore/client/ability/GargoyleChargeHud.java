package com.example.racecore.client.ability;

import com.example.racecore.RaceCore;
import com.example.racecore.race.RaceDefinition;
import com.example.racecore.race.RaceManager;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.resources.Identifier;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterGuiLayersEvent;
import net.neoforged.neoforge.client.gui.VanillaGuiLayers;

/**
 * Reuses Minecraft's vanilla horse-jump HUD sprites for Gargoyle charge.
 */
@EventBusSubscriber(
        modid = RaceCore.MODID,
        value = Dist.CLIENT
)
public final class GargoyleChargeHud {
    private static final Identifier LAYER_ID =
            Identifier.fromNamespaceAndPath(
                    RaceCore.MODID,
                    "gargoyle_charge"
            );

    private static final Identifier JUMP_BAR_BACKGROUND =
            Identifier.withDefaultNamespace(
                    "hud/jump_bar_background"
            );

    private static final Identifier JUMP_BAR_PROGRESS =
            Identifier.withDefaultNamespace(
                    "hud/jump_bar_progress"
            );

    private static final int BAR_WIDTH = 182;
    private static final int BAR_HEIGHT = 5;

    private GargoyleChargeHud() {
    }

    @SubscribeEvent
    public static void registerGuiLayers(
            RegisterGuiLayersEvent event
    ) {
        /*
         * Render after vanilla's contextual info bar. This intentionally sits
         * at the same location where vanilla displays the horse jump meter.
         */
        event.registerAbove(
                VanillaGuiLayers.CONTEXTUAL_INFO_BAR,
                LAYER_ID,
                GargoyleChargeHud::render
        );
    }

    private static void render(
            GuiGraphicsExtractor graphics,
            net.minecraft.client.DeltaTracker deltaTracker
    ) {
        Minecraft minecraft =
                Minecraft.getInstance();

        if (minecraft.player == null) {
            return;
        }

        RaceDefinition race =
                RaceManager.getRace(
                        minecraft.player
                );

        if (!race.chargedJumpEnabled()
                || !GargoyleMovementClientEvents.isCharging()) {
            return;
        }

        float progress =
                GargoyleMovementClientEvents
                        .getChargeProgress(race);

        int x =
                graphics.guiWidth() / 2
                        - BAR_WIDTH / 2;

        int y =
                graphics.guiHeight()
                        - 32
                        + 3;

        graphics.blitSprite(
                RenderPipelines.GUI_TEXTURED,
                JUMP_BAR_BACKGROUND,
                x,
                y,
                BAR_WIDTH,
                BAR_HEIGHT
        );

        int filled =
                Math.min(
                        BAR_WIDTH,
                        Math.max(
                                0,
                                Math.round(
                                        progress * BAR_WIDTH
                                )
                        )
                );

        if (filled <= 0) {
            return;
        }

        /*
         * Crop the vanilla progress sprite rather than stretching it.
         */
        graphics.blitSprite(
                RenderPipelines.GUI_TEXTURED,
                JUMP_BAR_PROGRESS,
                BAR_WIDTH,
                BAR_HEIGHT,
                0,
                0,
                x,
                y,
                filled,
                BAR_HEIGHT
        );
    }
}
