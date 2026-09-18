package com.collabflow.identity;

import java.nio.charset.StandardCharsets;
import java.time.Duration;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;
import org.springframework.validation.annotation.Validated;

/**
 * Settings under {@code collabflow.jwt.*}. The secret has no default on purpose: without it
 * the app refuses to start. Set it in config/application.properties or as COLLABFLOW_JWT_SECRET.
 */
@Validated
@ConfigurationProperties("collabflow.jwt")
public record JwtProperties(
        @NotBlank @Size(min = 32, message = "must be at least 32 characters") String secret,
        @DefaultValue("24h") Duration expiry) {

    public static final String ISSUER = "collabflow";

    /** The key used to sign tokens (HMAC-SHA256 needs at least 32 bytes). */
    public SecretKey secretKey() {
        return new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
    }
}
