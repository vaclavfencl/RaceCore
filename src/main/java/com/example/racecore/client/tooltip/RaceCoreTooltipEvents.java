package com.example.racecore.client.tooltip;

import com.example.racecore.RaceCore;
import com.example.racecore.stats.VanillaStatScaling;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.ItemTooltipEvent;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@EventBusSubscriber(modid = RaceCore.MODID, value = Dist.CLIENT)
public final class RaceCoreTooltipEvents {

    /*
     * Matches the first normal number in an attribute tooltip line.
     *
     * Examples:
     * "7 Attack Damage"
     * "+7 Attack Damage"
     * "7.5 Attack Damage"
     */
    private static final Pattern NUMBER_PATTERN =
            Pattern.compile("[-+]?\\d+(?:[.,]\\d+)?");

    private static final DecimalFormat NUMBER_FORMAT;

    static {
        DecimalFormatSymbols symbols =
                DecimalFormatSymbols.getInstance(Locale.ROOT);

        NUMBER_FORMAT = new DecimalFormat("0.##", symbols);
    }

    private RaceCoreTooltipEvents() {
    }

    @SubscribeEvent
    public static void onItemTooltip(ItemTooltipEvent event) {
        List<Component> tooltip = event.getToolTip();

        /*
         * Use Minecraft's translated attribute name instead of hardcoding
         * "Attack Damage", so this also works when Minecraft isn't in English.
         */
        String attackDamageName = Component.translatable(
                Attributes.ATTACK_DAMAGE.value().getDescriptionId()
        ).getString();

        for (int i = 0; i < tooltip.size(); i++) {
            Component original = tooltip.get(i);
            String text = original.getString();

            if (!text.contains(attackDamageName)) {
                continue;
            }

            String scaledText = scaleFirstNumber(text);

            /*
             * Preserve the vanilla line's style/color while replacing
             * only its displayed text.
             *
             * This is PRESENTATION ONLY.
             * Actual weapon attributes remain unchanged because damage is
             * already converted to RaceCore x5 units in VanillaStatScaling.
             */
            tooltip.set(
                    i,
                    Component.literal(scaledText)
                            .withStyle(original.getStyle())
            );
        }
    }

    private static String scaleFirstNumber(String text) {
        Matcher matcher = NUMBER_PATTERN.matcher(text);

        if (!matcher.find()) {
            return text;
        }

        String originalNumber = matcher.group();

        boolean explicitPlus = originalNumber.startsWith("+");

        String parseable = originalNumber
                .replace("+", "")
                .replace(',', '.');

        double value;

        try {
            value = Double.parseDouble(parseable);
        } catch (NumberFormatException exception) {
            return text;
        }

        double scaled =
                value * VanillaStatScaling.SCALE;

        String formatted = NUMBER_FORMAT.format(scaled);

        if (explicitPlus && scaled > 0) {
            formatted = "+" + formatted;
        }

        return text.substring(0, matcher.start())
                + formatted
                + text.substring(matcher.end());
    }
}