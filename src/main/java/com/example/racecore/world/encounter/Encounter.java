package com.example.racecore.world.encounter;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.UUID;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;

/**
 * Generic server-side encounter instance.
 *
 * The manager owns ticking/lifetime; subclasses own encounter-specific logic.
 * Participants are UUID-based so a future dungeon/boss encounter can track
 * arbitrary spawned entities without introducing another ownership system.
 */
public abstract class Encounter {
    private final UUID id = UUID.randomUUID();
    private final ServerLevel level;
    private final Set<UUID> participants = new LinkedHashSet<>();
    private EncounterState state = EncounterState.SPAWNING;

    protected Encounter(ServerLevel level) {
        this.level = level;
    }

    public final UUID id() {
        return id;
    }

    public final ServerLevel level() {
        return level;
    }

    public final EncounterState state() {
        return state;
    }

    protected final void setState(EncounterState state) {
        this.state = state;
    }

    public final boolean isFinished() {
        return state == EncounterState.FINISHED;
    }

    public final Set<UUID> participants() {
        return Collections.unmodifiableSet(participants);
    }

    public void addParticipant(Entity entity) {
        participants.add(entity.getUUID());
        entity.addTag(EncounterManager.ENCOUNTER_MEMBER_TAG);
        entity.addTag(EncounterManager.encounterTag(id));
    }

    public final void removeParticipant(UUID entityId) {
        participants.remove(entityId);
    }

    final void tickManaged() {
        if (!isFinished()) {
            tickEncounter();
        }
    }

    protected abstract void tickEncounter();

    public void finish(boolean discardRemainingParticipants) {
        if (isFinished()) {
            return;
        }

        setState(EncounterState.FINISHED);

        if (discardRemainingParticipants) {
            for (UUID participantId : Set.copyOf(participants)) {
                Entity entity = level.getEntity(participantId);
                if (entity != null) {
                    entity.discard();
                }
            }
        }

        participants.clear();
    }
}
