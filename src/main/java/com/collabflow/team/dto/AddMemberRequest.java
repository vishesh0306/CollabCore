package com.collabflow.team.dto;

import com.collabflow.team.TeamRole;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record AddMemberRequest(@NotBlank @Email String email, @NotNull TeamRole role) {

    public AddMemberRequest {
        email = email == null ? null : email.trim();
    }
}
