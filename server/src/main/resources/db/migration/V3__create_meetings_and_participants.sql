CREATE TABLE meetings (
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    group_id BIGINT REFERENCES groups(id) ON DELETE CASCADE,
    start_time TIMESTAMPTZ NOT NULL,
    end_time TIMESTAMPTZ,
    location VARCHAR(500),
    description VARCHAR(500),
    external_participant_names VARCHAR(50)[] DEFAULT '{}',
    creator_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    is_private BOOLEAN NOT NULL DEFAULT FALSE
);

CREATE TABLE meeting_participants (
    meeting_id BIGINT NOT NULL REFERENCES meetings(id) ON DELETE CASCADE,
    user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    PRIMARY KEY (meeting_id, user_id)
);

CREATE TABLE meeting_invites (
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    sender_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    meeting_id BIGINT NOT NULL REFERENCES meetings(id) ON DELETE CASCADE,
    recipient_id BIGINT REFERENCES users(id) ON DELETE CASCADE,
    recipient_email CITEXT,
    token_hash CHAR(64),
    responded_at TIMESTAMPTZ,
    status invite_status NOT NULL DEFAULT 'PENDING',
    CONSTRAINT unique_meeting_recipient_invite UNIQUE (meeting_id, recipient_id),
    CONSTRAINT meeting_invite_recipient_present
        CHECK (recipient_id IS NOT NULL OR recipient_email IS NOT NULL)
);

CREATE UNIQUE INDEX ux_meeting_invites_email
    ON meeting_invites(meeting_id, recipient_email) WHERE recipient_email IS NOT NULL;