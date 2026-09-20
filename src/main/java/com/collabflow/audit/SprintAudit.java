package com.collabflow.audit;

import java.util.List;

import com.collabflow.sprint.SprintCompletedEvent;
import com.collabflow.sprint.SprintEvents;
import com.collabflow.sprint.SprintStartedEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/**
 * The log for sprints. Starting and completing reuse the same two events the notification and
 * task modules already listen for — the difference is only when each listener runs.
 */
@Component
@RequiredArgsConstructor
class SprintAudit {

    private final AuditService audit;

    @EventListener
    void onCreated(SprintEvents.Created event) {
        audit.record(event.actorId(), event.teamId(), AuditEntityType.SPRINT, event.sprintId(),
                event.name(), AuditAction.CREATED, event.fields());
    }

    @EventListener
    void onUpdated(SprintEvents.Updated event) {
        audit.record(event.actorId(), event.teamId(), AuditEntityType.SPRINT, event.sprintId(),
                event.name(), AuditAction.UPDATED, event.changes());
    }

    @EventListener
    void onStarted(SprintStartedEvent event) {
        audit.record(event.actorId(), event.teamId(), AuditEntityType.SPRINT, event.sprintId(),
                event.sprintName(), AuditAction.STARTED, List.of());
    }

    @EventListener
    void onCompleted(SprintCompletedEvent event) {
        audit.record(event.actorId(), event.teamId(), AuditEntityType.SPRINT, event.sprintId(),
                event.sprintName(), AuditAction.COMPLETED, List.of());
    }
}
