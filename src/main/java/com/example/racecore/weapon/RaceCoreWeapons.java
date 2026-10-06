package com.example.racecore.weapon;

import com.example.racecore.RaceCore;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.EquipmentSlotGroup;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ToolMaterial;
import net.minecraft.world.item.component.ItemAttributeModifiers;
import net.neoforged.bus.api.IEventBus;
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

    /*
     * Iron keeps the original 8A.0 registry IDs for compatibility:
     * dagger / glaive / longsword.
     */

    public static final DeferredItem<Item> STONE_DAGGER =
            registerWeapon("stone_dagger", ToolMaterial.STONE, 1.25F, -1.80F, 0.0);
    public static final DeferredItem<Item> COPPER_DAGGER =
            registerWeapon("copper_dagger", ToolMaterial.COPPER, 1.25F, -1.80F, 0.0);
    public static final DeferredItem<Item> DAGGER =
            registerWeapon("dagger", ToolMaterial.IRON, 1.25F, -1.80F, 0.0);
    public static final DeferredItem<Item> GOLDEN_DAGGER =
            registerWeapon("golden_dagger", ToolMaterial.GOLD, 1.25F, -1.80F, 0.0);
    public static final DeferredItem<Item> DIAMOND_DAGGER =
            registerWeapon("diamond_dagger", ToolMaterial.DIAMOND, 1.25F, -1.80F, 0.0);
    public static final DeferredItem<Item> NETHERITE_DAGGER =
            registerWeapon("netherite_dagger", ToolMaterial.NETHERITE, 1.25F, -1.80F, 0.0);

    public static final DeferredItem<Item> STONE_GLAIVE =
            registerWeapon("stone_glaive", ToolMaterial.STONE, 3.0F, -2.65F, 1.25);
    public static final DeferredItem<Item> COPPER_GLAIVE =
            registerWeapon("copper_glaive", ToolMaterial.COPPER, 3.0F, -2.65F, 1.25);
    public static final DeferredItem<Item> GLAIVE =
            registerWeapon("glaive", ToolMaterial.IRON, 3.0F, -2.65F, 1.25);
    public static final DeferredItem<Item> GOLDEN_GLAIVE =
            registerWeapon("golden_glaive", ToolMaterial.GOLD, 3.0F, -2.65F, 1.25);
    public static final DeferredItem<Item> DIAMOND_GLAIVE =
            registerWeapon("diamond_glaive", ToolMaterial.DIAMOND, 3.0F, -2.65F, 1.25);
    public static final DeferredItem<Item> NETHERITE_GLAIVE =
            registerWeapon("netherite_glaive", ToolMaterial.NETHERITE, 3.0F, -2.65F, 1.25);

    public static final DeferredItem<Item> STONE_LONGSWORD =
            registerWeapon("stone_longsword", ToolMaterial.STONE, 4.0F, -2.65F, 0.25);
    public static final DeferredItem<Item> COPPER_LONGSWORD =
            registerWeapon("copper_longsword", ToolMaterial.COPPER, 4.0F, -2.65F, 0.25);
    public static final DeferredItem<Item> LONGSWORD =
            registerWeapon("longsword", ToolMaterial.IRON, 4.0F, -2.65F, 0.25);
    public static final DeferredItem<Item> GOLDEN_LONGSWORD =
            registerWeapon("golden_longsword", ToolMaterial.GOLD, 4.0F, -2.65F, 0.25);
    public static final DeferredItem<Item> DIAMOND_LONGSWORD =
            registerWeapon("diamond_longsword", ToolMaterial.DIAMOND, 4.0F, -2.65F, 0.25);
    public static final DeferredItem<Item> NETHERITE_LONGSWORD =
            registerWeapon("netherite_longsword", ToolMaterial.NETHERITE, 4.0F, -2.65F, 0.25);

    public static final DeferredItem<Item> CLUB =
            registerWeapon("club", ToolMaterial.WOOD, 3.0F, -2.50F, 0.0);

    public static final DeferredItem<Item> FLAIL =
            registerWeapon("flail", ToolMaterial.IRON, 5.0F, -3.20F, 0.75);

    private RaceCoreWeapons() {
    }

    public static void register(IEventBus modBus) {
        ITEMS.register(modBus);
    }

    private static DeferredItem<Item> registerWeapon(
            String name,
            ToolMaterial material,
            float attackDamageBaseline,
            float attackSpeedModifier,
            double reachBonus
    ) {
        return ITEMS.registerItem(
                name,
                properties -> new Item(
                        weaponProperties(
                                properties,
                                material,
                                attackDamageBaseline,
                                attackSpeedModifier,
                                reachBonus
                        )
                )
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

        return properties.attributes(attributes.build());
    }

    public static boolean isDagger(ItemStack stack) {
        return matchesAny(
                stack,
                STONE_DAGGER,
                COPPER_DAGGER,
                DAGGER,
                GOLDEN_DAGGER,
                DIAMOND_DAGGER,
                NETHERITE_DAGGER
        );
    }

    public static boolean isGlaive(ItemStack stack) {
        return matchesAny(
                stack,
                STONE_GLAIVE,
                COPPER_GLAIVE,
                GLAIVE,
                GOLDEN_GLAIVE,
                DIAMOND_GLAIVE,
                NETHERITE_GLAIVE
        );
    }

    public static boolean isLongsword(ItemStack stack) {
        return matchesAny(
                stack,
                STONE_LONGSWORD,
                COPPER_LONGSWORD,
                LONGSWORD,
                GOLDEN_LONGSWORD,
                DIAMOND_LONGSWORD,
                NETHERITE_LONGSWORD
        );
    }

    public static boolean isClub(ItemStack stack) {
        return stack != null
                && !stack.isEmpty()
                && stack.is(CLUB.get());
    }

    public static boolean isFlail(ItemStack stack) {
        return stack != null
                && !stack.isEmpty()
                && stack.is(FLAIL.get());
    }

    public static boolean isCustomWeapon(ItemStack stack) {
        return isDagger(stack)
                || isGlaive(stack)
                || isLongsword(stack)
                || isClub(stack)
                || isFlail(stack);
    }

    @SafeVarargs
    private static boolean matchesAny(
            ItemStack stack,
            DeferredItem<Item>... items
    ) {
        if (stack == null
                || stack.isEmpty()) {
            return false;
        }

        for (DeferredItem<Item> item : items) {
            if (stack.is(item.get())) {
                return true;
            }
        }

        return false;
    }

}
