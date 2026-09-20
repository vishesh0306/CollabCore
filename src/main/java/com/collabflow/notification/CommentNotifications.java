package com.collabflow.notification;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

import com.collabflow.comment.CommentEvents;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionalEventListener;

/** Turns new comments into notifications, for the people on the task and anyone mentioned. */
@Component
@RequiredArgsConstructor
class CommentNotifications {

    private final NotificationService notifications;
    private final NotificationText text;

    @TransactionalEventListener
    void onCommentAdded(CommentEvents.Added event) {
        String actor = text.nameOf(event.actorId());
        String link = NotificationText.taskLink(event.task().key());

        notifications.notifyAll(event.mentioned(), event.actorId(), NotificationType.MENTIONED,
                "%s mentioned you in a comment on %s".formatted(actor, event.task().key()), link);

        // People who were mentioned already got the more specific notification above,
        // so one comment never produces two entries for the same person.
        Set<UUID> others = new HashSet<>(event.participants());
        others.removeAll(event.mentioned());
        notifications.notifyAll(others, event.actorId(), NotificationType.TASK_COMMENTED,
                "%s commented on %s: %s".formatted(actor, event.task().key(), event.task().title()), link);
    }
}
