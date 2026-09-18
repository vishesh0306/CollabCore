package com.collabflow.sprint.dto;

import java.time.LocalDate;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/** Used both to create a sprint and to change its details. */
public record SprintRequest(
        @NotBlank @Size(max = 100) String name,
        @Size(max = 2000) String target,
        @NotNull LocalDate startDate,
        @NotNull LocalDate endDate) {

    public SprintRequest {
        name = name == null ? null : name.trim();
    }
}
