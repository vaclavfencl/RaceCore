package com.example.racecore.client.tooltip;

import com.example.racecore.RaceCore;
import com.example.racecore.armor.ArmorDefinition;
import com.example.racecore.armor.ArmorManager;
import com.example.racecore.armor.ArmorWeight;
import com.example.racecore.armor.ProtectionManager;
import com.example.racecore.durability.BrokenItemManager;
import com.example.racecore.enchanting.UtilityEnchantingManager;
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

@EventBusSubscriber(
        modid = RaceCore.MODID,
        value = Dist.CLIENT
)
public final class ArmorTooltipEvents {
    private static final DecimalFormat NUMBER_FORMAT;

    static {
        NUMBER_FORMAT =
                new DecimalFormat(
                        "0.###",
                        DecimalFormatSymbols
                                .getInstance(
                                        Locale.ROOT
                                )
                );
    }

    private ArmorTooltipEvents() {}

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
                Component.translatable(
                                "tooltip.racecore.armor.value",
                                Component.literal(
                                                NUMBER_FORMAT.format(
                                                        ArmorManager
                                                                .getArmorValue(
                                                                        stack
                                                                )
                                                )
                                        )
                                        .withStyle(
                                                BrokenItemManager
                                                        .isBroken(stack)
                                                        ? ChatFormatting.RED
                                                        : ChatFormatting.BLUE
                                        )
                        )
                        .withStyle(
                                ChatFormatting.GRAY
                        )
        );

        tooltip.add(
                Component.translatable(
                                "tooltip.racecore.armor.weight",
                                weightName(
                                        definition.weight()
                                ).copy()
                                        .withStyle(
                                                weightColor(
                                                        definition.weight()
                                                )
                                        )
                        )
                        .withStyle(
                                ChatFormatting.GRAY
                        )
        );

        String movementText =
                movementPenalty <= 0.000001
                        ? "0%"
                        : "-"
                        + NUMBER_FORMAT.format(
                                movementPenalty
                                        * 100.0
                        )
                        + "%";

        tooltip.add(
                Component.translatable(
                                "tooltip.racecore.armor.movement",
                                Component.literal(
                                                movementText
                                        )
                                        .withStyle(
                                                movementPenalty <= 0.000001
                                                        ? ChatFormatting.GREEN
                                                        : ChatFormatting.RED
                                        )
                        )
                        .withStyle(
                                ChatFormatting.GRAY
                        )
        );

        int protectionCapacity =
                ProtectionManager
                        .getCapacityForPiece(
                                event.getEntity(),
                                stack
                        );

        tooltip.add(
                Component.translatable(
                                "tooltip.racecore.armor.protection_cap",
                                Component.literal(
                                                Integer.toString(
                                                        protectionCapacity
                                                )
                                        )
                                        .withStyle(
                                                ChatFormatting.AQUA
                                        )
                        )
                        .withStyle(
                                ChatFormatting.GRAY
                        )
        );

        int utilityCapacity =
                UtilityEnchantingManager
                        .getCapacity(
                                event.getEntity(),
                                stack
                        );

        tooltip.add(
                Component.translatable(
                                "tooltip.racecore.armor.utility_cap",
                                Component.literal(
                                                Integer.toString(
                                                        utilityCapacity
                                                )
                                        )
                                        .withStyle(
                                                ChatFormatting.AQUA
                                        )
                        )
                        .withStyle(
                                ChatFormatting.GRAY
                        )
        );
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

        tooltip.removeIf(
                component -> {
                    String text =
                            component.getString();

                    return text.contains(
                            vanillaArmorName
                    )
                            || text.contains(
                            vanillaToughnessName
                    );
                }
        );
    }

    private static double getPieceMovementPenalty(
            ItemStack stack,
            ArmorWeight weight
    ) {
        Equippable equippable =
                stack.get(
                        DataComponents.EQUIPPABLE
                );

        if (equippable == null) {
            return 0.0;
        }

        double fullSetPenalty =
                switch (weight) {
                    case LIGHT ->
                            MovementRegistry
                                    .settings()
                                    .lightArmorFullSetPenalty();

                    case MEDIUM ->
                            MovementRegistry
                                    .settings()
                                    .mediumArmorFullSetPenalty();

                    case HEAVY ->
                            MovementRegistry
                                    .settings()
                                    .heavyArmorFullSetPenalty();

                    case ULTRA_HEAVY ->
                            MovementRegistry
                                    .settings()
                                    .ultraHeavyArmorFullSetPenalty();
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

    private static Component weightName(
            ArmorWeight weight
    ) {
        return Component.translatable(
                switch (weight) {
                    case LIGHT ->
                            "tooltip.racecore.weight.light";
                    case MEDIUM ->
                            "tooltip.racecore.weight.medium";
                    case HEAVY ->
                            "tooltip.racecore.weight.heavy";
                    case ULTRA_HEAVY ->
                            "tooltip.racecore.weight.ultra_heavy";
                }
        );
    }

    private static ChatFormatting weightColor(
            ArmorWeight weight
    ) {
        return switch (weight) {
            case LIGHT ->
                    ChatFormatting.GREEN;

            case MEDIUM ->
                    ChatFormatting.YELLOW;

            case HEAVY ->
                    ChatFormatting.RED;

            case ULTRA_HEAVY ->
                    ChatFormatting.DARK_RED;
        };
    }
}
