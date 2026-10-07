package com.example.racecore.client.screen;

import com.example.racecore.armor.ArmorManager;
import com.example.racecore.enchanting.DeterministicEnchantingManager;
import com.example.racecore.enchanting.UtilityEnchantingManager;
import com.example.racecore.enchanting.WeaponToolEnchantingManager;
import com.example.racecore.network.enchanting.ApplyEnchantPlanPayload;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.EnchantmentScreen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.EnchantmentMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantment;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Phase 9.0 enchanting-table UI.
 *
 * Books delegate completely to the vanilla screen. All other equipment gets
 * an exact enchantment selector while the underlying vanilla menu continues
 * to own the item slot, lapis slot and player inventory.
 */
public final class DeterministicEnchantmentScreen
        extends EnchantmentScreen {

    private static final int PANEL_WIDTH = 248;
    private static final int PANEL_HEIGHT = 166;
    private static final int PANEL_GAP = 6;

    private static final int ROW_HEIGHT = 17;
    private static final int VISIBLE_ROWS = 5;
    private static final int LIST_TOP = 48;
    private static final int LIST_HEIGHT =
            ROW_HEIGHT * VISIBLE_ROWS;

    /*
     * At high GUI scales there is not enough logical screen width for the
     * 248px side panel. Falling back to a centered side panel used to cover
     * the entire vanilla enchanting container, including its slots and the
     * player's inventory. In that case we now render a compact selector only
     * over the vanilla three-offer area.
     */
    private static final int COMPACT_X = 58;
    private static final int COMPACT_Y = 12;
    private static final int COMPACT_WIDTH = 112;
    private static final int COMPACT_HEIGHT = 61;
    private static final int COMPACT_LIST_TOP = 24;
    private static final int COMPACT_ROW_HEIGHT = 14;
    private static final int COMPACT_VISIBLE_ROWS = 2;
    private static final int COMPACT_APPLY_X = 13;
    private static final int COMPACT_APPLY_Y = 70;
    private static final int COMPACT_APPLY_WIDTH = 39;
    private static final int COMPACT_APPLY_HEIGHT = 12;

    // Long enchantment names in compact mode gently ping-pong one character
    // at a time so the complete name can be read without stealing room from
    // the +/- controls. 8 ticks = 0.4 s per character at 20 TPS.
    private static final int MARQUEE_TICKS_PER_CHARACTER = 8;
    // Minecraft 26.3 switched mouse input to SDL. SDL left-click is button 1,
    // not GLFW-style button 0. Using 0 made every real left click get consumed
    // as a non-primary click before the +/- or Apply action could run.
    private static final int PRIMARY_MOUSE_BUTTON = 1;
    private static final int COMPACT_NAME_WIDTH = 63;

    private static final int COLOR_PANEL = 0xF0202020;
    private static final int COLOR_BORDER = 0xFF707070;
    private static final int COLOR_TEXT = 0xFFF0F0F0;
    private static final int COLOR_MUTED = 0xFFA0A0A0;
    private static final int COLOR_GOOD = 0xFF7FE27F;
    private static final int COLOR_BAD = 0xFFFF7777;
    private static final int COLOR_ACCENT = 0xFFFFD966;
    private static final int COLOR_BUTTON = 0xFF4A4A4A;
    private static final int COLOR_BUTTON_HOVER = 0xFF666666;
    private static final int COLOR_BUTTON_DISABLED = 0xFF303030;

    private static final DecimalFormat NUMBER =
            new DecimalFormat(
                    "0.##",
                    DecimalFormatSymbols
                            .getInstance(
                                    Locale.ROOT
                            )
            );

    private final Map<String, Integer> plannedLevels =
            new LinkedHashMap<>();

    private ItemStack lastSeen =
            ItemStack.EMPTY;

    private int scrollOffset;
    private int marqueeTick;

    public DeterministicEnchantmentScreen(
            EnchantmentMenu menu,
            Inventory inventory,
            Component title
    ) {
        super(
                menu,
                inventory,
                title
        );
    }

    private boolean isVanillaBookMode() {
        return menu.getSlot(0)
                .getItem()
                .is(Items.BOOK);
    }

    @Override
    public void containerTick() {
        super.containerTick();
        marqueeTick++;

        if (isVanillaBookMode()) {
            plannedLevels.clear();
            scrollOffset = 0;
            lastSeen =
                    menu.getSlot(0)
                            .getItem()
                            .copy();

            return;
        }

        ItemStack current =
                menu.getSlot(0)
                        .getItem();

        if (!ItemStack.matches(
                current,
                lastSeen
        )) {
            plannedLevels.clear();
            scrollOffset = 0;
            lastSeen =
                    current.copy();
        }
    }

    @Override
    public void extractRenderState(
            GuiGraphicsExtractor graphics,
            int mouseX,
            int mouseY,
            float partialTick
    ) {
        if (isVanillaBookMode()) {
            super.extractRenderState(
                    graphics,
                    mouseX,
                    mouseY,
                    partialTick
            );

            return;
        }

        /*
         * Prevent vanilla's random-offer tooltip from appearing through the
         * deterministic overlay while keeping all normal slot tooltips.
         */
        boolean overVanillaOffers =
                isInside(
                        mouseX,
                        mouseY,
                        leftPos + 60,
                        topPos + 14,
                        108,
                        57
                );

        super.extractRenderState(
                graphics,
                overVanillaOffers
                        ? -10000
                        : mouseX,
                overVanillaOffers
                        ? -10000
                        : mouseY,
                partialTick
        );

        /*
         * Cover the three vanilla random offers. Books never reach this path,
         * so their classic three-choice UI remains untouched.
         */
        graphics.fill(
                leftPos + 58,
                topPos + 12,
                leftPos + 170,
                topPos + 73,
                0xF0181818
        );

        if (hasSidePanelRoom()) {
            graphics.text(
                    font,
                    Component.translatable(
                            "screen.racecore.enchanting.exact_active"
                    ),
                    leftPos + 65,
                    topPos + 20,
                    COLOR_ACCENT,
                    true
            );

            graphics.text(
                    font,
                    Component.translatable(
                            "screen.racecore.enchanting.exact_hint"
                    ),
                    leftPos + 65,
                    topPos + 37,
                    COLOR_MUTED,
                    false
            );

            drawSelectorPanel(
                    graphics,
                    mouseX,
                    mouseY
            );
        } else {
            drawCompactSelector(
                    graphics,
                    mouseX,
                    mouseY
            );
        }
    }

    private void drawCompactSelector(
            GuiGraphicsExtractor graphics,
            int mouseX,
            int mouseY
    ) {
        int areaX =
                leftPos + COMPACT_X;

        int areaY =
                topPos + COMPACT_Y;

        graphics.fill(
                areaX,
                areaY,
                areaX + COMPACT_WIDTH,
                areaY + COMPACT_HEIGHT,
                0xF0181818
        );

        graphics.text(
                font,
                Component.translatable(
                        "screen.racecore.enchanting.compact_title"
                ),
                areaX + 3,
                areaY + 2,
                COLOR_ACCENT,
                true
        );

        ItemStack original =
                menu.getSlot(0)
                        .getItem();

        if (original.isEmpty()) {
            graphics.text(
                    font,
                    Component.translatable(
                            "screen.racecore.enchanting.compact_insert"
                    ),
                    areaX + 3,
                    areaY + 15,
                    COLOR_MUTED,
                    false
            );

            return;
        }

        if (minecraft == null
                || minecraft.player == null) {

            return;
        }

        List<Option> options =
                getOptions(
                        original
                );

        Map<Holder<Enchantment>, Integer> targets =
                buildTargets(
                        options,
                        original
                );

        ItemStack preview =
                DeterministicEnchantingManager
                        .applyTargetsCopy(
                                original,
                                targets
                        );

        boolean compatible =
                DeterministicEnchantingManager
                        .areEnchantmentsCompatible(
                                preview
                        );

        boolean capacityValid =
                DeterministicEnchantingManager
                        .isWithinCapacity(
                                minecraft.player,
                                preview
                        );

        drawCompactCapacity(
                graphics,
                areaX + 3,
                areaY + 13,
                preview,
                capacityValid
        );

        if (options.isEmpty()) {
            graphics.text(
                    font,
                    Component.translatable(
                            "screen.racecore.enchanting.compact_no_options"
                    ),
                    areaX + 3,
                    areaY + 28,
                    COLOR_MUTED,
                    false
            );

            return;
        }

        int maxScroll =
                Math.max(
                        0,
                        options.size()
                                - COMPACT_VISIBLE_ROWS
                );

        scrollOffset =
                Math.max(
                        0,
                        Math.min(
                                scrollOffset,
                                maxScroll
                        )
                );

        for (int visibleIndex = 0;
             visibleIndex < COMPACT_VISIBLE_ROWS;
             visibleIndex++) {

            int optionIndex =
                    scrollOffset
                            + visibleIndex;

            if (optionIndex
                    >= options.size()) {

                break;
            }

            Option option =
                    options.get(
                            optionIndex
                    );

            int rowY =
                    areaY
                            + COMPACT_LIST_TOP
                            + visibleIndex
                            * COMPACT_ROW_HEIGHT;

            drawCompactOptionRow(
                    graphics,
                    areaX,
                    rowY,
                    mouseX,
                    mouseY,
                    option,
                    original,
                    options
            );
        }

        drawCompactScrollBar(
                graphics,
                areaX,
                areaY,
                options.size()
        );

        int cost =
                DeterministicEnchantingManager
                        .getAddedLevelCost(
                                original,
                                targets
                        );

        boolean free =
                minecraft.player
                        .hasInfiniteMaterials();

        boolean enoughLevels =
                free
                        || minecraft.player
                        .experienceLevel
                        >= cost;

        boolean enoughLapis =
                free
                        || menu.getGoldCount()
                        >= cost;

        boolean canApply =
                cost > 0
                        && compatible
                        && capacityValid
                        && enoughLevels
                        && enoughLapis;

        Component status;
        int statusColor;

        if (!compatible) {
            status =
                    Component.translatable(
                            "screen.racecore.enchanting.compact_incompatible"
                    );
            statusColor = COLOR_BAD;
        } else if (!capacityValid) {
            status =
                    Component.translatable(
                            "screen.racecore.enchanting.compact_capacity"
                    );
            statusColor = COLOR_BAD;
        } else if (free) {
            status =
                    Component.translatable(
                            "screen.racecore.enchanting.compact_free"
                    );
            statusColor =
                    cost > 0
                            ? COLOR_GOOD
                            : COLOR_MUTED;
        } else {
            status =
                    Component.translatable(
                            "screen.racecore.enchanting.cost_short",
                            cost,
                            cost
                    );
            statusColor =
                    canApply
                            ? COLOR_GOOD
                            : cost <= 0
                            ? COLOR_MUTED
                            : COLOR_BAD;
        }

        graphics.text(
                font,
                status,
                areaX + 3,
                areaY + 52,
                statusColor,
                false
        );

        int applyX =
                leftPos + COMPACT_APPLY_X;

        int applyY =
                topPos + COMPACT_APPLY_Y;

        boolean applyHover =
                isInside(
                        mouseX,
                        mouseY,
                        applyX,
                        applyY,
                        COMPACT_APPLY_WIDTH,
                        COMPACT_APPLY_HEIGHT
                );

        drawBox(
                graphics,
                applyX,
                applyY,
                COMPACT_APPLY_WIDTH,
                COMPACT_APPLY_HEIGHT,
                canApply,
                applyHover
        );

        graphics.centeredText(
                font,
                Component.translatable(
                        "screen.racecore.enchanting.apply"
                ),
                applyX
                        + COMPACT_APPLY_WIDTH / 2,
                applyY + 2,
                canApply
                        ? COLOR_TEXT
                        : COLOR_MUTED
        );
    }

    private void drawCompactCapacity(
            GuiGraphicsExtractor graphics,
            int x,
            int y,
            ItemStack preview,
            boolean capacityValid
    ) {
        int color =
                capacityValid
                        ? COLOR_GOOD
                        : COLOR_BAD;

        Component line;

        if (ArmorManager.getDefinition(
                preview
        ) != null) {
            line =
                    Component.translatable(
                            "screen.racecore.enchanting.compact_protection",
                            NUMBER.format(
                                    DeterministicEnchantingManager
                                            .getProtectionUsed(
                                                    preview
                                            )
                            ),
                            DeterministicEnchantingManager
                                    .getProtectionCapacity(
                                            minecraft.player,
                                            preview
                                    ),
                            UtilityEnchantingManager
                                    .getUsedCapacity(
                                            preview
                                    ),
                            UtilityEnchantingManager
                                    .getCapacity(
                                            minecraft.player,
                                            preview
                                    )
                    );
        } else if (WeaponToolEnchantingManager
                .isWeaponOrTool(
                        preview
                )) {
            line =
                    Component.translatable(
                            "screen.racecore.enchanting.compact_effective",
                            WeaponToolEnchantingManager
                                    .getUsedEffectiveCapacity(
                                            preview
                                    ),
                            WeaponToolEnchantingManager
                                    .getEffectiveCapacity(
                                            minecraft.player,
                                            preview
                                    ),
                            WeaponToolEnchantingManager
                                    .getUsedUtilityCapacity(
                                            preview
                                    ),
                            WeaponToolEnchantingManager
                                    .getUtilityCapacity(
                                            minecraft.player,
                                            preview
                                    )
                    );
        } else {
            line =
                    Component.translatable(
                            "screen.racecore.enchanting.compact_utility",
                            DeterministicEnchantingManager
                                    .getGenericUtilityUsed(
                                            preview
                                    ),
                            DeterministicEnchantingManager
                                    .getGenericUtilityCapacity(
                                            minecraft.player,
                                            preview
                                    )
                    );
        }

        graphics.text(
                font,
                line,
                x,
                y,
                color,
                false
        );
    }

    private void drawCompactOptionRow(
            GuiGraphicsExtractor graphics,
            int areaX,
            int rowY,
            int mouseX,
            int mouseY,
            Option option,
            ItemStack original,
            List<Option> options
    ) {
        int current =
                DeterministicEnchantingManager
                        .getCurrentLevel(
                                original,
                                option.enchantment()
                        );

        int target =
                getPlannedLevel(
                        option,
                        current
                );

        int minusX =
                areaX + 68;

        int plusX =
                areaX + 99;

        boolean canMinus =
                target > current;

        boolean canPlus =
                canIncrease(
                        option,
                        target,
                        original,
                        options
                );

        boolean minusHover =
                isInside(
                        mouseX,
                        mouseY,
                        minusX,
                        rowY + 1,
                        11,
                        12
                );

        boolean plusHover =
                isInside(
                        mouseX,
                        mouseY,
                        plusX,
                        rowY + 1,
                        11,
                        12
                );

        graphics.text(
                font,
                getCompactMarqueeName(
                        option.name(),
                        COMPACT_NAME_WIDTH
                ),
                areaX + 3,
                rowY + 3,
                target > current
                        ? COLOR_ACCENT
                        : COLOR_TEXT,
                false
        );

        drawBox(
                graphics,
                minusX,
                rowY + 1,
                11,
                12,
                canMinus,
                minusHover
        );

        graphics.centeredText(
                font,
                Component.literal("-"),
                minusX + 5,
                rowY + 3,
                canMinus
                        ? COLOR_TEXT
                        : COLOR_MUTED
        );

        graphics.centeredText(
                font,
                Component.literal(
                        current
                                + "→"
                                + target
                ),
                areaX + 89,
                rowY + 3,
                target > current
                        ? COLOR_ACCENT
                        : COLOR_MUTED
        );

        drawBox(
                graphics,
                plusX,
                rowY + 1,
                11,
                12,
                canPlus,
                plusHover
        );

        graphics.centeredText(
                font,
                Component.literal("+"),
                plusX + 5,
                rowY + 3,
                canPlus
                        ? COLOR_TEXT
                        : COLOR_MUTED
        );
    }

    private void drawCompactScrollBar(
            GuiGraphicsExtractor graphics,
            int areaX,
            int areaY,
            int optionCount
    ) {
        if (optionCount
                <= COMPACT_VISIBLE_ROWS) {

            return;
        }

        int maxScroll =
                optionCount
                        - COMPACT_VISIBLE_ROWS;

        int trackX =
                areaX
                        + COMPACT_WIDTH
                        - 2;

        int trackY =
                areaY
                        + COMPACT_LIST_TOP;

        int trackHeight =
                COMPACT_ROW_HEIGHT
                        * COMPACT_VISIBLE_ROWS;

        graphics.fill(
                trackX,
                trackY,
                trackX + 1,
                trackY + trackHeight,
                0xFF3A3A3A
        );

        int thumbHeight =
                Math.max(
                        6,
                        trackHeight
                                * COMPACT_VISIBLE_ROWS
                                / optionCount
                );

        int thumbTravel =
                trackHeight
                        - thumbHeight;

        int thumbY =
                trackY
                        + (maxScroll == 0
                        ? 0
                        : scrollOffset
                        * thumbTravel
                        / maxScroll);

        graphics.fill(
                trackX,
                thumbY,
                trackX + 1,
                thumbY + thumbHeight,
                0xFFB8B8B8
        );
    }

    private void drawSelectorPanel(
            GuiGraphicsExtractor graphics,
            int mouseX,
            int mouseY
    ) {
        int panelX =
                getPanelX();

        int panelY =
                topPos;

        graphics.fill(
                panelX - 1,
                panelY - 1,
                panelX + PANEL_WIDTH + 1,
                panelY + PANEL_HEIGHT + 1,
                COLOR_BORDER
        );

        graphics.fill(
                panelX,
                panelY,
                panelX + PANEL_WIDTH,
                panelY + PANEL_HEIGHT,
                COLOR_PANEL
        );

        graphics.text(
                font,
                Component.translatable(
                        "screen.racecore.enchanting.title"
                ),
                panelX + 8,
                panelY + 7,
                COLOR_ACCENT,
                true
        );

        ItemStack original =
                menu.getSlot(0)
                        .getItem();

        if (original.isEmpty()) {
            graphics.text(
                    font,
                    Component.translatable(
                            "screen.racecore.enchanting.insert_item"
                    ),
                    panelX + 8,
                    panelY + 28,
                    COLOR_MUTED,
                    false
            );

            return;
        }

        if (minecraft == null
                || minecraft.player == null) {

            return;
        }

        List<Option> options =
                getOptions(
                        original
                );

        Map<Holder<Enchantment>, Integer> targets =
                buildTargets(
                        options,
                        original
                );

        ItemStack preview =
                DeterministicEnchantingManager
                        .applyTargetsCopy(
                                original,
                                targets
                        );

        boolean compatible =
                DeterministicEnchantingManager
                        .areEnchantmentsCompatible(
                                preview
                        );

        boolean capacityValid =
                DeterministicEnchantingManager
                        .isWithinCapacity(
                                minecraft.player,
                                preview
                        );

        drawCapacity(
                graphics,
                panelX + 8,
                panelY + 22,
                preview,
                capacityValid
        );

        if (options.isEmpty()) {
            graphics.text(
                    font,
                    Component.translatable(
                            "screen.racecore.enchanting.no_options"
                    ),
                    panelX + 8,
                    panelY + LIST_TOP + 7,
                    COLOR_MUTED,
                    false
            );

            return;
        }

        int maxScroll =
                Math.max(
                        0,
                        options.size()
                                - VISIBLE_ROWS
                );

        scrollOffset =
                Math.max(
                        0,
                        Math.min(
                                scrollOffset,
                                maxScroll
                        )
                );

        for (int visibleIndex = 0;
             visibleIndex < VISIBLE_ROWS;
             visibleIndex++) {

            int optionIndex =
                    scrollOffset
                            + visibleIndex;

            if (optionIndex
                    >= options.size()) {

                break;
            }

            Option option =
                    options.get(
                            optionIndex
                    );

            int rowY =
                    panelY
                            + LIST_TOP
                            + visibleIndex
                            * ROW_HEIGHT;

            drawOptionRow(
                    graphics,
                    panelX,
                    rowY,
                    mouseX,
                    mouseY,
                    option,
                    original,
                    options
            );
        }

        drawScrollBar(
                graphics,
                panelX,
                panelY,
                options.size()
        );

        int cost =
                DeterministicEnchantingManager
                        .getAddedLevelCost(
                                original,
                                targets
                        );

        boolean free =
                minecraft.player
                        .hasInfiniteMaterials();

        boolean enoughLevels =
                free
                        || minecraft.player
                        .experienceLevel
                        >= cost;

        boolean enoughLapis =
                free
                        || menu.getGoldCount()
                        >= cost;

        boolean canApply =
                cost > 0
                        && compatible
                        && capacityValid
                        && enoughLevels
                        && enoughLapis;

        int costColor =
                canApply
                        ? COLOR_GOOD
                        : cost <= 0
                        ? COLOR_MUTED
                        : COLOR_BAD;

        graphics.text(
                font,
                free
                        ? Component.translatable(
                        "screen.racecore.enchanting.cost_creative"
                )
                        : Component.translatable(
                        "screen.racecore.enchanting.cost",
                        cost,
                        cost
                ),
                panelX + 8,
                panelY + 137,
                costColor,
                false
        );

        int applyX =
                panelX + PANEL_WIDTH - 68;

        int applyY =
                panelY + 145;

        boolean applyHover =
                isInside(
                        mouseX,
                        mouseY,
                        applyX,
                        applyY,
                        58,
                        15
                );

        drawBox(
                graphics,
                applyX,
                applyY,
                58,
                15,
                canApply,
                applyHover
        );

        graphics.centeredText(
                font,
                Component.translatable(
                        "screen.racecore.enchanting.apply"
                ),
                applyX + 29,
                applyY + 3,
                canApply
                        ? COLOR_TEXT
                        : COLOR_MUTED
        );

        if (!compatible) {
            graphics.text(
                    font,
                    Component.translatable(
                            "screen.racecore.enchanting.incompatible"
                    ),
                    panelX + 8,
                    panelY + 151,
                    COLOR_BAD,
                    false
            );
        } else if (!capacityValid) {
            graphics.text(
                    font,
                    Component.translatable(
                            "screen.racecore.enchanting.capacity_exceeded"
                    ),
                    panelX + 8,
                    panelY + 151,
                    COLOR_BAD,
                    false
            );
        }
    }

    private void drawCapacity(
            GuiGraphicsExtractor graphics,
            int x,
            int y,
            ItemStack preview,
            boolean capacityValid
    ) {
        int color =
                capacityValid
                        ? COLOR_GOOD
                        : COLOR_BAD;

        if (ArmorManager.getDefinition(
                preview
        ) != null) {

            graphics.text(
                    font,
                    Component.translatable(
                            "screen.racecore.enchanting.protection",
                            NUMBER.format(
                                    DeterministicEnchantingManager
                                            .getProtectionUsed(
                                                    preview
                                            )
                            ),
                            DeterministicEnchantingManager
                                    .getProtectionCapacity(
                                            minecraft.player,
                                            preview
                                    )
                    ),
                    x,
                    y,
                    color,
                    false
            );

            graphics.text(
                    font,
                    Component.translatable(
                            "screen.racecore.enchanting.utility",
                            UtilityEnchantingManager
                                    .getUsedCapacity(
                                            preview
                                    ),
                            UtilityEnchantingManager
                                    .getCapacity(
                                            minecraft.player,
                                            preview
                                    )
                    ),
                    x + 112,
                    y,
                    color,
                    false
            );

            return;
        }

        if (WeaponToolEnchantingManager
                .isWeaponOrTool(
                        preview
                )) {

            graphics.text(
                    font,
                    Component.translatable(
                            "screen.racecore.enchanting.effective",
                            WeaponToolEnchantingManager
                                    .getUsedEffectiveCapacity(
                                            preview
                                    ),
                            WeaponToolEnchantingManager
                                    .getEffectiveCapacity(
                                            minecraft.player,
                                            preview
                                    )
                    ),
                    x,
                    y,
                    color,
                    false
            );

            graphics.text(
                    font,
                    Component.translatable(
                            "screen.racecore.enchanting.utility",
                            WeaponToolEnchantingManager
                                    .getUsedUtilityCapacity(
                                            preview
                                    ),
                            WeaponToolEnchantingManager
                                    .getUtilityCapacity(
                                            minecraft.player,
                                            preview
                                    )
                    ),
                    x + 112,
                    y,
                    color,
                    false
            );

            return;
        }

        graphics.text(
                font,
                Component.translatable(
                        "screen.racecore.enchanting.utility",
                        DeterministicEnchantingManager
                                .getGenericUtilityUsed(
                                        preview
                                ),
                        DeterministicEnchantingManager
                                .getGenericUtilityCapacity(
                                        minecraft.player,
                                        preview
                                )
                ),
                x,
                y,
                color,
                false
        );
    }

    private void drawOptionRow(
            GuiGraphicsExtractor graphics,
            int panelX,
            int rowY,
            int mouseX,
            int mouseY,
            Option option,
            ItemStack original,
            List<Option> options
    ) {
        int current =
                DeterministicEnchantingManager
                        .getCurrentLevel(
                                original,
                                option.enchantment()
                        );

        int target =
                getPlannedLevel(
                        option,
                        current
                );

        int minusX =
                panelX + PANEL_WIDTH - 51;

        int plusX =
                panelX + PANEL_WIDTH - 27;

        boolean canMinus =
                target > current;

        boolean canPlus =
                canIncrease(
                        option,
                        target,
                        original,
                        options
                );

        boolean minusHover =
                isInside(
                        mouseX,
                        mouseY,
                        minusX,
                        rowY + 1,
                        18,
                        14
                );

        boolean plusHover =
                isInside(
                        mouseX,
                        mouseY,
                        plusX,
                        rowY + 1,
                        18,
                        14
                );

        graphics.text(
                font,
                option.name(),
                panelX + 8,
                rowY + 4,
                COLOR_TEXT,
                false
        );

        graphics.text(
                font,
                Component.translatable(
                        "screen.racecore.enchanting.current_to_target",
                        current,
                        target
                ),
                panelX + 125,
                rowY + 4,
                target > current
                        ? COLOR_ACCENT
                        : COLOR_MUTED,
                false
        );

        drawBox(
                graphics,
                minusX,
                rowY + 1,
                18,
                14,
                canMinus,
                minusHover
        );

        graphics.centeredText(
                font,
                Component.literal("-"),
                minusX + 9,
                rowY + 4,
                canMinus
                        ? COLOR_TEXT
                        : COLOR_MUTED
        );

        drawBox(
                graphics,
                plusX,
                rowY + 1,
                18,
                14,
                canPlus,
                plusHover
        );

        graphics.centeredText(
                font,
                Component.literal("+"),
                plusX + 9,
                rowY + 4,
                canPlus
                        ? COLOR_TEXT
                        : COLOR_MUTED
        );
    }

    private void drawScrollBar(
            GuiGraphicsExtractor graphics,
            int panelX,
            int panelY,
            int optionCount
    ) {
        if (optionCount <= VISIBLE_ROWS) {
            return;
        }

        int maxScroll =
                optionCount
                        - VISIBLE_ROWS;

        int trackX =
                panelX
                        + PANEL_WIDTH
                        - 4;

        int trackY =
                panelY
                        + LIST_TOP;

        int trackHeight =
                LIST_HEIGHT;

        graphics.fill(
                trackX,
                trackY,
                trackX + 2,
                trackY + trackHeight,
                0xFF3A3A3A
        );

        int thumbHeight =
                Math.max(
                        10,
                        trackHeight
                                * VISIBLE_ROWS
                                / optionCount
                );

        int thumbTravel =
                trackHeight
                        - thumbHeight;

        int thumbY =
                trackY
                        + (maxScroll == 0
                        ? 0
                        : scrollOffset
                        * thumbTravel
                        / maxScroll);

        graphics.fill(
                trackX,
                thumbY,
                trackX + 2,
                thumbY + thumbHeight,
                0xFFB8B8B8
        );
    }

    private static void drawBox(
            GuiGraphicsExtractor graphics,
            int x,
            int y,
            int width,
            int height,
            boolean enabled,
            boolean hovered
    ) {
        int color =
                !enabled
                        ? COLOR_BUTTON_DISABLED
                        : hovered
                        ? COLOR_BUTTON_HOVER
                        : COLOR_BUTTON;

        graphics.fill(
                x,
                y,
                x + width,
                y + height,
                color
        );
    }

    @Override
    public boolean mouseClicked(
            MouseButtonEvent event,
            boolean doubleClick
    ) {
        if (isVanillaBookMode()) {
            return super.mouseClicked(
                    event,
                    doubleClick
            );
        }

        if (handleOverlayClick(
                event.x(),
                event.y(),
                event.button()
        )) {
            return true;
        }

        /*
         * Never let a click reach the hidden vanilla random offer buttons.
         */
        if (isInside(
                event.x(),
                event.y(),
                leftPos + 60,
                topPos + 14,
                108,
                57
        )) {
            return true;
        }

        return super.mouseClicked(
                event,
                doubleClick
        );
    }

    /**
     * Handles only RaceCore's deterministic overlay controls.
     *
     * This is intentionally callable from ScreenEvent.MouseButtonPressed.Pre
     * as well as mouseClicked(). The Pre hook makes these tiny compact controls
     * reliable even when vanilla container input handling would otherwise win
     * the click at very large GUI scales. Returning true means the caller must
     * consume/cancel the click.
     */
    public boolean handleOverlayClick(
            double mouseX,
            double mouseY,
            int button
    ) {
        if (isVanillaBookMode()) {
            return false;
        }

        if (!hasSidePanelRoom()) {
            return handleCompactOverlayClick(
                    mouseX,
                    mouseY,
                    button
            );
        }

        int panelX =
                getPanelX();

        int panelY =
                topPos;

        if (!isInside(
                mouseX,
                mouseY,
                panelX,
                panelY,
                PANEL_WIDTH,
                PANEL_HEIGHT
        )) {
            return false;
        }

        // The custom panel owns all clicks inside its rectangle.
        if (button != PRIMARY_MOUSE_BUTTON) {
            return true;
        }

        ItemStack original =
                menu.getSlot(0)
                        .getItem();

        if (original.isEmpty()
                || minecraft == null
                || minecraft.player == null) {

            return true;
        }

        List<Option> options =
                getOptions(
                        original
                );

        for (int visibleIndex = 0;
             visibleIndex < VISIBLE_ROWS;
             visibleIndex++) {

            int optionIndex =
                    scrollOffset
                            + visibleIndex;

            if (optionIndex
                    >= options.size()) {

                break;
            }

            Option option =
                    options.get(
                            optionIndex
                    );

            int rowY =
                    panelY
                            + LIST_TOP
                            + visibleIndex
                            * ROW_HEIGHT;

            int minusX =
                    panelX
                            + PANEL_WIDTH
                            - 51;

            int plusX =
                    panelX
                            + PANEL_WIDTH
                            - 27;

            int current =
                    DeterministicEnchantingManager
                            .getCurrentLevel(
                                    original,
                                    option.enchantment()
                            );

            int target =
                    getPlannedLevel(
                            option,
                            current
                    );

            if (isInside(
                    mouseX,
                    mouseY,
                    minusX,
                    rowY + 1,
                    18,
                    14
            )) {
                if (target > current) {
                    setPlannedLevel(
                            option,
                            target - 1,
                            current
                    );
                }

                return true;
            }

            if (isInside(
                    mouseX,
                    mouseY,
                    plusX,
                    rowY + 1,
                    18,
                    14
            )) {
                if (canIncrease(
                        option,
                        target,
                        original,
                        options
                )) {
                    setPlannedLevel(
                            option,
                            target + 1,
                            current
                    );
                }

                return true;
            }
        }

        int applyX =
                panelX
                        + PANEL_WIDTH
                        - 68;

        int applyY =
                panelY + 145;

        if (isInside(
                mouseX,
                mouseY,
                applyX,
                applyY,
                58,
                15
        )) {
            tryApply(
                    original,
                    options
            );

            return true;
        }

        return true;
    }

    private boolean handleCompactOverlayClick(
            double mouseX,
            double mouseY,
            int button
    ) {
        int areaX =
                leftPos + COMPACT_X;

        int areaY =
                topPos + COMPACT_Y;

        int applyX =
                leftPos + COMPACT_APPLY_X;

        int applyY =
                topPos + COMPACT_APPLY_Y;

        if (isInside(
                mouseX,
                mouseY,
                applyX,
                applyY,
                COMPACT_APPLY_WIDTH,
                COMPACT_APPLY_HEIGHT
        )) {
            if (button != PRIMARY_MOUSE_BUTTON) {
                return true;
            }

            ItemStack original =
                    menu.getSlot(0)
                            .getItem();

            if (!original.isEmpty()
                    && minecraft != null
                    && minecraft.player != null) {

                tryApply(
                        original,
                        getOptions(
                                original
                        )
                );
            }

            return true;
        }

        if (!isInside(
                mouseX,
                mouseY,
                areaX,
                areaY,
                COMPACT_WIDTH,
                COMPACT_HEIGHT
        )) {
            return false;
        }

        // The compact selector owns clicks inside its area, including disabled
        // buttons. This prevents the hidden vanilla enchant offers underneath
        // from receiving the same click.
        if (button != PRIMARY_MOUSE_BUTTON) {
            return true;
        }

        ItemStack original =
                menu.getSlot(0)
                        .getItem();

        if (original.isEmpty()
                || minecraft == null
                || minecraft.player == null) {

            return true;
        }

        List<Option> options =
                getOptions(
                        original
                );

        int maxScroll =
                Math.max(
                        0,
                        options.size()
                                - COMPACT_VISIBLE_ROWS
                );

        scrollOffset =
                Math.max(
                        0,
                        Math.min(
                                scrollOffset,
                                maxScroll
                        )
                );

        for (int visibleIndex = 0;
             visibleIndex < COMPACT_VISIBLE_ROWS;
             visibleIndex++) {

            int optionIndex =
                    scrollOffset
                            + visibleIndex;

            if (optionIndex
                    >= options.size()) {

                break;
            }

            Option option =
                    options.get(
                            optionIndex
                    );

            int rowY =
                    areaY
                            + COMPACT_LIST_TOP
                            + visibleIndex
                            * COMPACT_ROW_HEIGHT;

            int minusX =
                    areaX + 68;

            int plusX =
                    areaX + 99;

            int current =
                    DeterministicEnchantingManager
                            .getCurrentLevel(
                                    original,
                                    option.enchantment()
                            );

            int target =
                    getPlannedLevel(
                            option,
                            current
                    );

            if (isInside(
                    mouseX,
                    mouseY,
                    minusX,
                    rowY + 1,
                    11,
                    12
            )) {
                if (target > current) {
                    setPlannedLevel(
                            option,
                            target - 1,
                            current
                    );
                }

                return true;
            }

            if (isInside(
                    mouseX,
                    mouseY,
                    plusX,
                    rowY + 1,
                    11,
                    12
            )) {
                if (canIncrease(
                        option,
                        target,
                        original,
                        options
                )) {
                    setPlannedLevel(
                            option,
                            target + 1,
                            current
                    );
                }

                return true;
            }
        }

        return true;
    }

    @Override
    public boolean mouseScrolled(
            double mouseX,
            double mouseY,
            double horizontalAmount,
            double verticalAmount
    ) {
        if (!isVanillaBookMode()) {
            boolean sidePanel =
                    hasSidePanelRoom();

            int listX =
                    sidePanel
                            ? getPanelX()
                            : leftPos + COMPACT_X;

            int listY =
                    sidePanel
                            ? topPos + LIST_TOP
                            : topPos
                            + COMPACT_Y
                            + COMPACT_LIST_TOP;

            int listWidth =
                    sidePanel
                            ? PANEL_WIDTH
                            : COMPACT_WIDTH;

            int listHeight =
                    sidePanel
                            ? LIST_HEIGHT
                            : COMPACT_ROW_HEIGHT
                            * COMPACT_VISIBLE_ROWS;

            if (isInside(
                    mouseX,
                    mouseY,
                    listX,
                    listY,
                    listWidth,
                    listHeight
            )) {
                ItemStack original =
                        menu.getSlot(0)
                                .getItem();

                int optionCount =
                        original.isEmpty()
                                ? 0
                                : getOptions(
                                original
                        ).size();

                int visibleRows =
                        sidePanel
                                ? VISIBLE_ROWS
                                : COMPACT_VISIBLE_ROWS;

                int maxScroll =
                        Math.max(
                                0,
                                optionCount
                                        - visibleRows
                        );

                int direction =
                        verticalAmount > 0.0
                                ? -1
                                : verticalAmount < 0.0
                                ? 1
                                : 0;

                if (direction != 0) {
                    scrollOffset =
                            Math.max(
                                    0,
                                    Math.min(
                                            maxScroll,
                                            scrollOffset
                                                    + direction
                                    )
                            );

                    return true;
                }
            }
        }

        return super.mouseScrolled(
                mouseX,
                mouseY,
                horizontalAmount,
                verticalAmount
        );
    }

    private Component getCompactMarqueeName(
            Component name,
            int maxWidth
    ) {
        String full =
                name.getString();

        if (full.isEmpty()
                || font.width(full) <= maxWidth) {

            return name;
        }

        int lastStart = 0;

        for (int i = 1;
             i < full.length();
             i++) {

            if (font.width(
                    full.substring(i)
            ) <= maxWidth) {
                lastStart = i;
                break;
            }
        }

        if (lastStart <= 0) {
            lastStart =
                    Math.max(
                            1,
                            full.length() - 1
                    );
        }

        int step =
                marqueeTick
                        / MARQUEE_TICKS_PER_CHARACTER;

        int cycle =
                Math.max(
                        1,
                        lastStart * 2
                );

        int phase =
                Math.floorMod(
                        step,
                        cycle
                );

        int start =
                phase <= lastStart
                        ? phase
                        : cycle - phase;

        int end = start + 1;

        while (end <= full.length()
                && font.width(
                full.substring(
                        start,
                        end
                )
        ) <= maxWidth) {
            end++;
        }

        end =
                Math.max(
                        start + 1,
                        end - 1
                );

        return Component.literal(
                full.substring(
                        start,
                        Math.min(
                                end,
                                full.length()
                        )
                )
        );
    }

    private boolean canIncrease(
            Option option,
            int target,
            ItemStack original,
            List<Option> options
    ) {
        if (minecraft == null
                || minecraft.player == null) {

            return false;
        }

        if (target >= option.enchantment()
                .value()
                .getMaxLevel()) {

            return false;
        }

        Map<Holder<Enchantment>, Integer> targets =
                buildTargets(
                        options,
                        original
                );

        targets.put(
                option.enchantment(),
                target + 1
        );

        ItemStack preview =
                DeterministicEnchantingManager
                        .applyTargetsCopy(
                                original,
                                targets
                        );

        return DeterministicEnchantingManager
                .areEnchantmentsCompatible(
                        preview
                )
                && DeterministicEnchantingManager
                .isWithinCapacity(
                        minecraft.player,
                        preview
                );
    }

    private void tryApply(
            ItemStack original,
            List<Option> options
    ) {
        if (minecraft == null
                || minecraft.player == null) {

            return;
        }

        Map<Holder<Enchantment>, Integer> targets =
                buildTargets(
                        options,
                        original
                );

        int cost =
                DeterministicEnchantingManager
                        .getAddedLevelCost(
                                original,
                                targets
                        );

        if (cost <= 0) {
            return;
        }

        ItemStack preview =
                DeterministicEnchantingManager
                        .applyTargetsCopy(
                                original,
                                targets
                        );

        if (!DeterministicEnchantingManager
                .areEnchantmentsCompatible(
                        preview
                )
                || !DeterministicEnchantingManager
                .isWithinCapacity(
                        minecraft.player,
                        preview
                )) {

            return;
        }

        boolean free =
                minecraft.player
                        .hasInfiniteMaterials();

        if (!free
                && (minecraft.player
                .experienceLevel
                < cost
                || menu.getGoldCount()
                < cost)) {

            return;
        }

        String encoded =
                encodeTargets(
                        original,
                        targets
                );

        if (encoded.isBlank()) {
            return;
        }

        ClientPacketDistributor
                .sendToServer(
                        new ApplyEnchantPlanPayload(
                                encoded
                        )
                );
    }

    private List<Option> getOptions(
            ItemStack stack
    ) {
        if (minecraft == null
                || minecraft.player == null
                || stack.isEmpty()) {

            return List.of();
        }

        HolderLookup.RegistryLookup<Enchantment> registry =
                minecraft.player
                        .level()
                        .registryAccess()
                        .lookupOrThrow(
                                Registries.ENCHANTMENT
                        );

        List<Option> options =
                new ArrayList<>();

        registry.listElements()
                .filter(
                        holder ->
                                DeterministicEnchantingManager
                                        .isTableEligible(
                                                stack,
                                                holder
                                        )
                )
                .forEach(
                        holder -> {
                            String id =
                                    DeterministicEnchantingManager
                                            .getEnchantmentId(
                                                    holder
                                            );

                            if (!id.isBlank()) {
                                options.add(
                                        new Option(
                                                holder,
                                                id,
                                                holder.value()
                                                        .description()
                                        )
                                );
                            }
                        }
                );

        options.sort(
                Comparator.comparing(
                        option ->
                                option.name()
                                        .getString()
                                        .toLowerCase(
                                                Locale.ROOT
                                        )
                )
        );

        return options;
    }

    private Map<Holder<Enchantment>, Integer> buildTargets(
            List<Option> options,
            ItemStack original
    ) {
        Map<Holder<Enchantment>, Integer> targets =
                new LinkedHashMap<>();

        for (Option option : options) {
            int current =
                    DeterministicEnchantingManager
                            .getCurrentLevel(
                                    original,
                                    option.enchantment()
                            );

            int target =
                    getPlannedLevel(
                            option,
                            current
                    );

            if (target > current) {
                targets.put(
                        option.enchantment(),
                        target
                );
            }
        }

        return targets;
    }

    private String encodeTargets(
            ItemStack original,
            Map<Holder<Enchantment>, Integer> targets
    ) {
        StringBuilder builder =
                new StringBuilder();

        for (Map.Entry<Holder<Enchantment>, Integer> entry
                : targets.entrySet()) {

            int current =
                    DeterministicEnchantingManager
                            .getCurrentLevel(
                                    original,
                                    entry.getKey()
                            );

            if (entry.getValue()
                    <= current) {

                continue;
            }

            String id =
                    DeterministicEnchantingManager
                            .getEnchantmentId(
                                    entry.getKey()
                            );

            if (id.isBlank()) {
                continue;
            }

            if (!builder.isEmpty()) {
                builder.append(';');
            }

            builder.append(id)
                    .append('=')
                    .append(
                            entry.getValue()
                    );
        }

        return builder.toString();
    }

    private int getPlannedLevel(
            Option option,
            int current
    ) {
        return Math.max(
                current,
                plannedLevels.getOrDefault(
                        option.id(),
                        current
                )
        );
    }

    private void setPlannedLevel(
            Option option,
            int level,
            int current
    ) {
        if (level <= current) {
            plannedLevels.remove(
                    option.id()
            );

            return;
        }

        plannedLevels.put(
                option.id(),
                level
        );
    }

    private boolean hasSidePanelRoom() {
        int right =
                leftPos
                        + imageWidth
                        + PANEL_GAP;

        if (right + PANEL_WIDTH
                <= width - 4) {

            return true;
        }

        int left =
                leftPos
                        - PANEL_GAP
                        - PANEL_WIDTH;

        return left >= 4;
    }

    private int getPanelX() {
        int right =
                leftPos
                        + imageWidth
                        + PANEL_GAP;

        if (right + PANEL_WIDTH
                <= width - 4) {

            return right;
        }

        return leftPos
                - PANEL_GAP
                - PANEL_WIDTH;
    }

    private static boolean isInside(
            double mouseX,
            double mouseY,
            int x,
            int y,
            int width,
            int height
    ) {
        return mouseX >= x
                && mouseX < x + width
                && mouseY >= y
                && mouseY < y + height;
    }

    private record Option(
            Holder.Reference<Enchantment> enchantment,
            String id,
            Component name
    ) {
    }
}
