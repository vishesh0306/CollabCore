package com.collabflow.audit;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;

/**
 * Which entries to read. Every field is optional.
 *
 * <p>Only the filters that were actually asked for become conditions. The tempting shortcut,
 * one fixed query with {@code (:teamId is null or team_id = :teamId)} for each filter, reads
 * badly for the database: it can't use an index on a column hidden behind an OR, and with a
 * null parameter PostgreSQL can't even work out the type ("could not determine data type").
 *
 * <p>{@code before} is the keyset cursor: everything older than that entry.
 */
record AuditFilters(UUID teamId,
                    UUID projectId,
                    UUID actorId,
                    AuditEntityType entityType,
                    UUID entityId,
                    AuditAction action,
                    Instant from,
                    Instant to,
                    Long before) {

    static AuditFilters forTeam(UUID teamId, Long before) {
        return new AuditFilters(teamId, null, null, null, null, null, null, null, before);
    }

    static AuditFilters forProject(UUID projectId, Long before) {
        return new AuditFilters(null, projectId, null, null, null, null, null, null, before);
    }

    Specification<AuditEntry> toSpecification() {
        return (root, query, builder) -> {
            List<Predicate> conditions = new ArrayList<>();
            if (teamId != null) {
                conditions.add(builder.equal(root.get("teamId"), teamId));
            }
            if (projectId != null) {
                conditions.add(builder.equal(root.get("projectId"), projectId));
            }
            if (actorId != null) {
                conditions.add(builder.equal(root.get("actorId"), actorId));
            }
            if (entityType != null) {
                conditions.add(builder.equal(root.get("entityType"), entityType));
            }
            if (entityId != null) {
                conditions.add(builder.equal(root.get("entityId"), entityId));
            }
            if (action != null) {
                conditions.add(builder.equal(root.get("action"), action));
            }
            if (from != null) {
                conditions.add(builder.greaterThanOrEqualTo(root.get("at"), from));
            }
            if (to != null) {
                conditions.add(builder.lessThan(root.get("at"), to));
            }
            if (before != null) {
                conditions.add(builder.lessThan(root.get("id"), before));
            }
            return builder.and(conditions.toArray(new Predicate[0]));
        };
    }
}
