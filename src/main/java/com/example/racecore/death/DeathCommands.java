package com.example.racecore.death;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

public final class DeathCommands {
    private DeathCommands() {}

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(
                Commands.literal("death")
                        .executes(DeathCommands::showLastDeath)
        );
    }

    private static int showLastDeath(
            CommandContext<CommandSourceStack> context
    ) throws CommandSyntaxException {
        ServerPlayer player = context.getSource().getPlayerOrException();
        LastDeathLocation location = player.getData(DeathAttachments.LAST_DEATH);

        if (!location.exists()) {
            context.getSource().sendFailure(Component.literal("No recorded death location."));
            return 0;
        }

        context.getSource().sendSuccess(
                () -> Component.literal(
                        "Last death: " + location.prettyDimension()
                                + " | X: " + location.deathX()
                                + " Y: " + location.deathY()
                                + " Z: " + location.deathZ()
                ),
                false
        );

        if (location.hasBarrel()) {
            context.getSource().sendSuccess(
                    () -> Component.literal(
                            "Death Barrel: " + location.prettyDimension()
                                    + " | X: " + location.barrelX()
                                    + " Y: " + location.barrelY()
                                    + " Z: " + location.barrelZ()
                    ),
                    false
            );
        }

        return 1;
    }
}
