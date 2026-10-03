package com.example.racecore.race.ability;

import com.example.racecore.RaceCore;
import com.example.racecore.race.RaceDefinition;
import com.example.racecore.race.RaceManager;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.arrow.AbstractArrow;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;

/**
 * Generic RaceCore ranged-trajectory abilities.
 *
 * AbstractArrow keeps the ItemStack it was actually fired from, so this works
 * correctly for main-hand/off-hand shots and crossbow multishot without
 * tracking timing state on the player.
 */
@EventBusSubscriber(modid = RaceCore.MODID)
public final class RaceProjectileEvents {
    private static final TagKey<Item> BOWS =
            TagKey.create(
                    Registries.ITEM,
                    Identifier.fromNamespaceAndPath(
                            RaceCore.MODID,
                            "bows"
                    )
            );

    private static final TagKey<Item> CROSSBOWS =
            TagKey.create(
                    Registries.ITEM,
                    Identifier.fromNamespaceAndPath(
                            RaceCore.MODID,
                            "crossbows"
                    )
            );

    private RaceProjectileEvents() {
    }

    @SubscribeEvent
    public static void onEntityJoin(
            EntityJoinLevelEvent event
    ) {
        if (event.getLevel().isClientSide()) {
            return;
        }

        if (!(event.getEntity()
                instanceof AbstractArrow arrow)) {
            return;
        }

        if (!(arrow.getOwner()
                instanceof Player player)) {
            return;
        }

        ItemStack weapon =
                arrow.getWeaponItem();

        if (weapon == null || weapon.isEmpty()) {
            return;
        }

        RaceDefinition race =
                RaceManager.getRace(player);

        boolean noFalloff =
                (race.bowNoFalloff()
                        && weapon.is(BOWS))
                || (race.crossbowNoFalloff()
                        && weapon.is(CROSSBOWS));

        if (noFalloff) {
            /*
             * "No falloff" currently means no projectile gravity/drop.
             * Vanilla horizontal air drag remains, so water and normal arrow
             * physics still behave sensibly.
             */
            arrow.setNoGravity(true);
        }
    }
}
