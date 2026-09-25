CREATE TYPE account_token_type AS ENUM ('EMAIL_VERIFY', 'PASSWORD_RESET', 'EMAIL_CHANGE', 'MAGIC_LINK');

CREATE TYPE email_status AS ENUM ('PENDING', 'SENT', 'FAILED');

CREATE TABLE account_tokens (
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    token_hash CHAR(64) NOT NULL UNIQUE,
    token_type account_token_type NOT NULL,
    email CITEXT,
    expires_at TIMESTAMPTZ NOT NULL,
    consumed_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    requested_ip INET
);

CREATE INDEX ix_account_tokens_user ON account_tokens(user_id, token_type);
CREATE INDEX ix_account_tokens_expiry ON account_tokens(expires_at);

CREATE TABLE email_outbox (
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    recipient_user_id BIGINT REFERENCES users(id) ON DELETE SET NULL,
    recipient_email CITEXT NOT NULL,
    template VARCHAR(64) NOT NULL,
    payload JSONB NOT NULL DEFAULT '{}',
    status email_status NOT NULL DEFAULT 'PENDING',
    attempts INT NOT NULL DEFAULT 0,
    next_attempt_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    last_error TEXT,
    provider_message_id VARCHAR(255),
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    sent_at TIMESTAMPTZ
);

CREATE INDEX ix_email_outbox_pending ON email_outbox(status, next_attempt_at) WHERE status = 'PENDING';