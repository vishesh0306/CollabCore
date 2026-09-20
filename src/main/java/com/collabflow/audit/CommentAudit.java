package com.collabflow.audit;

import java.util.List;
import java.util.UUID;

import com.collabflow.comment.CommentEvents;
import com.collabflow.task.TaskRef;
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
        record(event.commentId(), event.task(), event.actorId(), AuditAction.CREATED);
    }

    @EventListener
    void onEdited(CommentEvents.Edited event) {
        record(event.commentId(), event.task(), event.actorId(), AuditAction.UPDATED);
    }

    @EventListener
    void onDeleted(CommentEvents.Deleted event) {
        record(event.commentId(), event.task(), event.actorId(), AuditAction.DELETED);
    }

    private void record(UUID commentId, TaskRef task, UUID actorId, AuditAction action) {
        audit.record(actorId, task.teamId(), task.projectId(), AuditEntityType.COMMENT, commentId,
                task.key(), action, List.of());
    }
}
