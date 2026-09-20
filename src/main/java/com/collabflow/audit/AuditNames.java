package com.collabflow.audit;

import java.util.Collection;
import java.util.UUID;
import java.util.stream.Collectors;

import com.collabflow.identity.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/** Turns ids into the names the log should show. */
@Component
@RequiredArgsConstructor
class AuditNames {

    private final UserService userService;

    String of(UUID userId) {
        return userService.getById(userId).getName();
    }

    String of(Collection<UUID> userIds) {
        return userIds.stream().map(this::of).sorted().collect(Collectors.joining(", "));
    }
}
