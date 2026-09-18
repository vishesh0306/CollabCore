package com.collabflow.identity;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/**
 * Creates the single admin account when the app starts for the first time.
 *
 * <p>If an admin already exists, it does nothing, so restarting never resets the admin's
 * password. The admin changes their own credentials through the normal API afterwards.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AdminBootstrap implements ApplicationRunner {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AdminProperties adminProperties;

    @Override
    public void run(ApplicationArguments args) {
        if (userRepository.existsByAdminTrue()) {
            return;
        }
        if (!StringUtils.hasText(adminProperties.email()) || !StringUtils.hasText(adminProperties.password())) {
            throw new IllegalStateException("No admin account exists yet. Set collabflow.admin.email and "
                    + "collabflow.admin.password (e.g. in config/application.properties) and start again.");
        }
        String email = User.normalizeEmail(adminProperties.email());
        if (userRepository.existsByEmail(email)) {
            throw new IllegalStateException("Can't create the admin: " + email + " is already a normal user.");
        }

        User admin = new User(adminProperties.name(), email, passwordEncoder.encode(adminProperties.password()), true);
        try {
            userRepository.saveAndFlush(admin);
            log.info("Created the admin account {}", email);
        } catch (DataIntegrityViolationException e) {
            // Two app instances started at the same moment and both tried to create the admin.
            // The database's "only one admin" index let just one of them succeed, which is fine.
            log.info("The admin account was created by another instance");
        }
    }
}
