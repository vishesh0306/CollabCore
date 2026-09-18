package com.collabflow.shared;

import java.util.UUID;

import org.springframework.security.oauth2.jwt.Jwt;

/** Reads who is calling from the login token that Spring Security has already verified. */
public final class CurrentUser {

    private CurrentUser() {
    }

    /** The token's "subject" is the user's id (see TokenService). */
    public static UUID id(Jwt jwt) {
        return UUID.fromString(jwt.getSubject());
    }
}
