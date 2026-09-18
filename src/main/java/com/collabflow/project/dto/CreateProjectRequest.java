package com.collabflow.project.dto;

import java.util.Locale;
import java.util.UUID;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record CreateProjectRequest(
        @NotBlank
        @Pattern(regexp = "[A-Z][A-Z0-9]{1,9}", message = "must be 2-10 letters or digits, starting with a letter")
        String code,
        @NotBlank @Size(max = 100) String name,
        @Size(max = 2000) String description,
        @NotNull UUID leadUserId) {

    /** Runs before validation, so "pay " is accepted and stored as "PAY". */
    public CreateProjectRequest {
        code = code == null ? null : code.trim().toUpperCase(Locale.ROOT);
        name = name == null ? null : name.trim();
    }
}
