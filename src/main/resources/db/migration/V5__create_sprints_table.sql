CREATE TABLE sprints (
    id           UUID          PRIMARY KEY,
    team_id      UUID          NOT NULL REFERENCES teams (id),
    name         VARCHAR(100)  NOT NULL,
    target       VARCHAR(2000),
    start_date   DATE          NOT NULL,
    end_date     DATE          NOT NULL,
    status       VARCHAR(20)   NOT NULL CHECK (status IN ('PLANNED', 'ACTIVE', 'COMPLETED')),
    started_at   TIMESTAMPTZ,
    completed_at TIMESTAMPTZ,
    created_at   TIMESTAMPTZ   NOT NULL,
    updated_at   TIMESTAMPTZ   NOT NULL,
    CONSTRAINT ck_sprints_dates CHECK (end_date >= start_date)
);

CREATE INDEX ix_sprints_team_id ON sprints (team_id);

-- At most one ACTIVE sprint per team. The index only contains rows whose status is ACTIVE,
-- so a second active sprint for the same team would be a duplicate and is refused, even if
-- two managers press "start" at the same moment.
CREATE UNIQUE INDEX uq_sprints_one_active_per_team ON sprints (team_id) WHERE status = 'ACTIVE';

-- A task is in at most one sprint at a time. No sprint (NULL) means it is in its project's backlog.
ALTER TABLE tasks ADD COLUMN sprint_id UUID REFERENCES sprints (id);

CREATE INDEX ix_tasks_sprint_id ON tasks (sprint_id);
