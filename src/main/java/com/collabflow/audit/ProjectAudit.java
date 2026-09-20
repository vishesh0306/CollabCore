package com.collabflow.audit;

import java.util.List;

import com.collabflow.project.ProjectEvents;
import lombok.RequiredArgsConstructor;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/** The log for projects. Inside the transaction, like the rest of the audit listeners. */
@Component
@RequiredArgsConstructor
class ProjectAudit {

    private final AuditService audit;

    @EventListener
    void onCreated(ProjectEvents.Created event) {
        audit.record(event.actorId(), event.teamId(), AuditEntityType.PROJECT, event.projectId(),
                event.code(), AuditAction.CREATED, event.fields());
    }

    @EventListener
    void onUpdated(ProjectEvents.Updated event) {
        audit.record(event.actorId(), event.teamId(), AuditEntityType.PROJECT, event.projectId(),
                event.code(), AuditAction.UPDATED, event.changes());
    }

    @EventListener
    void onCompleted(ProjectEvents.Completed event) {
        audit.record(event.actorId(), event.teamId(), AuditEntityType.PROJECT, event.projectId(),
                event.code(), AuditAction.COMPLETED, List.of());
    }

    @EventListener
    void onReopened(ProjectEvents.Reopened event) {
        audit.record(event.actorId(), event.teamId(), AuditEntityType.PROJECT, event.projectId(),
                event.code(), AuditAction.REOPENED, List.of());
    }
}
