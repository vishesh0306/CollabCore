package com.collabflow.notification;

import java.util.Set;

import com.collabflow.sprint.SprintStartedEvent;
import com.collabflow.team.TeamMemberAddedEvent;
import com.collabflow.team.TeamMemberService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionalEventListener;

/** Notifications about the team itself: joining it, and its sprints starting. */
@Component
@RequiredArgsConstructor
class TeamNotifications {

    private final NotificationService notifications;
    private final NotificationText text;
    private final TeamMemberService teamMemberService;

    @TransactionalEventListener
    void onMemberAdded(TeamMemberAddedEvent event) {
        notifications.notifyAll(Set.of(event.userId()), event.actorId(), NotificationType.ADDED_TO_TEAM,
                "%s added you to the team %s".formatted(text.nameOf(event.actorId()), event.teamName()),
                "/teams/" + event.teamId());
    }

    @TransactionalEventListener
    void onSprintStarted(SprintStartedEvent event) {
        // Everyone in the team hears about it, except whoever started it.
        notifications.notifyAll(teamMemberService.findMemberIds(event.teamId()), event.actorId(),
                NotificationType.SPRINT_STARTED,
                "%s started %s".formatted(text.nameOf(event.actorId()), event.sprintName()),
                "/sprints/" + event.sprintId());
    }
}
