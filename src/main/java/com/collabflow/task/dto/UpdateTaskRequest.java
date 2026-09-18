package com.collabflow.task.dto;

import java.time.LocalDate;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Replaces the task's details. Leaving description or expectedDate out (null) clears it. */
public record UpdateTaskRequest(
        @NotBlank @Size(max = 200) String title,
        @Size(max = 10000) String description,
        LocalDate expectedDate) {

    public UpdateTaskRequest {
        title = title == null ? null : title.trim();
    }
}
