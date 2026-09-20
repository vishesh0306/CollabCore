package com.collabflow.audit;

import java.util.List;
import java.util.UUID;

import lombok.RequiredArgsConstructor;
import org.springframework.transaction.annotation.Transactional;

/** Stands in for a change that writes its log entry and then goes wrong. */
@RequiredArgsConstructor
class FailingWork {

    private final AuditService auditService;

    @Transactional
    public void logSomethingThenFail(UUID actorId, UUID teamId) {
        auditService.record(actorId, teamId, AuditEntityType.TASK, UUID.randomUUID(), "PAY-99",
                AuditAction.CREATED, List.of());
        throw new IllegalStateException("the change failed after the log was written");
    }
}
