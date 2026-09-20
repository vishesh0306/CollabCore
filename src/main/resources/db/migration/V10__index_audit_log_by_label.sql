-- A task's history is looked up by the key it was called at the time ("PAY-12"), so that it
-- still answers after the task is deleted. Without this index that read scans the whole log.
CREATE INDEX ix_audit_label ON audit_log (entity_label, id DESC);
