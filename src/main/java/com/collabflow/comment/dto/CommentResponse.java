package com.collabflow.comment.dto;

import java.time.Instant;
import java.util.UUID;

import com.collabflow.comment.Comment;

/** {@code editedAt} is null if the comment was never edited. */
public record CommentResponse(UUID id, Author author, String body, Instant createdAt, Instant editedAt) {

    public record Author(UUID id, String name) {
    }

    public static CommentResponse from(Comment comment) {
        return new CommentResponse(
                comment.getId(),
                new Author(comment.getAuthor().getId(), comment.getAuthor().getName()),
                comment.getBody(),
                comment.getCreatedAt(),
                comment.getEditedAt());
    }
}
