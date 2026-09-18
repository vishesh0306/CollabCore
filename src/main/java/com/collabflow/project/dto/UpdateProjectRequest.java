package com.collabflow.project.dto;

import java.util.UUID;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/** The code isn't here on purpose: it can't be changed once the project exists. */
public record UpdateProjectRequest(
        @NotBlank @Size(max = 100) String name,
        @Size(max = 2000) String description,
        @NotNull UUID leadUserId) {

    public UpdateProjectRequest {
        name = name == null ? null : name.trim();
    }
}
