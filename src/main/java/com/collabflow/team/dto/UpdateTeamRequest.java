package com.collabflow.team.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UpdateTeamRequest(
        @NotBlank @Size(max = 100) String name,
        @Size(max = 1000) String description) {

    public UpdateTeamRequest {
        name = name == null ? null : name.trim();
    }
}
