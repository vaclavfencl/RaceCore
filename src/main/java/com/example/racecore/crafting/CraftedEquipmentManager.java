package com.example.racecore.crafting;

import com.example.racecore.RaceCore;
import com.example.racecore.armor.ArmorDefinition;
import com.example.racecore.armor.ArmorManager;
import com.example.racecore.armor.ArmorWeight;
import com.example.racecore.race.RaceDefinition;
import com.example.racecore.race.RaceManager;
import com.example.racecore.race.RaceRegistry;
import com.example.racecore.weapon.RaceCoreWeapons;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.ItemTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.EquipmentSlotGroup;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.CrossbowItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.ProjectileWeaponItem;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.component.ItemAttributeModifiers;

public final class CraftedEquipmentManager {
    private static final String CRAFTED_RACE_KEY =
            "racecore_crafted_race";

    private static final String ARMOR_BONUS_KEY =
            "racecore_crafted_armor_bonus";

    private static final String GARGOYLE_SPEAR_KEY =
            "racecore_gargoyle_spear";

    private static final Identifier CRAFTED_DAMAGE_ID =
            Identifier.fromNamespaceAndPath(
                    RaceCore.MODID,
                    "crafted_damage_bonus"
            );

    private static final Identifier CRAFTED_ATTACK_SPEED_ID =
            Identifier.fromNamespaceAndPath(
                    RaceCore.MODID,
                    "crafted_attack_speed_bonus"
            );

    private static final TagKey<Item> SPEARS =
            TagKey.create(
                    Registries.ITEM,
                    Identifier.withDefaultNamespace(
                            "spears"
                    )
            );

    private CraftedEquipmentManager() {
    }

    public static void applyCraftingTraits(
            Player crafter,
            ItemStack stack
    ) {
        if (crafter == null
                || stack == null
                || stack.isEmpty()
                || !isTrackedEquipment(stack)) {
            return;
        }

        String raceId =
                RaceManager.getRaceId(crafter);

        RaceDefinition race =
                RaceRegistry.getOrHuman(raceId);

        markCraftedRace(
                stack,
                raceId
        );

        double durabilityMultiplier = 1.0;
        double damageBonus = 0.0;
        double attackSpeedBonus = 0.0;
        double armorBonus = 0.0;

        switch (raceId) {
            case "human" -> {
                if (isLongsword(stack)) {
                    durabilityMultiplier = 1.20;
                    damageBonus = 0.10;
                }
            }

            case "dwarf" -> {
                if (isTool(stack)) {
                    durabilityMultiplier = 1.25;
                }

                ArmorDefinition armor =
                        ArmorManager.getDefinition(
                                stack
                        );

                if (armor != null
                        && (
                        armor.weight()
                                == ArmorWeight.MEDIUM
                                || armor.weight()
                                == ArmorWeight.HEAVY
                                || armor.weight()
                                == ArmorWeight.ULTRA_HEAVY
                )) {
                    armorBonus = 5.0;
                }

                if (stack.is(Items.MACE)) {
                    attackSpeedBonus = 0.10;
                }
            }

            case "elf" -> {
                if (isWeapon(stack)
                        && !isClub(stack)
                        && !isFlail(stack)
                        && !isCrossbow(stack)) {
                    durabilityMultiplier = 1.40;
                }
            }

            case "giant" -> {
                if (isClub(stack)
                        || isFlail(stack)) {
                    durabilityMultiplier = 0.85;
                    damageBonus = 0.20;
                }
            }

            case "troll" -> {
                if (stack.is(
                        ItemTags.AXES
                )) {
                    attackSpeedBonus = 0.15;
                }
            }

            case "gargoyle" -> {
                if (isSpear(stack)) {
                    markGargoyleSpear(stack);
                }
            }

            case "goblin" -> {
                if (isDagger(stack)) {
                    durabilityMultiplier = 1.20;
                    attackSpeedBonus = 0.15;
                }
            }

            case "dragonborn" -> {
                if (ArmorManager.getDefinition(
                        stack
                ) != null) {
                    durabilityMultiplier = 1.20;
                }
            }

            case "tiefling" -> {
                if (isGlaive(stack)) {
                    damageBonus = 0.10;
                    attackSpeedBonus = 0.10;
                }

                if (isCrossbow(stack)) {
                    durabilityMultiplier = 1.25;
                }
            }

            default -> {
            }
        }

        applyDurabilityMultiplier(
                stack,
                durabilityMultiplier
        );

        applyCombatModifiers(
                stack,
                damageBonus,
                attackSpeedBonus
        );

        if (armorBonus > 0.0) {
            final double appliedArmorBonus =
                    armorBonus;

            CustomData.update(
                    DataComponents.CUSTOM_DATA,
                    stack,
                    tag -> tag.putDouble(
                            ARMOR_BONUS_KEY,
                            appliedArmorBonus
                    )
            );
        }

        if (race.displayName() == null
                || race.displayName().isBlank()) {
            RaceCore.LOGGER.warn(
                    "Crafted equipment race '{}' has no display name.",
                    raceId
            );
        }
    }

    public static String getCraftedRaceId(
            ItemStack stack
    ) {
        if (stack == null
                || stack.isEmpty()) {
            return "";
        }

        return stack.getOrDefault(
                        DataComponents.CUSTOM_DATA,
                        CustomData.EMPTY
                )
                .copyTag()
                .getStringOr(
                        CRAFTED_RACE_KEY,
                        ""
                );
    }

    public static double getArmorBonus(
            ItemStack stack
    ) {
        if (stack == null
                || stack.isEmpty()) {
            return 0.0;
        }

        return stack.getOrDefault(
                        DataComponents.CUSTOM_DATA,
                        CustomData.EMPTY
                )
                .copyTag()
                .getDoubleOr(
                        ARMOR_BONUS_KEY,
                        0.0
                );
    }

    public static boolean isGargoyleCraftedSpear(
            ItemStack stack
    ) {
        if (stack == null
                || stack.isEmpty()) {
            return false;
        }

        return stack.getOrDefault(
                        DataComponents.CUSTOM_DATA,
                        CustomData.EMPTY
                )
                .contains(
                        GARGOYLE_SPEAR_KEY
                );
    }

    private static void markCraftedRace(
            ItemStack stack,
            String raceId
    ) {
        CustomData.update(
                DataComponents.CUSTOM_DATA,
                stack,
                tag -> tag.putString(
                        CRAFTED_RACE_KEY,
                        raceId
                )
        );
    }

    private static void markGargoyleSpear(
            ItemStack stack
    ) {
        CustomData.update(
                DataComponents.CUSTOM_DATA,
                stack,
                tag -> tag.putBoolean(
                        GARGOYLE_SPEAR_KEY,
                        true
                )
        );
    }

    private static void applyDurabilityMultiplier(
            ItemStack stack,
            double multiplier
    ) {
        if (!stack.isDamageableItem()
                || Math.abs(multiplier - 1.0)
                <= 0.000001) {
            return;
        }

        int oldMax =
                stack.getMaxDamage();

        if (oldMax <= 0) {
            return;
        }

        stack.set(
                DataComponents.MAX_DAMAGE,
                Math.max(
                        1,
                        (int) Math.round(
                                oldMax
                                        * multiplier
                        )
                )
        );
    }

    private static void applyCombatModifiers(
            ItemStack stack,
            double damageBonus,
            double attackSpeedBonus
    ) {
        if (Math.abs(damageBonus)
                <= 0.000001
                && Math.abs(attackSpeedBonus)
                <= 0.000001) {
            return;
        }

        ItemAttributeModifiers attributes =
                stack.getOrDefault(
                        DataComponents.ATTRIBUTE_MODIFIERS,
                        ItemAttributeModifiers.EMPTY
                );

        if (Math.abs(damageBonus)
                > 0.000001) {
            attributes =
                    attributes.withModifierAdded(
                            Attributes.ATTACK_DAMAGE,
                            new AttributeModifier(
                                    CRAFTED_DAMAGE_ID,
                                    damageBonus,
                                    AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL
                            ),
                            EquipmentSlotGroup.MAINHAND
                    );
        }

        if (Math.abs(attackSpeedBonus)
                > 0.000001) {
            attributes =
                    attributes.withModifierAdded(
                            Attributes.ATTACK_SPEED,
                            new AttributeModifier(
                                    CRAFTED_ATTACK_SPEED_ID,
                                    attackSpeedBonus,
                                    AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL
                            ),
                            EquipmentSlotGroup.MAINHAND
                    );
        }

        stack.set(
                DataComponents.ATTRIBUTE_MODIFIERS,
                attributes
        );
    }

    private static boolean isTrackedEquipment(
            ItemStack stack
    ) {
        return ArmorManager.getDefinition(stack)
                != null
                || isTool(stack)
                || isWeapon(stack);
    }

    private static boolean isTool(
            ItemStack stack
    ) {
        return stack.is(ItemTags.PICKAXES)
                || stack.is(ItemTags.AXES)
                || stack.is(ItemTags.SHOVELS)
                || stack.is(ItemTags.HOES)
                || stack.is(Items.SHEARS);
    }

    private static boolean isWeapon(
            ItemStack stack
    ) {
        return isRaceCoreWeapon(stack)
                || stack.has(
                        DataComponents.WEAPON
                )
                || stack.has(
                        DataComponents.KINETIC_WEAPON
                )
                || stack.has(
                        DataComponents.PIERCING_WEAPON
                )
                || stack.getItem()
                instanceof ProjectileWeaponItem
                || stack.is(Items.TRIDENT)
                || stack.is(Items.MACE)
                || isSpear(stack);
    }

    private static boolean isRaceCoreWeapon(
            ItemStack stack
    ) {
        return RaceCoreWeapons
                .isCustomWeapon(stack);
    }

    private static boolean isDagger(
            ItemStack stack
    ) {
        return RaceCoreWeapons
                .isDagger(stack);
    }

    private static boolean isGlaive(
            ItemStack stack
    ) {
        return RaceCoreWeapons
                .isGlaive(stack);
    }

    private static boolean isLongsword(
            ItemStack stack
    ) {
        return RaceCoreWeapons
                .isLongsword(stack);
    }

    private static boolean isClub(
            ItemStack stack
    ) {
        return RaceCoreWeapons
                .isClub(stack);
    }

    private static boolean isFlail(
            ItemStack stack
    ) {
        return RaceCoreWeapons
                .isFlail(stack);
    }

    private static boolean isCrossbow(
            ItemStack stack
    ) {
        return stack.getItem()
                instanceof CrossbowItem;
    }

    private static boolean isSpear(
            ItemStack stack
    ) {
        return stack.is(SPEARS);
    }
}
