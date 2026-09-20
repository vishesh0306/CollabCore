package com.collabflow.comment;

import java.util.Set;
import java.util.UUID;

/** What happened to comments. Listeners (notifications now, more later) react to these. */
public final class CommentEvents {

    private CommentEvents() {
    }

    /**
     * @param participants the task's creator and assignees
     * @param mentioned    team members named with @ in the comment
     */
    public record Added(String taskKey, String taskTitle, UUID actorId,
                        Set<UUID> participants, Set<UUID> mentioned) {
    }
}
