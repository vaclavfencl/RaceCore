package com.example.racecore.mixin;

import net.minecraft.world.effect.MobEffectInstance;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(MobEffectInstance.class)
public interface MobEffectInstanceAccessor {
    @Accessor("duration")
    void racecore$setDuration(int duration);

    @Accessor("amplifier")
    void racecore$setAmplifier(int amplifier);
}
