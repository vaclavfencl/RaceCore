package com.example.racecore.mob.client;

import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.world.entity.AnimationState;

/** Client snapshot of River Drake movement and abilities. */
public final class RiverDrakeRenderState extends LivingEntityRenderState {
    public boolean walking;
    public final AnimationState biteAnimation = new AnimationState();
    public final AnimationState spinAnimation = new AnimationState();
}
