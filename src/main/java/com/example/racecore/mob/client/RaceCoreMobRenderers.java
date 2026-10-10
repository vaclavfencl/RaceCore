package com.example.racecore.mob.client;

import com.example.racecore.RaceCore;
import com.example.racecore.mob.RaceCoreMobs;
import net.minecraft.client.renderer.entity.IronGolemRenderer;
import net.minecraft.client.renderer.entity.PillagerRenderer;
import net.minecraft.client.renderer.entity.ThrownItemRenderer;
import net.minecraft.client.renderer.entity.WolfRenderer;
import net.minecraft.client.renderer.entity.ZombieRenderer;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;

/**
 * Temporary vanilla visuals.
 *
 * No Mojang texture files are copied into RaceCore; the vanilla renderers
 * reference the vanilla models/textures directly.
 */
@EventBusSubscriber(
        modid = RaceCore.MODID,
        value = Dist.CLIENT
)
public final class RaceCoreMobRenderers {
    private RaceCoreMobRenderers() {
    }

    @SubscribeEvent
    public static void registerRenderers(
            EntityRenderersEvent.RegisterRenderers event
    ) {
        event.registerEntityRenderer(
                RaceCoreMobs.KOBOLD.get(),
                ZombieRenderer::new
        );

        event.registerEntityRenderer(
                RaceCoreMobs.RIVER_DRAKE.get(),
                WolfRenderer::new
        );

        event.registerEntityRenderer(
                RaceCoreMobs.MARAUDER.get(),
                PillagerRenderer::new
        );

        event.registerEntityRenderer(
                RaceCoreMobs.STONE_SENTINEL.get(),
                IronGolemRenderer::new
        );

        event.registerEntityRenderer(
                RaceCoreMobs.LESSER_STONE_SENTINEL.get(),
                IronGolemRenderer::new
        );

        event.registerEntityRenderer(
                RaceCoreMobs.ROCK_PROJECTILE.get(),
                ThrownItemRenderer::new
        );
    }
}
