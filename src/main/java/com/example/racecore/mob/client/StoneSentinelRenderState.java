package com.example.racecore.mob.client;

import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.world.entity.AnimationState;

/** Per-frame client animation state, separate from server AI. */
public final class StoneSentinelRenderState extends LivingEntityRenderState {
    public boolean walking;
    public final AnimationState attackAnimation = new AnimationState();
    public final AnimationState throwAnimation = new AnimationState();
    public final AnimationState summonAnimation = new AnimationState();
}
