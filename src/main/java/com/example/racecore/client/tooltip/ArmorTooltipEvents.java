package com.example.racecore.client.tooltip;

import com.example.racecore.RaceCore;
import com.example.racecore.armor.ArmorDefinition;
import com.example.racecore.armor.ArmorManager;
import com.example.racecore.armor.ArmorWeight;
import com.example.racecore.armor.ProtectionManager;
import com.example.racecore.movement.MovementRegistry;
import net.minecraft.ChatFormatting;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.equipment.Equippable;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.ItemTooltipEvent;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.List;
import java.util.Locale;

/**
 * Replaces misleading vanilla armor/toughness tooltip numbers on mapped
 * RaceCore armor and shows the values RaceCore actually uses.
 */
@EventBusSubscriber(
        modid = RaceCore.MODID,
        value = Dist.CLIENT
)
public final class ArmorTooltipEvents {
    private static final DecimalFormat NUMBER_FORMAT;

    static {
        DecimalFormatSymbols symbols =
                DecimalFormatSymbols.getInstance(
                        Locale.ROOT
                );

        NUMBER_FORMAT =
                new DecimalFormat(
                        "0.###",
                        symbols
                );
    }

    private ArmorTooltipEvents() {
    }

    @SubscribeEvent
    public static void onItemTooltip(
            ItemTooltipEvent event
    ) {
        ItemStack stack =
                event.getItemStack();

        ArmorDefinition definition =
                ArmorManager.getDefinition(stack);

        if (definition == null) {
            return;
        }

        List<Component> tooltip =
                event.getToolTip();

        removeVanillaArmorLines(tooltip);

        double movementPenalty =
                getPieceMovementPenalty(
                        stack,
                        definition.weight()
                );

        tooltip.add(Component.empty());

        tooltip.add(
                Component.literal("RaceCore Armor: ")
                        .withStyle(ChatFormatting.GRAY)
                        .append(
                                Component.literal(
                                                NUMBER_FORMAT.format(
                                                        definition.armor()
                                                )
                                        )
                                        .withStyle(
                                                ChatFormatting.BLUE
                                        )
                        )
        );

        tooltip.add(
                Component.literal("Weight: ")
                        .withStyle(ChatFormatting.GRAY)
                        .append(
                                Component.literal(
                                                formatWeight(
                                                        definition.weight()
                                                )
                                        )
                                        .withStyle(
                                                weightColor(
                                                        definition.weight()
                                                )
                                        )
                        )
        );

        tooltip.add(
                Component.literal("Movement Speed: ")
                        .withStyle(ChatFormatting.GRAY)
                        .append(
                                Component.literal(
                                                "-"
                                                        + NUMBER_FORMAT.format(
                                                        movementPenalty
                                                                * 100.0
                                                )
                                                        + "%"
                                        )
                                        .withStyle(
                                                ChatFormatting.RED
                                        )
                        )
        );

        int protectionCapacity =
                ProtectionManager.getCapacityForPiece(
                        event.getEntity(),
                        stack
                );

        tooltip.add(
                Component.literal("Protection Cap: ")
                        .withStyle(ChatFormatting.GRAY)
                        .append(
                                Component.literal(
                                                Integer.toString(
                                                        protectionCapacity
                                                )
                                        )
                                        .withStyle(
                                                ChatFormatting.AQUA
                                        )
                        )
        );

        if (definition.protectionCapacityBonus() > 0) {
            tooltip.add(
                    Component.literal("Material Enchanting Bonus: ")
                            .withStyle(ChatFormatting.GRAY)
                            .append(
                                    Component.literal(
                                                    "+"
                                                            + definition.protectionCapacityBonus()
                                            )
                                            .withStyle(
                                                    ChatFormatting.LIGHT_PURPLE
                                            )
                            )
            );
        }
    }

    private static void removeVanillaArmorLines(
            List<Component> tooltip
    ) {
        String vanillaArmorName =
                Component.translatable(
                        Attributes.ARMOR
                                .value()
                                .getDescriptionId()
                ).getString();

        String vanillaToughnessName =
                Component.translatable(
                        Attributes.ARMOR_TOUGHNESS
                                .value()
                                .getDescriptionId()
                ).getString();

        tooltip.removeIf(component -> {
            String text =
                    component.getString();

            return text.contains(vanillaArmorName)
                    || text.contains(
                            vanillaToughnessName
                    );
        });
    }

    private static double getPieceMovementPenalty(
            ItemStack stack,
            ArmorWeight weight
    ) {
        Equippable equippable =
                stack.get(DataComponents.EQUIPPABLE);

        if (equippable == null) {
            return 0.0;
        }

        double fullSetPenalty =
                switch (weight) {
                    case LIGHT ->
                            MovementRegistry.settings()
                                    .lightArmorFullSetPenalty();

                    case MEDIUM ->
                            MovementRegistry.settings()
                                    .mediumArmorFullSetPenalty();

                    case HEAVY ->
                            MovementRegistry.settings()
                                    .heavyArmorFullSetPenalty();
                };

        return fullSetPenalty
                * slotShare(
                        equippable.slot()
                );
    }

    private static double slotShare(
            EquipmentSlot slot
    ) {
        return switch (slot) {
            case HEAD -> 0.15;
            case CHEST -> 0.40;
            case LEGS -> 0.30;
            case FEET -> 0.15;
            default -> 0.0;
        };
    }

    private static String formatWeight(
            ArmorWeight weight
    ) {
        return switch (weight) {
            case LIGHT -> "Light";
            case MEDIUM -> "Medium";
            case HEAVY -> "Heavy";
        };
    }

    private static ChatFormatting weightColor(
            ArmorWeight weight
    ) {
        return switch (weight) {
            case LIGHT -> ChatFormatting.GREEN;
            case MEDIUM -> ChatFormatting.YELLOW;
            case HEAVY -> ChatFormatting.RED;
        };
    }
}
