package com.example.racecore.mob.client;

import com.example.racecore.RaceCore;
import com.example.racecore.mob.entity.StoneSentinelEntity;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.resources.Identifier;

/** Shared model for the adult and the lesser Sentinel; the latter retains its SCALE attribute. */
public final class StoneSentinelRenderer<T extends StoneSentinelEntity>
        extends MobRenderer<T, StoneSentinelRenderState, StoneSentinelModel> {
    private static final Identifier TEXTURE = Identifier.fromNamespaceAndPath(
            RaceCore.MODID, "textures/entity/stone_sentinel.png");

    public StoneSentinelRenderer(EntityRendererProvider.Context context) {
        super(context, new StoneSentinelModel(context.bakeLayer(StoneSentinelModel.LAYER_LOCATION)), 0.7f);
    }

    @Override
    public StoneSentinelRenderState createRenderState() {
        return new StoneSentinelRenderState();
    }

    @Override
    public void extractRenderState(T entity, StoneSentinelRenderState state, float partialTick) {
        super.extractRenderState(entity, state, partialTick);
        // Slow stone sentinels often don't reach the vanilla .05 walking cutoff.
        // Use normal locomotion data with a velocity fallback for slow movement.
        state.walking = state.walkAnimationSpeed > 0.01F
                || entity.getDeltaMovement().horizontalDistanceSqr() > 0.0001D;
        state.attackAnimation.copyFrom(entity.attackAnimationState);
        state.throwAnimation.copyFrom(entity.throwAnimationState);
        state.summonAnimation.copyFrom(entity.summonAnimationState);
    }

    @Override
    public Identifier getTextureLocation(StoneSentinelRenderState state) {
        return TEXTURE;
    }
}
