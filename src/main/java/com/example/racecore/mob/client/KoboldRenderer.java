package com.example.racecore.mob.client;

import com.example.racecore.RaceCore;
import com.example.racecore.mob.entity.KoboldEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.HumanoidMobRenderer;
import net.minecraft.client.renderer.entity.layers.ItemInHandLayer;
import net.minecraft.client.renderer.entity.state.HumanoidRenderState;
import net.minecraft.resources.Identifier;

/** A 64x64 Blockbench Kobold model with vanilla-style humanoid animation. */
public final class KoboldRenderer
        extends HumanoidMobRenderer<KoboldEntity, HumanoidRenderState, KoboldModel> {
    private static final Identifier TEXTURE =
            Identifier.fromNamespaceAndPath(RaceCore.MODID, "textures/entity/kobold.png");

    // Blockbench's unscaled model is about 25 pixels high.
    // 0.65 makes it about 16 px / one block without destroying its 64x64 UV map.
    private static final float MODEL_SCALE = 0.65F;

    public KoboldRenderer(EntityRendererProvider.Context context) {
        super(context, new KoboldModel(context.bakeLayer(KoboldModel.LAYER_LOCATION)), 0.28F);
        this.addLayer(new ItemInHandLayer<>(this));
    }

    @Override
    public HumanoidRenderState createRenderState() {
        return new HumanoidRenderState();
    }

    @Override
    public Identifier getTextureLocation(HumanoidRenderState state) {
        return TEXTURE;
    }

    @Override
    protected void scale(HumanoidRenderState state, PoseStack poseStack) {
        super.scale(state, poseStack);
        poseStack.scale(MODEL_SCALE, MODEL_SCALE, MODEL_SCALE);
    }
}
