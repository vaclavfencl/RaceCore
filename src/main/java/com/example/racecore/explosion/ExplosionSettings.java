package com.example.racecore.explosion;

import java.util.List;

/**
 * Data-driven RaceCore explosion rules.
 *
 * crossbowRocketMinimumExplosions:
 * Plain rockets normally contain zero firework explosions and therefore deal
 * zero vanilla firework damage. RaceCore upgrades only crossbow-fired rockets
 * to at least this many explosion entries.
 *
 * noBlockDamageEntityTypes:
 * Explosions whose direct source entity type is listed here still damage and
 * knock back entities, but their affected block list is cleared.
 */
public record ExplosionSettings(
        int crossbowRocketMinimumExplosions,
        List<String> noBlockDamageEntityTypes
) {
}
