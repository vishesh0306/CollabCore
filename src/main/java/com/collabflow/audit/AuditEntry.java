package com.collabflow.audit;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import com.collabflow.shared.FieldChange;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/**
 * One thing that happened. Written once and never touched again: there is no setter, no update
 * method, and the database refuses UPDATE and DELETE on this table.
 */
@Entity
@Table(name = "audit_log")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class AuditEntry {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @CreationTimestamp
    private Instant at;

    /** Null when nothing human caused it: a scheduled job, or a cascade from another change. */
    private UUID actorId;

    private UUID teamId;

    /** Set for projects, tasks and comments; empty for teams and memberships. */
    private UUID projectId;

    @Enumerated(EnumType.STRING)
    private AuditEntityType entityType;

    private UUID entityId;

    private String entityLabel;

    @Enumerated(EnumType.STRING)
    private AuditAction action;

    /** Stored as JSON, because each kind of change has different fields. */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb")
    private List<FieldChange> changes;

    AuditEntry(UUID actorId, UUID teamId, UUID projectId, AuditEntityType entityType, UUID entityId,
               String entityLabel, AuditAction action, List<FieldChange> changes) {
        this.actorId = actorId;
        this.teamId = teamId;
        this.projectId = projectId;
        this.entityType = entityType;
        this.entityId = entityId;
        this.entityLabel = entityLabel;
        this.action = action;
        this.changes = changes == null ? List.of() : List.copyOf(changes);
    }
}
