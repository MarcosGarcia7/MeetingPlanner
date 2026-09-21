CREATE TABLE users (
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    username VARCHAR(20) NOT NULL UNIQUE,
    email CITEXT NOT NULL UNIQUE,
    password_hash VARCHAR(60) NOT NULL,
    first_name VARCHAR(50),
    last_name VARCHAR(50),
    is_private BOOLEAN NOT NULL DEFAULT FALSE,
    friends_can_view_private BOOLEAN NOT NULL DEFAULT FALSE,
    email_verified_at TIMESTAMPTZ,
    pending_email CITEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT chk_users_username_format CHECK (username ~ '^[A-Za-z0-9_-]{1,20}$')
);

CREATE UNIQUE INDEX ux_users_pending_email ON users (pending_email) WHERE pending_email IS NOT NULL;

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
    recipient_id BIGINT REFERENCES users(id) ON DELETE CASCADE,
    recipient_email CITEXT,
    token_hash CHAR(64),
    responded_at TIMESTAMPTZ,
    time_sent TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    status invite_status NOT NULL DEFAULT 'PENDING',
    CONSTRAINT unique_group_recipient_invite UNIQUE (group_id, recipient_id),
    CONSTRAINT group_invite_recipient_present
        CHECK (recipient_id IS NOT NULL OR recipient_email IS NOT NULL)
);

CREATE UNIQUE INDEX ux_group_invites_email
    ON group_invites(group_id, recipient_email) WHERE recipient_email IS NOT NULL;