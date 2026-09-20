package com.collabflow.sprint;

import java.util.UUID;

/**
 * Announced when a sprint is completed. The sprint module doesn't know about tasks; the task
 * module listens for this and sends unfinished tasks back to their backlog, and the audit log
 * writes it all down in the same transaction.
 */
public record SprintCompletedEvent(UUID sprintId, UUID teamId, String sprintName, UUID actorId,
                                   UUID carryOverToSprintId) {
}
