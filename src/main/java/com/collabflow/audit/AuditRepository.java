package com.collabflow.audit;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

/**
 * Only reads and inserts are ever used. Deleting is not just unused but impossible: the
 * database rejects it (see the V8 migration).
 *
 * <p>Lists are paged by "older than this id" rather than by page number. Page numbers mean
 * OFFSET, and OFFSET 100000 makes the database walk and throw away a hundred thousand rows
 * before reaching the ones asked for. This table is the one that grows without limit, so it is
 * where that matters most.
 */
public interface AuditRepository extends JpaRepository<AuditEntry, Long>,
        JpaSpecificationExecutor<AuditEntry> {

    /** One item's history, newest first. */
    List<AuditEntry> findByEntityTypeAndEntityIdOrderByIdDesc(AuditEntityType entityType, UUID entityId);

    /**
     * Everything that ever happened to one task, found by the label it was called at the time
     * ("PAY-12"), including its comments. Looked up by label rather than by task id on purpose:
     * the history has to survive the task being deleted, and a deleted task can't be loaded.
     */
    List<AuditEntry> findByEntityLabelAndEntityTypeInOrderByIdDesc(String entityLabel,
                                                                   Collection<AuditEntityType> entityTypes);
}
