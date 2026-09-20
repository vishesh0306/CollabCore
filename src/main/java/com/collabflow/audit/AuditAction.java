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
    TAGGED_INTO_SPRINT,
    UNTAGGED_FROM_SPRINT,
    /** No longer written. Kept because entries from before sprints became tags still say it. */
    MOVED_TO_SPRINT,
    MEMBER_ADDED,
    MEMBER_REMOVED,
    ROLE_CHANGED,
    STARTED,
    COMPLETED,
    REOPENED
}
