package com.collabflow.identity.dto;

/** Returned by login. The client sends it back as the header {@code Authorization: Bearer <accessToken>}. */
public record TokenResponse(String accessToken, String tokenType, long expiresInSeconds) {
}
