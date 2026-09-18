package com.collabflow.identity.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** The current password is required, so a stolen token alone can't take over the account. */
public record ChangeEmailRequest(
        @NotBlank @Email @Size(max = 255) String newEmail,
        @NotBlank String currentPassword) {

    public ChangeEmailRequest {
        newEmail = newEmail == null ? null : newEmail.trim();
    }
}
