package com.example.racecore.durability;

import com.example.racecore.RaceCore;
import com.example.racecore.weapon.RaceCoreWeapons;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.AnvilUpdateEvent;

@EventBusSubscriber(modid = RaceCore.MODID)
public final class RaceCoreRepairEvents {
    private static final double FULL_REPAIR_MATERIAL_FRACTION =
            0.40;

    private RaceCoreRepairEvents() {
    }

    @SubscribeEvent
    public static void onAnvilUpdate(
            AnvilUpdateEvent event
    ) {
        ItemStack left =
                event.getLeft();

        ItemStack right =
                event.getRight();

        if (!BrokenItemManager.isSupported(left)
                || left.getDamageValue() <= 0
                || right.isEmpty()) {
            return;
        }

        int originalMaterialCost =
                originalMaterialCost(left);

        if (originalMaterialCost <= 0
                || !isRepairMaterial(
                        left,
                        right
                )) {
            return;
        }

        int fullRepairUnits =
                Math.max(
                        1,
                        (int) Math.round(
                                originalMaterialCost
                                        * FULL_REPAIR_MATERIAL_FRACTION
                        )
                );

        int maxDamage =
                left.getMaxDamage();

        int currentDamage =
                left.getDamageValue();

        if (maxDamage <= 0
                || currentDamage <= 0) {
            return;
        }

        double missingFraction =
                (double) currentDamage
                        / (double) maxDamage;

        int unitsNeeded =
                Math.max(
                        1,
                        (int) Math.ceil(
                                missingFraction
                                        * fullRepairUnits
                        )
                );

        int unitsUsed =
                Math.min(
                        right.getCount(),
                        unitsNeeded
                );

        if (unitsUsed <= 0) {
            return;
        }

        int repairAmount =
                (int) Math.ceil(
                        maxDamage
                                * (
                                (double) unitsUsed
                                        / (double) fullRepairUnits
                        )
                );

        ItemStack output =
                left.copy();

        output.setDamageValue(
                Math.max(
                        0,
                        currentDamage
                                - repairAmount
                )
        );

        if (output.getDamageValue()
                < output.getMaxDamage() - 1) {
            BrokenItemManager
                    .clearBroken(output);
        }

        output.remove(
                DataComponents.REPAIR_COST
        );

        String name =
                event.getName();

        if (name != null) {
            if (name.isBlank()) {
                output.remove(
                        DataComponents.CUSTOM_NAME
                );
            } else {
                output.set(
                        DataComponents.CUSTOM_NAME,
                        Component.literal(name)
                );
            }
        }

        event.setOutput(output);
        event.setXpCost(1);
        event.setMaterialCost(unitsUsed);
    }

    private static boolean isRepairMaterial(
            ItemStack gear,
            ItemStack repair
    ) {
        Identifier id =
                BuiltInRegistries.ITEM
                        .getKeyOrNull(
                                gear.getItem()
                        );

        if (id == null) {
            return false;
        }

        String path =
                id.getPath();

        if (path.startsWith("chainmail_")) {
            return repair.is(
                    Items.IRON_NUGGET
            );
        }

        if (path.startsWith("netherite_")) {
            return repair.is(
                    Items.NETHERITE_SCRAP
            );
        }

        return gear.isValidRepairItem(
                repair
        );
    }

    private static int originalMaterialCost(
            ItemStack stack
    ) {
        Identifier id =
                BuiltInRegistries.ITEM
                        .getKeyOrNull(
                                stack.getItem()
                        );

        if (id == null) {
            return 0;
        }

        String path =
                id.getPath();

        if (path.startsWith("netherite_")) {
            return 4;
        }

        if (RaceCoreWeapons
                .isDagger(stack)) {
            return 1;
        }

        if (RaceCoreWeapons
                .isGlaive(stack)) {
            return 2;
        }

        if (RaceCoreWeapons
                .isLongsword(stack)) {
            return 3;
        }

        if (RaceCoreWeapons
                .isClub(stack)) {
            return 2;
        }

        if (RaceCoreWeapons
                .isFlail(stack)) {
            return 2;
        }

        if (path.endsWith("_helmet")) {
            return 5;
        }

        if (path.endsWith("_chestplate")) {
            return 8;
        }

        if (path.endsWith("_leggings")) {
            return 7;
        }

        if (path.endsWith("_boots")) {
            return 4;
        }

        if (path.endsWith("_pickaxe")
                || path.endsWith("_axe")) {
            return 3;
        }

        if (path.endsWith("_sword")
                || path.endsWith("_hoe")) {
            return 2;
        }

        if (path.endsWith("_shovel")) {
            return 1;
        }

        if (path.equals("shears")) {
            return 2;
        }

        return 0;
    }
}
