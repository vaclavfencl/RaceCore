package com.example.racecore.explosion;

import com.example.racecore.RaceCore;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.entity.projectile.FireworkRocketEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.FireworkExplosion;
import net.minecraft.world.item.component.Fireworks;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;

import java.util.ArrayList;
import java.util.List;

/**
 * Makes plain rockets meaningful crossbow ammunition.
 *
 * Vanilla FireworkRocketEntity only deals explosion damage when its FIREWORKS
 * component contains one or more FireworkExplosion entries. A normal
 * paper+gunpowder rocket has zero entries, so it deals zero explosion damage.
 *
 * CrossbowItem creates its firework projectile with shotAtAngle=true. We use
 * that vanilla flag to distinguish crossbow ammunition from normal placed /
 * Elytra-boost rockets.
 *
 * By inserting FireworkExplosion.DEFAULT before the entity begins flying,
 * vanilla itself handles:
 * - impact on entities
 * - impact on blocks
 * - lifetime expiry
 * - AoE damage/falloff/line-of-sight
 * - fireworks damage source and knockback
 * - visuals/sound
 *
 * Fireworks do not use a terrain-destroying block explosion, so structures are
 * not damaged by this change.
 */
@EventBusSubscriber(modid = RaceCore.MODID)
public final class CrossbowRocketEvents {
    private CrossbowRocketEvents() {
    }

    @SubscribeEvent
    public static void onEntityJoin(
            EntityJoinLevelEvent event
    ) {
        if (event.getLevel().isClientSide()) {
            return;
        }

        if (!(event.getEntity()
                instanceof FireworkRocketEntity rocket)) {

            return;
        }

        if (!rocket.isShotAtAngle()) {
            return;
        }

        int minimumExplosions =
                ExplosionRegistry.settings()
                        .crossbowRocketMinimumExplosions();

        if (minimumExplosions <= 0) {
            return;
        }

        ItemStack rocketStack =
                rocket.getItem();

        Fireworks current =
                rocketStack.get(
                        DataComponents.FIREWORKS
                );

        int flightDuration =
                current == null
                        ? 1
                        : current.flightDuration();

        List<FireworkExplosion> explosions =
                current == null
                        ? new ArrayList<>()
                        : new ArrayList<>(
                                current.explosions()
                        );

        if (explosions.size()
                >= minimumExplosions) {

            return;
        }

        while (explosions.size()
                < minimumExplosions) {

            explosions.add(
                    FireworkExplosion.DEFAULT
            );
        }

        rocketStack.set(
                DataComponents.FIREWORKS,
                new Fireworks(
                        flightDuration,
                        List.copyOf(explosions)
                )
        );
    }
}
