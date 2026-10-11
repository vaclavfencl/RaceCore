package com.example.racecore;

import java.util.Optional;
import net.minecraft.core.component.DataComponents;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.equipment.Equippable;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.ModifyDefaultComponentsEvent;

/**
 * Changes vanilla Golden Helmet's display geometry to RaceCore's item crown.
 * Keeps all armor, durability, enchanting, and equippable characteristics.
 * No equipment asset means the head slot uses the normal item model (and
 * therefore the custom display.head transform from Blockbench).
 */
@EventBusSubscriber(modid = RaceCore.MODID)
public final class GoldenCrownItemEvents {
    private GoldenCrownItemEvents() {}

    @SubscribeEvent
    public static void modifyGoldenHelmet(ModifyDefaultComponentsEvent event) {
        event.modify(Items.GOLDEN_HELMET, (components, context, item) -> {
            Equippable original = components.get(DataComponents.EQUIPPABLE);

            components.set(
                    DataComponents.ITEM_MODEL,
                    Identifier.fromNamespaceAndPath(RaceCore.MODID, "golden_crown")
            );

            if (original == null) {
                throw new IllegalStateException("Golden Helmet is missing EQUIPPABLE");
            }

            components.set(DataComponents.EQUIPPABLE, new Equippable(
                    original.slot(),
                    original.equipSound(),
                    Optional.empty(), // Fall back to normal item model in head slot.
                    original.cameraOverlay(),
                    original.allowedEntities(),
                    original.dispensable(),
                    original.swappable(),
                    original.damageOnHurt(),
                    original.equipOnInteract(),
                    original.canBeSheared(),
                    original.shearingSound()
            ));
        });
    }
}
