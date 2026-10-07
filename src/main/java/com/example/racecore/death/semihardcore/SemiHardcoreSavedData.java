package com.example.racecore.death.semihardcore;

import com.example.racecore.RaceCore;
import com.mojang.serialization.Codec;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * World-persistent semi-hardcore state.
 *
 * Stored in the overworld data storage because the lockout belongs to the
 * server/player, not to a particular dimension.
 */
public final class SemiHardcoreSavedData
        extends SavedData {

    private static final String PLAYERS_KEY =
            "players";

    public static final Codec<SemiHardcoreSavedData> CODEC =
            ExtraCodecs.converter(
                    NbtOps.INSTANCE
            ).xmap(
                    SemiHardcoreSavedData::decode,
                    SemiHardcoreSavedData::save
            );

    public static final SavedDataType<SemiHardcoreSavedData> TYPE =
            new SavedDataType<>(
                    Identifier.fromNamespaceAndPath(
                            RaceCore.MODID,
                            "semi_hardcore"
                    ),
                    SemiHardcoreSavedData::new,
                    CODEC
            );

    private final Map<UUID, SemiHardcoreState> states =
            new HashMap<>();

    public static SemiHardcoreSavedData get(
            MinecraftServer server
    ) {
        return server.overworld()
                .getDataStorage()
                .computeIfAbsent(TYPE);
    }

    public SemiHardcoreState getRaw(
            UUID playerId
    ) {
        return states.getOrDefault(
                playerId,
                SemiHardcoreState.empty()
        );
    }

    public SemiHardcoreState getNormalized(
            UUID playerId,
            long nowEpochMillis
    ) {
        SemiHardcoreState oldState =
                getRaw(playerId);

        SemiHardcoreState normalized =
                SemiHardcoreSystem.normalizeDailyState(
                        oldState,
                        nowEpochMillis
                );

        if (!normalized.equals(oldState)) {
            states.put(
                    playerId,
                    normalized
            );
            setDirty(true);
        }

        return normalized;
    }

    public void put(
            UUID playerId,
            SemiHardcoreState state
    ) {
        states.put(
                playerId,
                state
        );
        setDirty(true);
    }

    public void remove(
            UUID playerId
    ) {
        if (states.remove(playerId) != null) {
            setDirty(true);
        }
    }

    private static SemiHardcoreSavedData decode(
            Tag tag
    ) {
        SemiHardcoreSavedData data =
                new SemiHardcoreSavedData();

        if (!(tag instanceof CompoundTag root)) {
            return data;
        }

        CompoundTag players =
                root.getCompoundOrEmpty(
                        PLAYERS_KEY
                );

        for (String uuidText
                : players.keySet()) {

            final UUID uuid;

            try {
                uuid =
                        UUID.fromString(
                                uuidText
                        );
            } catch (IllegalArgumentException exception) {
                RaceCore.LOGGER.warn(
                        "Ignoring invalid semi-hardcore player UUID '{}'.",
                        uuidText
                );
                continue;
            }

            CompoundTag stateTag =
                    players.getCompoundOrEmpty(
                            uuidText
                    );

            int deaths =
                    Math.max(
                            0,
                            stateTag.getIntOr(
                                    "deaths_today",
                                    0
                            )
                    );

            long unlockAt =
                    Math.max(
                            0L,
                            stateTag.getLongOr(
                                    "unlock_at",
                                    0L
                            )
                    );

            long nextResetAt =
                    Math.max(
                            0L,
                            stateTag.getLongOr(
                                    "next_daily_reset_at",
                                    0L
                            )
                    );

            data.states.put(
                    uuid,
                    new SemiHardcoreState(
                            deaths,
                            unlockAt,
                            nextResetAt
                    )
            );
        }

        return data;
    }

    private CompoundTag save() {
        CompoundTag root =
                new CompoundTag();

        CompoundTag players =
                new CompoundTag();

        states.forEach((uuid, state) -> {
            CompoundTag stateTag =
                    new CompoundTag();

            stateTag.putInt(
                    "deaths_today",
                    state.deathsToday()
            );

            stateTag.putLong(
                    "unlock_at",
                    state.unlockAtEpochMillis()
            );

            stateTag.putLong(
                    "next_daily_reset_at",
                    state.nextDailyResetAtEpochMillis()
            );

            players.put(
                    uuid.toString(),
                    stateTag
            );
        });

        root.put(
                PLAYERS_KEY,
                players
        );

        return root;
    }
}
