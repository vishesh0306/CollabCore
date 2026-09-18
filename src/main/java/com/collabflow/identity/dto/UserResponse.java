package com.collabflow.identity.dto;

import java.time.Instant;
import java.util.UUID;

import com.collabflow.identity.User;

/** What the API shows about a user. The password hash is never included. */
public record UserResponse(UUID id, String name, String email, boolean admin, Instant createdAt) {

    public static UserResponse from(User user) {
        return new UserResponse(user.getId(), user.getName(), user.getEmail(), user.isAdmin(), user.getCreatedAt());
    }
}
