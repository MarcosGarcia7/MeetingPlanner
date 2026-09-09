CREATE TABLE IF NOT EXISTS meetings (
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    group_id BIGINT REFERENCES groups(id) ON DELETE CASCADE,
    start_time TIMESTAMPTZ NOT NULL,
    end_time TIMESTAMPTZ,
    location VARCHAR(500),
    description VARCHAR(500),
    external_participant_names VARCHAR(50)[] DEFAULT '{}', 
    creator_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    is_private BOOLEAN DEFAULT FALSE NOT NULL
);

CREATE TABLE IF NOT EXISTS meeting_participants (
    meeting_id BIGINT REFERENCES meetings(id) ON DELETE CASCADE,
    user_id BIGINT REFERENCES users(id) ON DELETE CASCADE,
    PRIMARY KEY (meeting_id, user_id)
);

CREATE TABLE IF NOT EXISTS meeting_invites (
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    sender_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    meeting_id BIGINT NOT NULL REFERENCES meetings(id) ON DELETE CASCADE,
    recipient_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    status invite_status DEFAULT 'PENDING' NOT NULL,
    CONSTRAINT unique_meeting_recipient_invite UNIQUE (meeting_id, recipient_id)
);
