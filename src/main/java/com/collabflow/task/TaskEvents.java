package com.collabflow.task;

import java.time.LocalDate;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import com.collabflow.shared.FieldChange;

/**
 * What happened to a task. Two listeners read these, and they read them differently:
 * notifications after the commit (a lost one is a nuisance), the audit log inside the same
 * transaction (a lost one would be a lie). Neither module is known to the task code.
 *
 * <p>Each event carries plain data, not entities: the notification listener runs when the
 * database session that could load an entity's details is already gone.
 */
public final class TaskEvents {

    private TaskEvents() {
    }

    public record Created(TaskRef task, UUID actorId, List<FieldChange> fields) {
    }

    public record DetailsUpdated(TaskRef task, UUID actorId, List<FieldChange> changes) {
    }

    public record AssigneesChanged(TaskRef task, UUID actorId, Set<UUID> added, Set<UUID> removed) {
    }

    public record StatusChanged(TaskRef task, UUID actorId, TaskStatus from, TaskStatus to,
                                Set<UUID> participants) {
    }

    /** Sprint names, or "Backlog" for no sprint. No actor when a sprint ending caused the move. */
    public record MovedToSprint(TaskRef task, UUID actorId, String fromSprint, String toSprint) {
    }

    /** Nobody caused this one: the expected date simply passed. */
    public record Overdue(TaskRef task, LocalDate expectedDate, Set<UUID> assignees) {
    }

    public record Deleted(TaskRef task, UUID actorId, Set<UUID> participants) {
    }
}
