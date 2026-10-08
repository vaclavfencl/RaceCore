package com.example.racecore.world;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

/**
 * Phase 12.5 testing/admin commands for player-hunt encounters.
 *
 * /racecore hunt start
 * /racecore hunt start <player>
 */
public final class HuntCommands {
    private HuntCommands() {
    }

    public static void register(
            CommandDispatcher<CommandSourceStack> dispatcher
    ) {
        dispatcher.register(
                Commands.literal("racecore")
                        .then(
                                Commands.literal("hunt")
                                        .requires(
                                                Commands.hasPermission(
                                                        Commands.LEVEL_GAMEMASTERS
                                                )
                                        )
                                        .then(
                                                Commands.literal("start")
                                                        .executes(HuntCommands::startSelf)
                                                        .then(
                                                                Commands.argument(
                                                                                "player",
                                                                                EntityArgument.player()
                                                                        )
                                                                        .executes(context ->
                                                                                startForPlayer(
                                                                                        context.getSource(),
                                                                                        EntityArgument.getPlayer(
                                                                                                context,
                                                                                                "player"
                                                                                        )
                                                                                )
                                                                        )
                                                        )
                                        )
                        )
        );
    }

    private static int startSelf(
            CommandContext<CommandSourceStack> context
    ) throws CommandSyntaxException {
        return startForPlayer(
                context.getSource(),
                context.getSource().getPlayerOrException()
        );
    }

    private static int startForPlayer(
            CommandSourceStack source,
            ServerPlayer target
    ) {
        HuntEncounterEvents.HuntStartResult result =
                HuntEncounterEvents.startHuntNow(target);

        switch (result) {
            case STARTED -> {
                source.sendSuccess(
                        () -> Component.literal(
                                "Started a RaceCore hunt targeting "
                                        + target.getName().getString()
                                        + "."
                        ),
                        true
                );
                return 1;
            }
            case INVALID_TARGET -> source.sendFailure(
                    Component.literal(
                            "That player cannot currently be hunted."
                    )
            );
            case WRONG_DIMENSION -> source.sendFailure(
                    Component.literal(
                            "Hunts currently start only in the Overworld."
                    )
            );
            case PEACEFUL -> source.sendFailure(
                    Component.literal(
                            "Cannot start a hunt while the world is on Peaceful difficulty."
                    )
            );
            case ACTIVE_HUNT -> source.sendFailure(
                    Component.literal(
                            "That player's nearby group already has an active hunt."
                    )
            );
            case PROTECTED -> source.sendFailure(
                    Component.literal(
                            "Cannot start a hunt while the target is inside a banner-protected surface chunk."
                    )
            );
            case NO_VALID_SPAWN -> source.sendFailure(
                    Component.literal(
                            "Could not find a valid place to spawn the hunt. Move into open terrain and try again."
                    )
            );
        }

        return 0;
    }
}
