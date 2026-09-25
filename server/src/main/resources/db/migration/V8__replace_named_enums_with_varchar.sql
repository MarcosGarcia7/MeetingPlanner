DROP INDEX ix_account_tokens_user;
DROP INDEX ix_email_outbox_pending;
DROP INDEX ix_friendships_user;
DROP INDEX ix_friendships_friend;

ALTER TABLE account_tokens ALTER COLUMN token_type TYPE VARCHAR(32) USING token_type::text;
ALTER TABLE email_outbox ALTER COLUMN status TYPE VARCHAR(32) USING status::text;
ALTER TABLE group_invites ALTER COLUMN status TYPE VARCHAR(32) USING status::text;
ALTER TABLE meeting_invites ALTER COLUMN status TYPE VARCHAR(32) USING status::text;
ALTER TABLE friendships ALTER COLUMN status TYPE VARCHAR(32) USING status::text;

ALTER TABLE account_tokens ALTER COLUMN token_type DROP DEFAULT;
ALTER TABLE email_outbox ALTER COLUMN status SET DEFAULT 'PENDING';
ALTER TABLE group_invites ALTER COLUMN status SET DEFAULT 'PENDING';
ALTER TABLE meeting_invites ALTER COLUMN status SET DEFAULT 'PENDING';
ALTER TABLE friendships ALTER COLUMN status SET DEFAULT 'PENDING';

DROP TYPE IF EXISTS account_token_type;
DROP TYPE IF EXISTS email_status;
DROP TYPE IF EXISTS invite_status;
DROP TYPE IF EXISTS friendship_status;

CREATE INDEX ix_account_tokens_user ON account_tokens(user_id, token_type);
CREATE INDEX ix_email_outbox_pending ON email_outbox(status, next_attempt_at) WHERE status = 'PENDING';
CREATE INDEX ix_friendships_user ON friendships(user_id, status);
CREATE INDEX ix_friendships_friend ON friendships(friend_id, status);