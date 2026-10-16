package com.example.racecore.raid;

import com.example.racecore.RaceCore;
import com.mojang.serialization.Codec;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Persistent, per-dimension raid start rosters and paid-out victory rewards.
 * Players cannot join a reward roster after the raid's first tick.
 */
public final class RaidRewardSavedData extends SavedData {
    public static final Codec<RaidRewardSavedData> CODEC =
            ExtraCodecs.converter(NbtOps.INSTANCE).xmap(
                    RaidRewardSavedData::decode, RaidRewardSavedData::save);

    public static final SavedDataType<RaidRewardSavedData> TYPE =
            new SavedDataType<>(
                    Identifier.fromNamespaceAndPath(RaceCore.MODID, "raid_rewards"),
                    RaidRewardSavedData::new,
                    CODEC);

    private final Map<Integer, RaidRecord> raids = new HashMap<>();

    public static RaidRewardSavedData get(ServerLevel level) {
        return level.getDataStorage().computeIfAbsent(TYPE);
    }

    public RaidRecord getRecord(int raidId) {
        return raids.get(raidId);
    }

    /** Register only once: never modify this snapshot while the raid continues. */
    public void rememberRaid(int raidId, BlockPos center, Set<UUID> players) {
        if (raids.containsKey(raidId)) return;
        raids.put(raidId, new RaidRecord(center.immutable(), players));
        setDirty(true);
    }

    public void markVictory(int raidId) {
        RaidRecord record = raids.get(raidId);
        if (record != null && !record.victory) {
            record.victory = true;
            setDirty(true);
        }
    }

    public void forgetDefeatedRaid(int raidId) {
        RaidRecord record = raids.get(raidId);
        if (record != null && !record.victory) {
            raids.remove(raidId);
            setDirty(true);
        }
    }

    public Map<Integer, RaidRecord> records() {
        return raids;
    }

    public void markPaid(int raidId, UUID player) {
        RaidRecord record = raids.get(raidId);
        if (record != null && record.eligible.contains(player)
                && record.paid.add(player)) {
            setDirty(true);
        }
    }

    public static final class RaidRecord {
        public final BlockPos center;
        public final Set<UUID> eligible;
        public final Set<UUID> paid;
        public boolean victory;

        private RaidRecord(BlockPos center, Set<UUID> eligible) {
            this(center, eligible, new HashSet<>(), false);
        }

        private RaidRecord(BlockPos center, Set<UUID> eligible,
                           Set<UUID> paid, boolean victory) {
            this.center = center;
            this.eligible = new HashSet<>(eligible);
            this.paid = new HashSet<>(paid);
            this.victory = victory;
        }
    }

    private static Set<UUID> readUuidSet(CompoundTag tag) {
        Set<UUID> result = new HashSet<>();
        for (String key : tag.keySet()) {
            try {
                result.add(UUID.fromString(key));
            } catch (IllegalArgumentException ignored) {
                RaceCore.LOGGER.warn("Invalid raid-reward player UUID '{}' in world save", key);
            }
        }
        return result;
    }

    private static CompoundTag writeUuidSet(Set<UUID> ids) {
        CompoundTag result = new CompoundTag();
        for (UUID id : ids) result.putBoolean(id.toString(), true);
        return result;
    }

    private static RaidRewardSavedData decode(Tag tag) {
        RaidRewardSavedData data = new RaidRewardSavedData();
        if (!(tag instanceof CompoundTag root)) return data;

        CompoundTag savedRaids = root.getCompoundOrEmpty("raids");
        for (String idText : savedRaids.keySet()) {
            try {
                int raidId = Integer.parseInt(idText);
                CompoundTag entry = savedRaids.getCompoundOrEmpty(idText);
                BlockPos center = BlockPos.of(entry.getLongOr("center", 0L));
                Set<UUID> eligible = readUuidSet(entry.getCompoundOrEmpty("eligible"));
                Set<UUID> paid = readUuidSet(entry.getCompoundOrEmpty("paid"));
                boolean won = entry.getBooleanOr("victory", false);
                data.raids.put(raidId, new RaidRecord(center, eligible, paid, won));
            } catch (NumberFormatException ignored) {
                RaceCore.LOGGER.warn("Invalid raid-reward ID '{}' in world save", idText);
            }
        }
        return data;
    }

    private CompoundTag save() {
        CompoundTag root = new CompoundTag();
        CompoundTag savedRaids = new CompoundTag();
        for (Map.Entry<Integer, RaidRecord> item : raids.entrySet()) {
            RaidRecord r = item.getValue();
            CompoundTag entry = new CompoundTag();
            entry.putLong("center", r.center.asLong());
            entry.put("eligible", writeUuidSet(r.eligible));
            entry.put("paid", writeUuidSet(r.paid));
            entry.putBoolean("victory", r.victory);
            savedRaids.put(Integer.toString(item.getKey()), entry);
        }
        root.put("raids", savedRaids);
        return root;
    }
}
