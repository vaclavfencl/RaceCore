package com.example.racecore.weapon;

import com.example.racecore.RaceCore;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.EquipmentSlotGroup;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ToolMaterial;
import net.minecraft.world.item.component.ItemAttributeModifiers;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class RaceCoreWeapons {
    public static final DeferredRegister.Items ITEMS =
            DeferredRegister.createItems(RaceCore.MODID);

    private static final Identifier WEAPON_REACH_ID =
            Identifier.fromNamespaceAndPath(
                    RaceCore.MODID,
                    "weapon_reach"
            );

    public static final DeferredItem<Item> DAGGER =
            ITEMS.registerItem(
                    "dagger",
                    properties -> new Item(
                            weaponProperties(
                                    properties,
                                    ToolMaterial.IRON,
                                    1.25F,
                                    -1.80F,
                                    0.0
                            )
                    )
            );

    public static final DeferredItem<Item> GLAIVE =
            ITEMS.registerItem(
                    "glaive",
                    properties -> new Item(
                            weaponProperties(
                                    properties,
                                    ToolMaterial.IRON,
                                    3.0F,
                                    -2.65F,
                                    1.25
                            )
                    )
            );

    public static final DeferredItem<Item> LONGSWORD =
            ITEMS.registerItem(
                    "longsword",
                    properties -> new Item(
                            weaponProperties(
                                    properties,
                                    ToolMaterial.IRON,
                                    4.0F,
                                    -2.65F,
                                    0.25
                            )
                    )
            );

    public static final DeferredItem<Item> CLUB =
            ITEMS.registerItem(
                    "club",
                    properties -> new Item(
                            weaponProperties(
                                    properties,
                                    ToolMaterial.WOOD,
                                    3.0F,
                                    -2.50F,
                                    0.0
                            )
                    )
            );

    public static final DeferredItem<Item> FLAIL =
            ITEMS.registerItem(
                    "flail",
                    properties -> new Item(
                            weaponProperties(
                                    properties,
                                    ToolMaterial.IRON,
                                    5.0F,
                                    -3.20F,
                                    0.75
                            )
                    )
            );

    private RaceCoreWeapons() {
    }

    public static void register(
            IEventBus modBus
    ) {
        ITEMS.register(modBus);
        modBus.addListener(
                RaceCoreWeapons::addCreativeItems
        );
    }

    private static Item.Properties weaponProperties(
            Item.Properties properties,
            ToolMaterial material,
            float attackDamageBaseline,
            float attackSpeedModifier,
            double reachBonus
    ) {
        properties.sword(
                material,
                attackDamageBaseline,
                attackSpeedModifier
        );

        ItemAttributeModifiers.Builder attributes =
                ItemAttributeModifiers.builder()
                        .add(
                                Attributes.ATTACK_DAMAGE,
                                new AttributeModifier(
                                        Item.BASE_ATTACK_DAMAGE_ID,
                                        attackDamageBaseline
                                                + material.attackDamageBonus(),
                                        AttributeModifier.Operation.ADD_VALUE
                                ),
                                EquipmentSlotGroup.MAINHAND
                        )
                        .add(
                                Attributes.ATTACK_SPEED,
                                new AttributeModifier(
                                        Item.BASE_ATTACK_SPEED_ID,
                                        attackSpeedModifier,
                                        AttributeModifier.Operation.ADD_VALUE
                                ),
                                EquipmentSlotGroup.MAINHAND
                        );

        if (Math.abs(reachBonus) > 0.000001) {
            attributes.add(
                    Attributes.ENTITY_INTERACTION_RANGE,
                    new AttributeModifier(
                            WEAPON_REACH_ID,
                            reachBonus,
                            AttributeModifier.Operation.ADD_VALUE
                    ),
                    EquipmentSlotGroup.MAINHAND
            );
        }

        return properties.attributes(
                attributes.build()
        );
    }

    private static void addCreativeItems(
            BuildCreativeModeTabContentsEvent event
    ) {
        if (event.getTabKey()
                != CreativeModeTabs.COMBAT) {
            return;
        }

        event.accept(DAGGER);
        event.accept(GLAIVE);
        event.accept(LONGSWORD);
        event.accept(CLUB);
        event.accept(FLAIL);
    }
}
