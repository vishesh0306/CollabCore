-- Each project hands out task numbers 1, 2, 3... Task keys are the project code plus
-- this number ("PAY-12").
ALTER TABLE projects ADD COLUMN last_task_number INTEGER NOT NULL DEFAULT 0;

CREATE TABLE tasks (
    id            UUID          PRIMARY KEY,
    project_id    UUID          NOT NULL REFERENCES projects (id),
    -- Copied from the project (projects never move between teams), so listing a
    -- team's tasks needs no join.
    team_id       UUID          NOT NULL REFERENCES teams (id),
    number        INTEGER       NOT NULL,
    title         VARCHAR(200)  NOT NULL,
    description   VARCHAR(10000),
    status        VARCHAR(20)   NOT NULL
                  CHECK (status IN ('TO_DO', 'IN_PROGRESS', 'IN_REVIEW', 'BLOCKED', 'DONE')),
    expected_date DATE,
    completed_at  TIMESTAMPTZ,
    created_by    UUID          NOT NULL REFERENCES users (id),
    created_at    TIMESTAMPTZ   NOT NULL,
    updated_at    TIMESTAMPTZ   NOT NULL,
    -- Soft delete: a deleted task keeps its row (and its number) but is hidden everywhere.
    deleted_at    TIMESTAMPTZ,
    CONSTRAINT uq_tasks_project_number UNIQUE (project_id, number)
);

CREATE INDEX ix_tasks_team_id ON tasks (team_id);

CREATE TABLE task_assignees (
    task_id UUID NOT NULL REFERENCES tasks (id),
    user_id UUID NOT NULL REFERENCES users (id),
    PRIMARY KEY (task_id, user_id)
);

-- For "tasks assigned to this person" filters and for unassigning someone who left a team.
CREATE INDEX ix_task_assignees_user_id ON task_assignees (user_id);
