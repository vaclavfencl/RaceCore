package com.example.racecore.armor;

import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;

/**
 * Classifies incoming damage into RaceCore protection categories.
 *
 * This is intentionally pragmatic, not exhaustive. You can refine it later if
 * a specific vanilla or modded source should land in another category.
 */
public final class ProtectionTypeResolver {
    private ProtectionTypeResolver() {
    }

    public static ProtectionType resolve(DamageSource source) {
        if (source == null) {
            return null;
        }

        if (source.is(DamageTypeTags.IS_EXPLOSION)) {
            return ProtectionType.BLAST;
        }

        if (source.is(DamageTypeTags.IS_PROJECTILE)) {
            return ProtectionType.PROJECTILE;
        }

        if (source.is(DamageTypeTags.IS_FIRE)) {
            return ProtectionType.FIRE;
        }

        if (source.is(DamageTypes.MAGIC)
                || source.is(DamageTypes.INDIRECT_MAGIC)
                || source.is(DamageTypes.DRAGON_BREATH)
                || source.is(DamageTypes.THORNS)
                || source.is(DamageTypes.SONIC_BOOM)) {
            return ProtectionType.MAGIC;
        }

        if (source.is(DamageTypes.WITHER)
                || source.is(DamageTypes.DROWN)
                || source.is(DamageTypes.STARVE)
                || source.is(DamageTypes.DRY_OUT)) {
            return ProtectionType.OVERTIME;
        }

        return ProtectionType.MELEE;
    }
}
