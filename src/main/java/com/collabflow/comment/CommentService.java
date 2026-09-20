package com.collabflow.comment;

import java.util.Set;
import java.util.UUID;

import com.collabflow.comment.dto.CommentRequest;
import com.collabflow.comment.dto.CommentResponse;
import com.collabflow.identity.User;
import com.collabflow.identity.UserService;
import com.collabflow.project.ProjectService;
import com.collabflow.shared.PageResponse;
import com.collabflow.shared.error.ForbiddenException;
import com.collabflow.shared.error.NotFoundException;
import com.collabflow.task.Task;
import com.collabflow.task.TaskRef;
import com.collabflow.task.TaskService;
import com.collabflow.team.TeamAccess;
import com.collabflow.team.TeamMemberService;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Comments on tasks. Anyone who can see a task can comment on it. Only the author can edit a
 * comment; the author, the team's managers and the admin can delete it. Like tasks, comments
 * of a completed project can't be changed.
 */
@Service
@RequiredArgsConstructor
public class CommentService {

    private final CommentRepository commentRepository;
    private final TaskService taskService;
    private final TeamAccess teamAccess;
    private final UserService userService;
    private final TeamMemberService teamMemberService;
    private final ApplicationEventPublisher events;

    @Transactional
    public CommentResponse addComment(UUID callerId, String taskKey, CommentRequest request) {
        Task task = taskService.findVisibleTask(taskKey, callerId);
        ProjectService.requireActive(task.getProject());
        User author = userService.getById(callerId);

        Comment comment = commentRepository.saveAndFlush(new Comment(task, author, request.body()));

        Set<UUID> mentioned = Mentions.findIn(request.body(), teamMemberService.findMembers(task.getTeamId()));
        events.publishEvent(new CommentEvents.Added(comment.getId(), TaskRef.of(task), callerId,
                task.participantIds(), mentioned));
        return CommentResponse.from(comment);
    }

    /** A task's comments, oldest first. The order is fixed, so only page and size are taken from the request. */
    @Transactional(readOnly = true)
    public PageResponse<CommentResponse> listComments(UUID callerId, String taskKey, Pageable pageable) {
        Task task = taskService.findVisibleTask(taskKey, callerId);
        Pageable oldestFirst = PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(), Sort.by("createdAt"));
        return PageResponse.from(commentRepository.findByTaskId(task.getId(), oldestFirst).map(CommentResponse::from));
    }

    @Transactional
    public CommentResponse editComment(UUID callerId, UUID commentId, CommentRequest request) {
        Comment comment = findVisibleComment(commentId, callerId);
        if (!comment.isWrittenBy(callerId)) {
            throw new ForbiddenException("Only the author can edit a comment");
        }
        ProjectService.requireActive(comment.getTask().getProject());
        comment.edit(request.body());
        events.publishEvent(new CommentEvents.Edited(comment.getId(), TaskRef.of(comment.getTask()), callerId));
        return CommentResponse.from(comment);
    }

    @Transactional
    public void deleteComment(UUID callerId, UUID commentId) {
        Comment comment = findVisibleComment(commentId, callerId);
        if (!comment.isWrittenBy(callerId) && !teamAccess.canManage(comment.getTask().getTeamId(), callerId)) {
            throw new ForbiddenException("Only the author or a manager can delete a comment");
        }
        ProjectService.requireActive(comment.getTask().getProject());
        comment.delete();
        events.publishEvent(new CommentEvents.Deleted(comment.getId(), TaskRef.of(comment.getTask()), callerId));
    }

    /** 404 unless the comment (and its task) exists and the caller may see the task's team. */
    private Comment findVisibleComment(UUID commentId, UUID callerId) {
        return commentRepository.findWithLiveTask(commentId)
                .filter(comment -> teamAccess.canView(comment.getTask().getTeamId(), callerId))
                .orElseThrow(() -> new NotFoundException("Comment not found"));
    }
}
