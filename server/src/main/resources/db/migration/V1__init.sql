CREATE EXTENSION IF NOT EXISTS "pgcrypto";

CREATE EXTENSION IF NOT EXISTS "citext";

CREATE TABLE users (
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    username VARCHAR(20) NOT NULL UNIQUE,
    email CITEXT NOT NULL UNIQUE,
    password_hash VARCHAR(60) NOT NULL,
    first_name VARCHAR(50),
    last_name VARCHAR(50),
    avatar_color VARCHAR(7) NOT NULL,
    is_private BOOLEAN NOT NULL DEFAULT FALSE,
    friends_can_view_private BOOLEAN NOT NULL DEFAULT FALSE,
    email_verified_at TIMESTAMPTZ,
    pending_email CITEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT chk_users_username_format CHECK (username ~ '^[A-Za-z0-9_-]{1,20}$')
);

CREATE UNIQUE INDEX ux_users_pending_email ON users (pending_email) WHERE pending_email IS NOT NULL;

CREATE TABLE user_availability (
    user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    sunday INT[][] NOT NULL DEFAULT '{}',
    monday INT[][] NOT NULL DEFAULT '{}',
    tuesday INT[][] NOT NULL DEFAULT '{}',
    wednesday INT[][] NOT NULL DEFAULT '{}',
    thursday INT[][] NOT NULL DEFAULT '{}',
    friday INT[][] NOT NULL DEFAULT '{}',
    saturday INT[][] NOT NULL DEFAULT '{}',
    availability_set BOOLEAN NOT NULL DEFAULT FALSE
);

CREATE TABLE groups (
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    group_name VARCHAR(50) NOT NULL UNIQUE,
    owner_id BIGINT REFERENCES users(id) ON DELETE CASCADE,
    is_private BOOLEAN NOT NULL DEFAULT TRUE,
    members_can_post BOOLEAN NOT NULL DEFAULT TRUE,
    comments_enabled BOOLEAN NOT NULL DEFAULT TRUE
);

CREATE TABLE group_members (
    group_id BIGINT NOT NULL REFERENCES groups(id) ON DELETE CASCADE,
    user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    is_admin BOOLEAN NOT NULL DEFAULT FALSE,
    share_schedule BOOLEAN NOT NULL DEFAULT TRUE,
    PRIMARY KEY (group_id, user_id)
);

CREATE TABLE group_invites (
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    sender_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    group_id BIGINT NOT NULL REFERENCES groups(id) ON DELETE CASCADE,
    recipient_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    recipient_email CITEXT NOT NULL,
    token_hash CHAR(64) NOT NULL,
    responded_at TIMESTAMPTZ,
    time_sent TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    status VARCHAR(32) NOT NULL DEFAULT 'PENDING'
);

CREATE UNIQUE INDEX ux_group_invites_email ON group_invites(group_id, recipient_email);

CREATE TABLE meetings (
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    group_id BIGINT REFERENCES groups(id) ON DELETE CASCADE,
    start_time TIMESTAMPTZ NOT NULL,
    end_time TIMESTAMPTZ,
    location VARCHAR(500),
    description VARCHAR(500),
    external_participant_names VARCHAR(255)[] DEFAULT '{}',
    creator_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    is_private BOOLEAN NOT NULL DEFAULT FALSE
);

CREATE TABLE meeting_participants (
    meeting_id BIGINT NOT NULL REFERENCES meetings(id) ON DELETE CASCADE,
    user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    PRIMARY KEY (meeting_id, user_id)
);

CREATE INDEX ix_meetings_start ON meetings(start_time DESC, id DESC);
CREATE INDEX ix_meetings_group_start ON meetings(group_id, start_time DESC, id DESC);
CREATE INDEX ix_meetings_creator_start ON meetings(creator_id, start_time DESC, id DESC);
CREATE INDEX ix_meeting_participants_user ON meeting_participants(user_id, meeting_id);

CREATE TABLE meeting_invites (
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    sender_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    meeting_id BIGINT NOT NULL REFERENCES meetings(id) ON DELETE CASCADE,
    recipient_id BIGINT REFERENCES users(id) ON DELETE CASCADE,
    recipient_email CITEXT NOT NULL,
    token_hash CHAR(64) NOT NULL,
    responded_at TIMESTAMPTZ,
    status VARCHAR(32) NOT NULL DEFAULT 'PENDING'
);

CREATE UNIQUE INDEX ux_meeting_invites_email ON meeting_invites(meeting_id, recipient_email);

CREATE TABLE account_tokens (
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    token_hash CHAR(64) NOT NULL UNIQUE,
    token_type VARCHAR(32) NOT NULL,
    email CITEXT,
    expires_at TIMESTAMPTZ NOT NULL,
    consumed_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    requested_ip VARCHAR(45)
);

CREATE INDEX ix_account_tokens_user ON account_tokens(user_id, token_type);
CREATE INDEX ix_account_tokens_expiry ON account_tokens(expires_at);

CREATE TABLE email_outbox (
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    recipient_user_id BIGINT REFERENCES users(id) ON DELETE SET NULL,
    recipient_email CITEXT NOT NULL,
    template VARCHAR(64) NOT NULL,
    payload JSONB NOT NULL DEFAULT '{}',
    status VARCHAR(32) NOT NULL DEFAULT 'PENDING',
    attempts INT NOT NULL DEFAULT 0,
    next_attempt_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    last_error TEXT,
    provider_message_id VARCHAR(255),
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    sent_at TIMESTAMPTZ
);

CREATE INDEX ix_email_outbox_pending ON email_outbox(status, next_attempt_at) WHERE status = 'PENDING';

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

CREATE TABLE group_posts (
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    group_id BIGINT NOT NULL REFERENCES groups(id) ON DELETE CASCADE,
    author_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    body TEXT NOT NULL,
    comments_enabled BOOLEAN,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX ix_group_posts_group ON group_posts(group_id, created_at DESC);

CREATE TABLE group_post_comments (
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    post_id BIGINT NOT NULL REFERENCES group_posts(id) ON DELETE CASCADE,
    author_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    body TEXT NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX ix_group_post_comments_post ON group_post_comments(post_id, created_at);

CREATE TABLE friendships (
    user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    friend_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    requester_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    status VARCHAR(32) NOT NULL DEFAULT 'PENDING',
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    responded_at TIMESTAMPTZ,
    PRIMARY KEY (user_id, friend_id),
    CHECK (user_id <> friend_id)
);

CREATE INDEX ix_friendships_user ON friendships(user_id, status);
CREATE INDEX ix_friendships_friend ON friendships(friend_id, status);
