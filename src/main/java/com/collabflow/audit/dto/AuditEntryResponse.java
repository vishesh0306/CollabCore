package com.collabflow.audit.dto;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import com.collabflow.audit.AuditAction;
import com.collabflow.audit.AuditEntityType;
import com.collabflow.audit.AuditEntry;
import com.collabflow.shared.FieldChange;

/** One line of the log: when, who, which item, what changed. */
public record AuditEntryResponse(long id,
                                 Instant at,
                                 Actor actor,
                                 AuditEntityType entityType,
                                 UUID entityId,
                                 String entityLabel,
                                 AuditAction action,
                                 List<FieldChange> changes) {

    /** Who did it. Null when nothing human did: a scheduled job, or a knock-on change. */
    public record Actor(UUID id, String name) {
    }

    public static AuditEntryResponse from(AuditEntry entry, Map<UUID, String> names) {
        Actor actor = entry.getActorId() == null
                ? null
                : new Actor(entry.getActorId(), names.getOrDefault(entry.getActorId(), "Unknown"));
        return new AuditEntryResponse(entry.getId(), entry.getAt(), actor, entry.getEntityType(),
                entry.getEntityId(), entry.getEntityLabel(), entry.getAction(), entry.getChanges());
    }
}
