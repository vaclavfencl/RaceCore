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
            context.getSource().sendFailure(
                    Component.translatable(
                            "command.racecore.death.none"
                    )
            );
            return 0;
        }

        context.getSource().sendSuccess(
                () -> Component.translatable(
                        "command.racecore.death.last",
                        location.prettyDimension(),
                        location.deathX(),
                        location.deathY(),
                        location.deathZ()
                ),
                false
        );

        if (location.hasBarrel()) {
            context.getSource().sendSuccess(
                    () -> Component.translatable(
                            "command.racecore.death.barrel",
                            location.prettyDimension(),
                            location.barrelX(),
                            location.barrelY(),
                            location.barrelZ()
                    ),
                    false
            );
        }

        return 1;
    }
}
