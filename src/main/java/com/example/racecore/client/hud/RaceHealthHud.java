package com.example.racecore.client.hud;

import com.example.racecore.RaceCore;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Player;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterGuiLayersEvent;
import net.neoforged.neoforge.client.gui.VanillaGuiLayers;

@EventBusSubscriber(modid = RaceCore.MODID, value = Dist.CLIENT)
public final class RaceHealthHud {
    private static final int HEART_COUNT = 10;
    private static final int HEART_SIZE = 9;
    private static final int HEART_SPACING = 8;

    private static final Identifier HEART_CONTAINER =
            Identifier.fromNamespaceAndPath("minecraft", "hud/heart/container");

    private static final Identifier HEART_FULL =
            Identifier.fromNamespaceAndPath("minecraft", "hud/heart/full");

    private static final Identifier HEART_HALF =
            Identifier.fromNamespaceAndPath("minecraft", "hud/heart/half");

    private RaceHealthHud() {
    }

    @SubscribeEvent
    public static void registerGuiLayers(RegisterGuiLayersEvent event) {
        event.wrapLayer(
                VanillaGuiLayers.PLAYER_HEALTH,
                original -> (graphics, partialTick) -> {
                    // Keep vanilla HUD layout bookkeeping, but hide its heart rendering.
                    graphics.enableScissor(0, 0, 0, 0);
                    try {
                        original.render(graphics, partialTick);
                    } finally {
                        graphics.disableScissor();
                    }

                    renderRaceCoreHealth(graphics);
                }
        );

        event.wrapLayer(
                VanillaGuiLayers.ARMOR_LEVEL,
                original -> (graphics, partialTick) -> {

                }
        );
    }

    private static void renderRaceCoreHealth(GuiGraphicsExtractor graphics) {
        Minecraft minecraft = Minecraft.getInstance();
        Player player = minecraft.player;

        if (player == null || player.isSpectator()) {
            return;
        }

        float maxHealth = player.getMaxHealth();
        if (maxHealth <= 0.0F) {
            return;
        }

        float currentHealth = Math.max(0.0F, Math.min(player.getHealth(), maxHealth));
        double healthRatio = currentHealth / maxHealth;

        // One half-heart = 5% max HP.
        int filledHalfHearts = currentHealth <= 0.0F
                ? 0
                : (int) Math.ceil((healthRatio * 20.0) - 1.0E-7);

        filledHalfHearts = Math.max(0, Math.min(20, filledHalfHearts));

        int xLeft = graphics.guiWidth() / 2 - 91;
        int y = graphics.guiHeight() - 39;

        for (int i = 0; i < HEART_COUNT; i++) {
            int x = xLeft + i * HEART_SPACING;

            graphics.blitSprite(
                    RenderPipelines.GUI_TEXTURED,
                    HEART_CONTAINER,
                    x,
                    y,
                    HEART_SIZE,
                    HEART_SIZE
            );
        }

        for (int i = 0; i < HEART_COUNT; i++) {
            int halvesForThisHeart = filledHalfHearts - (i * 2);

            if (halvesForThisHeart <= 0) {
                break;
            }

            Identifier sprite = halvesForThisHeart >= 2
                    ? HEART_FULL
                    : HEART_HALF;

            int x = xLeft + i * HEART_SPACING;

            graphics.blitSprite(
                    RenderPipelines.GUI_TEXTURED,
                    sprite,
                    x,
                    y,
                    HEART_SIZE,
                    HEART_SIZE
            );
        }

        int displayedCurrentHealth = currentHealth <= 0.0F
                ? 0
                : (int) Math.ceil(currentHealth - 1.0E-4F);

        int displayedMaxHealth = Math.round(maxHealth);

        String healthText =
                displayedCurrentHealth + "/" + displayedMaxHealth;

        int rowWidth = (HEART_COUNT - 1) * HEART_SPACING + HEART_SIZE;
        int rowCenter = xLeft + rowWidth / 2;
        int textX = rowCenter - minecraft.font.width(healthText) / 2;

        graphics.text(
                minecraft.font,
                healthText,
                textX,
                y + 1,
                0xFFFFFFFF,
                true
        );
    }
}
