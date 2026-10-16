package com.example.racecore.mob.client;

import com.example.racecore.RaceCore;
import com.example.racecore.mob.RaceCoreMobs;
import net.minecraft.client.renderer.entity.ThrownItemRenderer;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;

/** Dedicated Blockbench models for RaceCore's custom mobs. */
@EventBusSubscriber(modid = RaceCore.MODID, value = Dist.CLIENT)
public final class RaceCoreMobRenderers {
    private RaceCoreMobRenderers() {}

    @SubscribeEvent
    public static void registerLayers(EntityRenderersEvent.RegisterLayerDefinitions event) {
        event.registerLayerDefinition(KoboldModel.LAYER_LOCATION, KoboldModel::createBodyLayer);
        event.registerLayerDefinition(RiverDrakeModel.LAYER_LOCATION, RiverDrakeModel::createBodyLayer);
        event.registerLayerDefinition(MarauderModel.LAYER_LOCATION, MarauderModel::createBodyLayer);
        event.registerLayerDefinition(StoneSentinelModel.LAYER_LOCATION, StoneSentinelModel::createBodyLayer);
    }

    @SubscribeEvent
    public static void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(RaceCoreMobs.KOBOLD.get(), KoboldRenderer::new);
        event.registerEntityRenderer(RaceCoreMobs.RIVER_DRAKE.get(), RiverDrakeRenderer::new);
        event.registerEntityRenderer(RaceCoreMobs.MARAUDER.get(), MarauderRenderer::new);
        event.registerEntityRenderer(RaceCoreMobs.STONE_SENTINEL.get(), StoneSentinelRenderer::new);
        event.registerEntityRenderer(RaceCoreMobs.LESSER_STONE_SENTINEL.get(), StoneSentinelRenderer::new);
        event.registerEntityRenderer(RaceCoreMobs.ROCK_PROJECTILE.get(), ThrownItemRenderer::new);
    }
}
