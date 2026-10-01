package com.example.racecore.race;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

public final class RaceCommands {
    private RaceCommands() {
    }

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(
                Commands.literal("race")
                        .executes(RaceCommands::showCurrentRace)
                        .then(
                                Commands.argument("race", StringArgumentType.word())
                                        .suggests((context, builder) ->
                                                SharedSuggestionProvider.suggest(
                                                        RaceRegistry.ids(),
                                                        builder
                                                )
                                        )
                                        .executes(RaceCommands::changeRace)
                        )
        );
    }

    private static int showCurrentRace(
            CommandContext<CommandSourceStack> context
    ) throws CommandSyntaxException {

        ServerPlayer player = context.getSource().getPlayerOrException();

        String raceId = RaceManager.getRaceId(player);
        RaceDefinition race = RaceManager.getRace(player);

        context.getSource().sendSuccess(
                () -> Component.literal(
                        "Current race: " + race.displayName()
                                + " (" + raceId + ")"
                                + " | HP: " + formatNumber(race.maxHealth())
                                + " | Scale: " + formatNumber(race.scale())
                                + " | Step: " + formatNumber(race.stepHeight())
                                + " | Jump: " + formatNumber(race.jumpHeight())
                                + " | Speed: x" + formatNumber(race.movementSpeedMultiplier())
                                + " | Reach: x" + formatNumber(race.reachMultiplier())
                ),
                false
        );

        context.getSource().sendSuccess(
                () -> Component.literal(
                        "Available races: " + String.join(", ", RaceRegistry.ids())
                ),
                false
        );

        return 1;
    }

    private static int changeRace(
            CommandContext<CommandSourceStack> context
    ) throws CommandSyntaxException {

        ServerPlayer player = context.getSource().getPlayerOrException();
        String requested = StringArgumentType.getString(context, "race");

        if (!RaceManager.setRace(player, requested)) {
            context.getSource().sendFailure(
                    Component.literal(
                            "Unknown race '" + requested + "'. Available: "
                                    + String.join(", ", RaceRegistry.ids())
                    )
            );

            return 0;
        }

        RaceDefinition race = RaceManager.getRace(player);

        context.getSource().sendSuccess(
                () -> Component.literal(
                        "Race changed to " + race.displayName()
                                + " | HP: " + formatNumber(race.maxHealth())
                                + " | Scale: " + formatNumber(race.scale())
                                + " | Step: " + formatNumber(race.stepHeight())
                                + " | Jump: " + formatNumber(race.jumpHeight())
                                + " | Speed: x" + formatNumber(race.movementSpeedMultiplier())
                                + " | Reach: x" + formatNumber(race.reachMultiplier())
                ),
                false
        );

        return 1;
    }

    private static String formatNumber(double value) {
        if (value == Math.rint(value)) {
            return Long.toString(Math.round(value));
        }

        return String.format(java.util.Locale.ROOT, "%.3f", value)
                .replaceAll("0+$", "")
                .replaceAll("\\.$", "");
    }
}
