package com.collabflow.comment;

import java.util.Set;
import java.util.UUID;

/**
 * What happened to a comment. Notifications react to a new one after the commit; the audit log
 * records all three inside the transaction.
 *
 * <p>The text itself is never carried into the log: the log says a comment was written, edited
 * or deleted, and the comment table holds what it says.
 */
public final class CommentEvents {

    private CommentEvents() {
    }

    public record Added(UUID commentId, UUID teamId, String taskKey, String taskTitle, UUID actorId,
                        Set<UUID> participants, Set<UUID> mentioned) {
    }

    public record Edited(UUID commentId, UUID teamId, String taskKey, UUID actorId) {
    }

    public record Deleted(UUID commentId, UUID teamId, String taskKey, UUID actorId) {
    }
}
