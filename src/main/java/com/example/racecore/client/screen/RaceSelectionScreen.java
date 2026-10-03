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
import java.util.List;
import java.util.Locale;

/**
 * Texture-free RaceCore race-selection screen.
 *
 * V1 intentionally uses only vanilla widgets and colors.
 * Custom artwork, icons and player/race model previews can be layered on later.
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

    private static final DecimalFormat NUMBER;

    static {
        DecimalFormatSymbols symbols =
                DecimalFormatSymbols.getInstance(Locale.ROOT);

        NUMBER = new DecimalFormat("0.##", symbols);
    }

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
                Math.min(570, Math.max(340, width - 32));

        panelHeight =
                Math.min(310, Math.max(245, height - 32));

        panelX =
                (width - panelWidth) / 2;

        panelY =
                (height - panelHeight) / 2;

        listWidth =
                Math.min(230, panelWidth * 42 / 100);

        int raceAreaX =
                panelX + 14;

        int raceAreaY =
                panelY + 42;

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

        /*
         * The server is authoritative and validates the request.
         * The normal UI can only send ids that came from RaceRegistry.
         */
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

        // Main frame.
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

        // Header.
        graphics.fill(
                panelX,
                panelY,
                panelX + panelWidth,
                panelY + 30,
                0xF0282828
        );

        // Divider between race list and details.
        int dividerX =
                panelX + listWidth + 25;

        graphics.fill(
                dividerX,
                panelY + 38,
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
                panelY + 10,
                0xFFFFFFFF
        );

        graphics.text(
                font,
                Component.literal("Choose a race"),
                panelX + 14,
                panelY + 31,
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

        int x =
                panelX + listWidth + 40;

        int y =
                panelY + 43;

        graphics.text(
                font,
                Component.literal(race.displayName()),
                x,
                y,
                0xFFFFD966,
                true
        );

        y += 20;

        drawStat(
                graphics,
                x,
                y,
                "Maximum HP",
                NUMBER.format(race.maxHealth())
        );

        y += 15;

        drawStat(
                graphics,
                x,
                y,
                "Size",
                NUMBER.format(race.scale()) + "x"
        );

        y += 15;

        drawStat(
                graphics,
                x,
                y,
                "Step Height",
                NUMBER.format(race.stepHeight())
        );

        y += 15;

        drawStat(
                graphics,
                x,
                y,
                "Jump Height",
                NUMBER.format(race.jumpHeight())
        );

        y += 15;

        drawStat(
                graphics,
                x,
                y,
                "Movement",
                NUMBER.format(
                        race.movementSpeedMultiplier()
                ) + "x"
        );

        y += 15;

        drawStat(
                graphics,
                x,
                y,
                "Reach",
                NUMBER.format(
                        race.reachMultiplier()
                ) + "x"
        );

        y += 15;

        drawStat(
                graphics,
                x,
                y,
                "Enchant Capacity",
                Integer.toString(
                        race.enchantmentCapacity()
                )
        );

        y += 25;

        graphics.text(
                font,
                Component.literal(
                        "Race abilities and visual previews"
                ),
                x,
                y,
                0xFF9A9A9A,
                false
        );

        y += 12;

        graphics.text(
                font,
                Component.literal(
                        "will be added here later."
                ),
                x,
                y,
                0xFF9A9A9A,
                false
        );

        y += 24;

        graphics.text(
                font,
                Component.literal(
                        "This choice is permanent until"
                ),
                x,
                y,
                0xFFFFAA55,
                false
        );

        y += 12;

        graphics.text(
                font,
                Component.literal(
                        "you obtain a future race-respec item."
                ),
                x,
                y,
                0xFFFFAA55,
                false
        );
    }

    private void drawStat(
            GuiGraphicsExtractor graphics,
            int x,
            int y,
            String name,
            String value
    ) {
        graphics.text(
                font,
                Component.literal(name + ":"),
                x,
                y,
                0xFFB8B8B8,
                false
        );

        graphics.text(
                font,
                Component.literal(value),
                x + 105,
                y,
                0xFFFFFFFF,
                false
        );
    }

    /**
     * First-time race selection is intentionally mandatory.
     */
    @Override
    public boolean shouldCloseOnEsc() {
        return false;
    }

    @Override
    public void onClose() {
        // Intentionally do nothing.
        // A first-time player must confirm one race.
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
