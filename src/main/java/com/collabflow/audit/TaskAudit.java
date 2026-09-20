package com.collabflow.audit;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import com.collabflow.shared.FieldChange;
import com.collabflow.task.TaskEvents;
import com.collabflow.task.TaskRef;
import lombok.RequiredArgsConstructor;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/**
 * Writes the log for everything that happens to a task.
 *
 * <p>Plain {@code @EventListener}, not {@code @TransactionalEventListener}: this runs the moment
 * the event is published, inside the transaction that is making the change. So the entry and the
 * change are one commit, and a failure here takes the change down with it. That is the rule the
 * requirements ask for, not an accident.
 */
@Component
@RequiredArgsConstructor
class TaskAudit {

    private final AuditService audit;
    private final AuditNames names;

    @EventListener
    void onCreated(TaskEvents.Created event) {
        record(event.task(), event.actorId(), AuditAction.CREATED, event.fields());
    }

    @EventListener
    void onDetailsUpdated(TaskEvents.DetailsUpdated event) {
        record(event.task(), event.actorId(), AuditAction.UPDATED, event.changes());
    }

    @EventListener
    void onStatusChanged(TaskEvents.StatusChanged event) {
        record(event.task(), event.actorId(), AuditAction.STATUS_CHANGED,
                List.of(FieldChange.of("status", event.from(), event.to())));
    }

    @EventListener
    void onAssigneesChanged(TaskEvents.AssigneesChanged event) {
        List<FieldChange> changes = new ArrayList<>();
        if (!event.added().isEmpty()) {
            changes.add(FieldChange.set("assigneeAdded", names.of(event.added())));
        }
        if (!event.removed().isEmpty()) {
            changes.add(FieldChange.of("assigneeRemoved", names.of(event.removed()), null));
        }
        record(event.task(), event.actorId(), AuditAction.ASSIGNEES_CHANGED, changes);
    }

    @EventListener
    void onSprintTagged(TaskEvents.SprintTagged event) {
        record(event.task(), event.actorId(), AuditAction.TAGGED_INTO_SPRINT,
                List.of(FieldChange.set("sprint", event.sprintName())));
    }

    @EventListener
    void onSprintUntagged(TaskEvents.SprintUntagged event) {
        record(event.task(), event.actorId(), AuditAction.UNTAGGED_FROM_SPRINT,
                List.of(FieldChange.of("sprint", event.sprintName(), null)));
    }

    @EventListener
    void onDeleted(TaskEvents.Deleted event) {
        record(event.task(), event.actorId(), AuditAction.DELETED, List.of());
    }

    private void record(TaskRef task, UUID actorId, AuditAction action, List<FieldChange> changes) {
        audit.record(actorId, task.teamId(), task.projectId(), AuditEntityType.TASK, task.id(),
                task.key(), action, changes);
    }
}
