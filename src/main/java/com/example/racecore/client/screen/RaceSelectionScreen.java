package com.example.racecore.client.screen;

import com.example.racecore.network.raceselection.SelectRacePayload;
import com.example.racecore.race.RaceDefinition;
import com.example.racecore.race.RaceRegistry;
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
 * RaceCore race-selection screen.
 *
 * The details panel is generated from RaceDefinition rather than from a second
 * hardcoded race-description table. If a balance value changes in races.json,
 * the selection screen therefore updates with it automatically.
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
    private static final int COLOR_MIXED = 0xFFFFCC66;
    private static final int COLOR_MUTED = 0xFF9A9A9A;

    /*
     * Step/jump are internal physical values and are noisy when every tiny
     * difference is shown. Only display them if they differ by >=30% from
     * Human, which catches truly race-defining cases such as Giant, Dwarf,
     * Goblin and Dragonborn.
     */
    private static final double IMPORTANT_PHYSICAL_DIFFERENCE = 0.30;

    private String selectedRaceId = "human";

    private int panelX;
    private int panelY;
    private int panelWidth;
    private int panelHeight;
    private int listWidth;

    public RaceSelectionScreen() {
        super(Component.literal("Choose Your Race"));
    }

    @Override
    protected void init() {
        super.init();

        panelWidth =
                Math.min(680, Math.max(430, width - 24));

        panelHeight =
                Math.min(390, Math.max(300, height - 24));

        panelX =
                (width - panelWidth) / 2;

        panelY =
                (height - panelHeight) / 2;

        listWidth =
                Math.min(230, panelWidth * 38 / 100);

        int raceAreaX =
                panelX + 14;

        int raceAreaY =
                panelY + 46;

        int gap = 6;

        int buttonWidth =
                (listWidth - gap) / 2;

        int buttonHeight = 20;

        for (int i = 0; i < RACE_ORDER.size(); i++) {
            String raceId =
                    RACE_ORDER.get(i);

            RaceDefinition definition =
                    RaceRegistry.get(raceId);

            String label =
                    definition != null
                            ? definition.displayName()
                            : raceId;

            int column = i % 2;
            int row = i / 2;

            int x =
                    raceAreaX
                            + column
                            * (buttonWidth + gap);

            int y =
                    raceAreaY
                            + row
                            * 25;

            addRenderableWidget(
                    Button.builder(
                                    Component.literal(label),
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
                Math.min(210, panelWidth - 40);

        addRenderableWidget(
                Button.builder(
                                Component.literal(
                                        "Confirm Selected Race"
                                ),
                                button -> confirmSelection()
                        )
                        .bounds(
                                panelX
                                        + panelWidth
                                        - confirmWidth
                                        - 16,
                                panelY
                                        + panelHeight
                                        - 32,
                                confirmWidth,
                                20
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
                panelY + 32,
                0xF0282828
        );

        int dividerX =
                panelX + listWidth + 25;

        graphics.fill(
                dividerX,
                panelY + 40,
                dividerX + 1,
                panelY + panelHeight - 42,
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
                panelY + 11,
                COLOR_NORMAL
        );

        graphics.text(
                font,
                Component.literal("Choose a race"),
                panelX + 14,
                panelY + 34,
                0xFFAAAAAA,
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
                panelX + listWidth + 40;

        int y =
                panelY + 45;

        graphics.text(
                font,
                Component.literal(race.displayName()),
                x,
                y,
                COLOR_TITLE,
                true
        );

        y += 17;

        graphics.text(
                font,
                Component.literal("Physical"),
                x,
                y,
                COLOR_HEADING,
                true
        );

        y += 13;

        List<InfoLine> physical =
                buildPhysicalLines(
                        race,
                        human
                );

        for (InfoLine line : physical) {
            drawInfoLine(
                    graphics,
                    x,
                    y,
                    line
            );

            y += 12;
        }

        y += 5;

        graphics.text(
                font,
                Component.literal("Abilities"),
                x,
                y,
                COLOR_HEADING,
                true
        );

        y += 13;

        List<InfoLine> abilities =
                buildAbilityLines(race);

        if (abilities.isEmpty()) {
            drawInfoLine(
                    graphics,
                    x,
                    y,
                    new InfoLine(
                            "Generalist - no special downside",
                            COLOR_MUTED
                    )
            );

            y += 12;
        } else {
            for (InfoLine line : abilities) {
                drawInfoLine(
                        graphics,
                        x,
                        y,
                        line
                );

                y += 12;
            }
        }

        /*
         * Keep the permanent-choice warning pinned above the confirm area
         * instead of letting it drift based on the number of abilities.
         */
        int warningY =
                panelY + panelHeight - 57;

        graphics.text(
                font,
                Component.literal(
                        "Race changes will require a future respec item."
                ),
                x,
                warningY,
                0xFFFFAA55,
                false
        );
    }

    private List<InfoLine> buildPhysicalLines(
            RaceDefinition race,
            RaceDefinition human
    ) {
        List<InfoLine> lines =
                new ArrayList<>();

        lines.add(
                new InfoLine(
                        "HP: "
                                + NUMBER.format(
                                        race.maxHealth()
                                ),
                        COLOR_NORMAL
                )
        );

        lines.add(
                new InfoLine(
                        "Size: "
                                + NUMBER.format(
                                        race.scale() * 100.0
                                )
                                + "% of Human"
                                + " ("
                                + NUMBER.format(
                                        race.scale()
                                )
                                + "x)",
                        physicalComparisonColor(
                                race.scale(),
                                1.0
                        )
                )
        );

        if (!approximately(
                race.movementSpeedMultiplier(),
                1.0
        )) {
            lines.add(
                    new InfoLine(
                            "Movement: "
                                    + signedPercentFromMultiplier(
                                            race.movementSpeedMultiplier()
                                    ),
                            comparisonColor(
                                    race.movementSpeedMultiplier(),
                                    1.0
                            )
                    )
            );
        }

        if (!approximately(
                race.reachMultiplier(),
                1.0
        )) {
            lines.add(
                    new InfoLine(
                            "Reach: "
                                    + signedPercentFromMultiplier(
                                            race.reachMultiplier()
                                    ),
                            comparisonColor(
                                    race.reachMultiplier(),
                                    1.0
                            )
                    )
            );
        }

        if (race.enchantmentCapacity() != 4) {
            lines.add(
                    new InfoLine(
                            "Enchant capacity: "
                                    + race.enchantmentCapacity(),
                            race.enchantmentCapacity() > 4
                                    ? COLOR_POSITIVE
                                    : COLOR_NEGATIVE
                    )
            );
        }

        if (human != null
                && isImportantDifference(
                        race.stepHeight(),
                        human.stepHeight()
                )) {
            lines.add(
                    new InfoLine(
                            "Step height: "
                                    + NUMBER.format(
                                            race.stepHeight()
                                    ),
                            comparisonColor(
                                    race.stepHeight(),
                                    human.stepHeight()
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
                    new InfoLine(
                            "Jump height: "
                                    + NUMBER.format(
                                            race.jumpHeight()
                                    ),
                            comparisonColor(
                                    race.jumpHeight(),
                                    human.jumpHeight()
                            )
                    )
            );
        }

        return lines;
    }

    private List<InfoLine> buildAbilityLines(
            RaceDefinition race
    ) {
        List<InfoLine> lines =
                new ArrayList<>();

        if (race.xpBonus() != 0.0) {
            lines.add(
                    positive(
                            signedPercent(
                                    race.xpBonus()
                            )
                                    + " XP"
                    )
            );
        }

        if (!approximately(
                race.miningSpeedMultiplier(),
                1.0
        )) {
            lines.add(
                    comparison(
                            "Mining speed: "
                                    + signedPercentFromMultiplier(
                                            race.miningSpeedMultiplier()
                                    ),
                            race.miningSpeedMultiplier(),
                            1.0
                    )
            );
        }

        if (race.fortuneBonus() != 0) {
            lines.add(
                    positive(
                            "+"
                                    + race.fortuneBonus()
                                    + " effective Fortune"
                    )
            );
        }

        if (race.cropFortuneBonus() != 0) {
            lines.add(
                    positive(
                            "+"
                                    + race.cropFortuneBonus()
                                    + " Fortune on crops"
                    )
            );
        }

        if (race.knockbackResistanceBonus() != 0.0) {
            lines.add(
                    positive(
                            signedPercent(
                                    race.knockbackResistanceBonus()
                            )
                                    + " knockback resistance"
                    )
            );
        }

        if (race.heavyArmorPenaltyMultiplier() < 1.0) {
            lines.add(
                    positive(
                            "Heavy armor slowdown reduced by "
                                    + NUMBER.format(
                                            (1.0
                                                    - race.heavyArmorPenaltyMultiplier())
                                                    * 100.0
                                    )
                                    + "%"
                    )
            );
        }

        if (!approximately(
                race.outgoingDamageMultiplier(),
                1.0
        )) {
            lines.add(
                    comparison(
                            "Outgoing damage: "
                                    + signedPercentFromMultiplier(
                                            race.outgoingDamageMultiplier()
                                    ),
                            race.outgoingDamageMultiplier(),
                            1.0
                    )
            );
        }

        if (!approximately(
                race.incomingDamageMultiplier(),
                1.0
        )) {
            /*
             * Higher incoming damage is bad, so reverse the usual multiplier
             * color meaning.
             */
            lines.add(
                    new InfoLine(
                            "Incoming damage: "
                                    + signedPercentFromMultiplier(
                                            race.incomingDamageMultiplier()
                                    ),
                            race.incomingDamageMultiplier() > 1.0
                                    ? COLOR_NEGATIVE
                                    : COLOR_POSITIVE
                    )
            );
        }

        if (!approximately(
                race.nonNetherIncomingDamageMultiplier(),
                1.0
        )) {
            lines.add(
                    new InfoLine(
                            "Outside Nether: "
                                    + signedPercentFromMultiplier(
                                            race.nonNetherIncomingDamageMultiplier()
                                    )
                                    + " incoming damage",
                            race.nonNetherIncomingDamageMultiplier() > 1.0
                                    ? COLOR_NEGATIVE
                                    : COLOR_POSITIVE
                    )
            );
        }

        if (!approximately(
                race.dragonDamageMultiplier(),
                1.0
        )) {
            lines.add(
                    comparison(
                            NUMBER.format(
                                    race.dragonDamageMultiplier()
                            )
                                    + "x damage to dragons",
                            race.dragonDamageMultiplier(),
                            1.0
                    )
            );
        }

        if (race.poisonDamageMultiplier() == 0.0) {
            lines.add(
                    positive(
                            "Immune to poison damage"
                    )
            );
        } else if (!approximately(
                race.poisonDamageMultiplier(),
                1.0
        )) {
            lines.add(
                    positive(
                            "Poison damage taken: "
                                    + NUMBER.format(
                                            race.poisonDamageMultiplier()
                                                    * 100.0
                                    )
                                    + "%"
                    )
            );
        }

        if (!approximately(
                race.fireDamageMultiplier(),
                1.0
        )) {
            lines.add(
                    new InfoLine(
                            "Fire/lava damage taken: "
                                    + NUMBER.format(
                                            race.fireDamageMultiplier()
                                                    * 100.0
                                    )
                                    + "%",
                            race.fireDamageMultiplier() < 1.0
                                    ? COLOR_POSITIVE
                                    : COLOR_NEGATIVE
                    )
            );
        }

        if (!approximately(
                race.fireworkDamageMultiplier(),
                1.0
        )) {
            lines.add(
                    new InfoLine(
                            "Firework damage taken: "
                                    + NUMBER.format(
                                            race.fireworkDamageMultiplier()
                                    )
                                    + "x",
                            race.fireworkDamageMultiplier() > 1.0
                                    ? COLOR_NEGATIVE
                                    : COLOR_POSITIVE
                    )
            );
        }

        if (race.passiveRegenerationPerSecond() > 0.0) {
            lines.add(
                    positive(
                            "Regenerates "
                                    + NUMBER.format(
                                            race.passiveRegenerationPerSecond()
                                    )
                                    + " HP/s"
                    )
            );
        }

        if (race.permanentNightVision()) {
            lines.add(
                    positive(
                            "Permanent Night Vision"
                    )
            );
        }

        if (race.canLoadFireworkRockets()) {
            lines.add(
                    positive(
                            "Can load firework rockets into crossbows"
                    )
            );
        }

        if (race.canEatWhenFull()) {
            lines.add(
                    positive(
                            "Can eat while full"
                    )
            );
        }

        if (!approximately(
                race.foodHungerMultiplier(),
                1.0
        )) {
            lines.add(
                    positive(
                            "Food restores "
                                    + NUMBER.format(
                                            race.foodHungerMultiplier()
                                    )
                                    + "x hunger"
                    )
            );
        }

        if (race.foodHealing() > 0.0) {
            lines.add(
                    positive(
                            "Eating heals "
                                    + NUMBER.format(
                                            race.foodHealing()
                                    )
                                    + " HP"
                    )
            );
        }

        if (!race.canWearBoots()) {
            lines.add(
                    negative(
                            "Cannot wear boots"
                    )
            );
        }

        if (race.canWearLightArmor()
                && !race.canWearMediumArmor()
                && !race.canWearHeavyArmor()) {
            lines.add(
                    negative(
                            "Can wear only Light armor"
                    )
            );
        } else {
            if (!race.canWearLightArmor()) {
                lines.add(
                        negative(
                                "Cannot wear Light armor"
                        )
                );
            }

            if (!race.canWearMediumArmor()) {
                lines.add(
                        negative(
                                "Cannot wear Medium armor"
                        )
                );
            }

            if (!race.canWearHeavyArmor()) {
                lines.add(
                        negative(
                                "Cannot wear Heavy armor"
                        )
                );
            }
        }

        boolean hasDarkBonus =
                !approximately(
                        race.darkMovementMultiplier(),
                        1.0
                )
                        || !approximately(
                        race.darkDamageMultiplier(),
                        1.0
                );

        if (hasDarkBonus) {
            StringBuilder text =
                    new StringBuilder("Darkness:");

            if (!approximately(
                    race.darkMovementMultiplier(),
                    1.0
            )) {
                text.append(" ")
                        .append(
                                signedPercentFromMultiplier(
                                        race.darkMovementMultiplier()
                                )
                        )
                        .append(" speed");
            }

            if (!approximately(
                    race.darkDamageMultiplier(),
                    1.0
            )) {
                if (!approximately(
                        race.darkMovementMultiplier(),
                        1.0
                )) {
                    text.append(",");
                }

                text.append(" ")
                        .append(
                                signedPercentFromMultiplier(
                                        race.darkDamageMultiplier()
                                )
                        )
                        .append(" damage");
            }

            lines.add(
                    positive(text.toString())
            );
        }

        boolean hasBrightPenalty =
                !approximately(
                        race.brightDamageMultiplier(),
                        1.0
                )
                        || !race.rangedAllowedInBrightLight();

        if (hasBrightPenalty) {
            StringBuilder text =
                    new StringBuilder("Bright light:");

            if (!approximately(
                    race.brightDamageMultiplier(),
                    1.0
            )) {
                text.append(" ")
                        .append(
                                signedPercentFromMultiplier(
                                        race.brightDamageMultiplier()
                                )
                        )
                        .append(" damage");
            }

            if (!race.rangedAllowedInBrightLight()) {
                if (!approximately(
                        race.brightDamageMultiplier(),
                        1.0
                )) {
                    text.append(",");
                }

                text.append(" no ranged weapons");
            }

            lines.add(
                    negative(text.toString())
            );
        }

        boolean hasNetherBonus =
                !approximately(
                        race.netherMovementMultiplier(),
                        1.0
                )
                        || !approximately(
                        race.netherDamageMultiplier(),
                        1.0
                );

        if (hasNetherBonus) {
            StringBuilder text =
                    new StringBuilder("Nether:");

            if (!approximately(
                    race.netherMovementMultiplier(),
                    1.0
            )) {
                text.append(" ")
                        .append(
                                signedPercentFromMultiplier(
                                        race.netherMovementMultiplier()
                                )
                        )
                        .append(" speed");
            }

            if (!approximately(
                    race.netherDamageMultiplier(),
                    1.0
            )) {
                if (!approximately(
                        race.netherMovementMultiplier(),
                        1.0
                )) {
                    text.append(",");
                }

                text.append(" ")
                        .append(
                                signedPercentFromMultiplier(
                                        race.netherDamageMultiplier()
                                )
                        )
                        .append(" damage");
            }

            lines.add(
                    positive(text.toString())
            );
        }

        if (race.netherMobsNeutral()) {
            lines.add(
                    positive(
                            "Nether mobs are neutral until attacked"
                    )
            );
        }

        if (race.bowNoFalloff()) {
            lines.add(
                    positive(
                            "Bow arrows have no gravity/drop"
                    )
            );
        }

        if (race.crossbowNoFalloff()) {
            lines.add(
                    positive(
                            "Crossbow arrows have no gravity/drop"
                    )
            );
        }

        if (!approximately(
                race.forestMovementMultiplier(),
                1.0
        )) {
            lines.add(
                    positive(
                            "Forests: "
                                    + signedPercentFromMultiplier(
                                            race.forestMovementMultiplier()
                                    )
                                    + " movement"
                    )
            );
        }

        if (race.leafClimbing()) {
            lines.add(
                    positive(
                            "Leaves: Space climbs, Shift hangs"
                    )
            );
        }

        if (race.crackedIncomingDamageMultiplier() > 1.0
                && race.crackedDurationTicks() > 0) {
            lines.add(
                    negative(
                            "Pickaxe hits apply Cracked: +"
                                    + NUMBER.format(
                                            (race.crackedIncomingDamageMultiplier()
                                                    - 1.0)
                                                    * 100.0
                                    )
                                    + "% damage taken for "
                                    + NUMBER.format(
                                            race.crackedDurationTicks()
                                                    / 20.0
                                    )
                                    + "s"
                    )
            );
        }

        if (race.chargedJumpEnabled()) {
            lines.add(
                    positive(
                            "Charged jump: up to "
                                    + NUMBER.format(
                                            race.chargedJumpMaxHeight()
                                    )
                                    + " blocks"
                    )
            );
        }

        if (race.glideEnabled()) {
            lines.add(
                    positive(
                            "Hold Space in air to glide"
                    )
            );
        }

        if (!race.rocketFlightBoostAllowed()) {
            lines.add(
                    negative(
                            "Cannot use rockets to boost flight"
                    )
            );
        }

        return lines;
    }

    private void drawInfoLine(
            GuiGraphicsExtractor graphics,
            int x,
            int y,
            InfoLine line
    ) {
        graphics.text(
                font,
                Component.literal("- " + line.text()),
                x,
                y,
                line.color(),
                false
        );
    }

    private static InfoLine positive(
            String text
    ) {
        return new InfoLine(
                text,
                COLOR_POSITIVE
        );
    }

    private static InfoLine negative(
            String text
    ) {
        return new InfoLine(
                text,
                COLOR_NEGATIVE
        );
    }

    private static InfoLine comparison(
            String text,
            double value,
            double baseline
    ) {
        return new InfoLine(
                text,
                comparisonColor(
                        value,
                        baseline
                )
        );
    }

    private static int comparisonColor(
            double value,
            double baseline
    ) {
        if (value > baseline) {
            return COLOR_POSITIVE;
        }

        if (value < baseline) {
            return COLOR_NEGATIVE;
        }

        return COLOR_NORMAL;
    }

    private static int physicalComparisonColor(
            double value,
            double baseline
    ) {
        /*
         * Body size itself is not intrinsically a buff or debuff.
         */
        return approximately(
                value,
                baseline
        )
                ? COLOR_NORMAL
                : COLOR_MIXED;
    }

    private static boolean isImportantDifference(
            double value,
            double baseline
    ) {
        if (baseline == 0.0) {
            return !approximately(
                    value,
                    0.0
            );
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

    private static boolean approximately(
            double a,
            double b
    ) {
        return Math.abs(a - b) <= 0.000001;
    }

    private record InfoLine(
            String text,
            int color
    ) {
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
