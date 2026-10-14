package com.example.racecore.mob.client;

import com.example.racecore.RaceCore;
import com.example.racecore.mob.entity.RiverDrakeEntity;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.resources.Identifier;

/** Dedicated River Drake renderer; preserves its 64x64 authored texture. */
public final class RiverDrakeRenderer
        extends MobRenderer<RiverDrakeEntity, RiverDrakeRenderState, RiverDrakeModel> {
    private static final Identifier TEXTURE = Identifier.fromNamespaceAndPath(
            RaceCore.MODID, "textures/entity/river_drake.png");

    public RiverDrakeRenderer(EntityRendererProvider.Context context) {
        super(context, new RiverDrakeModel(context.bakeLayer(RiverDrakeModel.LAYER_LOCATION)), 0.4F);
    }

    @Override
    public RiverDrakeRenderState createRenderState() {
        return new RiverDrakeRenderState();
    }

    @Override
    public void extractRenderState(RiverDrakeEntity dragon, RiverDrakeRenderState state, float partialTick) {
        super.extractRenderState(dragon, state, partialTick);
        state.walking = state.walkAnimationSpeed > 0.01F
                || dragon.getDeltaMovement().horizontalDistanceSqr() > 0.0001D;
        state.biteAnimation.copyFrom(dragon.biteAnimationState);
        state.spinAnimation.copyFrom(dragon.spinAnimationState);
    }

    @Override
    public Identifier getTextureLocation(RiverDrakeRenderState state) {
        return TEXTURE;
    }

}
