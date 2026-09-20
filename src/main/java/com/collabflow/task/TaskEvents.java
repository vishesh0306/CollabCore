package com.collabflow.task;

import java.util.Set;
import java.util.UUID;

/**
 * What happened to a task. Other features (notifications now; the audit log, live updates
 * and email later) listen for these instead of the task code calling them one by one.
 *
 * <p>Each event carries plain data, not entities: listeners run after the change is
 * committed, when the database session that could load an entity's details is already gone.
 */
public final class TaskEvents {

    private TaskEvents() {
    }

    public record AssigneesChanged(String taskKey, String taskTitle, UUID actorId,
                                   Set<UUID> added, Set<UUID> removed) {
    }

    public record StatusChanged(String taskKey, String taskTitle, UUID actorId,
                                TaskStatus from, TaskStatus to, Set<UUID> participants) {
    }

    public record Deleted(String taskKey, String taskTitle, UUID actorId, Set<UUID> participants) {
    }
}
