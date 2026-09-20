package com.collabflow.audit;

import java.util.List;

import com.collabflow.comment.CommentEvents;
import lombok.RequiredArgsConstructor;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/**
 * The log for comments. The entry is filed under the comment and labelled with its task, so a
 * task's history reads as a comment on PAY-12 without the text being copied in here.
 */
@Component
@RequiredArgsConstructor
class CommentAudit {

    private final AuditService audit;

    @EventListener
    void onAdded(CommentEvents.Added event) {
        audit.record(event.actorId(), event.teamId(), AuditEntityType.COMMENT, event.commentId(),
                event.taskKey(), AuditAction.CREATED, List.of());
    }

    @EventListener
    void onEdited(CommentEvents.Edited event) {
        audit.record(event.actorId(), event.teamId(), AuditEntityType.COMMENT, event.commentId(),
                event.taskKey(), AuditAction.UPDATED, List.of());
    }

    @EventListener
    void onDeleted(CommentEvents.Deleted event) {
        audit.record(event.actorId(), event.teamId(), AuditEntityType.COMMENT, event.commentId(),
                event.taskKey(), AuditAction.DELETED, List.of());
    }
}
