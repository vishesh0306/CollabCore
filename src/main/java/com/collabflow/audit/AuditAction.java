package com.collabflow.audit;

/**
 * What was done. Read together with the entity type: TASK + STATUS_CHANGED, SPRINT + STARTED.
 * One shared list keeps the log filterable by "everything that was created", across all types.
 */
public enum AuditAction {
    CREATED,
    UPDATED,
    DELETED,
    STATUS_CHANGED,
    ASSIGNEES_CHANGED,
    MOVED_TO_SPRINT,
    MEMBER_ADDED,
    MEMBER_REMOVED,
    ROLE_CHANGED,
    STARTED,
    COMPLETED,
    REOPENED
}
