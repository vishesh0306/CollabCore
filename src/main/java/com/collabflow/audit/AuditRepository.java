package com.collabflow.audit;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Only reads and inserts are ever used. Deleting is not just unused but impossible: the
 * database rejects it (see the V8 migration).
 */
public interface AuditRepository extends JpaRepository<AuditEntry, Long> {

    /** One item's history, newest first: the task page (TSK-8) and the sprint and project views. */
    List<AuditEntry> findByEntityTypeAndEntityIdOrderByIdDesc(AuditEntityType entityType, UUID entityId);
}
