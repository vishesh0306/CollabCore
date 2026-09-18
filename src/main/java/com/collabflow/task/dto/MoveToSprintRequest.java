package com.collabflow.task.dto;

import java.util.UUID;

/** The sprint to put the task into, or null to send it back to its project's backlog. */
public record MoveToSprintRequest(UUID sprintId) {
}
