CREATE TABLE teams (
    id          UUID          PRIMARY KEY,
    name        VARCHAR(100)  NOT NULL,
    description VARCHAR(1000),
    created_at  TIMESTAMPTZ   NOT NULL,
    updated_at  TIMESTAMPTZ   NOT NULL
);

-- Team names are unique ignoring case: "Alpha" and "alpha" are the same team.
CREATE UNIQUE INDEX uq_teams_name ON teams (lower(name));

CREATE TABLE team_members (
    id        UUID        PRIMARY KEY,
    team_id   UUID        NOT NULL REFERENCES teams (id),
    user_id   UUID        NOT NULL REFERENCES users (id),
    role      VARCHAR(20) NOT NULL CHECK (role IN ('MANAGER', 'MEMBER')),
    joined_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT uq_team_members_team_user UNIQUE (team_id, user_id)
);

-- Speeds up "which teams is this user in?". Lookups by team_id already use the
-- index behind the unique constraint above, because team_id is its first column.
CREATE INDEX ix_team_members_user_id ON team_members (user_id);
