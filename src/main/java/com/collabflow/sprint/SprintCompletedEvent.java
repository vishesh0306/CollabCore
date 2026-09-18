package com.collabflow.sprint;

import java.util.UUID;

/**
 * Announced when a sprint is completed. The sprint module doesn't know about tasks; the task
 * module listens for this and sends unfinished tasks back to their backlog.
 */
public record SprintCompletedEvent(UUID sprintId, UUID teamId) {
}
