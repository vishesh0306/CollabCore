package com.collabflow.notification;

import java.util.UUID;

import com.collabflow.identity.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/** Small pieces the notification listeners share when writing messages. */
@Component
@RequiredArgsConstructor
class NotificationText {

    private final UserService userService;

    String nameOf(UUID userId) {
        return userService.getById(userId).getName();
    }

    static String taskLink(String taskKey) {
        return "/tasks/" + taskKey;
    }
}
