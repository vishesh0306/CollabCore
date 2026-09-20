-- A sprint is a tag on a task, and a task can carry several at once: it then shows up in every
-- one of those sprints. Replaces the single tasks.sprint_id, which could only ever say
-- "the sprint this task is in right now".
CREATE TABLE task_sprints (
    task_id   UUID NOT NULL REFERENCES tasks (id),
    sprint_id UUID NOT NULL REFERENCES sprints (id),
    PRIMARY KEY (task_id, sprint_id)
);

-- The primary key already covers "which sprints is this task in"; this covers the other
-- direction, "which tasks are in this sprint", which is the sprint page.
CREATE INDEX ix_task_sprints_sprint ON task_sprints (sprint_id);

-- Keep what the old column knew.
INSERT INTO task_sprints (task_id, sprint_id)
SELECT id, sprint_id FROM tasks WHERE sprint_id IS NOT NULL;

DROP INDEX ix_tasks_sprint_id;
ALTER TABLE tasks DROP COLUMN sprint_id;
