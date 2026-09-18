package com.collabflow.identity;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

/**
 * Settings under {@code collabflow.admin.*}, used only to create the admin account on the
 * very first start. Set them in config/application.properties or as COLLABFLOW_ADMIN_EMAIL
 * and COLLABFLOW_ADMIN_PASSWORD.
 */
@ConfigurationProperties("collabflow.admin")
public record AdminProperties(String email, String password, @DefaultValue("Admin") String name) {
}
