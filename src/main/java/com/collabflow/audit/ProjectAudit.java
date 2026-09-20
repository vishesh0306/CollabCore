package com.collabflow.audit;

import java.util.List;
import java.util.UUID;

import com.collabflow.project.ProjectEvents;
import com.collabflow.shared.FieldChange;
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
        record(event.projectId(), event.teamId(), event.code(), event.actorId(),
                AuditAction.CREATED, event.fields());
    }

    @EventListener
    void onUpdated(ProjectEvents.Updated event) {
        record(event.projectId(), event.teamId(), event.code(), event.actorId(),
                AuditAction.UPDATED, event.changes());
    }

    @EventListener
    void onCompleted(ProjectEvents.Completed event) {
        record(event.projectId(), event.teamId(), event.code(), event.actorId(),
                AuditAction.COMPLETED, List.of());
    }

    @EventListener
    void onReopened(ProjectEvents.Reopened event) {
        record(event.projectId(), event.teamId(), event.code(), event.actorId(),
                AuditAction.REOPENED, List.of());
    }

    private void record(UUID projectId, UUID teamId, String code, UUID actorId, AuditAction action,
                        List<FieldChange> changes) {
        audit.record(actorId, teamId, projectId, AuditEntityType.PROJECT, projectId, code, action, changes);
    }
}
