CREATE TABLE projects (
    id          UUID          PRIMARY KEY,
    team_id     UUID          NOT NULL REFERENCES teams (id),
    code        VARCHAR(10)   NOT NULL,
    name        VARCHAR(100)  NOT NULL,
    description VARCHAR(2000),
    lead_id     UUID          NOT NULL REFERENCES users (id),
    status      VARCHAR(20)   NOT NULL CHECK (status IN ('ACTIVE', 'COMPLETED')),
    created_at  TIMESTAMPTZ   NOT NULL,
    updated_at  TIMESTAMPTZ   NOT NULL,
    -- The code becomes part of every task key ("PAY-12"), so it is unique company-wide
    -- and always 2-10 capital letters or digits, starting with a letter.
    CONSTRAINT uq_projects_code UNIQUE (code),
    CONSTRAINT ck_projects_code_format CHECK (code ~ '^[A-Z][A-Z0-9]{1,9}$')
);

-- PostgreSQL doesn't index foreign keys automatically. Projects are listed per team,
-- so team_id gets an index; lead_id is never searched by, so it doesn't need one.
CREATE INDEX ix_projects_team_id ON projects (team_id);
