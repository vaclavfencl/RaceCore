package com.example.racecore.client.screen;

import com.example.racecore.crafting.CraftedEquipmentManager;
import com.example.racecore.network.raceselection.SelectRacePayload;
import com.example.racecore.race.RaceDefinition;
import com.example.racecore.race.RaceRegistry;
import com.example.racecore.race.RaceText;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Compact race-selection screen.
 *
 * Neutral/basic body data and crafted-equipment specialties are shown under
 * Characteristics. Actual buffs are grouped under Positives and actual
 * drawbacks under Negatives.
 */
public final class RaceSelectionScreen extends Screen {
    private static final List<String> RACE_ORDER = List.of(
            "human",
            "elf",
            "dwarf",
            "dragonborn",
            "hobbit",
            "troll",
            "gargoyle",
            "giant",
            "goblin",
            "tiefling"
    );

    private static final DecimalFormat NUMBER =
            new DecimalFormat(
                    "0.##",
                    DecimalFormatSymbols.getInstance(Locale.ROOT)
            );

    private static final int COLOR_TITLE = 0xFFFFD966;
    private static final int COLOR_HEADING = 0xFFB8B8B8;
    private static final int COLOR_NORMAL = 0xFFFFFFFF;
    private static final int COLOR_POSITIVE = 0xFF7FE27F;
    private static final int COLOR_NEGATIVE = 0xFFFF7777;
    private static final int COLOR_CHARACTERISTIC = 0xFFFFCC66;
    private static final int COLOR_MUTED = 0xFF9A9A9A;

    private static final double IMPORTANT_PHYSICAL_DIFFERENCE = 0.30;

    private String selectedRaceId = "human";

    private int panelX;
    private int panelY;
    private int panelWidth;
    private int panelHeight;
    private int listWidth;

    public RaceSelectionScreen() {
        super(
                Component.translatable(
                        "screen.racecore.race_selection.title"
                )
        );
    }

    @Override
    protected void init() {
        super.init();

        /*
         * Never force a minimum larger than the actual GUI. The old screen
         * forced a 300px panel, which could push Confirm below small/scaled
         * game windows.
         */
        panelWidth =
                Math.max(
                        1,
                        Math.min(
                                640,
                                width - 10
                        )
                );

        panelHeight =
                Math.max(
                        1,
                        Math.min(
                                330,
                                height - 10
                        )
                );

        panelX =
                (width - panelWidth) / 2;

        panelY =
                (height - panelHeight) / 2;

        listWidth =
                Math.min(
                        190,
                        Math.max(
                                120,
                                panelWidth * 32 / 100
                        )
                );

        int raceAreaX =
                panelX + 12;

        int raceAreaY =
                panelY + 42;

        int gap = 5;

        int buttonWidth =
                Math.max(
                        48,
                        (listWidth - gap) / 2
                );

        int buttonHeight = 18;

        for (int i = 0; i < RACE_ORDER.size(); i++) {
            String raceId =
                    RACE_ORDER.get(i);

            int column = i % 2;
            int row = i / 2;

            int x =
                    raceAreaX
                            + column
                            * (buttonWidth + gap);

            int y =
                    raceAreaY
                            + row
                            * 21;

            addRenderableWidget(
                    Button.builder(
                                    RaceText.name(raceId),
                                    button ->
                                            selectedRaceId =
                                                    raceId
                            )
                            .bounds(
                                    x,
                                    y,
                                    buttonWidth,
                                    buttonHeight
                            )
                            .build()
            );
        }

        int confirmWidth =
                Math.min(
                        180,
                        Math.max(
                                1,
                                panelWidth - 28
                        )
                );

        addRenderableWidget(
                Button.builder(
                                Component.translatable(
                                        "screen.racecore.race_selection.confirm"
                                ),
                                button -> confirmSelection()
                        )
                        .bounds(
                                panelX
                                        + panelWidth
                                        - confirmWidth
                                        - 12,
                                panelY
                                        + panelHeight
                                        - 27,
                                confirmWidth,
                                18
                        )
                        .build()
        );
    }

    private void confirmSelection() {
        RaceDefinition selected =
                RaceRegistry.get(selectedRaceId);

        if (selected == null) {
            return;
        }

        ClientPacketDistributor.sendToServer(
                new SelectRacePayload(
                        selectedRaceId
                )
        );

        minecraft.gui.setScreen(null);
    }

    @Override
    public void extractBackground(
            GuiGraphicsExtractor graphics,
            int mouseX,
            int mouseY,
            float partialTick
    ) {
        extractTransparentBackground(graphics);

        graphics.fill(
                panelX - 1,
                panelY - 1,
                panelX + panelWidth + 1,
                panelY + panelHeight + 1,
                0xFF707070
        );

        graphics.fill(
                panelX,
                panelY,
                panelX + panelWidth,
                panelY + panelHeight,
                0xF0181818
        );

        graphics.fill(
                panelX,
                panelY,
                panelX + panelWidth,
                panelY + 30,
                0xF0282828
        );

        int dividerX =
                panelX + listWidth + 22;

        graphics.fill(
                dividerX,
                panelY + 36,
                dividerX + 1,
                panelY + panelHeight - 35,
                0xFF505050
        );
    }

    @Override
    public void extractRenderState(
            GuiGraphicsExtractor graphics,
            int mouseX,
            int mouseY,
            float partialTick
    ) {
        super.extractRenderState(
                graphics,
                mouseX,
                mouseY,
                partialTick
        );

        graphics.centeredText(
                font,
                title,
                panelX + panelWidth / 2,
                panelY + 10,
                COLOR_NORMAL
        );

        graphics.text(
                font,
                Component.translatable(
                        "screen.racecore.race_selection.choose"
                ),
                panelX + 12,
                panelY + 31,
                COLOR_MUTED,
                false
        );

        drawSelectedRace(graphics);
    }

    private void drawSelectedRace(
            GuiGraphicsExtractor graphics
    ) {
        RaceDefinition race =
                RaceRegistry.get(selectedRaceId);

        if (race == null) {
            return;
        }

        RaceDefinition human =
                RaceRegistry.get("human");

        int x =
                panelX + listWidth + 34;

        int y =
                panelY + 39;

        graphics.text(
                font,
                RaceText.name(selectedRaceId),
                x,
                y,
                COLOR_TITLE,
                true
        );

        y += 14;

        y = drawSection(
                graphics,
                x,
                y,
                Component.translatable(
                        "screen.racecore.race_selection.characteristics"
                ),
                buildCharacteristicLines(
                        selectedRaceId,
                        race,
                        human
                ),
                COLOR_CHARACTERISTIC
        );

        y = drawSection(
                graphics,
                x,
                y,
                Component.translatable(
                        "screen.racecore.race_selection.positives"
                ),
                buildPositiveLines(
                        race,
                        human
                ),
                COLOR_POSITIVE
        );

        drawSection(
                graphics,
                x,
                y,
                Component.translatable(
                        "screen.racecore.race_selection.negatives"
                ),
                buildNegativeLines(
                        race,
                        human
                ),
                COLOR_NEGATIVE
        );

        graphics.text(
                font,
                Component.translatable(
                        "screen.racecore.race_selection.warning"
                ),
                x,
                panelY + panelHeight - 42,
                0xFFFFAA55,
                false
        );
    }

    private int drawSection(
            GuiGraphicsExtractor graphics,
            int x,
            int y,
            Component heading,
            List<Component> lines,
            int lineColor
    ) {
        graphics.text(
                font,
                heading,
                x,
                y,
                COLOR_HEADING,
                true
        );

        y += 11;

        if (lines.isEmpty()) {
            graphics.text(
                    font,
                    Component.translatable(
                            "screen.racecore.race_selection.none"
                    ),
                    x,
                    y,
                    COLOR_MUTED,
                    false
            );

            return y + 11;
        }

        for (Component line : lines) {
            graphics.text(
                    font,
                    Component.literal("- ")
                            .append(line),
                    x,
                    y,
                    lineColor,
                    false
            );

            y += 9;
        }

        return y + 3;
    }

    private List<Component> buildCharacteristicLines(
            String raceId,
            RaceDefinition race,
            RaceDefinition human
    ) {
        List<Component> lines =
                new ArrayList<>();

        lines.add(
                Component.translatable(
                        "screen.racecore.race_selection.characteristic.health",
                        NUMBER.format(
                                race.maxHealth()
                        )
                )
        );

        lines.add(
                Component.translatable(
                        "screen.racecore.race_selection.characteristic.size",
                        NUMBER.format(
                                race.scale() * 100.0
                        ),
                        NUMBER.format(
                                race.scale()
                        )
                )
        );

        if (human != null
                && isImportantDifference(
                        race.stepHeight(),
                        human.stepHeight()
                )) {

            lines.add(
                    Component.translatable(
                            "screen.racecore.race_selection.characteristic.step",
                            NUMBER.format(
                                    race.stepHeight()
                            )
                    )
            );
        }

        if (human != null
                && isImportantDifference(
                        race.jumpHeight(),
                        human.jumpHeight()
                )) {

            lines.add(
                    Component.translatable(
                            "screen.racecore.race_selection.characteristic.jump",
                            NUMBER.format(
                                    race.jumpHeight()
                            )
                    )
            );
        }

        addCraftingCharacteristics(
                raceId,
                lines
        );

        return lines;
    }

    private void addCraftingCharacteristics(
            String raceId,
            List<Component> lines
    ) {
        switch (raceId) {
            case "human" ->
                    lines.add(
                            Component.translatable(
                                    "screen.racecore.race_selection.characteristic.human_longsword",
                                    signedPercent(
                                            CraftedEquipmentManager
                                                    .HUMAN_LONGSWORD_DAMAGE_BONUS
                                    ),
                                    signedPercent(
                                            CraftedEquipmentManager
                                                    .HUMAN_LONGSWORD_DURABILITY_BONUS
                                    )
                            )
                    );

            case "dwarf" -> {
                lines.add(
                        Component.translatable(
                                "screen.racecore.race_selection.characteristic.dwarf_tools",
                                signedPercent(
                                        CraftedEquipmentManager
                                                .DWARF_TOOL_DURABILITY_BONUS
                                )
                        )
                );

                lines.add(
                        Component.translatable(
                                "screen.racecore.race_selection.characteristic.dwarf_armor",
                                NUMBER.format(
                                        CraftedEquipmentManager
                                                .DWARF_MEDIUM_HEAVY_ARMOR_BONUS
                                )
                        )
                );

                lines.add(
                        Component.translatable(
                                "screen.racecore.race_selection.characteristic.dwarf_mace",
                                signedPercent(
                                        CraftedEquipmentManager
                                                .DWARF_MACE_ATTACK_SPEED_BONUS
                                )
                        )
                );
            }

            case "elf" ->
                    lines.add(
                            Component.translatable(
                                    "screen.racecore.race_selection.characteristic.elf_weapons",
                                    signedPercent(
                                            CraftedEquipmentManager
                                                    .ELF_WEAPON_DURABILITY_BONUS
                                    )
                            )
                    );

            case "giant" ->
                    lines.add(
                            Component.translatable(
                                    "screen.racecore.race_selection.characteristic.giant_weapons",
                                    signedPercent(
                                            CraftedEquipmentManager
                                                    .GIANT_CLUB_FLAIL_DAMAGE_BONUS
                                    ),
                                    signedPercent(
                                            -CraftedEquipmentManager
                                                    .GIANT_CLUB_FLAIL_DURABILITY_PENALTY
                                    )
                            )
                    );

            case "troll" ->
                    lines.add(
                            Component.translatable(
                                    "screen.racecore.race_selection.characteristic.troll_axes",
                                    signedPercent(
                                            CraftedEquipmentManager
                                                    .TROLL_AXE_ATTACK_SPEED_BONUS
                                    )
                            )
                    );

            case "goblin" ->
                    lines.add(
                            Component.translatable(
                                    "screen.racecore.race_selection.characteristic.goblin_daggers",
                                    signedPercent(
                                            CraftedEquipmentManager
                                                    .GOBLIN_DAGGER_ATTACK_SPEED_BONUS
                                    ),
                                    signedPercent(
                                            CraftedEquipmentManager
                                                    .GOBLIN_DAGGER_DURABILITY_BONUS
                                    )
                            )
                    );

            case "dragonborn" ->
                    lines.add(
                            Component.translatable(
                                    "screen.racecore.race_selection.characteristic.dragonborn_armor",
                                    signedPercent(
                                            CraftedEquipmentManager
                                                    .DRAGONBORN_ARMOR_DURABILITY_BONUS
                                    )
                            )
                    );

            case "tiefling" -> {
                lines.add(
                        Component.translatable(
                                "screen.racecore.race_selection.characteristic.tiefling_glaive",
                                signedPercent(
                                        CraftedEquipmentManager
                                                .TIEFLING_GLAIVE_DAMAGE_BONUS
                                ),
                                signedPercent(
                                        CraftedEquipmentManager
                                                .TIEFLING_GLAIVE_ATTACK_SPEED_BONUS
                                )
                        )
                );

                lines.add(
                        Component.translatable(
                                "screen.racecore.race_selection.characteristic.tiefling_crossbow",
                                signedPercent(
                                        CraftedEquipmentManager
                                                .TIEFLING_CROSSBOW_DURABILITY_BONUS
                                )
                        )
                );
            }

            default -> {
            }
        }
    }

    private List<Component> buildPositiveLines(
            RaceDefinition race,
            RaceDefinition human
    ) {
        List<Component> lines =
                new ArrayList<>();

        addPositiveMultiplier(
                lines,
                "screen.racecore.race_selection.stat.movement",
                race.movementSpeedMultiplier()
        );

        addPositiveMultiplier(
                lines,
                "screen.racecore.race_selection.stat.reach",
                race.reachMultiplier()
        );

        if (race.enchantmentCapacity() > 4) {
            lines.add(
                    Component.translatable(
                            "screen.racecore.race_selection.stat.enchant_capacity",
                            race.enchantmentCapacity()
                    )
            );
        }

        if (race.xpBonus() > 0.0) {
            lines.add(
                    Component.translatable(
                            "screen.racecore.race_selection.stat.xp",
                            signedPercent(
                                    race.xpBonus()
                            )
                    )
            );
        }

        if (race.lootLuck() > 0.0) {
            lines.add(
                    Component.translatable(
                            "screen.racecore.race_selection.stat.loot_luck",
                            NUMBER.format(
                                    race.lootLuck()
                            )
                    )
            );
        }

        addPositiveMultiplier(
                lines,
                "screen.racecore.race_selection.stat.mining",
                race.miningSpeedMultiplier()
        );

        if (race.fortuneBonus() > 0) {
            lines.add(
                    Component.translatable(
                            "screen.racecore.race_selection.stat.fortune",
                            race.fortuneBonus()
                    )
            );
        }

        if (race.cropFortuneBonus() > 0) {
            lines.add(
                    Component.translatable(
                            "screen.racecore.race_selection.stat.crop_fortune",
                            race.cropFortuneBonus()
                    )
            );
        }

        if (race.knockbackResistanceBonus() > 0.0) {
            lines.add(
                    Component.translatable(
                            "screen.racecore.race_selection.stat.knockback",
                            signedPercent(
                                    race.knockbackResistanceBonus()
                            )
                    )
            );
        }

        if (race.heavyArmorPenaltyMultiplier() < 1.0) {
            lines.add(
                    Component.translatable(
                            "screen.racecore.race_selection.stat.heavy_slowdown",
                            NUMBER.format(
                                    (1.0
                                            - race.heavyArmorPenaltyMultiplier())
                                            * 100.0
                            )
                    )
            );
        }

        addPositiveMultiplier(
                lines,
                "screen.racecore.race_selection.stat.outgoing_damage",
                race.outgoingDamageMultiplier()
        );

        addLowerIsBetterPositive(
                lines,
                "screen.racecore.race_selection.stat.incoming_damage",
                race.incomingDamageMultiplier()
        );

        addLowerIsBetterPositive(
                lines,
                "screen.racecore.race_selection.stat.non_nether_incoming",
                race.nonNetherIncomingDamageMultiplier()
        );

        addPositiveMultiplier(
                lines,
                "screen.racecore.race_selection.stat.dragon_damage",
                race.dragonDamageMultiplier()
        );

        addLowerIsBetterPositive(
                lines,
                "screen.racecore.race_selection.stat.poison_damage",
                race.poisonDamageMultiplier()
        );

        addLowerIsBetterPositive(
                lines,
                "screen.racecore.race_selection.stat.fire_damage",
                race.fireDamageMultiplier()
        );

        addLowerIsBetterPositive(
                lines,
                "screen.racecore.race_selection.stat.firework_damage",
                race.fireworkDamageMultiplier()
        );

        if (race.passiveRegenerationPerSecond() > 0.0) {
            lines.add(
                    Component.translatable(
                            "screen.racecore.race_selection.stat.regeneration",
                            NUMBER.format(
                                    race.passiveRegenerationPerSecond()
                            )
                    )
            );
        }

        if (race.permanentNightVision()) {
            lines.add(
                    Component.translatable(
                            "screen.racecore.race_selection.stat.night_vision"
                    )
            );
        }

        if (race.canLoadFireworkRockets()) {
            lines.add(
                    Component.translatable(
                            "screen.racecore.race_selection.stat.firework_crossbow"
                    )
            );
        }

        if (race.canEatWhenFull()) {
            lines.add(
                    Component.translatable(
                            "screen.racecore.race_selection.stat.eat_when_full"
                    )
            );
        }

        if (race.foodHungerMultiplier() > 1.0) {
            lines.add(
                    Component.translatable(
                            "screen.racecore.race_selection.stat.food_hunger",
                            NUMBER.format(
                                    race.foodHungerMultiplier()
                            )
                    )
            );
        }

        if (race.foodHealing() > 0.0) {
            lines.add(
                    Component.translatable(
                            "screen.racecore.race_selection.stat.food_heal",
                            NUMBER.format(
                                    race.foodHealing()
                            )
                    )
            );
        }

        addPositiveMultiplier(
                lines,
                "screen.racecore.race_selection.stat.dark_movement",
                race.darkMovementMultiplier()
        );

        addPositiveMultiplier(
                lines,
                "screen.racecore.race_selection.stat.dark_damage",
                race.darkDamageMultiplier()
        );

        addPositiveMultiplier(
                lines,
                "screen.racecore.race_selection.stat.bright_damage",
                race.brightDamageMultiplier()
        );

        addPositiveMultiplier(
                lines,
                "screen.racecore.race_selection.stat.nether_movement",
                race.netherMovementMultiplier()
        );

        addPositiveMultiplier(
                lines,
                "screen.racecore.race_selection.stat.nether_damage",
                race.netherDamageMultiplier()
        );

        if (race.netherMobsNeutral()) {
            lines.add(
                    Component.translatable(
                            "screen.racecore.race_selection.stat.nether_neutral"
                    )
            );
        }

        if (race.bowNoFalloff()) {
            lines.add(
                    Component.translatable(
                            "screen.racecore.race_selection.stat.bow_no_drop"
                    )
            );
        }

        if (race.crossbowNoFalloff()) {
            lines.add(
                    Component.translatable(
                            "screen.racecore.race_selection.stat.crossbow_no_drop"
                    )
            );
        }

        addPositiveMultiplier(
                lines,
                "screen.racecore.race_selection.stat.forest_movement",
                race.forestMovementMultiplier()
        );

        if (race.leafClimbing()) {
            lines.add(
                    Component.translatable(
                            "screen.racecore.race_selection.stat.leaf_climbing"
                    )
            );
        }

        if (human != null
                && race.safeFallDistance()
                > human.safeFallDistance()
                + 0.000001) {

            lines.add(
                    Component.translatable(
                            "screen.racecore.race_selection.stat.safe_fall",
                            NUMBER.format(
                                    race.safeFallDistance()
                            )
                    )
            );
        }

        if (race.chargedJumpEnabled()) {
            lines.add(
                    Component.translatable(
                            "screen.racecore.race_selection.stat.charged_jump",
                            NUMBER.format(
                                    race.chargedJumpMaxHeight()
                            )
                    )
            );
        }

        if (race.glideEnabled()) {
            lines.add(
                    Component.translatable(
                            "screen.racecore.race_selection.stat.glide"
                    )
            );
        }

        return lines;
    }

    private List<Component> buildNegativeLines(
            RaceDefinition race,
            RaceDefinition human
    ) {
        List<Component> lines =
                new ArrayList<>();

        addNegativeMultiplier(
                lines,
                "screen.racecore.race_selection.stat.movement",
                race.movementSpeedMultiplier()
        );

        addNegativeMultiplier(
                lines,
                "screen.racecore.race_selection.stat.reach",
                race.reachMultiplier()
        );

        if (race.enchantmentCapacity() < 4) {
            lines.add(
                    Component.translatable(
                            "screen.racecore.race_selection.stat.enchant_capacity",
                            race.enchantmentCapacity()
                    )
            );
        }

        if (race.xpBonus() < 0.0) {
            lines.add(
                    Component.translatable(
                            "screen.racecore.race_selection.stat.xp",
                            signedPercent(
                                    race.xpBonus()
                            )
                    )
            );
        }

        if (race.lootLuck() < 0.0) {
            lines.add(
                    Component.translatable(
                            "screen.racecore.race_selection.stat.loot_luck",
                            NUMBER.format(
                                    race.lootLuck()
                            )
                    )
            );
        }

        addNegativeMultiplier(
                lines,
                "screen.racecore.race_selection.stat.mining",
                race.miningSpeedMultiplier()
        );

        if (race.fortuneBonus() < 0) {
            lines.add(
                    Component.translatable(
                            "screen.racecore.race_selection.stat.fortune",
                            race.fortuneBonus()
                    )
            );
        }

        if (race.cropFortuneBonus() < 0) {
            lines.add(
                    Component.translatable(
                            "screen.racecore.race_selection.stat.crop_fortune",
                            race.cropFortuneBonus()
                    )
            );
        }

        if (race.knockbackResistanceBonus() < 0.0) {
            lines.add(
                    Component.translatable(
                            "screen.racecore.race_selection.stat.knockback",
                            signedPercent(
                                    race.knockbackResistanceBonus()
                            )
                    )
            );
        }

        if (race.heavyArmorPenaltyMultiplier() > 1.0) {
            lines.add(
                    Component.translatable(
                            "screen.racecore.race_selection.stat.heavy_slowdown_extra",
                            signedPercentFromMultiplier(
                                    race.heavyArmorPenaltyMultiplier()
                            )
                    )
            );
        }

        addNegativeMultiplier(
                lines,
                "screen.racecore.race_selection.stat.outgoing_damage",
                race.outgoingDamageMultiplier()
        );

        addLowerIsBetterNegative(
                lines,
                "screen.racecore.race_selection.stat.incoming_damage",
                race.incomingDamageMultiplier()
        );

        addLowerIsBetterNegative(
                lines,
                "screen.racecore.race_selection.stat.non_nether_incoming",
                race.nonNetherIncomingDamageMultiplier()
        );

        addNegativeMultiplier(
                lines,
                "screen.racecore.race_selection.stat.dragon_damage",
                race.dragonDamageMultiplier()
        );

        addLowerIsBetterNegative(
                lines,
                "screen.racecore.race_selection.stat.poison_damage",
                race.poisonDamageMultiplier()
        );

        addLowerIsBetterNegative(
                lines,
                "screen.racecore.race_selection.stat.fire_damage",
                race.fireDamageMultiplier()
        );

        addLowerIsBetterNegative(
                lines,
                "screen.racecore.race_selection.stat.firework_damage",
                race.fireworkDamageMultiplier()
        );

        if (race.passiveRegenerationPerSecond() < 0.0) {
            lines.add(
                    Component.translatable(
                            "screen.racecore.race_selection.stat.regeneration",
                            NUMBER.format(
                                    race.passiveRegenerationPerSecond()
                            )
                    )
            );
        }

        if (race.foodHungerMultiplier() < 1.0) {
            lines.add(
                    Component.translatable(
                            "screen.racecore.race_selection.stat.food_hunger",
                            NUMBER.format(
                                    race.foodHungerMultiplier()
                            )
                    )
            );
        }

        if (!race.canWearBoots()) {
            lines.add(
                    Component.translatable(
                            "screen.racecore.race_selection.stat.no_boots"
                    )
            );
        }

        if (!race.canWearLightArmor()) {
            lines.add(
                    Component.translatable(
                            "screen.racecore.race_selection.stat.no_light_armor"
                    )
            );
        }

        if (!race.canWearMediumArmor()) {
            lines.add(
                    Component.translatable(
                            "screen.racecore.race_selection.stat.no_medium_armor"
                    )
            );
        }

        if (!race.canWearHeavyArmor()) {
            lines.add(
                    Component.translatable(
                            "screen.racecore.race_selection.stat.no_heavy_armor"
                    )
            );
        }

        addNegativeMultiplier(
                lines,
                "screen.racecore.race_selection.stat.dark_movement",
                race.darkMovementMultiplier()
        );

        addNegativeMultiplier(
                lines,
                "screen.racecore.race_selection.stat.dark_damage",
                race.darkDamageMultiplier()
        );

        addNegativeMultiplier(
                lines,
                "screen.racecore.race_selection.stat.bright_damage",
                race.brightDamageMultiplier()
        );

        if (!race.rangedAllowedInBrightLight()) {
            lines.add(
                    Component.translatable(
                            "screen.racecore.race_selection.stat.no_bright_ranged"
                    )
            );
        }

        addNegativeMultiplier(
                lines,
                "screen.racecore.race_selection.stat.nether_movement",
                race.netherMovementMultiplier()
        );

        addNegativeMultiplier(
                lines,
                "screen.racecore.race_selection.stat.nether_damage",
                race.netherDamageMultiplier()
        );

        addNegativeMultiplier(
                lines,
                "screen.racecore.race_selection.stat.forest_movement",
                race.forestMovementMultiplier()
        );

        if (human != null
                && race.safeFallDistance()
                + 0.000001
                < human.safeFallDistance()) {

            lines.add(
                    Component.translatable(
                            "screen.racecore.race_selection.stat.safe_fall",
                            NUMBER.format(
                                    race.safeFallDistance()
                            )
                    )
            );
        }

        if (race.crackedIncomingDamageMultiplier() > 1.0
                && race.crackedDurationTicks() > 0) {

            lines.add(
                    Component.translatable(
                            "screen.racecore.race_selection.stat.cracked",
                            signedPercentFromMultiplier(
                                    race.crackedIncomingDamageMultiplier()
                            ),
                            NUMBER.format(
                                    race.crackedDurationTicks()
                                            / 20.0
                            )
                    )
            );
        }

        if (!race.rocketFlightBoostAllowed()) {
            lines.add(
                    Component.translatable(
                            "screen.racecore.race_selection.stat.no_rocket_boost"
                    )
            );
        }

        return lines;
    }

    private static void addPositiveMultiplier(
            List<Component> lines,
            String key,
            double multiplier
    ) {
        if (multiplier > 1.0 + 0.000001) {
            lines.add(
                    Component.translatable(
                            key,
                            signedPercentFromMultiplier(
                                    multiplier
                            )
                    )
            );
        }
    }

    private static void addNegativeMultiplier(
            List<Component> lines,
            String key,
            double multiplier
    ) {
        if (multiplier < 1.0 - 0.000001) {
            lines.add(
                    Component.translatable(
                            key,
                            signedPercentFromMultiplier(
                                    multiplier
                            )
                    )
            );
        }
    }

    private static void addLowerIsBetterPositive(
            List<Component> lines,
            String key,
            double multiplier
    ) {
        if (multiplier < 1.0 - 0.000001) {
            if (multiplier <= 0.000001
                    && key.equals(
                    "screen.racecore.race_selection.stat.poison_damage"
            )) {

                lines.add(
                        Component.translatable(
                                "screen.racecore.race_selection.stat.poison_immune"
                        )
                );

                return;
            }

            lines.add(
                    Component.translatable(
                            key,
                            signedPercentFromMultiplier(
                                    multiplier
                            )
                    )
            );
        }
    }

    private static void addLowerIsBetterNegative(
            List<Component> lines,
            String key,
            double multiplier
    ) {
        if (multiplier > 1.0 + 0.000001) {
            lines.add(
                    Component.translatable(
                            key,
                            signedPercentFromMultiplier(
                                    multiplier
                            )
                    )
            );
        }
    }

    private static boolean isImportantDifference(
            double value,
            double baseline
    ) {
        if (baseline == 0.0) {
            return Math.abs(value) > 0.000001;
        }

        return Math.abs(
                value / baseline - 1.0
        ) >= IMPORTANT_PHYSICAL_DIFFERENCE;
    }

    private static String signedPercentFromMultiplier(
            double multiplier
    ) {
        return signedPercent(
                multiplier - 1.0
        );
    }

    private static String signedPercent(
            double fraction
    ) {
        double percent =
                fraction * 100.0;

        String prefix =
                percent > 0.0
                        ? "+"
                        : "";

        return prefix
                + NUMBER.format(percent)
                + "%";
    }

    @Override
    public boolean shouldCloseOnEsc() {
        return false;
    }

    @Override
    public void onClose() {
        // First-time race selection remains mandatory.
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
