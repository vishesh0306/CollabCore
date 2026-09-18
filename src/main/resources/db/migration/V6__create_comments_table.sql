CREATE TABLE comments (
    id         UUID          PRIMARY KEY,
    task_id    UUID          NOT NULL REFERENCES tasks (id),
    author_id  UUID          NOT NULL REFERENCES users (id),
    body       VARCHAR(5000) NOT NULL,
    created_at TIMESTAMPTZ   NOT NULL,
    updated_at TIMESTAMPTZ   NOT NULL,
    -- Set when the author edits the comment, so it can be shown as "edited".
    edited_at  TIMESTAMPTZ,
    deleted_at TIMESTAMPTZ
);

-- A task's comments are always read together, oldest first.
CREATE INDEX ix_comments_task_id_created_at ON comments (task_id, created_at);
