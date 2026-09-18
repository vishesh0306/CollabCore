package com.collabflow.comment.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Used both to write a comment and to edit it. */
public record CommentRequest(@NotBlank @Size(max = 5000) String body) {

    public CommentRequest {
        body = body == null ? null : body.strip();
    }
}
