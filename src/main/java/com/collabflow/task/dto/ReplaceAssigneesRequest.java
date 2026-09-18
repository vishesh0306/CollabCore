package com.collabflow.task.dto;

import java.util.List;
import java.util.UUID;

import jakarta.validation.constraints.NotNull;

/** The complete new list of assignees. An empty list unassigns everyone. */
public record ReplaceAssigneesRequest(@NotNull List<UUID> assigneeIds) {
}
