-- The reminder job runs every 15 minutes and asks for tasks past their expected date. Without
-- an index that is a full scan of every task in the company, forever, on a timer.
-- Partial, because the job only ever looks at tasks that are alive, dated and unfinished, so
-- the index stays small: finished work leaves it instead of sitting in it.
CREATE INDEX ix_tasks_overdue ON tasks (expected_date)
    WHERE deleted_at IS NULL AND expected_date IS NOT NULL AND status <> 'DONE';

-- LOG-5 lets the admin filter the log by date. Filtering by team, project, item or person all
-- have an index already; the date range was the one that had none.
CREATE INDEX ix_audit_at ON audit_log (at DESC);
