package com.collabflow.team.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** A team starts with one manager; more people are added afterwards. */
public record CreateTeamRequest(
        @NotBlank @Size(max = 100) String name,
        @Size(max = 1000) String description,
        @NotBlank @Email String managerEmail) {

    public CreateTeamRequest {
        name = name == null ? null : name.trim();
        managerEmail = managerEmail == null ? null : managerEmail.trim();
    }
}
