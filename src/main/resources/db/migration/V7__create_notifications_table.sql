CREATE TABLE notifications (
    id           UUID          PRIMARY KEY,
    recipient_id UUID          NOT NULL REFERENCES users (id),
    -- Who caused it. Empty for reminders, which nobody triggers.
    actor_id     UUID          REFERENCES users (id),
    type         VARCHAR(40)   NOT NULL,
    -- Written when it happens, so it keeps saying what was true then.
    message      VARCHAR(500)  NOT NULL,
    link         VARCHAR(200),
    created_at   TIMESTAMPTZ   NOT NULL,
    read_at      TIMESTAMPTZ
);

-- "My notifications, newest first".
CREATE INDEX ix_notifications_recipient ON notifications (recipient_id, created_at DESC);

-- The unread count and the unread list only look at unread rows, so the index only holds those.
CREATE INDEX ix_notifications_unread ON notifications (recipient_id) WHERE read_at IS NULL;

-- Which expected date a task was already reminded about, so a reminder is sent only once.
ALTER TABLE tasks ADD COLUMN overdue_reminded_for DATE;
