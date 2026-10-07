package com.example.racecore.death.semihardcore;

/**
 * Persisted per-player semi-hardcore state.
 *
 * unlockAtEpochMillis is an absolute timestamp so server restarts do not erase
 * an active lockout.
 *
 * nextDailyResetAtEpochMillis is also absolute. The next boundary is chosen
 * using the server machine's local timezone when the current daily window is
 * created. If the machine timezone changes later, the already-persisted
 * boundary is still honored; a new boundary is calculated only after it passes.
 */
public record SemiHardcoreState(
        int deathsToday,
        long unlockAtEpochMillis,
        long nextDailyResetAtEpochMillis
) {
    public static SemiHardcoreState empty() {
        return new SemiHardcoreState(
                0,
                0L,
                0L
        );
    }

    public boolean isLocked(long nowEpochMillis) {
        return unlockAtEpochMillis > nowEpochMillis;
    }

    public long remainingMillis(long nowEpochMillis) {
        return Math.max(
                0L,
                unlockAtEpochMillis - nowEpochMillis
        );
    }

    public SemiHardcoreState withUnlockAt(
            long unlockAt
    ) {
        return new SemiHardcoreState(
                deathsToday,
                unlockAt,
                nextDailyResetAtEpochMillis
        );
    }

    public SemiHardcoreState withDeathsToday(
            int deaths
    ) {
        return new SemiHardcoreState(
                Math.max(0, deaths),
                unlockAtEpochMillis,
                nextDailyResetAtEpochMillis
        );
    }
}
