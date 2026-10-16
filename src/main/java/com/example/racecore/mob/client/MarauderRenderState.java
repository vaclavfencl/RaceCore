package com.example.racecore.mob.client;

import net.minecraft.client.renderer.entity.state.HumanoidRenderState;
import net.minecraft.world.entity.AnimationState;

/** The entity's synced attack and wind-up, plus client walking flag. */
public final class MarauderRenderState extends HumanoidRenderState {
    public boolean walking;
    public final AnimationState attackAnimation = new AnimationState();
    public final AnimationState powerupAnimation = new AnimationState();
}
