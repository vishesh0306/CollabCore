-- A project's activity is its own entries plus those of its tasks and their comments. Copying the
-- project onto the row turns that into one indexed read instead of a join against tasks.
-- Safe to denormalise here: audit entries are written once and never updated.
ALTER TABLE audit_log ADD COLUMN project_id UUID REFERENCES projects (id);

CREATE INDEX ix_audit_project ON audit_log (project_id, id DESC);
