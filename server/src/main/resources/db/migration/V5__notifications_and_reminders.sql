CREATE TABLE notifications (
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    type VARCHAR(64) NOT NULL,
    payload JSONB NOT NULL DEFAULT '{}',
    read_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX ix_notifications_user ON notifications(user_id, created_at DESC);

CREATE TABLE notification_preferences (
    user_id BIGINT PRIMARY KEY REFERENCES users(id) ON DELETE CASCADE,
    email_enabled BOOLEAN NOT NULL DEFAULT TRUE,
    group_invites BOOLEAN NOT NULL DEFAULT TRUE,
    meeting_invites BOOLEAN NOT NULL DEFAULT TRUE,
    meeting_reminders BOOLEAN NOT NULL DEFAULT TRUE,
    friend_requests BOOLEAN NOT NULL DEFAULT TRUE,
    post_comments BOOLEAN NOT NULL DEFAULT TRUE,
    group_posts BOOLEAN NOT NULL DEFAULT TRUE,
    reminder_lead_minutes INT NOT NULL DEFAULT 30
);

CREATE TABLE meeting_reminders_sent (
    meeting_id BIGINT NOT NULL REFERENCES meetings(id) ON DELETE CASCADE,
    user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    reminded_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    PRIMARY KEY (meeting_id, user_id)
);