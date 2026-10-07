package com.example.racecore.death.semihardcore;

import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Shared rules for Phase 10 semi-hardcore death.
 */
public final class SemiHardcoreSystem {
    /*
     * Production schedule:
     * 1st  = 1 min
     * 2nd  = 5 min
     * 3rd  = 10 min
     * 4th  = 30 min
     * 5th  = 1 h
     * 6th  = 2 h
     * 7th  = 4 h
     * 8th+ = 8 h
     */
    private static final long[] PENALTY_MILLIS = {
            1L * 60L * 1000L,
            5L * 60L * 1000L,
            10L * 60L * 1000L,
            30L * 60L * 1000L,
            1L * 60L * 60L * 1000L,
            2L * 60L * 60L * 1000L,
            4L * 60L * 60L * 1000L,
            8L * 60L * 60L * 1000L
    };

    /*
     * Transient hook for future duels/minigames/admin tooling.
     *
     * Calling bypassNextDeath(player) makes the next actual death go through
     * the normal respawn flow but WITHOUT incrementing the daily counter or
     * applying a lockout.
     */
    private static final Set<UUID> BYPASS_NEXT_DEATH =
            ConcurrentHashMap.newKeySet();

    private SemiHardcoreSystem() {
    }

    public static boolean isEnabled(
            MinecraftServer server
    ) {
        /*
         * Phase 10 deliberately replaces vanilla Hardcore death consequences.
         * It is completely dormant in non-Hardcore worlds.
         */
        return server != null
                && server.isHardcore();
    }

    public static SemiHardcoreState normalizeDailyState(
            SemiHardcoreState state,
            long nowEpochMillis
    ) {
        long nextResetAt =
                state.nextDailyResetAtEpochMillis();

        if (nextResetAt <= 0L) {
            return new SemiHardcoreState(
                    state.deathsToday(),
                    state.unlockAtEpochMillis(),
                    calculateNextLocalMidnight(
                            nowEpochMillis
                    )
            );
        }

        if (nowEpochMillis >= nextResetAt) {
            /*
             * Midnight resets/decays the DAILY death count only.
             * An already active lockout remains active until its absolute
             * unlock timestamp.
             */
            return new SemiHardcoreState(
                    0,
                    state.unlockAtEpochMillis(),
                    calculateNextLocalMidnight(
                            nowEpochMillis
                    )
            );
        }

        return state;
    }

    public static SemiHardcoreState recordRealDeath(
            SemiHardcoreState current,
            long nowEpochMillis
    ) {
        SemiHardcoreState normalized =
                normalizeDailyState(
                        current,
                        nowEpochMillis
                );

        int newDeathCount =
                normalized.deathsToday()
                        + 1;

        long penalty =
                getPenaltyMillis(
                        newDeathCount
                );

        return new SemiHardcoreState(
                newDeathCount,
                nowEpochMillis + penalty,
                normalized.nextDailyResetAtEpochMillis()
        );
    }

    public static long getPenaltyMillis(
            int deathNumber
    ) {
        if (deathNumber <= 0) {
            return 0L;
        }

        int index =
                Math.min(
                        deathNumber - 1,
                        PENALTY_MILLIS.length - 1
                );

        return PENALTY_MILLIS[index];
    }

    public static void bypassNextDeath(
            ServerPlayer player
    ) {
        BYPASS_NEXT_DEATH.add(
                player.getUUID()
        );
    }

    public static void bypassNextDeath(
            UUID playerId
    ) {
        BYPASS_NEXT_DEATH.add(
                playerId
        );
    }

    public static boolean consumeBypass(
            UUID playerId
    ) {
        return BYPASS_NEXT_DEATH.remove(
                playerId
        );
    }

    public static Component lockoutMessage(
            SemiHardcoreState state,
            long nowEpochMillis
    ) {
        return Component.literal(
                "Semi-hardcore death #"
                        + state.deathsToday()
                        + ". You can rejoin in "
                        + formatDuration(
                        state.remainingMillis(
                                nowEpochMillis
                        )
                )
                        + "."
        );
    }

    public static String formatDuration(
            long millis
    ) {
        long seconds =
                Math.max(
                        0L,
                        (millis + 999L) / 1000L
                );

        long days =
                seconds / 86_400L;

        seconds %= 86_400L;

        long hours =
                seconds / 3_600L;

        seconds %= 3_600L;

        long minutes =
                seconds / 60L;

        seconds %= 60L;

        StringBuilder result =
                new StringBuilder();

        if (days > 0L) {
            result.append(days)
                    .append("d ");
        }

        if (hours > 0L) {
            result.append(hours)
                    .append("h ");
        }

        if (minutes > 0L) {
            result.append(minutes)
                    .append("m ");
        }

        if (seconds > 0L
                || result.isEmpty()) {

            result.append(seconds)
                    .append("s");
        }

        return result.toString()
                .trim();
    }

    private static long calculateNextLocalMidnight(
            long nowEpochMillis
    ) {
        ZoneId zone =
                ZoneId.systemDefault();

        ZonedDateTime now =
                Instant.ofEpochMilli(
                        nowEpochMillis
                ).atZone(zone);

        return now.toLocalDate()
                .plusDays(1L)
                .atStartOfDay(zone)
                .toInstant()
                .toEpochMilli();
    }
}
