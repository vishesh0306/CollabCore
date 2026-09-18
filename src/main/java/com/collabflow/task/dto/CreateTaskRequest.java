package com.collabflow.task.dto;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** A new task starts as TO_DO. Assignees are optional; each must be a member of the team. */
public record CreateTaskRequest(
        @NotBlank @Size(max = 200) String title,
        @Size(max = 10000) String description,
        LocalDate expectedDate,
        List<UUID> assigneeIds) {

    public CreateTaskRequest {
        title = title == null ? null : title.trim();
    }
}
