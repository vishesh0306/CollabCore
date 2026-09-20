-- Every change to the company's work, written in the same transaction as the change itself.
CREATE TABLE audit_log (
    -- A running number, not a UUID: the log is read newest-first and paged by "older than this
    -- entry", which needs an ordering key. It also keeps inserts at the end of the index.
    id           BIGSERIAL    PRIMARY KEY,
    at           TIMESTAMPTZ  NOT NULL,
    -- Empty when nobody did it: the reminder job, or tasks moved by a sprint completing.
    actor_id     UUID         REFERENCES users (id),
    -- Copied in so a team's activity needs no joins, and survives if the item is deleted.
    team_id      UUID         REFERENCES teams (id),
    entity_type  VARCHAR(20)  NOT NULL,
    entity_id    UUID         NOT NULL,
    -- How the item was called at the time, e.g. "PAY-12".
    entity_label VARCHAR(200) NOT NULL,
    action       VARCHAR(40)  NOT NULL,
    -- [{"field": "status", "oldValue": "TO_DO", "newValue": "IN_PROGRESS"}]
    changes      JSONB        NOT NULL DEFAULT '[]'
);

-- "This team's activity, newest first" and "this item's history".
CREATE INDEX ix_audit_team ON audit_log (team_id, id DESC);
CREATE INDEX ix_audit_entity ON audit_log (entity_type, entity_id, id DESC);
CREATE INDEX ix_audit_actor ON audit_log (actor_id, id DESC);

-- LOG-3: entries can never be edited or deleted, not even by the admin. The application has no
-- code that would try; this makes it impossible anyway, including by hand in psql.
CREATE FUNCTION audit_log_is_append_only() RETURNS TRIGGER AS $$
BEGIN
    RAISE EXCEPTION 'audit_log is append-only: % is not allowed', TG_OP;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER audit_log_no_change
    BEFORE UPDATE OR DELETE ON audit_log
    FOR EACH ROW EXECUTE FUNCTION audit_log_is_append_only();

-- TRUNCATE ignores row triggers, so it needs its own.
CREATE TRIGGER audit_log_no_truncate
    BEFORE TRUNCATE ON audit_log
    FOR EACH STATEMENT EXECUTE FUNCTION audit_log_is_append_only();
