package com.collabflow.audit;

import java.util.List;
import java.util.UUID;

import com.collabflow.shared.FieldChange;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * Writes audit entries.
 *
 * <p>{@code MANDATORY} is the point of this whole feature: this method refuses to run unless a
 * transaction is already open, which means the entry is always part of the change it describes.
 * They commit together or not at all (LOG-4). Compare with notifications, which deliberately do
 * the opposite and use REQUIRES_NEW after the commit.
 */
@Service
@RequiredArgsConstructor
public class AuditService {

    private final AuditRepository auditRepository;

    @Transactional(propagation = Propagation.MANDATORY)
    public void record(UUID actorId, UUID teamId, UUID projectId, AuditEntityType entityType, UUID entityId,
                       String entityLabel, AuditAction action, List<FieldChange> changes) {
        auditRepository.save(new AuditEntry(actorId, teamId, projectId, entityType, entityId, entityLabel,
                action, changes));
    }

    /** For changes that belong to no project: teams and who is in them. */
    @Transactional(propagation = Propagation.MANDATORY)
    public void record(UUID actorId, UUID teamId, AuditEntityType entityType, UUID entityId,
                       String entityLabel, AuditAction action, List<FieldChange> changes) {
        record(actorId, teamId, null, entityType, entityId, entityLabel, action, changes);
    }
}
