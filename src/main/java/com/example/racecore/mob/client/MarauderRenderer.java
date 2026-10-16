package com.example.racecore.mob.client;

import com.example.racecore.RaceCore;
import com.example.racecore.mob.entity.MarauderEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.HumanoidMobRenderer;
import net.minecraft.client.renderer.entity.layers.ItemInHandLayer;
import net.minecraft.resources.Identifier;

/** Replaces the Pillager visuals but keeps the Marauder's current entity/AI. */
public final class MarauderRenderer
        extends HumanoidMobRenderer<MarauderEntity, MarauderRenderState, MarauderModel> {
    private static final Identifier TEXTURE = Identifier.fromNamespaceAndPath(
            RaceCore.MODID, "textures/entity/marauder.png");

    // Blockbench geometry is ~54 model pixels tall. Scale to ~2 blocks tall.
    private static final float MODEL_SCALE = 0.60F;

    public MarauderRenderer(EntityRendererProvider.Context context) {
        super(context, new MarauderModel(context.bakeLayer(MarauderModel.LAYER_LOCATION)), 0.50F);
        this.addLayer(new ItemInHandLayer<>(this));
    }

    @Override
    public MarauderRenderState createRenderState() {
        return new MarauderRenderState();
    }

    @Override
    public void extractRenderState(MarauderEntity entity, MarauderRenderState state, float partialTick) {
        super.extractRenderState(entity, state, partialTick);
        state.walking = state.walkAnimationSpeed > 0.01F
                || entity.getDeltaMovement().horizontalDistanceSqr() > 0.0001D;
        state.attackAnimation.copyFrom(entity.attackAnimationState);
        state.powerupAnimation.copyFrom(entity.powerupAnimationState);
    }

    @Override
    public Identifier getTextureLocation(MarauderRenderState state) {
        return TEXTURE;
    }

    @Override
    protected void scale(MarauderRenderState state, PoseStack poseStack) {
        super.scale(state, poseStack);
        poseStack.scale(MODEL_SCALE, MODEL_SCALE, MODEL_SCALE);
    }
}
