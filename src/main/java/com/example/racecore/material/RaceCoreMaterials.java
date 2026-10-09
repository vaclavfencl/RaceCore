package com.example.racecore.material;

import com.example.racecore.RaceCore;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ToolMaterial;
import net.minecraft.world.item.equipment.ArmorMaterial;
import net.minecraft.world.item.equipment.ArmorMaterials;
import net.minecraft.world.item.equipment.ArmorType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.Map;

/**
 * RaceCore material content that does not fit the vanilla material set.
 *
 * Steel:
 * - made from iron + coal;
 * - armor uses the iron visual asset for now;
 * - weapon material sits between iron and diamond.
 *
 * Mithril:
 * - armor only; no Mithril weapon/tool tier is registered;
 * - armor uses the chainmail visual asset for now;
 * - Mithril Ore has no world generation here. Generation is intentionally
 *   left for the later world-generation/content phase.
 */
public final class RaceCoreMaterials {
    public static final DeferredRegister.Blocks BLOCKS =
            DeferredRegister.createBlocks(RaceCore.MODID);

    public static final DeferredRegister.Items ITEMS =
            DeferredRegister.createItems(RaceCore.MODID);

    public static final TagKey<Item> REPAIRS_STEEL =
            itemTag("repairs_steel");

    public static final TagKey<Item> REPAIRS_MITHRIL =
            itemTag("repairs_mithril");

    /**
     * Steel is intentionally between Iron and Diamond as a weapon material.
     */
    public static final ToolMaterial STEEL_TOOL_MATERIAL =
            new ToolMaterial(
                    BlockTags.INCORRECT_FOR_IRON_TOOL,
                    750,
                    6.5F,
                    2.5F,
                    12,
                    REPAIRS_STEEL
            );

    /**
     * RaceCore's real armor protection comes from data/racecore/armor.json.
     * These vanilla-style values provide durability/enchant/equip components
     * and a sensible vanilla armor-bar fallback.
     */
    public static final ArmorMaterial STEEL_ARMOR_MATERIAL =
            new ArmorMaterial(
                    25,
                    Map.copyOf(ArmorMaterials.IRON.defense()),
                    12,
                    ArmorMaterials.IRON.equipSound(),
                    1.0F,
                    0.0F,
                    REPAIRS_STEEL,
                    ArmorMaterials.IRON.assetId()
            );

    /**
     * Mithril deliberately reuses chainmail's equipment asset for the current
     * placeholder visuals while retaining its own durability and repair item.
     */
    public static final ArmorMaterial MITHRIL_ARMOR_MATERIAL =
            new ArmorMaterial(
                    45,
                    Map.copyOf(ArmorMaterials.DIAMOND.defense()),
                    15,
                    ArmorMaterials.CHAINMAIL.equipSound(),
                    2.0F,
                    0.0F,
                    REPAIRS_MITHRIL,
                    ArmorMaterials.CHAINMAIL.assetId()
            );

    public static final DeferredBlock<Block> MITHRIL_ORE =
            BLOCKS.registerBlock(
                    "mithril_ore",
                    Block::new,
                    () -> Block.Properties
                            .ofFullCopy(Blocks.ANCIENT_DEBRIS)
            );

    public static final DeferredItem<Item> STEEL_INGOT =
            ITEMS.registerSimpleItem("steel_ingot");

    public static final DeferredItem<Item> MITHRIL_INGOT =
            ITEMS.registerSimpleItem("mithril_ingot");

    public static final DeferredItem<Item> STEEL_PICKAXE =
            ITEMS.registerItem(
                    "steel_pickaxe",
                    properties -> new Item(
                            properties.pickaxe(
                                    STEEL_TOOL_MATERIAL,
                                    1.0F,
                                    -2.8F
                            )
                    )
            );

    public static final DeferredItem<Item> STEEL_AXE =
            ITEMS.registerItem(
                    "steel_axe",
                    properties -> new Item(
                            properties.axe(
                                    STEEL_TOOL_MATERIAL,
                                    5.0F,
                                    -3.0F
                            )
                    )
            );

    public static final DeferredItem<Item> STEEL_SHOVEL =
            ITEMS.registerItem(
                    "steel_shovel",
                    properties -> new Item(
                            properties.shovel(
                                    STEEL_TOOL_MATERIAL,
                                    1.5F,
                                    -3.0F
                            )
                    )
            );

    public static final DeferredItem<Item> STEEL_HOE =
            ITEMS.registerItem(
                    "steel_hoe",
                    properties -> new Item(
                            properties.hoe(
                                    STEEL_TOOL_MATERIAL,
                                    -2.5F,
                                    -0.5F
                            )
                    )
            );

    public static final DeferredItem<BlockItem> MITHRIL_ORE_ITEM =
            ITEMS.registerSimpleBlockItem(MITHRIL_ORE);

    public static final DeferredItem<Item> STEEL_HELMET =
            registerArmor(
                    "steel_helmet",
                    STEEL_ARMOR_MATERIAL,
                    ArmorType.HELMET
            );

    public static final DeferredItem<Item> STEEL_CHESTPLATE =
            registerArmor(
                    "steel_chestplate",
                    STEEL_ARMOR_MATERIAL,
                    ArmorType.CHESTPLATE
            );

    public static final DeferredItem<Item> STEEL_LEGGINGS =
            registerArmor(
                    "steel_leggings",
                    STEEL_ARMOR_MATERIAL,
                    ArmorType.LEGGINGS
            );

    public static final DeferredItem<Item> STEEL_BOOTS =
            registerArmor(
                    "steel_boots",
                    STEEL_ARMOR_MATERIAL,
                    ArmorType.BOOTS
            );

    public static final DeferredItem<Item> MITHRIL_HELMET =
            registerArmor(
                    "mithril_helmet",
                    MITHRIL_ARMOR_MATERIAL,
                    ArmorType.HELMET
            );

    public static final DeferredItem<Item> MITHRIL_CHESTPLATE =
            registerArmor(
                    "mithril_chestplate",
                    MITHRIL_ARMOR_MATERIAL,
                    ArmorType.CHESTPLATE
            );

    public static final DeferredItem<Item> MITHRIL_LEGGINGS =
            registerArmor(
                    "mithril_leggings",
                    MITHRIL_ARMOR_MATERIAL,
                    ArmorType.LEGGINGS
            );

    public static final DeferredItem<Item> MITHRIL_BOOTS =
            registerArmor(
                    "mithril_boots",
                    MITHRIL_ARMOR_MATERIAL,
                    ArmorType.BOOTS
            );

    private RaceCoreMaterials() {
    }

    public static void register(IEventBus modBus) {
        BLOCKS.register(modBus);
        ITEMS.register(modBus);
    }

    private static DeferredItem<Item> registerArmor(
            String name,
            ArmorMaterial material,
            ArmorType type
    ) {
        return ITEMS.registerItem(
                name,
                properties -> new Item(
                        properties.humanoidArmor(
                                material,
                                type
                        )
                )
        );
    }

    private static TagKey<Item> itemTag(String path) {
        return TagKey.create(
                Registries.ITEM,
                Identifier.fromNamespaceAndPath(
                        RaceCore.MODID,
                        path
                )
        );
    }
}
