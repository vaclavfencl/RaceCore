package com.example.racecore.death.semihardcore;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.GameProfileArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.players.NameAndId;

import java.util.Collection;

/**
 * Phase 10 testing/admin commands.
 *
 * /racecore semihardcore status
 * /racecore semihardcore status <targets>
 * /racecore semihardcore clear <targets>
 * /racecore semihardcore unlock <targets>
 * /racecore semihardcore setdeaths <targets> <count>
 * /racecore semihardcore lock <targets> <seconds>
 * /racecore semihardcore bypassnext <targets>
 */
public final class SemiHardcoreCommands {
    private SemiHardcoreCommands() {
    }

    public static void register(
            CommandDispatcher<CommandSourceStack> dispatcher
    ) {
        dispatcher.register(
                Commands.literal("racecore")
                        .then(
                                Commands.literal(
                                                "semihardcore"
                                        )
                                        .then(
                                                Commands.literal(
                                                                "status"
                                                        )
                                                        .executes(context ->
                                                                statusSelf(
                                                                        context.getSource()
                                                                )
                                                        )
                                                        .then(
                                                                Commands.argument(
                                                                                "targets",
                                                                                GameProfileArgument.gameProfile()
                                                                        )
                                                                        .requires(
                                                                                Commands.hasPermission(
                                                                                        Commands.LEVEL_GAMEMASTERS
                                                                                )
                                                                        )
                                                                        .executes(context ->
                                                                                statusTargets(
                                                                                        context.getSource(),
                                                                                        GameProfileArgument.getGameProfiles(
                                                                                                context,
                                                                                                "targets"
                                                                                        )
                                                                                )
                                                                        )
                                                        )
                                        )
                                        .then(
                                                Commands.literal(
                                                                "clear"
                                                        )
                                                        .requires(
                                                                Commands.hasPermission(
                                                                        Commands.LEVEL_GAMEMASTERS
                                                                )
                                                        )
                                                        .then(
                                                                Commands.argument(
                                                                                "targets",
                                                                                GameProfileArgument.gameProfile()
                                                                        )
                                                                        .executes(context ->
                                                                                clearTargets(
                                                                                        context.getSource(),
                                                                                        GameProfileArgument.getGameProfiles(
                                                                                                context,
                                                                                                "targets"
                                                                                        )
                                                                                )
                                                                        )
                                                        )
                                        )
                                        .then(
                                                Commands.literal(
                                                                "unlock"
                                                        )
                                                        .requires(
                                                                Commands.hasPermission(
                                                                        Commands.LEVEL_GAMEMASTERS
                                                                )
                                                        )
                                                        .then(
                                                                Commands.argument(
                                                                                "targets",
                                                                                GameProfileArgument.gameProfile()
                                                                        )
                                                                        .executes(context ->
                                                                                unlockTargets(
                                                                                        context.getSource(),
                                                                                        GameProfileArgument.getGameProfiles(
                                                                                                context,
                                                                                                "targets"
                                                                                        )
                                                                                )
                                                                        )
                                                        )
                                        )
                                        .then(
                                                Commands.literal(
                                                                "setdeaths"
                                                        )
                                                        .requires(
                                                                Commands.hasPermission(
                                                                        Commands.LEVEL_GAMEMASTERS
                                                                )
                                                        )
                                                        .then(
                                                                Commands.argument(
                                                                                "targets",
                                                                                GameProfileArgument.gameProfile()
                                                                        )
                                                                        .then(
                                                                                Commands.argument(
                                                                                                "count",
                                                                                                IntegerArgumentType.integer(
                                                                                                        0,
                                                                                                        1000
                                                                                                )
                                                                                        )
                                                                                        .executes(context ->
                                                                                                setDeaths(
                                                                                                        context.getSource(),
                                                                                                        GameProfileArgument.getGameProfiles(
                                                                                                                context,
                                                                                                                "targets"
                                                                                                        ),
                                                                                                        IntegerArgumentType.getInteger(
                                                                                                                context,
                                                                                                                "count"
                                                                                                        )
                                                                                                )
                                                                                        )
                                                                        )
                                                        )
                                        )
                                        .then(
                                                Commands.literal(
                                                                "lock"
                                                        )
                                                        .requires(
                                                                Commands.hasPermission(
                                                                        Commands.LEVEL_GAMEMASTERS
                                                                )
                                                        )
                                                        .then(
                                                                Commands.argument(
                                                                                "targets",
                                                                                GameProfileArgument.gameProfile()
                                                                        )
                                                                        .then(
                                                                                Commands.argument(
                                                                                                "seconds",
                                                                                                IntegerArgumentType.integer(
                                                                                                        1,
                                                                                                        86_400
                                                                                                )
                                                                                        )
                                                                                        .executes(context ->
                                                                                                lockTargets(
                                                                                                        context.getSource(),
                                                                                                        GameProfileArgument.getGameProfiles(
                                                                                                                context,
                                                                                                                "targets"
                                                                                                        ),
                                                                                                        IntegerArgumentType.getInteger(
                                                                                                                context,
                                                                                                                "seconds"
                                                                                                        )
                                                                                                )
                                                                                        )
                                                                        )
                                                        )
                                        )
                                        .then(
                                                Commands.literal(
                                                                "bypassnext"
                                                        )
                                                        .requires(
                                                                Commands.hasPermission(
                                                                        Commands.LEVEL_GAMEMASTERS
                                                                )
                                                        )
                                                        .then(
                                                                Commands.argument(
                                                                                "targets",
                                                                                GameProfileArgument.gameProfile()
                                                                        )
                                                                        .executes(context ->
                                                                                bypassNext(
                                                                                        context.getSource(),
                                                                                        GameProfileArgument.getGameProfiles(
                                                                                                context,
                                                                                                "targets"
                                                                                        )
                                                                                )
                                                                        )
                                                        )
                                        )
                        )
        );
    }

    private static int statusSelf(
            CommandSourceStack source
    ) {
        if (!requireHardcore(source)) {
            return 0;
        }

        final ServerPlayer player;

        try {
            player =
                    source.getPlayerOrException();
        } catch (Exception exception) {
            source.sendFailure(
                    Component.literal(
                            "Specify a player when running this command from the server console."
                    )
            );
            return 0;
        }

        sendStatus(
                source,
                player.nameAndId()
        );

        return 1;
    }

    private static int statusTargets(
            CommandSourceStack source,
            Collection<NameAndId> targets
    ) {
        if (!requireHardcore(source)) {
            return 0;
        }

        for (NameAndId target : targets) {
            sendStatus(
                    source,
                    target
            );
        }

        return targets.size();
    }

    private static int clearTargets(
            CommandSourceStack source,
            Collection<NameAndId> targets
    ) {
        if (!requireHardcore(source)) {
            return 0;
        }

        SemiHardcoreSavedData data =
                SemiHardcoreSavedData.get(
                        source.getServer()
                );

        for (NameAndId target : targets) {
            data.remove(
                    target.id()
            );

            source.sendSuccess(
                    () -> Component.literal(
                            "Cleared all semi-hardcore state for "
                                    + target.name()
                                    + "."
                    ),
                    true
            );
        }

        return targets.size();
    }

    private static int unlockTargets(
            CommandSourceStack source,
            Collection<NameAndId> targets
    ) {
        if (!requireHardcore(source)) {
            return 0;
        }

        long now =
                System.currentTimeMillis();

        SemiHardcoreSavedData data =
                SemiHardcoreSavedData.get(
                        source.getServer()
                );

        for (NameAndId target : targets) {
            SemiHardcoreState state =
                    data.getNormalized(
                            target.id(),
                            now
                    );

            data.put(
                    target.id(),
                    state.withUnlockAt(
                            0L
                    )
            );

            source.sendSuccess(
                    () -> Component.literal(
                            "Unlocked "
                                    + target.name()
                                    + " without changing today's death count ("
                                    + state.deathsToday()
                                    + ")."
                    ),
                    true
            );
        }

        return targets.size();
    }

    private static int setDeaths(
            CommandSourceStack source,
            Collection<NameAndId> targets,
            int count
    ) {
        if (!requireHardcore(source)) {
            return 0;
        }

        long now =
                System.currentTimeMillis();

        SemiHardcoreSavedData data =
                SemiHardcoreSavedData.get(
                        source.getServer()
                );

        for (NameAndId target : targets) {
            SemiHardcoreState state =
                    data.getNormalized(
                            target.id(),
                            now
                    );

            data.put(
                    target.id(),
                    state.withDeathsToday(
                            count
                    )
            );

            source.sendSuccess(
                    () -> Component.literal(
                            "Set today's semi-hardcore death count for "
                                    + target.name()
                                    + " to "
                                    + count
                                    + "."
                    ),
                    true
            );
        }

        return targets.size();
    }

    private static int lockTargets(
            CommandSourceStack source,
            Collection<NameAndId> targets,
            int seconds
    ) {
        if (!requireHardcore(source)) {
            return 0;
        }

        MinecraftServer server =
                source.getServer();

        long now =
                System.currentTimeMillis();

        long unlockAt =
                now + seconds * 1000L;

        SemiHardcoreSavedData data =
                SemiHardcoreSavedData.get(
                        server
                );

        for (NameAndId target : targets) {
            SemiHardcoreState state =
                    data.getNormalized(
                            target.id(),
                            now
                    );

            SemiHardcoreState updated =
                    state.withUnlockAt(
                            unlockAt
                    );

            data.put(
                    target.id(),
                    updated
            );

            ServerPlayer online =
                    server.getPlayerList()
                            .getPlayer(
                                    target.id()
                            );

            if (online != null) {
                online.connection.disconnect(
                        SemiHardcoreSystem
                                .lockoutMessage(
                                        updated,
                                        now
                                )
                );
            }

            source.sendSuccess(
                    () -> Component.literal(
                            "Locked "
                                    + target.name()
                                    + " for "
                                    + SemiHardcoreSystem
                                    .formatDuration(
                                            seconds * 1000L
                                    )
                                    + ". Death count unchanged."
                    ),
                    true
            );
        }

        return targets.size();
    }

    private static int bypassNext(
            CommandSourceStack source,
            Collection<NameAndId> targets
    ) {
        if (!requireHardcore(source)) {
            return 0;
        }

        for (NameAndId target : targets) {
            SemiHardcoreSystem.bypassNextDeath(
                    target.id()
            );

            source.sendSuccess(
                    () -> Component.literal(
                            "The next death for "
                                    + target.name()
                                    + " will respawn normally without a semi-hardcore penalty."
                    ),
                    true
            );
        }

        return targets.size();
    }

    private static void sendStatus(
            CommandSourceStack source,
            NameAndId target
    ) {
        long now =
                System.currentTimeMillis();

        SemiHardcoreState state =
                SemiHardcoreSavedData
                        .get(
                                source.getServer()
                        )
                        .getNormalized(
                                target.id(),
                                now
                        );

        String lock =
                state.isLocked(now)
                        ? SemiHardcoreSystem.formatDuration(
                        state.remainingMillis(
                                now
                        )
                )
                        : "not locked";

        String reset =
                SemiHardcoreSystem.formatDuration(
                        Math.max(
                                0L,
                                state.nextDailyResetAtEpochMillis()
                                        - now
                        )
                );

        source.sendSuccess(
                () -> Component.literal(
                        target.name()
                                + ": deaths today="
                                + state.deathsToday()
                                + ", "
                                + lock
                                + ", daily reset in "
                                + reset
                                + "."
                ),
                false
        );
    }

    private static boolean requireHardcore(
            CommandSourceStack source
    ) {
        if (SemiHardcoreSystem.isEnabled(
                source.getServer()
        )) {
            return true;
        }

        source.sendFailure(
                Component.literal(
                        "RaceCore semi-hardcore is disabled because this world is not Hardcore."
                )
        );

        return false;
    }
}
