package com.collabflow.notification;

import java.time.format.DateTimeFormatter;

import com.collabflow.task.TaskEvents;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * Turns what happened to tasks into notifications.
 *
 * <p>{@code @TransactionalEventListener} runs these <em>after</em> the change is committed,
 * so a change that was rolled back never notifies anyone. The flip side: if the app dies
 * between the commit and this code, the notification is lost. Making that impossible needs
 * the outbox and a message queue, which come later.
 */
@Component
@RequiredArgsConstructor
class TaskNotifications {

    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("d MMM yyyy");

    private final NotificationService notifications;
    private final NotificationText text;

    @TransactionalEventListener
    void onAssigneesChanged(TaskEvents.AssigneesChanged event) {
        String actor = text.nameOf(event.actorId());
        notifications.notifyAll(event.added(), event.actorId(), NotificationType.TASK_ASSIGNED,
                "%s assigned you to %s: %s".formatted(actor, event.taskKey(), event.taskTitle()),
                NotificationText.taskLink(event.taskKey()));
        notifications.notifyAll(event.removed(), event.actorId(), NotificationType.TASK_UNASSIGNED,
                "%s removed you from %s: %s".formatted(actor, event.taskKey(), event.taskTitle()),
                NotificationText.taskLink(event.taskKey()));
    }

    @TransactionalEventListener
    void onStatusChanged(TaskEvents.StatusChanged event) {
        notifications.notifyAll(event.participants(), event.actorId(), NotificationType.TASK_STATUS_CHANGED,
                "%s moved %s from %s to %s".formatted(text.nameOf(event.actorId()), event.taskKey(),
                        event.from().label(), event.to().label()),
                NotificationText.taskLink(event.taskKey()));
    }

    @TransactionalEventListener
    void onOverdue(TaskEvents.Overdue event) {
        // No actor: nobody did this, the date simply passed.
        notifications.notifyAll(event.assignees(), null, NotificationType.TASK_OVERDUE,
                "%s is overdue. It was expected by %s: %s".formatted(event.taskKey(),
                        DATE.format(event.expectedDate()), event.taskTitle()),
                NotificationText.taskLink(event.taskKey()));
    }

    @TransactionalEventListener
    void onDeleted(TaskEvents.Deleted event) {
        notifications.notifyAll(event.participants(), event.actorId(), NotificationType.TASK_DELETED,
                "%s deleted %s: %s".formatted(text.nameOf(event.actorId()), event.taskKey(), event.taskTitle()),
                null); // no link: the task is gone
    }
}
