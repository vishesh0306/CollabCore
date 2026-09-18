package com.collabflow.comment;

import java.util.UUID;

import com.collabflow.comment.dto.CommentRequest;
import com.collabflow.comment.dto.CommentResponse;
import com.collabflow.shared.CurrentUser;
import com.collabflow.shared.PageResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/** Comments are written and listed under a task (by its key), then edited or deleted by their own id. */
@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class CommentController {

    private final CommentService commentService;

    @PostMapping("/tasks/{key}/comments")
    @ResponseStatus(HttpStatus.CREATED)
    public CommentResponse addComment(@AuthenticationPrincipal Jwt jwt, @PathVariable String key,
                                      @Valid @RequestBody CommentRequest request) {
        return commentService.addComment(CurrentUser.id(jwt), key, request);
    }

    @GetMapping("/tasks/{key}/comments")
    public PageResponse<CommentResponse> listComments(@AuthenticationPrincipal Jwt jwt, @PathVariable String key,
                                                      @ParameterObject @PageableDefault(size = 20) Pageable pageable) {
        return commentService.listComments(CurrentUser.id(jwt), key, pageable);
    }

    @PutMapping("/comments/{commentId}")
    public CommentResponse editComment(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID commentId,
                                       @Valid @RequestBody CommentRequest request) {
        return commentService.editComment(CurrentUser.id(jwt), commentId, request);
    }

    @DeleteMapping("/comments/{commentId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteComment(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID commentId) {
        commentService.deleteComment(CurrentUser.id(jwt), commentId);
    }
}
