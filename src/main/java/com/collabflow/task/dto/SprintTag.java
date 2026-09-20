package com.collabflow.task.dto;

import java.util.UUID;

/** One of the sprints a task is tagged into. */
public record SprintTag(UUID id, String name) {
}
