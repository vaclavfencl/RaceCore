package com.example.racecore.world.encounter;

import com.example.racecore.RaceCore;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.server.ServerStoppingEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

/**
 * Shared runtime owner for all RaceCore encounters.
 *
 * Phase 12.5 only registers PlayerHuntEncounter, but dungeon rooms, waves and
 * bosses can register other Encounter subclasses later and reuse the same
 * membership/lifecycle/cleanup infrastructure.
 */
@EventBusSubscriber(modid = RaceCore.MODID)
public final class EncounterManager {
    public static final String ENCOUNTER_MEMBER_TAG = "racecore_encounter_member";
    private static final String ENCOUNTER_ID_TAG_PREFIX = "racecore_encounter_";

    private static final Map<UUID, Encounter> ACTIVE = new LinkedHashMap<>();

    private EncounterManager() {
    }

    public static synchronized void register(Encounter encounter) {
        ACTIVE.put(encounter.id(), encounter);
    }

    public static synchronized boolean hasActiveHuntForTarget(UUID targetId) {
        for (Encounter encounter : ACTIVE.values()) {
            if (encounter instanceof PlayerHuntEncounter hunt
                    && !hunt.isFinished()
                    && hunt.targetId().equals(targetId)) {
                return true;
            }
        }

        return false;
    }

    public static String encounterTag(UUID encounterId) {
        return ENCOUNTER_ID_TAG_PREFIX + encounterId;
    }

    @SubscribeEvent
    public static void onServerTick(ServerTickEvent.Post event) {
        List<Encounter> snapshot;

        synchronized (EncounterManager.class) {
            snapshot = List.copyOf(ACTIVE.values());
        }

        for (Encounter encounter : snapshot) {
            encounter.tickManaged();
        }

        synchronized (EncounterManager.class) {
            ACTIVE.values().removeIf(Encounter::isFinished);
        }
    }

    @SubscribeEvent
    public static synchronized void onServerStopping(ServerStoppingEvent event) {
        for (Encounter encounter : List.copyOf(ACTIVE.values())) {
            encounter.finish(false);
        }

        ACTIVE.clear();
    }
}
